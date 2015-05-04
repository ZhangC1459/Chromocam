package com.chromocam.chromocam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

public class LivestreamActivity extends Activity {

    // Declare variables
    private ProgressDialog progressDialog;
    private String deviceID;
    private String uniqueToken;
    private String targetURL;
    ImageView mjpegView;
    Button btn_snap;
    MJPEGasyncTask task;

    // Stream Source
    String videoURL = "https:/chromocam.co:3000/stream2";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the layout from video_main.xml
        setContentView(R.layout.livestreamvideo);
        this.deviceID = getIntent().getExtras().getString("ID");
        this.uniqueToken = getIntent().getExtras().getString("token");
        this.targetURL = getIntent().getExtras().getString("targetURL");

        // Find your VideoView in your video_main.xml layout
        this.mjpegView = (ImageView) findViewById(R.id.mjpegView);
        this.btn_snap = (Button) findViewById(R.id.snapshot);

        btn_snap.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                try{
                    new AsyncTask<Void, Void, String>() {

                        @Override
                        protected String doInBackground (Void...params){
                            try {
                                JSONObject json = new JSONObject();
                                json.put("id", deviceID);
                                json.put("token", uniqueToken);
                                int TIMEOUT_MILLISEC = 10000;  // = 10 seconds
                                HttpParams httpParams = new BasicHttpParams();
                                HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
                                HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
                                HttpPost request = new HttpPost(targetURL + "/motion/snapshot"); //sets URL for POST request
                                request.setHeader("Content-Type", "application/json; charset=utf-8"); //Sets content type header
                                StringEntity se = new StringEntity(json.toString()); //turns json to string
                                se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json")); //encodes as "json type"
                                request.setEntity(se); //sets entity
                                HttpClient client = new DefaultHttpClient(request.getParams());
                                HttpResponse response = client.execute(request);
                                InputStream in = response.getEntity().getContent();
                                BufferedReader sr = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                                StringBuilder res = new StringBuilder();
                                String inputStr;
                                while((inputStr = sr.readLine()) != null)
                                    res.append(inputStr);
                                Log.d("ChromoServer POST", res.toString());
                                return res.toString();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                        //After execution, make toast
                        protected void onPostExecute (String result){
                            if(result.contains("true")){
                                makingToast("Success!");
                            } else {
                                makingToast("Error");
                            }
                        }
                    }.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            Log.d("Livestream", "Initializing MJPEG Stream");
            MJPEG mjpeg = new MJPEG(mjpegView, new URL(targetURL + "/stream"), this);
            //this.connection = new Thread(new MjpegRunnable(mjpegView, videoURL));
            this.task = new MJPEGasyncTask();
            task.execute(mjpeg);

        } catch (IOException e) {
            Log.d("Stream ERROR", "Stream Lost Connection!");
        }

    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item){
        int itemId = item.getItemId();
        switch(itemId){
            case android.R.id.home:
                this.onBackPressed();
                break;
        }
        return true;
    }
    @Override
    public void onBackPressed()
    {
        //Stop Stream
        this.progressDialog.dismiss();
        this.task.stop();
        //Previous Activity
        super.onBackPressed();


        Log.d("BACK-PRESSED-DEBUG", "Livestream Stopping");
    }
    public void makingToast(String status){
        Toast.makeText(this, status, Toast.LENGTH_LONG).show();
    }
    //MJPEG Convenience Class
    private class MJPEG {
        //Variables
        private ImageView streamView;
        private URL streamSource;
        private LivestreamActivity livestream;

        MJPEG (ImageView streamView, URL streamSource, LivestreamActivity livestream){
            this.streamView = streamView;
            this.streamSource = streamSource;
            this.livestream = livestream;
        }
    }

    //Asynchronously Update View
    private class MJPEGasyncTask extends AsyncTask<MJPEG, Void, Bitmap> {

        //Parent Activity Info
        MJPEG mjpeg = null;
        private InputStream urlStream;
        private boolean processing = true;

        //Byte Buffers
        private int byteBufferSize = 50000;

        //Image Buffer
        private byte[] byteBuffer;
        private Bitmap image;

        public Bitmap getImage()
        {    return this.image;     }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            progressDialog = new ProgressDialog(LivestreamActivity.this);
            progressDialog.setCancelable(true);
            progressDialog.setMessage("Loading Livestream...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setProgress(0);
            progressDialog.show();
        }



        @Override
        protected Bitmap doInBackground(MJPEG... streams) {
            Log.d("Livestream", "Background Task getting image");

            //Initialize Byte Buffer, Image Buffer
            this.byteBuffer = new byte[byteBufferSize];

            //Load MJPEG
            this.mjpeg = streams[0];

            //Open Connection to Server MJPEG Stream
            try {
                //this.urlStream = mjpeg.streamSource.openStream();

                JSONObject json = new JSONObject();
                json.put("id", deviceID);
                json.put("token", uniqueToken);
                int TIMEOUT_MILLISEC = 10000;  // = 10 seconds
                HttpParams httpParams = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
                HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
                HttpPost request = new HttpPost(mjpeg.streamSource.toURI()); //sets URL for POST request
                request.setHeader("Content-Type", "application/json; charset=utf-8"); //Sets content type header
                StringEntity se = new StringEntity(json.toString()); //turns json to string
                //se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json")); //encodes as "json type"
                request.setEntity(se); //sets entity
                HttpClient client = new DefaultHttpClient(request.getParams());
                HttpResponse response = client.execute(request);
                InputStream in = response.getEntity().getContent();

                this.urlStream = in;
















            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Background Processing, constantly change UI image
            while(processing)
            {
                Log.d("MJPEG RUNNABLE", "Processing");
                try
                {
                    //Get JPEG from Stream
                    byte[] b = retrieveNextImage();

                    //Convert JPEG Byte Array to Bitmap
                    Bitmap image = BitmapFactory.decodeByteArray(b, 0, b.length);

                    //Set imageView to Bitmap Image if valid
                    Log.d("MJPEG Image", image.toString());
                    if(!image.toString().equals("Can't read")) {
                        this.image = image;
                        this.mjpeg.livestream.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                //Hide Progress Bar
                                if(progressDialog.isShowing())
                                {   progressDialog.hide();  }

                                //Set UI Image
                                mjpegView.setImageBitmap(task.getImage());
                            }

                        });
                    }
                }catch(SocketTimeoutException ste){
                    Log.d("MJPEG RUNNABLE", "failed stream read: " + ste);
                    stop();
                }catch(IOException e){
                    Log.d("MJPEG RUNNABLE", "failed stream read: " + e);
                    stop();
                }catch(NullPointerException e)
                {
                    Log.d("MJPEG RUNNABLE", "Can't read");
                }
            }

            // Close Streams
            Log.d("MJPEG RUNNABLE", "Closing");
            try {
                this.urlStream.close();
            } catch (IOException ioe) {
                Log.d("MJPEG RUNNABLE", "Failed to close the stream: " + ioe);
            }



            return null;
        }

