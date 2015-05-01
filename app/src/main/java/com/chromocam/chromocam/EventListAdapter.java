package com.chromocam.chromocam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.app.Activity;

import com.chromocam.chromocam.util.ChromoServer;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class EventListAdapter extends ArrayAdapter<EventContent> {

    public ChromoServer serv;

    public EventListAdapter(Context context, ArrayList<EventContent> items, ChromoServer serv) {
        super(context, R.layout.item_list, items);
        this.serv = serv;
    }

    private class ViewHolder{ //holder object to reduce findViewByID calls
        TextView imageID;
        TextView dateTime;
        ImageView image;
        ProgressBar bar;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        EventContent item = getItem(position);
        final ViewHolder holder = new ViewHolder();
        View viewToUse = null;
        // If there is no view being reused
        if(convertView == null) {
            viewToUse = LayoutInflater.from(getContext()).inflate(R.layout.item_list, parent, false);
        } else {viewToUse = convertView;}
        holder.imageID = (TextView) viewToUse.findViewById(R.id.eventID);
        holder.dateTime = (TextView) viewToUse.findViewById((R.id.dateTime));
        holder.image = (ImageView) viewToUse.findViewById(R.id.thumbnail);
        holder.bar = (ProgressBar) viewToUse.findViewById(R.id.loader);
        viewToUse.setTag(holder);
        //Fill views with data
        holder.imageID.setText(item.getImageID());
        holder.dateTime.setText(item.getTime() + " - " + item.getDate());
        try {
            new AsyncTask<EventContent, Void, Bitmap>() {
                EventContent item;

                @Override
                protected Bitmap doInBackground (EventContent...item){
                    this.item = item[0];
                    try {
                        JSONObject postData = serv.prepareSecureJSONAuth();
                        HttpPost post = serv.prepareSecurePostRequest(postData, serv.getTargetURL() + "/files/" + this.item.getImageID());
                        HttpClient client = new DefaultHttpClient(post.getParams());
                        HttpResponse response = client.execute(post);
                        InputStream is = response.getEntity().getContent();
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
                protected void onPostExecute (Bitmap result){
                    holder.bar.setVisibility(View.GONE);
                    item.setImage(result);
                    holder.image.setImageBitmap(result);
                    holder.image.setVisibility(View.VISIBLE);
                }
            }.execute(item);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return viewToUse;

    }

}
