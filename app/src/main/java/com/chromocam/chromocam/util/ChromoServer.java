package com.chromocam.chromocam.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.chromocam.chromocam.MainActivity;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
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
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChromoServer{
    private ProgressDialog progressDialog;
    private Activity currentActivity;
    //Registration Parameters
    private String uniqueToken;
    private String targetURLroot;

    private URL targetURL;

    private boolean connectedToServer;
    //Async Information
    protected String target;

    protected JSONObject payload;

    //Instantiation
    public void initChromoServer(String targetURLroot, String password, Activity current)
    {
        Log.d("Chromo Server", "Initializing ChromoServer Connection");
        this.currentActivity = current;
        this.targetURLroot = targetURLroot;
        this.registerDevice(password);
        Log.d("Chromo Server", "Returning connection status");
    }

    public void setActivity(Activity x){
        this.currentActivity = x;
    }




    //Register Device to Server
    private void registerDevice(String password)
    {
        String registerString = "/devices/register";
        Map<String, String> params = new HashMap<String, String>();

        try {
            params.put("hashedPass", this.sha1(password));
        } catch (NoSuchAlgorithmException e) {
            Log.d("ChromoServer", "SHA1 Failed");
            e.printStackTrace();
        }

        Payload p = new Payload(new JSONObject(params), this.targetURLroot + registerString, Purpose.REGISTER);
        Log.d("Chromo Server", "Executing Async POST Request");
        Log.d("Chromo Server", "Parameters: " + params.get("hashedPass") + ", " + this.targetURLroot + registerString);
        new processPostRequest().execute(p);
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
    private class processPostRequest extends AsyncTask<Payload, Void, String>
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
        protected String doInBackground(Payload... params) {
            p = params[0];
            //Prepare Connection
            int TIMEOUT_MILLISEC = 10000;  // = 10 seconds
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
            HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
            HttpClient client = new DefaultHttpClient(httpParams);
            HttpPost request = new HttpPost(p.getURL()); //sets URL for POST request
            request.setHeader("Content-Type", "application/json; charset=utf-8"); //Sets content type header
            HttpResponse response;
            try {
                StringEntity se = new StringEntity(p.getPost().toString()); //turns json to string
                se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json")); //encodes as "json type"
                request.setEntity(se); //sets entity
                response = client.execute(request);
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
        protected void onPostExecute(String response){
            progressDialog.dismiss();
            if(response.toUpperCase().contains("FORBIDDEN")){
                Toast.makeText(currentActivity, "Registration failed!", Toast.LENGTH_LONG).show();
                p.setResult(false);
            } else {
                try {
                    JSONArray x = new JSONArray(response);
                    JSONObject res = x.getJSONObject(0);
                    uniqueToken = res.getString("token");
                    p.setResult(true);
                    Toast.makeText(currentActivity, "Registration Success!", Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    Log.d("ChromoServ Error", "Bad JSON");
                }
            }
            if(currentActivity instanceof MainActivity){
                ((MainActivity) currentActivity).onTaskCompleted(p);
            }
        }
    }

}
enum Purpose{
    REGISTER,
    GET_FILE_LIST,
    GET_SPECIFIC_FILE,
    SETTINGS

}
