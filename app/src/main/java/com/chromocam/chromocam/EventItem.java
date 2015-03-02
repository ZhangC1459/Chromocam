package com.chromocam.chromocam;

import android.media.Image;
import android.net.Uri;

public class EventItem {
    private String dateTime;
    private int eventID;
    private Image image;
    private Uri imageURI;

    public EventItem(String test, int ID){
        this.eventID = ID;
        this.dateTime = test;
    }

    public String getDateTime(){
        return this.dateTime;
    }

    public void setDateTime(String dT){
        this.dateTime = dT;
    }

    public int getEventID(){
        return this.eventID;
    }

    public void setEventID(int ID){
        this.eventID = ID;
    }

    public Image getImage(){
        return this.image;
    }

    public Uri getImageURI(){
        return this.imageURI;
    }

    public void setImageURI(Uri locate){
        this.imageURI = locate;
    }

}
