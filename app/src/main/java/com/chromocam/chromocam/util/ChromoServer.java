package com.chromocam.chromocam.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import com.chromocam.chromocam.EventContent;
import com.chromocam.chromocam.EventListTab;
import com.chromocam.chromocam.MainActivity;
import com.chromocam.chromocam.R;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChromoServer implements SharedPreferences.OnSharedPreferenceChangeListener{

    //Logging
    static final String TAG = "Chromocam";

    //Preferences Settings
    //Notifications
    public static final String PROPERTY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String PROPERTY_NOTIFICATIONS_REGISTERED = "notifications_registered";
    //Camera Settings
    public static final String PROPERTY_DEADAZONE_KEY = "deadzone_key";
    public static final String PROPERTY_THRESHOLD_KEY = "threshold";

    //Resolution
    public static final String PROPERTY_RESOLUTION_KEY = "resolution_key";
    public static final String PROPERTY_RESOLUTION_WIDTH = "resolution_width";
    public static final String PROPERTY_RESOLUTION_HEIGHT = "resolution_width";

    public static final String PROPERTY_FRAMERATE_KEY = "framerate_key";
    public static final String PROPERTY_DELAY_KEY = "delay_key";

    //App Settings
    public static final String PROPERTY_PANEL_NUM = "panel_num";

    //Registration Properties
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_APP_VERSION = "appVersion";
    public static final String PROPERTY_IS_REGISTERED = "login_settings";
    public static final String PROPERTY_TOKEN = "token";
    public static final String PROPERTY_DEVICE_ID = "device_id";

    //Public URLS
    public static final String PROPERTY_TARGET = "target";


    //Registration ID
    String regid;
    GoogleCloudMessaging gcm;

    //Scope
    private Context context;
    private Activity currentActivity;

    //Push Notification Project Key
    private String SENDER_ID = "1026539547295";
    private ProgressDialog progressDialog;

    //Registration Parameters
    private String uniqueToken;
    private String targetURLroot;
    private String deviceID;

    private URL targetURL;
    private boolean connectedToServer;

    //Async Information
    protected String target;
    protected JSONObject payload;


    public ChromoServer(Activity current, Context context)
    {
        this.currentActivity = current;
        this.context = context;

        //Register Listener for changes in Preferences Settings
        getSettingsPreferences(context).registerOnSharedPreferenceChangeListener(this);
        getGcmPreferences(context).registerOnSharedPreferenceChangeListener(this);

        this.logRegistrationInfo();
        this.logPreferences();

        //Check for Preset Information
        if(getSettingsPreferences(this.context).getBoolean(PROPERTY_IS_REGISTERED, false))
        {
            Payload registered = new Payload(null, Purpose.REGISTERED);
            registered.setResult(true);

            this.logRegistrationInfo();
            this.logPreferences();

            if(currentActivity instanceof ChromoComplete){
                ((ChromoComplete) currentActivity).onTaskCompleted(registered);
            }

        }


    }


    private void logRegistrationInfo()
    {
        Log.d(TAG, "Current Registration Information");
        Log.d("Chromo Server", "Current token:" + getSharedPrefInfoString(PROPERTY_TOKEN, getSettingsPreferences(this.context)));
        Log.d("Chromo Server", "Current target:" + getSharedPrefInfoString(PROPERTY_TARGET, getSettingsPreferences(this.context)));
        Log.d("Chromo Server", "Current device id:" + getSharedPrefInfoString(PROPERTY_DEVICE_ID, getSettingsPreferences(this.context)));
    }

    public void logPreferences()
    {
        Log.d("ChromoServer", "Current Preferences");
        Log.d("ChromoServer", "Preferences:" + this.getSettingsPreferences(this.context).getAll().toString());
    }

    //Registers for Push Notifications if Necessary
    public void registerPushNotifcations()
    {
        if(this.getSharedPrefInfoString(PROPERTY_REG_ID, getGcmPreferences(this.context)).isEmpty())
        {
            Log.d("GCM Push Reg", "Starting GCM Push Registration");
            this.GCMregisterInBackground();
        }
    }


    //Instantiation
    public void initChromoServer(String targetURLroot, String password)
    {
        this.targetURLroot = targetURLroot;
        Log.d("Chromo Server", "Initializing ChromoServer Connection");
        this.registerDevice(password);
        Log.d("Chromo Server", "Returning connection status");
    }

    public void setActivity(Activity x){
        this.currentActivity = x;
    }

    //Register Device to Server
    private void registerDevice(String password)
    {
//        this.regid = this.getSharedPrefInfo(this.context, PROPERTY_REG_ID,getGcmPreferences(this.context));
//
//        if(regid.isEmpty())
//        {
//            Log.d("ChromoServer Reg", "Push Reg ID Not found");
//            return;
//        }

        String registerString = "/devices/register";
        HashMap<String, String> params = new HashMap<String, String>();

        try {
            params.put("hashedPass", this.sha1(password));
        } catch (NoSuchAlgorithmException e) {
            Log.d("ChromoServer", "SHA1 Failed");
            e.printStackTrace();
        }

        Log.d("Chromo Server", "Executing Async POST Request");
        Log.d("Chromo Server", "Parameters: " + params.get("hashedPass") + ", " + this.targetURLroot + registerString);
        //Log.d("Chromo Server", "GCM Registration ID: " + regid);

        try {

             new processRegistration().execute(prepareSecurePostRequest(new JSONObject(params), this.targetURLroot + registerString));

        } catch (UnsupportedEncodingException e) {
            Log.d("ChromoServer", "JSON encoding not accepted");
            e.printStackTrace();
        }

    }


    //http://www.sha1-online.com/sha1-java/
    static String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("Preference Changed:", key);

        HashMap<String, String> params = new HashMap<String, String>();
        String target_end = "";
        String motionConfigSet = "/motion/config/set";

        //Push Notification Toggled
        if(key.equals(PROPERTY_NOTIFICATIONS_ENABLED))
        {

            registerPushNotifcations();


            //Send Request to ChromocamAPI to toggle Push notifications
            String ChromoAPIpush = "/devices/notifications/set";
            JSONObject chromopush = prepareSecureJSONAuth();

            try {
                Integer setting = getSharedPrefInfoBoolean(PROPERTY_NOTIFICATIONS_ENABLED) ? 1 : 0;
                chromopush.put("enabled", setting);
                Log.d("NOTIF_ENABLED", chromopush.toString());
                HttpPropertyHelper pay = new HttpPropertyHelper(prepareSecurePostRequest(chromopush, getTargetURL() + ChromoAPIpush), PROPERTY_REG_ID);
                new processJSON().execute(pay);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else if(key.equals(PROPERTY_DEADAZONE_KEY))
        {
            JSONObject deadzoneParams = prepareSecureJSONAuth();

            try {
                deadzoneParams.put("option", "area_detect");
                deadzoneParams.put("value", this.getSharedPrefInfoString(PROPERTY_DEADAZONE_KEY).toString());
                Log.d("PARAMS", deadzoneParams.toString());
                try {
                    new processJSON().execute(new HttpPropertyHelper(prepareSecurePostRequest(deadzoneParams, getTargetURL() + motionConfigSet), PROPERTY_DEADAZONE_KEY));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else if(key.equals(PROPERTY_RESOLUTION_KEY))
        {

            String[] resolution = this.getSharedPrefInfoString(PROPERTY_RESOLUTION_KEY).split("x");
            if(resolution.length >1)
            {
                String width = resolution[0];
                String height = resolution[1];
                Log.d("RESOLUTION", this.getSharedPrefInfoString(PROPERTY_RESOLUTION_KEY));

                JSONObject widthParams = prepareSecureJSONAuth();
                JSONObject heightParams = prepareSecureJSONAuth();

                try {
                    widthParams.put("option", "width");
                    widthParams.put("value", width);

                    heightParams.put("option", "height");
                    heightParams.put("value", height);

                    Log.d("SENDING WIDTHxHEIGHT", widthParams.toString() + heightParams.toString());

                    try {
                        new processJSON().execute(new HttpPropertyHelper(prepareSecurePostRequest(widthParams, getTargetURL() + motionConfigSet), PROPERTY_RESOLUTION_WIDTH));
                        new processJSON().execute(new HttpPropertyHelper(prepareSecurePostRequest(heightParams, getTargetURL() + motionConfigSet), PROPERTY_RESOLUTION_HEIGHT));

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        else if(key.equals(PROPERTY_FRAMERATE_KEY))
        {
            JSONObject frameParams = prepareSecureJSONAuth();


            try {
                frameParams.put("option", "framerate");
                frameParams.put("value", this.getSharedPrefInfoString(PROPERTY_FRAMERATE_KEY).toString());
                try {
                    new processJSON().execute(new HttpPropertyHelper(prepareSecurePostRequest(frameParams, getTargetURL() + motionConfigSet), PROPERTY_FRAMERATE_KEY));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        else if(key.equals(PROPERTY_DELAY_KEY))
        {
            JSONObject delayParams = prepareSecureJSONAuth();

            try {
                delayParams.put("option", "gap");
                delayParams.put("value", this.getSharedPrefInfoString(PROPERTY_DELAY_KEY).toString());
                try {
                    Log.d("PARAMS", delayParams.toString());
                    new processJSON().execute(new HttpPropertyHelper(prepareSecurePostRequest(delayParams, getTargetURL() + motionConfigSet), PROPERTY_DELAY_KEY));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else if(key.equals(PROPERTY_PANEL_NUM))
        {

        }
        else if(key.equals(PROPERTY_REG_ID))
        {
            String ChromoAPIpush = "/devices/notifications/setToken";
            JSONObject chromopush = prepareSecureJSONAuth();
            try {
                chromopush.put("gcmId", getSharedPrefInfoString(PROPERTY_REG_ID, this.getGcmPreferences(this.context)));
                HttpPropertyHelper pay = new HttpPropertyHelper(prepareSecurePostRequest(chromopush, getTargetURL() + ChromoAPIpush), PROPERTY_REG_ID);
                new processJSON().execute(pay);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        Log.d("PREFERENCES STATUS:", this.getSettingsPreferences(this.context).getAll().toString());

        Log.d("NumPanes", this.getPanelNum().toString());
    }

    public void syncServerCurrentSettings()
    {
        //Config Option Strings
        String setConfig = "/motion/config/get";
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("framerate", PROPERTY_FRAMERATE_KEY);
        options.put("width",PROPERTY_RESOLUTION_WIDTH);
        options.put("height",PROPERTY_RESOLUTION_HEIGHT);
        options.put("gap", PROPERTY_DELAY_KEY);
        options.put("area_detect", PROPERTY_DEADAZONE_KEY);


        for(HashMap.Entry<String, String> entry : options.entrySet())
        {
            JSONObject params = prepareSecureJSONAuth();
            try {
                params.put("option", entry.getKey());
                try {

                    Log.d("JSONOBJECT:", params.toString());
                    HttpPropertyHelper pay = new HttpPropertyHelper(prepareSecurePostRequest(params, this.getTargetURL() + setConfig), options.get(entry.getKey()));
                    pay.updateShared = true;
                    new processJSON().execute(pay);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Log.d("Chromoserver", "Unsupported Encoding");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d("ChromoserverSync", "JSON ERROR");
            }
        }
        JSONObject widthParams = prepareSecureJSONAuth();
        JSONObject heightParams = prepareSecureJSONAuth();
        try {
            widthParams.put("option", "width");
            heightParams.put("option", "height");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("JSONOBJECT:", widthParams.toString());
        Log.d("JSONOBJECT:", heightParams.toString());


        try {
            new processJSONresolution().execute(prepareSecurePostRequest(widthParams, this.getTargetURL() + setConfig),prepareSecurePostRequest(heightParams, this.getTargetURL() + setConfig));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private class HttpPropertyHelper
    {
        HttpPost httppost;
        String sharedRefName;
        Boolean updateShared = false;

        HttpPropertyHelper(HttpPost httppost, String sharedRefName)
        {
            this.httppost = httppost;
            this.sharedRefName = sharedRefName;
        }

    }

    private class processJSON extends AsyncTask<HttpPropertyHelper, Void, String>
    {
        HttpPost httpPost;
        String sharedRefName;
        Boolean updateShared = false;

        @Override
        protected void onPreExecute()
        {

        }

        @Override
        protected String doInBackground(HttpPropertyHelper... params) {
            this.httpPost = params[0].httppost;
            this.sharedRefName = params[0].sharedRefName;
            this.updateShared = params[0].updateShared;
            return getJSONResponse(httpPost);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.toUpperCase().contains("FORBIDDEN")) {
                Toast.makeText(currentActivity, "Auth failed!", Toast.LENGTH_LONG).show();
            } else {
                try {
                    JSONObject res = new JSONObject(result);

                    Log.d("SYNCHRONIZE", res.toString());

                    //Store Credentials
                    if(updateShared)
                    {
                        setSharedPrefInfo(this.sharedRefName, res.getString("value"));
                        Log.d("Updating Preferences", this.sharedRefName + " and value: " + res.getString("value"));

                    }
                    //Success Toast Message
                    //Toast.makeText(currentActivity, "Registration Success!", Toast.LENGTH_LONG).show();

                } catch (JSONException e) {
                    Log.d("ChromoServ Error", "Bad JSON");
                }
            }
        }
    }


    private class resolutionHelper
    {
        int width;
        int height;
        HttpPost httppost;

        resolutionHelper(int width, int height, HttpPost httppost)
        {
            this.width = width;
            this.height = height;
            this.httppost = httppost;
        }
    }

    private class processJSONresolution extends AsyncTask<HttpPost, Void, ArrayList<String>>
    {
        int width =  0;
        int height = 0;
        String resolution = "";
        HttpPost httppost;

        @Override
        protected void onPreExecute()
        {

        }

        @Override
        protected ArrayList<String> doInBackground(HttpPost... params) {
            this.httppost = params[0];

            ArrayList<String> responses = new ArrayList<String>();

            responses.add(getJSONResponse(params[0]));
            responses.add(getJSONResponse(params[1]));


            return responses;
        }

        @Override
        protected void onPostExecute(ArrayList<String> resultRows) {
            for(String result : resultRows) {
                if (result.toUpperCase().contains("FORBIDDEN")) {
                    Toast.makeText(currentActivity, "Auth failed!", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        JSONObject res = new JSONObject(result);
                        Log.d("SYNCHRONIZE", res.toString());

                        if(res.get("option").toString().equals("width"))
                        {
                            this.width = Integer.parseInt(res.get("value").toString());
                        }
                        else if(res.get("option").toString().equals("height"))
                        {
                            this.height = Integer.parseInt(res.get("value").toString());
                        }
                    } catch (JSONException e) {
                        Log.d("ChromoServ Error", "Bad JSON");
                    }
                }
            }

            if(this.width!= 0 && this.height !=0)
            {
                this.resolution = this.width + "x" + this.height;
                Log.d("RESOLUTION", resolution);
                setSharedPrefInfo(PROPERTY_RESOLUTION_KEY, this.resolution);
            }
        }
    }

    //standard post request
    private class processRegistration extends AsyncTask<HttpPost, Void, String>
    {
        Payload p;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            progressDialog = new ProgressDialog(currentActivity);
            progressDialog.setCancelable(true);
            progressDialog.setMessage("Registering...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setProgress(0);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(HttpPost... params) {
            return getJSONResponse(params[0]);
        }
        protected void onPostExecute(String response){
            progressDialog.dismiss();
            p = new Payload(null, Purpose.REGISTER);
            if(response.toUpperCase().contains("FORBIDDEN")){
                Toast.makeText(currentActivity, "Auth failed!", Toast.LENGTH_LONG).show();
                p.setResult(false);
            } else {
                try {
                    JSONArray x = new JSONArray(response);
                    JSONObject res = x.getJSONObject(0);
                    uniqueToken = res.getString("token");
                    deviceID = res.getString("device_id");
                    p.setResult(true);

                    //Store Credentials
                    Log.d("Credentials", "Token and Target now stored for later use");
                    setSharedPrefInfo(PROPERTY_TARGET, targetURLroot);
                    setSharedPrefInfo(PROPERTY_TOKEN, uniqueToken);
                    setSharedPrefInfo(PROPERTY_DEVICE_ID, deviceID);
                    setSharedPrefInfo(PROPERTY_NOTIFICATIONS_REGISTERED, false);

                    //Success Toast Message
                    Toast.makeText(currentActivity, "Registration Success!", Toast.LENGTH_LONG).show();

                } catch (JSONException e) {
                    Log.d("ChromoServ Error", "Bad JSON");
                }
            }
            if(currentActivity instanceof ChromoComplete){
                ((ChromoComplete) currentActivity).onTaskCompleted(p);
            }
        }
    }

    public void loadList(int pageNo, int calling){
        //Step 0. Initialize Payload
        final Payload p;
        //Step 1. Prepare the post request object
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("offset", Integer.toString((pageNo-1)*getPanelNum()));
        switch (calling) {
            case 1:
                params.put("archive", "0");
                p = new Payload(null, Purpose.GET_FILE_LIST_E);
                break;
            case 2:
                params.put("archive", "1");
                p = new Payload(null, Purpose.GET_FILE_LIST_A);
                break;
            default:
                params.put("archive", "0");
                p = new Payload(null, Purpose.GET_FILE_LIST_E);
                break;
        }
        params.put("limit", Integer.toString(getPanelNum()));
        JSONObject JSONpost = prepareSecureJSONAuth(params);
        Log.d("Watcher", "Posting: " + JSONpost.toString());
        //Step 2: AsyncTask
        try {
            new AsyncTask<HttpPost, Void, String>() {

                @Override
                protected String doInBackground(HttpPost... params) {
                    return getJSONResponse(params[0]);
                }

                @Override
                protected void onPostExecute(String files) {
                    ArrayList<EventContent> list = new ArrayList<EventContent>();
                    Log.d("Watcher", "LoadList reached onPostExecute");
                    if(files.equalsIgnoreCase("forbidden")){
                        Log.d("Error","FORBIDDEN JSON IS FUCKING UP");
                    } else {
                        Log.d("Watcher", "LoadList postExecute not Forbidden - good response");
                        Log.d("Watcher", "Response: " + files);
                        try {
                            JSONArray fileList = new JSONArray(files);
                            JSONObject row;
                            for (int i = 0; i < fileList.length(); i++) {
                                Log.d("Watcher", "adding item " + (i+1) + " to list");
                                row = fileList.getJSONObject(i);
                                EventContent item = new EventContent(row);
                                list.add(item);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                            Log.d("ERROR", "AYYYY");
                            Log.d("Dump", files);
                        }
                        p.setContent(list);
                        p.setResult(true);
                        if (currentActivity instanceof ChromoComplete) {
                            ((MainActivity) currentActivity).onTaskCompleted(p);
                        }
                    }
                }

            }.execute(prepareSecurePostRequest(JSONpost, getSharedPrefInfoString(PROPERTY_TARGET, getSettingsPreferences(this.context)) + "/files"), null, null);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void GCMregisterInBackground() {
        new AsyncTask<Void, Void, String>() {


            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
                progressDialog = new ProgressDialog(currentActivity);
                progressDialog.setCancelable(true);
                progressDialog.setMessage("Registering Push Notifications...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setProgress(0);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                Log.d("GCM Push", "Starting Background process");
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;
                    storeRegistrationId(context, regid);


                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                 }
                return msg;
            }

            @Override

            protected void onPostExecute(String msg){
                    progressDialog.dismiss();

                    Log.d("GCM Registration Result", msg + "\n");
            }

        }.execute(null, null, null);
    }

    //Prepares Credentials for secure JSON Auth
    public JSONObject prepareSecureJSONAuth(HashMap<String, String> params)
    {
        params.put("id", getSharedPrefInfoString(PROPERTY_DEVICE_ID, getSettingsPreferences(this.context)));
        params.put("token", getSharedPrefInfoString(PROPERTY_TOKEN, getSettingsPreferences(this.context)));
        return new JSONObject(params);
    }

    public JSONObject prepareSecureJSONAuth() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("id", getSharedPrefInfoString(PROPERTY_DEVICE_ID, getSettingsPreferences(this.context)));
        params.put("token", getSharedPrefInfoString(PROPERTY_TOKEN, getSettingsPreferences(this.context)));
        return new JSONObject(params);
    }

    //Prepares a Secure Post request to be executed
    public HttpPost prepareSecurePostRequest(JSONObject json, String url) throws UnsupportedEncodingException {
        //Prepare Connection
        int TIMEOUT_MILLISEC = 10000;  // = 10 seconds
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
        HttpPost request = new HttpPost(url); //sets URL for POST request
        request.setHeader("Content-Type", "application/json; charset=utf-8"); //Sets content type header
        StringEntity se = new StringEntity(json.toString()); //turns json to string
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json")); //encodes as "json type"
        request.setEntity(se); //sets entity
        return request;
    }

    //Parses String buffers to get response from request
    private String getJSONResponse(HttpPost post){

        HttpClient client = new DefaultHttpClient(post.getParams());
        try {
            HttpResponse response = client.execute(post);
            InputStream in = response.getEntity().getContent();
            BufferedReader sr = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder res = new StringBuilder();
            String inputStr;
            while((inputStr = sr.readLine()) != null)
                res.append(inputStr);
            Log.d("ChromoServer POST", res.toString());
            return res.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(this.currentActivity, "Cannot connect to target URL!", Toast.LENGTH_LONG).show();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            Toast.makeText(this.currentActivity, "Cannot connect to target URL!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this.currentActivity, "Cannot connect to target URL!", Toast.LENGTH_LONG).show();
        } catch (IllegalStateException e)
        {
            e.printStackTrace();
            Toast.makeText(this.currentActivity, "Cannot connect to target URL!", Toast.LENGTH_LONG).show();
        }

        return null;
    }



    //Store Registration ID
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.d(TAG, "Saving regId on app version " + appVersion);

        this.setSharedPrefInfo(PROPERTY_REG_ID, regId, prefs);
        this.setSharedPrefInfo(PROPERTY_APP_VERSION, appVersion, prefs);
        this.setSharedPrefInfo(PROPERTY_NOTIFICATIONS_REGISTERED, true);
    }

    //Initalize Default Preferences on Registration
    private void initPreferences()
    {
        this.setSharedPrefInfo(PROPERTY_IS_REGISTERED, false);
        this.setSharedPrefInfo(PROPERTY_NOTIFICATIONS_ENABLED, false);
        this.setSharedPrefInfo(PROPERTY_NOTIFICATIONS_REGISTERED, false);
        this.setSharedPrefInfo(PROPERTY_PANEL_NUM, 10);
    }

    //Get SharedPreferences Value
    private String getSharedPrefInfoString(String sharedPrefName, SharedPreferences sharedPref)
    {
        String result = sharedPref.getString(sharedPrefName, "");
        if (result.isEmpty()) {Log.i(TAG, sharedPrefName +" not found.");}
        return result;
    }
    private Integer getSharedPrefInfoInteger(String sharedPrefName, SharedPreferences sharedPref)
    {
        Integer result = sharedPref.getInt(sharedPrefName, 0);
        if (result == 0) {Log.i(TAG, sharedPrefName +" not found.");}
        return result;
    }
    private Boolean getSharedPrefInfoBoolean(String sharedPrefName, SharedPreferences sharedPref)
    {
        Boolean result = sharedPref.getBoolean(sharedPrefName, false);
        if (!result) {Log.i(TAG, sharedPrefName +" not found.");}
        return result;
    }
    private String getSharedPrefInfoString(String sharedPrefName)
    {
        SharedPreferences sharedPref = this.getSettingsPreferences(this.context);
        String result = sharedPref.getString(sharedPrefName, "");
        if (result.isEmpty()) {Log.i(TAG, sharedPrefName +" not found.");}
        return result;
    }
    private Integer getSharedPrefInfoInteger(String sharedPrefName)
    {
        SharedPreferences sharedPref = this.getSettingsPreferences(this.context);
        Integer result = sharedPref.getInt(sharedPrefName, 0);
        if (result == 0) {Log.i(TAG, sharedPrefName +" not found.");}
        return result;
    }
    private Boolean getSharedPrefInfoBoolean(String sharedPrefName)
    {
        SharedPreferences sharedPref = this.getSettingsPreferences(this.context);
        Boolean result = sharedPref.getBoolean(sharedPrefName, false);
        if (!result) {Log.i(TAG, sharedPrefName +" not found.");}
        return result;
    }

    //Set SharedPreferences Value
    private void setSharedPrefInfo(String sharedPrefName, String sharedPrefValue)
    {
        SharedPreferences sharedPref = this.getSettingsPreferences(this.context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(sharedPrefName, sharedPrefValue);
        editor.commit();
    }
    private void setSharedPrefInfo(String sharedPrefName, Integer sharedPrefValue)
    {
        SharedPreferences sharedPref = this.getSettingsPreferences(this.context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(sharedPrefName, sharedPrefValue);
        editor.commit();
    }
    private void setSharedPrefInfo(String sharedPrefName, Boolean sharedPrefValue)
    {
        SharedPreferences sharedPref = this.getSettingsPreferences(this.context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(sharedPrefName, sharedPrefValue);
        editor.commit();
    }
    private void setSharedPrefInfo(String sharedPrefName, String sharedPrefValue, SharedPreferences sharedPref)
    {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(sharedPrefName, sharedPrefValue);
        editor.commit();
    }
    private void setSharedPrefInfo(String sharedPrefName, Integer sharedPrefValue, SharedPreferences sharedPref)
    {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(sharedPrefName, sharedPrefValue);
        editor.commit();
    }
    private void setSharedPrefInfo(String sharedPrefName, Boolean sharedPrefValue, SharedPreferences sharedPref)
    {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(sharedPrefName, sharedPrefValue);
        editor.commit();
    }


    //Get Application's Version Number
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    //Get GCM Preferences
    private SharedPreferences getGcmPreferences(Context context)
    {return context.getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);}

    //Get Settings Preferences
    private SharedPreferences getSettingsPreferences(Context context)
    {return PreferenceManager.getDefaultSharedPreferences(context);}

    public String getTargetURL ()
    {return getSharedPrefInfoString(PROPERTY_TARGET, getSettingsPreferences(context));}
    public String getDeviceID()
    {return getSharedPrefInfoString(PROPERTY_DEVICE_ID, getSettingsPreferences(context));}
    public String getUniqueToken()
    {return getSharedPrefInfoString(PROPERTY_TOKEN, getSettingsPreferences(context));}
    public Integer getPanelNum()
    {
            SharedPreferences sharedPref = this.getSettingsPreferences(this.context);
            String paneNumString =sharedPref.getString(PROPERTY_PANEL_NUM, "10");
            String intPattern ="^[+-]?\\d+$";

            Pattern r = Pattern.compile(intPattern);

            Matcher m = r.matcher(paneNumString);

            if(m.find())
            {
                return Integer.parseInt(paneNumString);
            }
            else
            {
                return 10;
            }

    }
}

