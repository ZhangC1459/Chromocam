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

public class EventListAdapter extends ArrayAdapter<EventContent.EventItem> {

    public EventListAdapter(Context context, List<EventContent.EventItem> items) {
        super(context, R.layout.event_item_list, items);
    }


    public View getView(int position, View convertView, ViewGroup parent) {
        EventContent.EventItem item = (EventContent.EventItem)getItem(position);

        // If there is no view being reused
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.event_item_list, parent, false);
        }
        //Lookup view for data population
        TextView imageID = (TextView) convertView.findViewById(R.id.eventID);
        TextView dateAndTime = (TextView) convertView.findViewById((R.id.dateTime));
        ImageView image = (ImageView) convertView.findViewById(R.id.thumbnail);
        //Fill views with data
        imageID.setText(item.getImageID());
        dateAndTime.setText(item.getTime() + " - " + item.getDate());
        image.setImageDrawable(item.getImage());

        return convertView;

    }
}
