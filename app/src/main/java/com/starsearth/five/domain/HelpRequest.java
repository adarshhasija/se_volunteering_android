package com.starsearth.five.domain;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.HashMap;

public class HelpRequest implements Parcelable {

    public String uid;
    public String phone;
    public String name;
    public String guestName;
    public String guestPhone;
    public String landmark;
    public String volunteerOrganization;
    public String request;

    //request type = DISTRIBUTION
    public String noOfFamilyMembers;
    public String aidType;
    public String rationCard;
    //

    public SEAddress address;
    public String status; //ACTIVE / COMPLETE / CANCELLED
    public String picCompleteUrl;
    public String completedByUserId;
    public String completedByName;
    public String completedByPhone;
    public long timestampCompletion;
    public long timestamp;

    public HelpRequest() {

    }

    public HelpRequest(String key, HashMap<String, Object> map) {
        this.uid =  key != null ? key : null;
        this.phone = map.containsKey("phone") ? (String) map.get("phone") : null;
        this.name = map.containsKey("name") ? (String) map.get("name") : null;
        this.guestPhone = map.containsKey("guest_phone") ? (String) map.get("guest_phone") : null;
        this.guestName = map.containsKey("guest_name") ? (String) map.get("guest_name") : null;
        this.landmark = map.containsKey("landmark") ? (String) map.get("landmark") : null;
        this.volunteerOrganization = map.containsKey("volunteer_organization") ? (String) map.get("volunteer_organization") : null;
        this.request = map.containsKey("request") ? (String) map.get("request") : null;
        this.noOfFamilyMembers = map.containsKey("no_of_family_members") ? (String) map.get("no_of_family_members") : null;
        this.aidType = map.containsKey("aid_type") ? (String) map.get("aid_type") : null;
        this.rationCard = map.containsKey("ration_card") ? (String) map.get("ration_card") : null;
        this.address = map.containsKey("address") ? new SEAddress((HashMap<String, Object>) map.get("address")) : null;
        this.status = map.containsKey("status") ? (String) map.get("status") : null;
        this.picCompleteUrl = map.containsKey("pic_complete_url") ? (String) map.get("pic_complete_url") : null;
        this.completedByUserId = map.containsKey("completed_user_id") ? (String) map.get("completed_user_id") : null;
        this.completedByName = map.containsKey("completed_user_name") ? (String) map.get("completed_user_name") : null;
        this.completedByPhone = map.containsKey("completed_user_phone") ? (String) map.get("completed_user_phone") : null;
        this.timestampCompletion = map.containsKey("timestamp_completion") ? (long) map.get("timestamp_completion") : 0;
        this.timestamp = map.containsKey("timestamp") ? (long) map.get("timestamp") : 0;
    }

    protected HelpRequest(Parcel in) {
        uid = in.readString();
        phone = in.readString();
        name = in.readString();
        guestName = in.readString();
        guestPhone = in.readString();
        landmark = in.readString();
        volunteerOrganization = in.readString();
        request = in.readString();
        noOfFamilyMembers = in.readString();
        aidType = in.readString();
        rationCard = in.readString();
        address = in.readParcelable(SEAddress.class.getClassLoader());
        status = in.readString();
        picCompleteUrl = in.readString();
        completedByUserId = in.readString();
        completedByName = in.readString();
        completedByPhone = in.readString();
        timestampCompletion = in.readLong();
        timestamp = in.readLong();
    }

    public static final Creator<HelpRequest> CREATOR = new Creator<HelpRequest>() {
        @Override
        public HelpRequest createFromParcel(Parcel in) {
            return new HelpRequest(in);
        }

        @Override
        public HelpRequest[] newArray(int size) {
            return new HelpRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uid);
        parcel.writeString(phone);
        parcel.writeString(name);
        parcel.writeString(guestName);
        parcel.writeString(guestPhone);
        parcel.writeString(landmark);
        parcel.writeString(volunteerOrganization);
        parcel.writeString(request);
        parcel.writeString(noOfFamilyMembers);
        parcel.writeString(aidType);
        parcel.writeString(rationCard);
        parcel.writeParcelable(address, 0);
        parcel.writeString(status);
        parcel.writeString(picCompleteUrl);
        parcel.writeString(completedByUserId);
        parcel.writeString(completedByName);
        parcel.writeString(completedByPhone);
        parcel.writeLong(timestampCompletion);
        parcel.writeLong(timestamp);
    }
}
