package com.starsearth.five.domain;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Map;

/**
 * Created by faimac on 4/15/17.
 */

public class User implements Parcelable {

    public String uid;
    public String phone;
    public String name;
    public String pic;
    public String volunteerOrganization;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValueString(User.class)
    }

    public User(String key) {
        this.uid = key;
    }

    public User(String key, Map<String, Object> map) {
        this.uid = key;
        this.phone = map.containsKey("phone") ? (String) map.get("phone") : null;
        this.name = map.containsKey("name") ? (String) map.get("name") : null;
        this.pic = map.containsKey("pic") ? (String) map.get("pic") : null;
        this.volunteerOrganization = map.containsKey("volunteer_organization") ? (String) map.get("volunteer_organization") : null;
    }


    protected User(Parcel in) {
        uid = in.readString();
        phone = in.readString();
        name = in.readString();
        pic = in.readString();
        volunteerOrganization = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(phone);
        dest.writeString(name);
        dest.writeString(pic);
        dest.writeString(volunteerOrganization);
    }
}
