package com.chromocam.chromocam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

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
    public JSONArray files;
    public List<EventItem> ITEMS = new ArrayList<EventItem>();
    public Map<String, EventItem> ITEM_MAP = new HashMap<String, EventItem>();
    //This "type" variable tells the Content generator which fragment called it, and thus, which
    // content (archived or events) it should be populating the listView with
    public String type;
    public EventContent(int type){
        this.type = Integer.toString(type);
    }
    { //This function will populate the list of Items upon creation of an instance of "EventContent"
        //The method will be to pull the JSON Array from the webserv and iterate through a loop
        //Depending on the calling fragment (Archive or events) it'll throw out non-archived ones
        try {
            getJSONTask task = new getJSONTask();
            task.execute(new URL("http://downyhouse.homenet.org:3000/files"));
        } catch (NullPointerException e){
            Log.d("ERROR", "Null Pointer Exception on files");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    //addItem method
    private void addItem(EventItem item){
        ITEMS.add(item);
        ITEM_MAP.put(item.imageID, item);
        Log.d("EventContent", "JSON item being added to item MAP");
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

    private class getJSONTask extends AsyncTask<URL, Void, Void>{
        int eventLimit = 25;


        @Override
        protected Void doInBackground(URL...u){
            try{
                Log.d("NOTIFICATION", "Started JSON loading task");
                URL url = u[0];
                InputStream in = url.openConnection().getInputStream();
                BufferedReader sr = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                StringBuilder res = new StringBuilder();

                ArrayList<JSONObject> jsonObjList = new ArrayList<JSONObject>();
//                try {
//                    jsonObjList.add(new JSONObject("{\"event_id\":33013,\"time_stamp\":\"2015-04-18T22:50:35.000Z\",\"file_type\":1,\"archive\":0}"));
//                    jsonObjList.add(new JSONObject("{\"event_id\":33012,\"time_stamp\":\"2015-04-18T22:50:34.000Z\",\"file_type\":1,\"archive\":0}"));
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }

                //Process Buffered Stream Until Single JSON Object is completed
                boolean parsingJSON = true;
                int currentCharValue;
                char currentChar;
                StringBuilder jsonObjStr = new StringBuilder();

                //Read character by character until end of line
                    while((currentCharValue = sr.read()) != -1)
                    {
                        //Beginning of JSON Object
                        if(currentCharValue == (int)'{')
                        {
                            jsonObjStr.append("{");
                            while((currentCharValue = sr.read()) != -1 && currentCharValue != (int)'}')
                            {
                                jsonObjStr.append((char)currentCharValue);
                            }
                            jsonObjStr.append("}");
                            Log.d("JSON OBJ STRING", jsonObjStr.toString());
                            try {
                                jsonObjList.add(new JSONObject(jsonObjStr.toString()));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            jsonObjStr.setLength(0);
                        }

                        if (jsonObjList.size() == eventLimit)
                        {
                            break;
                        }
                    }




//                String jsonObjStr;

//                while()


                //This will cause outOfMemory issues
                //String inputStr;
                //while((inputStr = sr.readLine()) != null)
                //    res.append(inputStr);

                //Log.d("NOTIFICATION", "Received string" + res);
                //files = new JSONArray(res.toString());
                files = new JSONArray(jsonObjList);
                JSONObject row;
                String fileType;
                String archiveState;
                try {
                    for (int i = 0; i < eventLimit; i++) {
                        row = files.getJSONObject(i);
                        fileType = row.getString("file_type");
                        archiveState = row.getString("archive");
                        if (fileType.equals("1") && archiveState.equals(type)) {
                            addItem(new EventItem(row));
                            Log.d("NOTIFICATION", "added item");
                        }
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                } catch (NullPointerException e){
                    e.printStackTrace();
                }
//            } catch (JSONException e) {
//                e.printStackTrace();
            } catch(IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}