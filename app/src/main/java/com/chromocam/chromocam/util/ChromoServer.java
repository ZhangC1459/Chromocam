package com.chromocam.chromocam.util;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Aleks on 4/19/2015.
 */
public class ChromoServer{

    //Registration Parameters
    private String uniqueToken;
    private String targetURLroot;

    private URL targetURL;

    private boolean connectedToServer;

    //Async Information
    protected String target;
    protected JSONObject payload;

    //Instantiation
    public boolean initChromoServer(String targetURLroot, String password)
    {
        Log.d("Chromo Server", "Initializing ChromoServer Connection");
        this.targetURLroot = targetURLroot;
        this.connectedToServer = this.registerDevice(password);
        return this.connectedToServer;
    }


    //Register Device to Server
    private boolean registerDevice(String password)
    {
        String registerString = "/devices/register";
        Map<String, String> params = new HashMap<String, String>();

        try {
            params.put("hashedPass", this.sha1(password));
        } catch (NoSuchAlgorithmException e) {
            Log.d("ChromoServer", "SHA1 Failed");
            e.printStackTrace();
            return false;
        }

        return this.processPostRequest(new JSONObject(params), this.targetURLroot + registerString);
    }





    //Processes Post request to Chromo Server
    boolean processPostRequest(JSONObject jsonObj, String target) {
        //Prepare Connection
        int TIMEOUT_MILLISEC = 10000;  // = 10 seconds
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
        HttpClient client = new DefaultHttpClient(httpParams);

        HttpPost request = new HttpPost(target);
        HttpResponse response;
        try {
            request.setEntity(new ByteArrayEntity(jsonObj.toString().getBytes("UTF-8")));
            response = client.execute(request);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Log.d("ChromServer POST", response.toString());
        return true;

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

    private class ChromoServerExec extends AsyncTask<ChromoServer, Void, Void>
    {

        @Override
        protected Void doInBackground(ChromoServer... params) {
            return null;
        }
    }
}
