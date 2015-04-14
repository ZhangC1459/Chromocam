package com.chromocam.chromocam;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.chromocam.chromocam.util.DisplayPictureActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class ArchiveListTab extends ListFragment {
    private static final String param1 = null;
    private OnEventSelectionListener mListener;
    private EventContent content = new EventContent(1);

    public static ArchiveListTab newInstance(String param) {
        ArchiveListTab fragment = new ArchiveListTab();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArchiveListTab() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //sets adapter to display content using a custom adapter with a custom list view
        EventListAdapter mAdapter = new EventListAdapter(getActivity(), content.ITEMS);

        setListAdapter(mAdapter);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnEventSelectionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        EventContent.EventItem item = content.ITEMS.get(position);
        //Intent to call DisplayPictureActivity
        Intent intent = new Intent(this.getActivity(), DisplayPictureActivity.class);
        //Encodes the bitmap into an array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            item.getImage().compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] b = stream.toByteArray();
            //Adds the bitmap to the intent
            intent.putExtra("image", b);
            //Adds the calling fragment type to the intent
            intent.putExtra("calling", 2);
            //Adds the image id for archive/unarchive purposes to the intent
            intent.putExtra("imageID", item.getImageID());
            //starts the activity
            startActivity(intent);
        } catch (NullPointerException e){
            Toast.makeText(this.getActivity(), "ERROR: unable to load image", Toast.LENGTH_LONG).show();
            Log.d("SERVER-ERROR", "Error: unable to load image.  Is the server up?");
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnEventSelectionListener {
        public void onEventSelection(EventContent.EventItem item);
    }

}
