package com.chromocam.chromocam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventContent{

    public List<EventItem> ITEMS = new ArrayList<EventItem>();
    public Map<String, EventItem> ITEM_MAP = new HashMap<String, EventItem>();

    { //This function will populate the list of Items upon creation of an instance of "EventContent"
        //The method will be to pull the JSON Array from the webserv and iterate through a loop
        //Depending on the calling fragment (Archive or events) it'll throw out non-archived ones
        //TODO: implement a method for distinguishing between Archive and Event calling fragments
        JSONObject test1 = new JSONObject();
        try {
            test1.put("event_id", "62");
            test1.put("time_stamp","2015-02-17T21:25:15.000Z");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        addItem(new EventItem(test1));
    }
    //addItem method
    private void addItem(EventItem item){
        ITEMS.add(item);
        ITEM_MAP.put(item.imageID, item);
    }

    //item representing a piece of content
    public class EventItem{
        public String imageID;
        public String date;
        public String time;
        public Bitmap image;
        public String url;
        private String[] temp;
        //Constructor, accepts a JSONObject from the method up top
        public EventItem(JSONObject input){
            try { //attempts to grab the event id and its timestamp
                imageID = input.get("event_id").toString();
                temp = input.get("time_stamp").toString().split("[T|\\.]");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            this.url = "http://downyhouse.homenet.org:3000/files/" + this.imageID;
            new getFileTask().execute(this);
            date = temp[0];
            time = temp[1];
        }
        //getter methods.  Setters are not needed because none of these fields should change after construction anyways
        public String getImageID(){
            return this.imageID;
        }

        public String getDate(){
            return this.date;
        }

        public String getTime(){
            return this.time;
        }

        public Bitmap getImage(){
           return this.image;
        }
    }

    private class getFileTask extends AsyncTask<EventItem, Void, Bitmap>{
        EventItem item = null;
        @Override
        protected Bitmap doInBackground(EventItem... item) {
            this.item = item[0];
            try {
                InputStream is = (InputStream) new URL(item[0].url).getContent();
                return BitmapFactory.decodeStream(is);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        protected void onPostExecute(Bitmap result){
            item.image = result;
        }

    }
}