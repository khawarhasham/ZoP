package fi.aalto.legroup.zop.views.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import fi.aalto.legroup.zop.R;
import fi.aalto.legroup.zop.entities.CommentReply;
//import java.util.zip.Inflater;

/**
 * Created by khawar on 30/01/2016.
 */
public class CommentReplyAdapter extends ArrayAdapter<CommentReply> {

    Context context;
    int layoutResourceId;
    //List<String> comments;
    List<CommentReply> comments;
    private final String TAG = "DiscussionAdapter";

    public CommentReplyAdapter(Context context, int layoutResourceId, List<CommentReply> comments) {
        super(context, layoutResourceId, comments);
        context = context;
        this.layoutResourceId = layoutResourceId;
        this.comments = comments;
        if( this.comments!=null && this.comments.size() > 0 ){
            System.out.println("CommentReplyAdapter TOtal replies: " + this.comments.size());
        }else{
            System.out.println("CommentReplyAdapter no replies");
        }
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

        //get the view
        View commentReplyView = inflator.inflate(R.layout.commentreply_view, parent, false);

        //get the control items references
        TextView reply = (TextView)commentReplyView.findViewById(R.id.commentReply);
        TextView replyAuthor = (TextView)commentReplyView.findViewById(R.id.replyAuthor);
        TextView replyTime = (TextView)commentReplyView.findViewById(R.id.replyTime);



        //get the comment reply
        //String comment = this.comments.get(position);
        CommentReply commentReply = this.comments.get(position);
        System.out.println("commentReplyAdapter=> " + commentReply);

        //set the control items
        reply.setText(commentReply.getContent());
        replyAuthor.setText(commentReply.getAuthor());
        replyTime.setText(DateFormat.format("MM/dd/yyyy HH:mm:ss",
                new Date(commentReply.getDate())).toString());

        return commentReplyView;
    }

}
