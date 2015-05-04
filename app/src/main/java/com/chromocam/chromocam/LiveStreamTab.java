package com.chromocam.chromocam;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.Fragment;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;

import com.chromocam.chromocam.util.ChromoServer;

public class LiveStreamTab extends Fragment {

    String targetURL = "http://192.168.1.16:3000/stream2";
    Button button;
    View rootView;
    ChromoServer serv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.livestream, container, false);
        serv = ((MainActivity) getActivity()).getServ();
        // Locate the button in activity_main.xml
        button = (Button) rootView.findViewById(R.id.livestreamButton);

        // Capture button clicks
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {

                // Start NewActivity.class
                Intent myIntent = new Intent(arg0.getContext(), LivestreamActivity.class);
                myIntent.putExtra("ID", serv.getDeviceID());
                myIntent.putExtra("token", serv.getUniqueToken());
                myIntent.putExtra("targetURL", serv.getTargetURL());
                startActivity(myIntent);
            }
        });

        return rootView;
    }

}