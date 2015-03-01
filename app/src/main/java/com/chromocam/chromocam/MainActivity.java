package com.chromocam.chromocam;

import android.app.ActionBar;
import android.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {
    ActionBar.Tab events, archives, livestream;
    Fragment eventTab = new EventListTab();
    Fragment archiveTab = new EventListTab();
    Fragment livestreamTab = new LiveStreamTab();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ActionBar actionBar = getActionBar();

        //Hide Actionbar Icon
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        //Set Tab Titles
        events = actionBar.newTab().setText("Events");
        archives = actionBar.newTab().setText("Archives");
        livestream = actionBar.newTab().setText("Livestream");

        //Set TabListeners
        events.setTabListener(new TabListener(eventTab));
        archives.setTabListener(new TabListener(archiveTab));
        livestream.setTabListener(new TabListener(livestreamTab));

        actionBar.addTab(events);
        actionBar.addTab(archives);
        actionBar.addTab(livestream);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //openSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