        //End Background Process
        public void stop()
        {
            processing = false;
            this.cancel(true);
        }


        /**
         * Using the <i>urlStream</i> get the next JPEG image as a byte[]
         * @return byte[] of the JPEG
         * @throws java.io.IOException
         */
        private byte[] retrieveNextImage() throws IOException, NullPointerException
        {
            Log.d("MJPEG RUNNABLE", "Retrieving Next Image");

            //JPEG Format: http://www.onicos.com/staff/iz/formats/jpeg.html
            //Beginning of File
                //0xff
                //0xd8
                //0xff
                //0xe0
            //End of File
                //0xff
                //0xd9

            boolean startImage = false;
            int startPosition = 0;

            boolean endImage = false;
            int endPosition = 0;

            boolean haveImage = false;
            int byteCounter = 0;
            int currByte = -1;
            int prevByte = -1;

            //Start Scanning Input Stream
            while((currByte = urlStream.read()) > -1 && haveImage == false)
            {
                byteCounter++;
                this.byteBuffer[byteCounter] = (byte)currByte;

                //Notifies beginning of JPEG
                if(currByte == 0xe0 && prevByte ==0xff && startImage == false)
                {
                    startImage = true;
                    startPosition = byteCounter - 3;

                }

                //End of JPEG
                if(currByte == 0xd9 && prevByte == 0xff && startImage == true)
                {
                    endImage = true;
                    endPosition = byteCounter;
                }

                if(endImage)
                {
                    haveImage = true;
                    int imageLength = endPosition - startPosition + 1;
                    byte[] tempImage = new byte[imageLength];
                    System.arraycopy(this.byteBuffer, startPosition, tempImage, 0, imageLength);
                    return tempImage;
                }

                prevByte = currByte;
            }

            return null;
        }
    }
}

