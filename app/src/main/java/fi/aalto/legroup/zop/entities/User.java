package fi.aalto.legroup.zop.entities;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;

import fi.aalto.legroup.zop.entities.serialization.json.JsonSerializable;

public class User implements JsonSerializable, Parcelable {

    protected String name;
    protected Uri uri;
    protected String id;

    @SuppressWarnings("UnusedDeclaration")
    private User() {
        // For serialization
        this.id = "";
    }

    public User(String name, Uri uri, String id) {
        this.name = name;
        this.uri = uri;
        this.id = id;
    }

    public User(String name, Uri uri) {
        this.name = name;
        this.uri = uri;
        this.id = "";
    }

    protected User(Parcel parcel) {
        this.name = parcel.readString();
        this.uri = (Uri) parcel.readValue(Uri.class.getClassLoader());
        this.id = parcel.readString();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Uri getUri() {
        return this.uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof User)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        User other = (User) obj;
        return Objects.equal(name, other.name) && Objects.equal(uri, other.uri)
                && Objects.equal(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, uri, id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.name);
        parcel.writeValue(this.uri);
        parcel.writeValue(this.id);
    }

    public static final Creator<User> CREATOR = new Creator<User>() {

        @Override
        public User createFromParcel(Parcel parcel) {
            return new User(parcel);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }

    };

}
