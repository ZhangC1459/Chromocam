package com.chromocam.chromocam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

public class LivestreamActivity extends Activity {

    // Declare variables
    private ProgressDialog progressDialog;
    ImageView mjpegView;
    MJPEGasyncTask task;

    // Stream Source
    String videoURL = "http://192.168.1.16:3000/stream2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the layout from video_main.xml
        setContentView(R.layout.livestreamvideo);

        // Find your VideoView in your video_main.xml layout
        this.mjpegView = (ImageView) findViewById(R.id.mjpegView);

        try {
            Log.d("Livestream", "Initializing MJPEG Stream");
            MJPEG mjpeg = new MJPEG(mjpegView, new URL(videoURL), this);
            //this.connection = new Thread(new MjpegRunnable(mjpegView, videoURL));
            this.task = new MJPEGasyncTask();
            task.execute(mjpeg);

        } catch (IOException e) {
            Log.d("Stream ERROR", "Stream Lost Connection!");
        }

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
        private ArrayList<byte[]> imageListBuffer;
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
            this.imageListBuffer = new ArrayList<byte[]>();

            //Load MJPEG
            this.mjpeg = streams[0];

            //Open Connection to Server MJPEG Stream
            try {
                this.urlStream = mjpeg.streamSource.openStream();
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
                    this.imageListBuffer.add(tempImage);
                    return tempImage;
                }

                prevByte = currByte;
            }

            return null;
        }
    }
}

