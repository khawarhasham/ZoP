package fi.aalto.legroup.zop.sss;

import android.accounts.Account;
import android.net.Uri;
import android.os.AsyncTask;
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
import java.util.List;

import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.entities.Comment;
import fi.aalto.legroup.zop.entities.CommentReply;
import fi.aalto.legroup.zop.views.adapters.DiscussionAdapter;

/**
 * Created by khawar on 31/05/2016.
 */
public class CommentAction extends AsyncTask<Comment, Void, String> {
    DiscussionActionType.Type type;
    DiscussionAdapter parent;

    public CommentAction(DiscussionActionType.Type t, DiscussionAdapter p){
        this.type = t;
        this.parent = p;
    }

    @Override
    protected String doInBackground(Comment... params) {
        String status = "success";
        try{
            switch(this.type){
                case CommentLike:
                    Response likeResp = likeComment(params[0]);
                    if (likeResp.isSuccessful()) {
                        System.out.println("like: " + likeResp.message());
                        params[0].getLike().increaseLikes();

                    }
                    break;
                case CommentReply:
                    Response replyResp = replyComment(params[0], params[1]);

                    if ( replyResp != null && replyResp.isSuccessful() ){
                        CommentReply cmntReply = new CommentReply();
                        cmntReply.setContent(params[1].getContent());
                        cmntReply.setDate(params[1].getDate());
                        cmntReply.setAuthor(params[1].getAuthor());
                        System.out.println("Comment Reply: " + cmntReply.toString());
                        //params[0].getReplies().add(params[1].getContent());
                        params[0].getReplies().add(cmntReply);
                    }else{
                        status = "fail";
                    }
                    if ( replyResp != null){
                        System.out.println(replyResp.message());
                    }
                    break;
                case DiscReply:
                    Comment newComment = (Comment)params[0];
                    Response discResp = CommentAction.discReplyComment(newComment);
                    if ( discResp != null && discResp.isSuccessful() ){
                        status = "success";
                        //parse this response
                        String response = discResp.body().string();
                        System.out.println("DiscReply => " + response);
                        if (!Strings.isNullOrEmpty(response)){
                            /* successful Response format
                            {
                                 "op" : "discEntryAdd",
                                "disc" : "http://sss.eu/19309649420644990246",
                                "entry" : "http://sss.eu/19309926752500247252"
                            }
                             */
                            try{
                                JSONObject respJson = new JSONObject(response);
                                String commentID = respJson.getString("entry");
                                System.out.println("Comment ID " + commentID + " is set for new comment.");
                                newComment.setCommentEntity(commentID);

                            }catch(JSONException jex){
                                jex.printStackTrace();
                            }
                        }
                    }
            }
        }catch(IOException ioe){
            ioe.printStackTrace();
            status = "fail";
        }

        return status;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        String message;
        if ( s.equals("success")){
            message = this.type.toString() + " Successful!";
        }else{
            message = this.type.toString() + " failed!";
        }
        Toast.makeText(this.parent.getContext(), message, Toast.LENGTH_LONG).show();

        //notify adapter about this change:
        //but this will cause re-rendering. think over it
        this.parent.notifyDataSetChanged();
    }

    public Response likeComment(Comment comment) throws IOException {
        String commentEntity = comment.getCommentEntity();
        commentEntity = commentEntity.substring(commentEntity.lastIndexOf("/")+1);

        Uri uri = Uri.parse(DiscussionAdapter.SSS_endpoint + "/likes/entities");
        Request.Builder req = new Request.Builder()
                .url(uri.buildUpon()
                        .appendPath(commentEntity)
                        .appendPath("value")
                        .appendPath("1")
                        .toString())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .put(null);
        return execute(req.build());
    }

    public Response replyComment(Comment comment, Comment newcomment) throws IOException {

        String commentEntity = comment.getCommentEntity();
        commentEntity = commentEntity.substring(commentEntity.lastIndexOf("/")+1);

        Uri uri = Uri.parse( DiscussionAdapter.SSS_endpoint + "/entities");
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {
            List<String> list = new ArrayList<String>();
            list.add(newcomment.getContent());
            JSONArray listComments = new JSONArray(list);
            json = json.put("comments", listComments);

            System.out.println("replyComment: " + json.toString());
            RequestBody body = RequestBody.create(JSON, json.toString());

            Request.Builder req = new Request.Builder()
                    .url(uri.buildUpon()
                            .appendPath(commentEntity)
                            .appendPath("comments")
                            .toString())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .post(body);

            return execute(req.build());
        }catch(JSONException jsonexp){
            jsonexp.printStackTrace();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }

        return null;
    }

    public static Response discReplyComment(Comment newcomment) throws IOException {

        String discId = newcomment.getDiscID();
        discId = discId.substring(discId.lastIndexOf("/")+1);

        Uri uri = Uri.parse( DiscussionAdapter.SSS_endpoint + "/discs");
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {
            json = json.put("disc", discId)
                    .put("entry", newcomment.getContent())
                    .put("label", newcomment.getContent())
                    .put("type", "comment");

            RequestBody body = RequestBody.create(JSON, json.toString());

            Request.Builder req = new Request.Builder()
                    .url(uri.toString())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .post(body);

            return execute(req.build());
        }catch(JSONException jsonexp){
            jsonexp.printStackTrace();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }

        return null;
    }

    public static Response execute(Request request) throws IOException {
        Account account = App.loginManager.getAccount();
        System.out.println("DiscussionAdapter Reqesting : " + request.urlString());
        return App.authenticatedHttpClient.execute(request, account);
    }
}



