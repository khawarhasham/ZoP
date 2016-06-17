package fi.aalto.legroup.zop.entities;

import java.util.List;

/**
 * Created by khawar on 25/05/2016.
 */
public class Comment {

    private String author;
    private String content;
    private List<CommentReply> replies;
    private String discID;
    private String commentEntity;
    private long date;
    private Like like;

    public String getCommentEntity() {
        return commentEntity;
    }

    public void setCommentEntity(String commentEntity) {
        this.commentEntity = commentEntity;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<CommentReply> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentReply> replies) {
        this.replies = replies;
    }

    public String getDiscID() {
        return discID;
    }

    public void setDiscID(String discID) {
        this.discID = discID;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public Like getLike() {
        return like;
    }

    public void setLike(Like like) {
        this.like = like;
    }

}
