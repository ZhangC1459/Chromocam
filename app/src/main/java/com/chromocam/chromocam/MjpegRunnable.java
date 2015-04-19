package com.chromocam.chromocam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

public class MjpegRunnable implements Runnable{

    private static final String CONTENT_LENGTH = "Content-length: ";
    private static final String CONTENT_TYPE = "Content-type: image/jpeg";

    //private MJpegViewer viewer;
    private ImageView streamImage;
    private String urlString;
    private InputStream urlStream;
    private StringWriter stringWriter;
    private boolean processing = true;

    MjpegRunnable (ImageView view, String targetStreamAddress) throws IOException
    {
        Log.d("MJPEG RUNNABLE", "Initializing");
        this.streamImage = view;
        this.urlString = targetStreamAddress;

    }


    public void setProcessing(boolean set)
    {this.processing = set;}
    public void start()
    {

    }

    public void stop()
    {
        processing = false;
    }

    //Grabbing a new JPEG from stream constantly
    @Override
    public void run()
    {
        try
        {
            Log.d("MJPEG RUNNABLE", "Starting:" + this.urlString);

            URL mjpegURL = new URL(this.urlString);

            URLConnection urlConn = mjpegURL.openConnection();

            //Timeout Time
            urlConn.setReadTimeout(5000);
            urlConn.connect();

            this.urlStream = urlConn.getInputStream();
            stringWriter = new StringWriter(128);
        }
        catch (MalformedURLException e) {
            Log.d("MJPEG RUNNABLE", "Malformed URL");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("MJPEG RUNNABLE", "Could not Connect!");
            e.printStackTrace();
        }

        while(processing)
        {
            Log.d("MJPEG RUNNABLE", "Processing");
            try
            {
                byte[] b = retrieveNextImage();
                Bitmap image = BitmapFactory.decodeByteArray(b, 0, b.length);
                this.streamImage.setImageBitmap(image);
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


    }

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


        int currByte = -1;

        String header = null;
        // build headers
        // the DCS-930L stops it's headers
        while((currByte = urlStream.read()) > -1 && !haveHeader)
        {
            stringWriter.write(currByte);

            String tempString = stringWriter.toString();
            int indexOf = tempString.indexOf(CONTENT_LENGTH);
            if(indexOf > 0)
            {
                haveHeader = true;
                header = tempString;

//                while((currByte = urlStream.read()) > -1 && !contentLenNumFound)
//                {
//                    indexOf = tempString.indexOf(" ");
//                    if (indexOf > 0)
//                    {
//                        contentLenNumFound = true;
//                    }
//                }
            }
        }

        Log.d("MJPEG RUNNABLE", "Header: " + header);
        // 255 indicates the start of the jpeg image
        while((urlStream.read()) != 255)
        {
            // just skip extras
        }

        // rest is the buffer
        int contentLength = contentLength(header);
        Log.d("MJPEG RUNNABLE", "Content Length:" + contentLength);
        byte[] imageBytes = new byte[contentLength + 1];
        // since we ate the original 255 , shove it back in
        imageBytes[0] = (byte)255;
        int offset = 1;
        int numRead = 0;
        while (offset < imageBytes.length
                && (numRead=urlStream.read(imageBytes, offset, imageBytes.length-offset)) >= 0) {
            offset += numRead;
        }

        stringWriter = new StringWriter(128);

        return imageBytes;
    }

    // dirty but it works content-length parsing
    private static int contentLength(String header)
    {
        int indexOfContentLength = header.indexOf(CONTENT_LENGTH);
        int valueStartPos = indexOfContentLength + CONTENT_LENGTH.length();
        int indexOfEOL = header.indexOf(' ', indexOfContentLength);

        String lengthValStr = header.substring(valueStartPos, indexOfEOL).trim();

        int retValue = Integer.parseInt(lengthValStr);

        return retValue;
    }
}

