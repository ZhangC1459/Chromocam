package com.chromocam.chromocam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import com.chromocam.chromocam.MjpegRunnable;

public class LivestreamActivity extends Activity {

    // Declare variables
    ProgressDialog pDialog;
    ImageView mjpegView;
    Thread connection;

    // Insert your Video URL
    String videoURL = "http://192.168.1.16:3000/stream2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the layout from video_main.xml
        setContentView(R.layout.livestreamvideo);

        // Find your VideoView in your video_main.xml layout
        mjpegView = (ImageView) findViewById(R.id.mjpegView);

        try {
            this.connection = new Thread(new MjpegRunnable(mjpegView, videoURL));
            this.connection.start ();
        } catch (IOException e) {
            Log.d("Stream ERROR", "Stream Lost Connection!");
        }

        // Execute StreamVideo AsyncTask

//        // Create a progressbar
//        pDialog = new ProgressDialog(LivestreamActivity.this);
//        // Set progressbar title
//        pDialog.setTitle("Downy Studios");
//        // Set progressbar message
//        pDialog.setMessage("Buffering...");
//        pDialog.setIndeterminate(false);
//        pDialog.setCancelable(false);
//        // Show progressbar
//        pDialog.show();

    }
    @Override
    public void onBackPressed()
    {
        //If Top level fragment, show action bar
        super.onBackPressed();

        Log.d("BACK-PRESSED-DEBUG", "Livestream Stopping");
        //this.actionBar.show();

    }
}

