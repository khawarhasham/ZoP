package fi.aalto.legroup.zop.sss;

import android.accounts.Account;
import android.net.Uri;
import android.os.AsyncTask;

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

import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.sss.SSSEvent;
import fi.aalto.legroup.zop.sss.SSSEventType;
import fi.aalto.legroup.zop.views.adapters.DiscussionAdapter;

/**
 * Created by khawar on 31/05/2016.
 */
public class TagAction extends AsyncTask<String, Void, ArrayList<String>> {
    SSSEvent eventHandler;
    SSSEventType.Type sssEventType;

    public TagAction(SSSEvent handler, SSSEventType.Type sssEventType){
        this.eventHandler = handler;
        this.sssEventType = sssEventType;
    }

    @Override
    protected ArrayList<String> doInBackground(String... params) {
        ArrayList<String> result = new ArrayList<String>();
        try {

            if ( this.sssEventType == SSSEventType.Type.GetTag ) {
                Response userlist = getUsersList();
                if (userlist != null && userlist.isSuccessful()) {
                    String usersssid = getUserSSSID(userlist.body().string(),
                                                    params[0], params[1]); //0:email, 1:subid
                    //set App User Id field
                    App.loginManager.getUser().setId(usersssid);

                    Response tagResp = getUserInfoTags(usersssid);
                    if (tagResp != null && tagResp.isSuccessful()) {
                        //System.out.println("tags: " + tagResp.body().string());
                        result = getUserTags(tagResp.body().string());
                    }
                }
            }else if ( this.sssEventType == SSSEventType.Type.AddTag ) {
                Response addTag = addUserTag(params[0], params[1]); //0:sssid, 1: tag label
                if ( addTag != null && addTag.isSuccessful()){
                    result.add(params[1]);
                }else{
                    System.out.println("Some error in adding tag " + addTag.message());
                    result = null;
                }
            }

        }catch(IOException ioe){
            ioe.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("Found user tags: " + result.size());
        return result;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (this.sssEventType == SSSEventType.Type.GetTag) {
            this.eventHandler.startEvent();
        }
    }

    @Override
    protected void onPostExecute(ArrayList<String> s) {
        super.onPostExecute(s);
        //call the event handler
        this.eventHandler.processEvent(s);

        if (this.sssEventType == SSSEventType.Type.GetTag) {
            this.eventHandler.stopEvent();
        }
    }

    public String getUserSSSID(String userlistJson, String useremail, String usersubid) throws Exception {
        String userssid = "";
        if ( userlistJson != null ) {
            JSONObject json = new JSONObject(userlistJson);
            JSONArray users = json.getJSONArray("users");

            //iterate over the user list
            for(int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                String email = user.getString("email");
                //String oidcsub = user.getString("oidcSub");
                if ( email.equals(useremail)){
                    userssid = user.getString("id");
                    if ( !Strings.isNullOrEmpty(userssid)) {
                        //parse the ID integer part only
                        userssid = userssid.substring(userssid.lastIndexOf("/")+1);
                        break;
                    }
                }
            }
        }
        return userssid;
    }

    public Response getUsersList(){
        Uri uri = Uri.parse( DiscussionAdapter.SSS_endpoint + "/users");
        try{
            Request.Builder req = new Request.Builder()
                    .url(uri.toString())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .get();
            return execute ( req.build() );
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        return null;
    }

    public Response getUserInfoTags(String userid) throws IOException {

        Uri uri = Uri.parse( DiscussionAdapter.SSS_endpoint + "/users/filtered");
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {
            json = json.put("setTags", true);

            System.out.println("userInfoTags: " + json.toString() + " for user " + userid);
            RequestBody body = RequestBody.create(JSON, json.toString());

            Request.Builder req = new Request.Builder()
                    .url(uri.buildUpon()
                            .appendPath(userid)
                            .toString())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .post(body);

            return execute( req.build() );
        }catch(JSONException jsonexp){
            jsonexp.printStackTrace();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }

        return null;
    }

    public Response addUserTag(String userid, String tagLabel) throws IOException {

        Uri uri = Uri.parse( DiscussionAdapter.SSS_endpoint + "/tags");
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {

            json = json.put("label", tagLabel)
                    .put("entity", userid);

            System.out.println("addUserTag: " + json.toString() + " for user " + userid);
            RequestBody body = RequestBody.create(JSON, json.toString());

            Request.Builder req = new Request.Builder()
                    .url(uri.toString())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .post(body);

            return execute( req.build() );
        }catch(JSONException jsonexp){
            jsonexp.printStackTrace();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        return null;
    }

    public ArrayList<String> getUserTags(String userTagsJson) throws Exception {
        //System.out.println("User Tags: " + userTagsJson);
        ArrayList<String> userTags = new ArrayList<String>();
        if ( !Strings.isNullOrEmpty(userTagsJson) ) {
            JSONObject json = new JSONObject(userTagsJson);
            JSONArray users = json.getJSONArray("users");
            if ( users.length() != 1){
                System.out.println("Something happend. got multiple users: " + users.length());
                return userTags;
            }

            JSONObject user = users.getJSONObject(0); //first object
            JSONArray tags = user.getJSONArray("tags");

            for(int i = 0; i < tags.length(); i++ ){
                JSONObject tagObj = tags.getJSONObject(i);
                String tag = tagObj.getString("label"); //or use tagLabel
                userTags.add(tag);
            }
        }
        return userTags;
    }

    public Response execute(Request request) throws IOException {
        Account account = App.loginManager.getAccount();
        System.out.println("TagAction Reqesting : " + request.urlString());
        return App.authenticatedHttpClient.execute(request, account);
    }
}



