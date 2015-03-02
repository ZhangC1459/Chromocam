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

public class EventListAdapter extends ArrayAdapter {

    private Context context;
    private boolean useList = true;

    public EventListAdapter(Context context, List items) {
        super(context, android.R.layout.simple_list_item_1, items);
        this.context = context;
    }

    /**
     * Holder for the list items.
     */
    private class ViewHolder{
        TextView ID;
        TextView dateTime;
        //ImageView thumbnail;
    }

    /**
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        EventItem item = (EventItem)getItem(position);
        View viewToUse = null;

        // This block exists to inflate the settings list item conditionally based on whether
        // we want to support a grid or list view.
        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if(convertView == null) {
            viewToUse = mInflater.inflate(R.layout.event_item_list, null);
            holder = new ViewHolder();
            holder.ID = (TextView) viewToUse.findViewById(R.id.ID);
            holder.dateTime = (TextView) viewToUse.findViewById(R.id.dateTime);
            //holder.thumbnail = (ImageView)viewToUse.findViewById(R.id.thumbnail);
            viewToUse.setTag(holder);
        } else {
            viewToUse = convertView;
            holder = (ViewHolder) viewToUse.getTag();
        }

        holder.ID.setText(item.getEventID());
        holder.dateTime.setText(item.getEventID());
        //holder.thumbnail.setImageURI(item.getImageURI());
        return viewToUse;
    }
}
