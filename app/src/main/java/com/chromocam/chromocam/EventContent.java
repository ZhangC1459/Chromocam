package com.chromocam.chromocam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.chromocam.chromocam.util.ChromoServer;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventContent{
    private String imageID;
    private String date;
    private String time;
    private Bitmap image;
    private String url;
    private String[] temp;
    //Constructor, accepts a JSONObject from the Async Loader method
    public EventContent(JSONObject input){
        try { //attempts to grab the event id and its timestamp
            imageID = input.get("event_id").toString();
            temp = input.get("time_stamp").toString().split("[T|\\.]");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.url = "http://downyhouse.homenet.org:3000/files/" + this.imageID;
        date = temp[0];
        time = temp[1];
    }
    //getter methods.  Setters are not needed because none of these fields should change after
    // construction anyways
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

    public String getUrl() { return this.url; }

    public void setImage(Bitmap bitmap) { this.image = bitmap; }

    //This is the AsyncLoader for each individual file.  It is passed an instance of an EventItem
    // with a URL pre-loaded, and downloads the picture from that URL, returning it as a bitmap

    //This part should be moved to ChromoServer

}