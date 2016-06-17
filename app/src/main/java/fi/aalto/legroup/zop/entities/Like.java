package fi.aalto.legroup.zop.entities;

/**
 * Created by khawar on 25/05/2016.
 */
public class Like {
    private int likes;
    private int dislikes;
    private int like;

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }

    public int getLike() {
        return like;
    }

    public void setLike(int like) {
        this.like = like;
    }

    public void increaseLikes(){
        this.likes++;
    }

    @Override
    public String toString() {
        return "Like{" +
                "likes=" + likes +
                ", dislikes=" + dislikes +
                ", like=" + like +
                '}';
    }
}
