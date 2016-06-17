package fi.aalto.legroup.zop.views.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.repackaged.com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import fi.aalto.legroup.zop.R;
import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.browsing.DiscussionView;
import fi.aalto.legroup.zop.entities.Comment;
import fi.aalto.legroup.zop.entities.CommentReply;
//import java.util.zip.Inflater;

/**
 * Created by khawar on 30/01/2016.
 */
public class DiscussionAdapter extends ArrayAdapter<Comment> {

    Context context;
    int layoutResourceId;
    ArrayList<Comment> comments;
    private final String TAG = "DiscussionAdapter";
    public static final String SSS_endpoint = "http://test-ll.know-center.tugraz.at/test/rest";
    EditText discCommentTextBox;

    public DiscussionAdapter(Context context, int layoutResourceId, ArrayList<Comment> comments) {
        super(context, layoutResourceId, comments);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.comments = comments;
    }

    public void setDiscCommentTextBox(EditText cbox){
        this.discCommentTextBox = cbox;
    }
    /**
     * Critical method
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //get the inflator
        LayoutInflater inflator = LayoutInflater.from(getContext());

        /*
        //get discussion view to get the Comment EditText box
        View discView = inflator.inflate(R.layout.discussion_view, null, false);
        //get the Comment EditText box
        final EditText commentTextBox = (EditText) discView.findViewById(R.id.writeComment);
        final TextView commentLength = (TextView)discView.findViewById(R.id.commentLength);


        //attach keypressed watcher
        final TextWatcher mTextEditorWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //This sets a textview to the current length
                commentLength.setText(String.valueOf(DiscussionView.COMMENT_LIMIT - s.length()));
            }
            public void afterTextChanged(Editable s) {}
        };

        //attach keypressed events for editbox
        commentTextBox.addTextChangedListener(mTextEditorWatcher);
        */

        //get the view
        View commentView = inflator.inflate(R.layout.comment_view, parent, false);

        //get the control items references

        TextView content = (TextView)commentView.findViewById(R.id.cContent);
        TextView author = (TextView)commentView.findViewById(R.id.cAuthor);
        final TextView likes = (TextView)commentView.findViewById(R.id.cLikes);
        TextView ctime = (TextView)commentView.findViewById(R.id.ctime);

        ////TODO: comment inline replybtn till we get the user data along with comments.
        TextView replyBtn = (TextView)commentView.findViewById(R.id.replyBtn);
        TextView likeBtn = (TextView)commentView.findViewById(R.id.likeBtn);
        LinearLayout repliesList = (LinearLayout)commentView.findViewById(R.id.replyListView);

        //get the video link
        final Comment comment = this.comments.get(position);

        //set the control items
        //System.out.println("comment:" + comment.getContent());
        content.setText(comment.getContent());
        author.setText(comment.getAuthor());
        likes.setText(comment.getLike().getLikes() + "");
        ctime.setText(DateFormat.format("MM/dd/yyyy HH:mm:ss",
                new Date(comment.getDate())).toString());


        //CommentReplyAdapter commentReplies = new CommentReplyAdapter(getContext(), R.layout.commentreply_view, comment.getReplies());
        //repliesList.setAdapter(commentReplies);


        //for nested listview, generate our own view dynamically.
        //TODO: comment this until we get user info with inline replies
        //Iterator<String> iter = comment.getReplies().iterator();
        Iterator<CommentReply> iter = comment.getReplies().iterator();
        while(iter.hasNext()){
            CommentReply reply = iter.next();

            View replyView = inflator.inflate(R.layout.commentreply_view, null);
            TextView replyContent = (TextView)replyView.findViewById(R.id.commentReply);
            TextView replyAuthor = (TextView)replyView.findViewById(R.id.replyAuthor);
            TextView replyTime = (TextView)replyView.findViewById(R.id.replyTime);

            replyContent.setText(reply.getContent());
            replyAuthor.setText(reply.getAuthor());
            replyTime.setText(DateFormat.format("MM/dd/yyyy HH:mm:ss",
                    new Date(reply.getDate())).toString());

            repliesList.addView(replyView);
        }

        //attach replyBtn event handler
        //TODO: comment the action on replyBtn till the time we get user data with inline comments.
        replyBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //check if we can access the parent comment of this replybtn
                //Toast.makeText(getContext(), "Reply Clicked:" + comment.getContent(), Toast.LENGTH_LONG).show();

                //get data from the commentBox
                String replyCommentTxt = discCommentTextBox.getText().toString();
                System.out.println("Reply Text: " + replyCommentTxt);
                if ( Strings.isNullOrEmpty(replyCommentTxt)
                        && (replyCommentTxt.length() < 1 || replyCommentTxt.length() > DiscussionView.COMMENT_LIMIT)){
                    Toast.makeText(context, "Comment reply can not be empty or above "+DiscussionView.COMMENT_LIMIT +" characters", Toast.LENGTH_LONG).show();
                    return;
                }

                Comment newComment = new Comment();
                newComment.setContent(replyCommentTxt);
                newComment.setAuthor(App.loginManager.getUserInfo().get("email").getAsString());
                newComment.setDate(new Date().getTime());

                new CommentAction(DiscussionActionType.Type.CommentReply, DiscussionAdapter.this).execute(comment, newComment);
                discCommentTextBox.setText("");
            }
        });

        //attach likeBtn event handler
        likeBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                //steps
                //step1: call the SSS api for liking the entity
                //uri: "likes/entities/".urlencode($entity_id)."/value/1"
                new CommentAction(DiscussionActionType.Type.CommentLike, DiscussionAdapter.this).execute(comment);
                //step2: increase the number of likes on interface
                //comment.getLike().increaseLikes();
                //likes.setText(comment.getLike().getLikes());
            }
        });

        return commentView;
    }


}
