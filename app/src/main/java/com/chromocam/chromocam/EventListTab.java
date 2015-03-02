package com.chromocam.chromocam;

import android.app.Activity;
import android.os.Bundle;
import android.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


public class EventListTab extends ListFragment {


    private OnFragmentInteractionListener mListener;
    private ArrayList eventItemList;
    private EventListAdapter mAdapter;

    public static EventListTab newInstance() {
        EventListTab fragment = new EventListTab();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public EventListTab() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        eventItemList = new ArrayList();
        eventItemList.add(new EventItem("03/01/2015 7:03PM", 1));
        eventItemList.add(new EventItem("03/01/2015 7:03PM", 2));
        eventItemList.add(new EventItem("03/01/2015 7:03PM", 3));
        mAdapter = new EventListAdapter(getActivity(), eventItemList);
        //setListAdapter(mAdapter);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
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

        EventItem item = (EventItem)this.eventItemList.get(position);
        Toast.makeText(getActivity(), "Clicked Event ID: " + item.getEventID(), Toast.LENGTH_LONG).show();
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

}
