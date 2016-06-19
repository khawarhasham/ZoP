package fi.aalto.legroup.zop.browsing;

import android.accounts.Account;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import fi.aalto.legroup.zop.R;
import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.entities.Comment;
import fi.aalto.legroup.zop.entities.CommentReply;
import fi.aalto.legroup.zop.entities.Like;
import fi.aalto.legroup.zop.entities.Video;
import fi.aalto.legroup.zop.playback.PlayerActivity;
import fi.aalto.legroup.zop.sss.CommentAction;
import fi.aalto.legroup.zop.sss.DiscussionActionType;
import fi.aalto.legroup.zop.views.adapters.DiscussionAdapter;
import fi.aalto.legroup.zop.sss.SSSObject;
//import fi.aalto.legroup.zop.views.adapters.SSSRequestWrapper;

/**
 * Created by khawar on 27/05/2016.
 */
public class DiscussionView implements SSSObject{

    private ArrayList<Comment> comments;
    DiscussionAdapter adapter;
    private String SSS_endpoint = "http://test-ll.know-center.tugraz.at/test/rest"; //TODO: change it with config param
    private String discID;
    public final static int COMMENT_LIMIT = 140;
    EditText commentTextBox;

    public DiscussionView(){
        comments = new ArrayList<Comment>();
    }

    public ArrayList<Comment> getComments() {
        return comments;
    }

    public void setComments(ArrayList<Comment> comments) {
        this.comments = comments;
    }

    public void addComment(Comment comment){
        this.comments.add(comment);
    }

