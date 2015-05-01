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

public class ChromoServer{
    /**
     * Tag used on log messages.
     */
    static final String TAG = "Chromocam";

    //Push Variables
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    private static final String PROPERTY_IS_REGISTERED = "login_settings";
    private static final String PROPERTY_TOKEN = "token";
    private static final String PROPERTY_TARGET = "target";
    private static final String PROPERTY_DEVICE_ID = "device_id";

    String regid;

    TextView mDisplay;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    Context context;

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "1026539547295";

    private ProgressDialog progressDialog;
    private Activity currentActivity;

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

        Log.d("Chromo Server", "Current token:" + getSharedPrefInfo(context, PROPERTY_TOKEN,getSettingsPreferences(this.context)));
        Log.d("Chromo Server", "Current target:" + getSharedPrefInfo(context, PROPERTY_TARGET,getSettingsPreferences(this.context)));
        Log.d("Chromo Server", "Current device id:" + getSharedPrefInfo(context, PROPERTY_DEVICE_ID,getSettingsPreferences(this.context)));

        if(this.getSharedPrefInfo(this.context, PROPERTY_REG_ID,getGcmPreferences(this.context)).isEmpty())
        {
            Log.d("GCM Push Reg", "Starting GCM Push Registration");
            this.GCMregisterInBackground();
        }

        //Check for Preset Information
        else if(getSettingsPreferences(this.context).getBoolean(PROPERTY_IS_REGISTERED, false))
        {
            Log.d("Chromo Server", "ChromoServer Registration Information Found");
            Log.d("Chromo Server", "Current token:" + getSharedPrefInfo(context, PROPERTY_TOKEN,getSettingsPreferences(this.context)));
            Log.d("Chromo Server", "Current target:" + getSharedPrefInfo(context, PROPERTY_TARGET,getSettingsPreferences(this.context)));
            Log.d("Chromo Server", "Current device id:" + getSharedPrefInfo(context, PROPERTY_DEVICE_ID,getSettingsPreferences(this.context)));

            Payload registered = new Payload(null, Purpose.REGISTERED);
            registered.setResult(true);

            if(currentActivity instanceof ChromoComplete){
                ((ChromoComplete) currentActivity).onTaskCompleted(registered);
            }

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
        this.regid = this.getSharedPrefInfo(this.context, PROPERTY_REG_ID,getGcmPreferences(this.context));

        if(regid.isEmpty())
        {
            Log.d("ChromoServer Reg", "Push Reg ID Not found");
            return;
        }

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
        Log.d("Chromo Server", "GCM Registration ID: " + regid);

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

            }.execute(prepareSecurePostRequest(JSONpost, getSharedPrefInfo(context, PROPERTY_TARGET,getSettingsPreferences(this.context)) + "/files"), null, null);
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

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    //sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
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
        params.put("id", getSharedPrefInfo(context, PROPERTY_DEVICE_ID,getSettingsPreferences(this.context)));
        params.put("token", getSharedPrefInfo(context, PROPERTY_TOKEN,getSettingsPreferences(this.context)));
        return new JSONObject(params);
    }
    public JSONObject prepareSecureJSONAuth()
    {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("id", getSharedPrefInfo(context, PROPERTY_DEVICE_ID,getSettingsPreferences(this.context)));
        params.put("token", getSharedPrefInfo(context, PROPERTY_TOKEN,getSettingsPreferences(this.context)));
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
        editor.putBoolean(PROPERTY_IS_REGISTERED, true);

        editor.commit();

    }

    private String getSharedPrefInfo(Context context, String sharedPrefName, SharedPreferences sharedPref)
    {
        final SharedPreferences prefs = sharedPref;
        String result = prefs.getString(sharedPrefName, "");
        if (result.isEmpty()) {
            Log.i(TAG, sharedPrefName +" not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = getGcmPreferences(context).getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return result;
    }

     /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
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
        return getSharedPrefInfo(context, PROPERTY_TARGET,getSettingsPreferences(context));
    }
}

