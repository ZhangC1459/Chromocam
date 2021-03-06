package com.chromocam.chromocam.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.chromocam.chromocam.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

public class DisplayPictureActivity extends Activity {
    private int type = 1;
    private String imageID = "0";
    private Menu menu;
    private Bitmap image = null;
    private String deviceID;
    private String token;
    private String targetURL;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_picture);
        //get intent data
        type = getIntent().getIntExtra("calling", 1); //Determines the type.  1 is Event, 2 is Archive
        imageID = getIntent().getStringExtra("imageID");
        byte[] b = getIntent().getByteArrayExtra("image");
        image = BitmapFactory.decodeByteArray(b, 0, b.length);
        this.deviceID = getIntent().getStringExtra("deviceID");
        this.token = getIntent().getStringExtra("token");
        this.targetURL = getIntent().getStringExtra("targetURL");
        ImageView imageView = (ImageView) findViewById(R.id.full_image_view);
        imageView.setImageBitmap(image);
    }

    public void finishedPushDownload(){
        ImageView imageView = (ImageView) findViewById(R.id.full_image_view);
        imageView.setImageBitmap(image);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu=menu;
        getMenuInflater().inflate(R.menu.menu_display_picture, menu);
        if(type==1){ //if the calling fragment is the Event fragment
            hideOption(R.id.action_unarchive);
            showOption(R.id.action_archive);
        } else if(type ==2 ){ //else if it's the Archive fragment
            showOption(R.id.action_unarchive);
            hideOption(R.id.action_archive);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_save){
            FileOutputStream out = null;
            File sd = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsoluteFile(), imageID + ".png");
                if(!sd.exists()){
                    try{
                        sd.createNewFile();
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
            try{
                if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
                    out = new FileOutputStream(sd);
                    image.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                    Toast.makeText(getApplicationContext(), "Saved to SD", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "No External Storage detected", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (id == R.id.action_archive || id == R.id.action_unarchive) {
            JSONObject keyIDPair = new JSONObject();
            try {
                //passes the device id and the token
                keyIDPair.put("id", deviceID);
                keyIDPair.put("token", token);
                //if statements depending on whether we're archiving or not
                if(id == R.id.action_archive){
                    keyIDPair.put("archive", "1");
                }
                if(id == R.id.action_unarchive){
                    keyIDPair.put("archive", "0");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Make the actual request via an asynctask
            UploadAsyncTask upload = new UploadAsyncTask();
            upload.execute(keyIDPair);

        }

        return super.onOptionsItemSelected(item);
    }

    private void hideOption(int id){
        MenuItem item = menu.findItem(id);
        item.setVisible(false);
    }

    private void showOption(int id){
        MenuItem item = menu.findItem(id);
        item.setVisible(true);
    }

    private class UploadAsyncTask extends AsyncTask<JSONObject, Void, String>{
        @Override
        protected String doInBackground(JSONObject...keyIDPair){
            try{
                String imgUrl = targetURL + "/files/" + imageID + "/setArchive";
                //create an HTTPClient and post header
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(imgUrl);
                httpPost.setHeader("Content-Type", "application/json; charset=utf-8");
                //Pass the data to POST
                StringEntity se = new StringEntity(keyIDPair[0].toString());
                se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setEntity(se);
                //Execute
                Log.d("TEST", "Sending: " + se);
                HttpResponse response = httpClient.execute(httpPost);
                InputStream in = response.getEntity().getContent();
                BufferedReader sr = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                StringBuilder res = new StringBuilder();
                String inputStr;
                while((inputStr = sr.readLine()) != null)
                    res.append(inputStr);
                Log.d("ChromoServer POST", res.toString());
                if(res.toString().contains("1")){
                    return "Success";
                } else {
                    return "Error";
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        return null;
        }

        protected void onPostExecute(String result){
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
        }
    }
    private class getFileTask extends AsyncTask<String, Void, Bitmap>{
        @Override
        protected Bitmap doInBackground(String... item) {
            String url = item[0];
            try {
                InputStream is = (InputStream) new URL(url).getContent();
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
        //After execution, the event item's image is updated to contain the result
        protected void onPostExecute(Bitmap result){
            image = result;
            finishedPushDownload();
        }

    }
}