    Request.Builder buildVideoDiscRequest(String videoUUID) {
        String disc_endpoint = SSS_endpoint + "/discs/filtered/targets";
        Uri uri = Uri.parse(disc_endpoint);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try{
            json = json.put("setComments", true)
                    .put("setLikes", true);
            RequestBody body = RequestBody.create(JSON, json.toString());

            return new Request.Builder()
                    .url(uri.buildUpon()
                            .appendPath(videoUUID)
                            .toString())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .post(body);

        }catch(JSONException jsonexp){
            System.out.println("ZoP: " +jsonexp.getMessage());

        }

        return null;
    }
/*
    {
       "op" : "discEntryAdd",
            "disc" : "http://sss.eu/19308674334117686242"
        }

        19309155635979313244
        */
    public String processCreateDiscResponse(String discResp){
        /*** its result format
         {
         "op" : "discEntryAdd",
         "disc" : "http://sss.eu/19137789200995678216"
         }
         **/
        String result = "#";
        //System.out.println("create Disc resp: " + discResp);
        try {
            JSONObject json = new JSONObject(discResp);
            //System.out.println("Check if json has disc: " + json.has("disc"));
            if (json.has("disc")) {
                String disc = json.getString("disc");
                String discID = disc.substring(disc.lastIndexOf("/") + 1);
                System.out.println("Found newly created DiscID " + discID);
                return discID;
            }
            System.out.println("No disc ID is found.");
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return result;
    }

    public Response shareDiscussion (String discID) throws IOException {
        /**
         * Valid response json
         */
        Uri uri = Uri.parse(DiscussionAdapter.SSS_endpoint + "/entities");
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {

            json = json.put("setPublic", true);

            RequestBody body = RequestBody.create(JSON, json.toString());

            Request.Builder req = new Request.Builder()
                    .url(uri.buildUpon()
                            .appendPath(discID)
                            .appendPath("share")
                            .toString())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .put(body);

            return executeRequest(req.build());
        }catch(JSONException jsonexp){
            jsonexp.printStackTrace();
        }
        return null;
    }

    public Response createDiscussion(String videoUUID, String videoTitle) throws IOException{
        Uri uri = Uri.parse( DiscussionAdapter.SSS_endpoint + "/discs");
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {

            List<String> targets = new ArrayList<String>();
            targets.add(videoUUID);
            JSONArray jsonTargets = new JSONArray(targets);

            json = json.put("label", videoTitle)
                    .put("description", videoTitle)
                    .put("addNewDisc", true)
                    .put("type", "qa")
                    .put("targets", jsonTargets);

            System.out.println("Creating disc for: " + json.toString());
            RequestBody body = RequestBody.create(JSON, json.toString());

            Request.Builder req = new Request.Builder()
                    .url(uri.toString())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .post(body);

            return executeRequest(req.build());
        }catch(JSONException jsonexp){
            jsonexp.printStackTrace();
        }
        return null;
    }

    private Response executeRequestNoFail(Request request) throws IOException {
        Account account = App.loginManager.getAccount();
        return App.authenticatedHttpClient.execute(request, account);
    }

    private Response validateResponse(Response response) throws IOException {
        if (!response.isSuccessful()) {
            String errorMessage = response.code() + " " + response.message();
            throw new IOException(errorMessage);
        }
        return response;
    }

    Response executeRequest(Request request) throws IOException {

        return validateResponse(executeRequestNoFail(request));
    }


    /*SSSRequestWrapper
    *     RequestType => action
    *     RequestObject(s) => parameters of type Object
    *     Comment is Object
    *     String is Object (type cast the passed params)
    *
    * */

    private class LoadDiscussion extends AsyncTask<String, Void, ArrayList<Comment>> {

        ProgressDialog progressDialog;
        Activity activity;
        DiscussionActionType.Type type;

        public LoadDiscussion(Activity activity, DiscussionActionType.Type t){
            this.activity = activity;
            this.type = t;
        }
        @Override
        protected void onPostExecute(ArrayList<Comment> result) {
            //stop refreshing animation
            progressDialog.dismiss();

            comments.addAll(result);
            System.out.println("post exec: " + comments.size());
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected ArrayList<Comment> doInBackground(String... params) {
            ArrayList<Comment> result = new ArrayList<Comment>();

            String videoUUID = params[0];
            System.out.println("Creating discussion for video " + videoUUID);
            Request request = buildVideoDiscRequest(videoUUID).build();
            System.out.println("Reqesting : " + request.urlString());
            try {
                Response response = executeRequest(request);
                if ( response.isSuccessful() ) {
                    result = parseDiscussion(response.body().string());
                    //if result is null, this means there is no discussion found in parsing the response
                    //so create a discussion for this video
                    if ( result == null){
                        System.out.println("No discID found. so going to create one");
                        String videoTitle = params[1];
                        Response discResp = createDiscussion(videoUUID, videoTitle);

                        if (discResp != null && discResp.isSuccessful()) {
                            //process the return json
                            String newDiscID = processCreateDiscResponse(discResp.body().string());
                            if (newDiscID.equals("#")) {
                                System.out.println("Something wrong in creating Discussion for Vidid " + videoUUID);
                            }else{
                                discID = newDiscID;
                                System.out.println("Setting DiscID with correct value " + discID);
                            }
                            //now make this discussion public so that users can add comments
                            Response shareResp = shareDiscussion(discID);
                            if (shareResp != null && shareResp.isSuccessful()) {
                                System.out.println("Disc "+ discID + " has been shared as public.");
                            }else{
                                System.out.println("Problem in sharing the disc " + discID);
                            }
                        }

                        result = new ArrayList<Comment>();
                    }
                    System.out.println("zop comments: " + result.size());
                }
            }catch(IOException ioe){
                ioe.printStackTrace();
                System.out.println("ZoP Error: " + ioe.getMessage());
            }catch(Exception e){
                e.printStackTrace();
                System.out.println("ZoP Error: " + e.getMessage());
            }
            return result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(this.activity, "Discussion", "Loading video discussion", true);
        }
    }

    //replace parser with jsonSerializer
    private ArrayList<Comment> parseDiscussion(String discStr)throws Exception{
        //System.out.println("discussion response: " + discStr);
        ArrayList<Comment> comments = new ArrayList();
        if ( discStr != null ){
            JSONObject json = new JSONObject(discStr);
            JSONArray discs = json.getJSONArray("discs");
            if ( discs.length() < 1){
                //add discussion here and get the disc ID

                //return comments;
                return null;
            }
            JSONObject disc = discs.getJSONObject(0);
            if (disc != null && disc.has("id")){
                discID = disc.getString("id");
            }

            JSONArray entries = disc.getJSONArray("entries");
            for(int i = 0; i < entries.length(); i++) {

                JSONObject entry = entries.getJSONObject(i);
                JSONObject author = entry.getJSONObject("author");

                //create a new comment object
                Comment c = new Comment();
                c.setCommentEntity(entry.getString("id"));
                c.setDiscID(discID);

                c.setAuthor(author.getString("label"));
                c.setContent(entry.getString("content"));
                c.setDate(entry.getLong("creationTime"));

                //get likes
                Like like = new Like();
                JSONObject likejson = entry.getJSONObject("likes");
                like.setLikes(likejson.getInt("likes"));
                like.setDislikes(likejson.getInt("dislikes"));
                like.setLike((likejson.has("like") ? likejson.getInt("like") : -1));

                c.setLike(like);

                //System.out.println(like.toString());

                //get replies
                //15/06/2016: it is updated to commentObjs
                JSONArray replyJson = entry.getJSONArray("commentObjs");
                //List<String> replies = new ArrayList<String>();
                List<CommentReply> replies = new ArrayList<CommentReply>();
                for(int j = 0; j < replyJson.length(); j++){
                    //System.out.println("reply " + j + "=>" + replyJson.getString(j));
                    CommentReply commentReply = new CommentReply();

                    JSONObject reply = replyJson.getJSONObject(j);
                    JSONObject replyAuthor = reply.getJSONObject("author");
                    String authorName = replyAuthor.getString("label");

                    commentReply.setAuthor(authorName);
                    commentReply.setDate(reply.getLong("creationTime"));
                    commentReply.setContent(reply.getString("comment"));

                    //replies.add(replyJson.getString(j));
                    replies.add(commentReply);
                }
                c.setReplies(replies);

                //add comment to collection
                comments.add(c);
            }
        }
        return comments;
    }


    //getting network on mainthread exception
    public void createDiscussionView(final Fragment context, final Video vid) {
        try {
            String videoUUID = vid.getId().toString();
            String videoTitle = vid.getTitle();
            new LoadDiscussion(context.getActivity(),
                    DiscussionActionType.Type.DiscLoad).execute(videoUUID, videoTitle); //asynchronously loading discussion

            System.out.println("After AsyncTask call");
            //System.out.println(App.loginManager.getUserInfo().toString());

            LayoutInflater inflater = (LayoutInflater)context.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View inflatedView = inflater.inflate(R.layout.discussion_view, null, false);
            final ListView listView = (ListView)inflatedView.findViewById(R.id.commentsListView);

            adapter  = new DiscussionAdapter(context.getActivity(), R.layout.comment_view, comments);
            listView.setAdapter(adapter);


            commentTextBox = (EditText) inflatedView.findViewById(R.id.writeComment);
            final TextView commentLength = (TextView)inflatedView.findViewById(R.id.commentLength);
            adapter.setDiscCommentTextBox(commentTextBox);

            //attach keypressed watcher
            final TextWatcher mTextEditorWatcher = new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    //This sets a textview to the current length
                    //commentLength.setText(String.valueOf(COMMENT_LIMIT - s.length())+"/"+COMMENT_LIMIT);
                    commentLength.setText(String.valueOf(COMMENT_LIMIT - s.length()));

                    //System.out.println("New length: " + String.valueOf(COMMENT_LIMIT - s.length())+"/"+COMMENT_LIMIT);
                }

                public void afterTextChanged(Editable s) {
                }
            };

            //attach keypressed events for editbox
            commentTextBox.addTextChangedListener(mTextEditorWatcher);

            //attach action "Done" for adding comment in discussion
            commentTextBox.setOnEditorActionListener(new EditText.OnEditorActionListener(){

                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if ( actionId == EditorInfo.IME_ACTION_DONE){

                        String discComment = commentTextBox.getText().toString();
                        if ( Strings.isNullOrEmpty(discComment) &&
                                (discComment.length() < 1 || discComment.length() > COMMENT_LIMIT)){
                            Toast.makeText(context.getActivity(), "Comment can not be empty or above "+COMMENT_LIMIT +" characters", Toast.LENGTH_LONG).show();
                            return true;
                        }

                        //check if have the discID for this video
                        //if there is an ID, add this as a new comment.
                        System.out.println("Disc Comment => " + discComment);
                        Comment newComment = new Comment();
                        newComment.setContent(discComment);
                        Like like = new Like();
                        newComment.setLike(like);
                        //newComment.setReplies(new ArrayList<String>());
                        newComment.setReplies(new ArrayList<CommentReply>());
                        newComment.setDate(new Date().getTime()); //setting timestamp
                        newComment.setAuthor(App.loginManager.getUserInfo().get("email").getAsString()); // also author info

                        if ( !Strings.isNullOrEmpty(discID) ) {

                            newComment.setDiscID(discID);

                            //TODO: how to set this new comment entity
                            //Entity might come from the response. keep an eye there.
                            new CommentAction(DiscussionActionType.Type.DiscReply, adapter).execute(newComment);

                            //reset the edit field
                            commentTextBox.setText("");

                            //add this comment to array
                            comments.add(newComment);
                            adapter.notifyDataSetChanged();

                        }/*else{
                            //if there is no ID, means no discussion exists. So create the discussion with this new comment
                            new SSSRequestWrapper(SSSEventType.Type.NewDisc,
                                    DiscussionView.this).execute(vid.getId().toString(),
                                                                 vid.getTitle(), newComment);
                        }*/
                        return true;
                    }
                    return false;
                }
            });

            TextView popular = (TextView)inflatedView.findViewById(R.id.popularBtn);
            TextView recent = (TextView)inflatedView.findViewById(R.id.recentBtn);

            popular.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    //System.out.println("DiscussionView popular clicked");
                    Collections.sort(comments, new Comparator<Comment>() {
                        @Override
                        public int compare(Comment lhs, Comment rhs) {
                            return rhs.getLike().getLikes() - lhs.getLike().getLikes();
                        }
                    });
                    adapter.notifyDataSetChanged();
                }
            });

            recent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //System.out.println("DiscussionView recent clicked");
                    Collections.sort(comments, new Comparator<Comment>() {
                        @Override
                        public int compare(Comment lhs, Comment rhs) {
                            return (int)(rhs.getDate() - lhs.getDate());
                        }
                    });
                    adapter.notifyDataSetChanged();
                }
            });

            //create popwindow
            System.out.println("now creating popupwindow");

            //set display keyboard for this fragment

            Display display = context.getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            //System.out.println("Display size:" + size.toString());

            PopupWindow popWindow = new PopupWindow(inflatedView, size.x - 50,size.y - 300, true );
            popWindow.setBackgroundDrawable(context.getActivity().getResources().getDrawable(R.drawable.discussion_bg));
            popWindow.setFocusable(true);
            popWindow.setOutsideTouchable(true);
            popWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            popWindow.showAtLocation((View)context.getActivity().findViewById(R.id.playPauseButton), Gravity.BOTTOM, 0, 150);
            //popWindow.showAtLocation(context.getActivity().getCurrentFocus(), Gravity.BOTTOM, 0, 150);

            //KH: attach popupWindow.onDismissListener
            popWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    //unpause the player if it was paused earlier

                    PlayerActivity playerActivity = (PlayerActivity)context.getActivity();
                    playerActivity.togglePlayback();
                }
            });
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public void setOutput(Object output){

        this.comments.addAll((ArrayList<Comment>)output);
        //this.discID = (String)output;
    }
}
