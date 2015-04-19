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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LivestreamActivity extends Activity {

    // Declare variables
    ProgressDialog pDialog;
    ImageView mjpegView;
    Thread connection;
    MJPEGasyncTask task;
    // Insert your Video URL
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
        //this.connection.get
        this.task.stop();
        super.onBackPressed();

        Log.d("BACK-PRESSED-DEBUG", "Livestream Stopping");
        //this.actionBar.show();

    }

    private void updateStreamImage(Bitmap image)
    {
        this.mjpegView.setImageBitmap(image);
    }

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

    //Asynchronusly Update View
    private class MJPEGasyncTask extends AsyncTask<MJPEG, Void, Bitmap> {
        private static final String CONTENT_LENGTH = "Content-length:";

        private static final String CONTENT_TYPE = "Content-type: image/jpeg";

        MJPEG mjpeg = null;
        //private MJpegViewer viewer;
        private ImageView streamImage;
        private String urlString;
        private InputStream urlStream;
        private StringBuilder stringWriter;
        private boolean processing = true;
        private Bitmap image;


        public Bitmap getImage()
        {    return this.image;     }

        @Override
        protected Bitmap doInBackground(MJPEG... streams) {
            Log.d("Livestream", "Background Task getting image");
            this.mjpeg = streams[0];
            this.streamImage = mjpeg.streamView;
            this.stringWriter = new StringBuilder();
            //InputStream in = mjpeg.streamSource.openConnection().getInputStream();
            try {
                this.urlStream = mjpeg.streamSource.openStream();

                //InputStream is = (InputStream) this.mjpeg.streamSource.getContent();
                //return BitmapFactory.decodeStream(is);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }


            while(processing)
            {
                Log.d("MJPEG RUNNABLE", "Processing");
                try
                {
                    byte[] b = retrieveNextImage();
                    Bitmap image = BitmapFactory.decodeByteArray(b, 0, b.length);
                    Log.d("MJPEG Image", image.toString());
                    if(!image.toString().equals("Can't read")) {
                        this.image = image;
                        this.mjpeg.livestream.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // This code will always run on the UI thread, therefore is safe to modify UI elements.
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
            // close streams
            Log.d("MJPEG RUNNABLE", "Closing");
            try {
                this.urlStream.close();
            } catch (IOException ioe) {
                Log.d("MJPEG RUNNABLE", "Failed to close the stream: " + ioe);
            }



            return null;
        }

        public void stop()
        {   processing = false;}

        /**
         * Using the <i>urlStream</i> get the next JPEG image as a byte[]
         * @return byte[] of the JPEG
         * @throws java.io.IOException
         */
        private byte[] retrieveNextImage() throws IOException, NullPointerException
        {
            Log.d("MJPEG RUNNABLE", "Retrieving Next Image");
            boolean haveHeader = false;
            boolean contentLenNumFound = false;
            String tempString;
            int contentLength = 0;
            ArrayList headerbytes = new ArrayList<Integer>();

            String pattern = "Content-Length:\\s+([0-9]+)";
            Pattern lengthPat = Pattern.compile(pattern);

            int currByte = -1;
            int prevByte = -1;
            String header = null;
            // build headers
            // the DCS-930L stops it's headers
            while((currByte = urlStream.read()) > -1 && haveHeader == false)
            {

                //255 Notifies beginning of JPEG
                if(currByte != 0xe0 && prevByte !=0xff)
                {   stringWriter.append((char)currByte);
                    headerbytes.add(currByte);
                }
                else
                {
                    headerbytes.add(currByte);
                    header = stringWriter.toString();
                    Matcher m = lengthPat.matcher(header);
                    Log.d("MJPEG Header", "Header: " + header);
                    if(m.find())
                    {
                        Log.d("MJPEG Header", m.group(1));
                        contentLength = Integer.parseInt(m.group(1));
                        haveHeader = true;
                    }
                }

                prevByte = currByte;
            }
            Log.d("MJPEG RUNNABLE", "Header: " + header);
            // 255 indicates the start of the jpeg image
            boolean haveImage = false;


            contentLength -= 1;
            Log.d("MJPEG RUNNABLE", "Content Length:" + contentLength);
            byte[] imageBytes = new byte[contentLength + 1];
            // since we ate the original 255 , shove it back in
//            imageBytes[0] = (byte)255;
//            int offset = 1;
//            while(haveImage == false && (currByte = urlStream.read()) > -1)
//            {
//                imageBytes[offset] = currByte;
//            }


            imageBytes[0] = (byte) 0xff;
            imageBytes[1] = (byte) 0xd8;
            imageBytes[2] = (byte) 0xff;

            int numRead = 0;
            numRead=urlStream.read(imageBytes, 3, imageBytes.length-3);

//            while (offset < imageBytes.length && (numRead=urlStream.read(imageBytes, offset, imageBytes.length-offset)) >= 0) {
//                offset += numRead;
//            }

            stringWriter.setLength(0);

            return imageBytes;
        }

        // dirty but it works content-length parsing
        private int contentLength(String header)
        {
            int indexOfContentLength = header.indexOf(CONTENT_LENGTH);
            int valueStartPos = indexOfContentLength + CONTENT_LENGTH.length();
            int indexOfEOL = header.indexOf(' ', indexOfContentLength);

            String lengthValStr = header.substring(valueStartPos, indexOfEOL).trim();

            int retValue = Integer.parseInt(lengthValStr);

            return retValue;
        }
    }
}

