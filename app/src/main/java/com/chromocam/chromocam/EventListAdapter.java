package com.chromocam.chromocam;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import java.util.List;

public class EventListAdapter extends ArrayAdapter<EventContent> {

    public EventListAdapter(Context context, List<EventContent> items) {
        super(context, R.layout.event_item_list, items);
    }

    private class ViewHolder{ //holder object to reduce findViewByID calls
        TextView imageID;
        TextView dateTime;
        ImageView image;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        EventContent item = (EventContent)getItem(position);
        ViewHolder holder = null;
        View viewToUse = null;
        // If there is no view being reused
        if(convertView == null) {
            viewToUse = LayoutInflater.from(getContext()).inflate(R.layout.event_item_list, parent, false);
        } else {viewToUse = convertView;}

        holder = new ViewHolder();
        holder.imageID = (TextView) viewToUse.findViewById(R.id.eventID);
        holder.dateTime = (TextView) viewToUse.findViewById((R.id.dateTime));
        holder.image = (ImageView) viewToUse.findViewById(R.id.thumbnail);
        viewToUse.setTag(holder);
        //Fill views with data
        holder.imageID.setText(item.getImageID());
        holder.dateTime.setText(item.getTime() + " - " + item.getDate());
        holder.image.setImageBitmap(item.getImage());

        return viewToUse;

    }
}
