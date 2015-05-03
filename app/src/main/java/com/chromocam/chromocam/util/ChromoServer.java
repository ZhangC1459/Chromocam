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

public class ChromoServer implements SharedPreferences.OnSharedPreferenceChangeListener{

    //Logging
    static final String TAG = "Chromocam";

    //Preferences Settings
    //Notifcations
    public static final String PROPERTY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String PROPERTY_NOTIFICATIONS_REGISTERED = "notifications_registered";
    //Camera Settings
    public static final String PROPERTY_DEADAZONE_KEY = "deadzone_key";
    public static final String PROPERTY_RESOLUTION_KEY = "resolution_key";
    public static final String PROPERTY_FRAMERATE_KEY = "framerate_key";
    public static final String PROPERTY_DELAY_KEY = "delay_key";
    //App Settings
    public static final String PROPERTY_PANEL_NUM = "panel_num";

    //Registration Properties
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_IS_REGISTERED = "login_settings";
    private static final String PROPERTY_TOKEN = "token";
    private static final String PROPERTY_TARGET = "target";
    private static final String PROPERTY_DEVICE_ID = "device_id";

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

        this.logRegistrationInfo();
        this.logPreferences();

        if(this.getSharedPrefInfoString(PROPERTY_REG_ID, getGcmPreferences(this.context)).isEmpty())
        {
            Log.d("GCM Push Reg", "Starting GCM Push Registration");
            this.GCMregisterInBackground();
        }

        //Check for Preset Information
        else if(getSettingsPreferences(this.context).getBoolean(PROPERTY_IS_REGISTERED, false))
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
        //Check if App Versions Mismatched
        int registeredVersion = getGcmPreferences(this.context).getInt(this.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(this.context);

        //If Version Mismatch
        if (registeredVersion != currentVersion) {
            Log.d(TAG, "App version changed.");
            try {
                this.gcm.unregister();
                this.getGcmPreferences(this.context).edit().putString(PROPERTY_REG_ID, "");
            } catch (IOException e) {
                Log.d(TAG, "Could not unregister ");
                e.printStackTrace();
                return;
            }
        }

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
        Map<String, String> params = new HashMap<String, String>();

        try {
            params.put("hashedPass", this.sha1(password));
            params.put("gcmId", regid);
        } catch (NoSuchAlgorithmException e) {
            Log.d("ChromoServer", "SHA1 Failed");
            e.printStackTrace();
        }

        Payload p = new Payload(this.targetURLroot + registerString, Purpose.REGISTER);
        Log.d("Chromo Server", "Executing Async POST Request");
        Log.d("Chromo Server", "Parameters: " + params.get("hashedPass") + ", " + this.targetURLroot + registerString);
        //Log.d("Chromo Server", "GCM Registration ID: " + regid);

        try {
             new processPostRequest().execute(prepareSecurePostRequest(new JSONObject(params), this.targetURLroot + registerString));

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

        //Push Notification Toggled
        if(key.equals(PROPERTY_NOTIFICATIONS_ENABLED))
        {

        }
        else if(key.equals(PROPERTY_DEADAZONE_KEY))
        {

        }
        else if(key.equals(PROPERTY_RESOLUTION_KEY))
        {

        }
        else if(key.equals(PROPERTY_FRAMERATE_KEY))
        {

        }
        else if(key.equals(PROPERTY_DELAY_KEY))
        {

        }
        else if(key.equals(PROPERTY_PANEL_NUM))
        {

        }
        else if(key.equals(PROPERTY_IS_REGISTERED))
        {

        }




    }

    //standard post request
    private class processPostRequest extends AsyncTask<HttpPost, Void, String>
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
                    storeCredentials(context, uniqueToken, targetURLroot, deviceID);
                    Toast.makeText(currentActivity, "Registration Success!", Toast.LENGTH_LONG).show();
                    //Register Push Notifications on Registration Success
                    registerPushNotifcations();

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
        params.put("offset", Integer.toString((pageNo-1)*10));
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
        params.put("limit", "10");
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
     * <p>
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
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
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
    public JSONObject prepareSecureJSONAuth()
    {
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
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }



    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private void storeCredentials(Context context, String token, String target, String deviceID)
    {
        final SharedPreferences prefs = getSettingsPreferences(context);
        Log.d("Credentials", "Token and Target now stored for later use");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_TOKEN, token);
        editor.putString(PROPERTY_TARGET, target);
        editor.putString(PROPERTY_DEVICE_ID, deviceID);
        editor.commit();

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

    //Set SharedPreferences Value
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


    /**
     * @return Application's version code from the {@code PackageManager}.
     */
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

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    private SharedPreferences getSettingsPreferences(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getTargetURL () {
        return getSharedPrefInfoString(PROPERTY_TARGET,getSettingsPreferences(context));
    }

    public String getDeviceID(){
        return getSharedPrefInfo(context, PROPERTY_DEVICE_ID, getSettingsPreferences(context));
    }

    public String getUniqueToken(){
        return getSharedPrefInfo(context, PROPERTY_TOKEN, getSettingsPreferences(context));
    }
}

