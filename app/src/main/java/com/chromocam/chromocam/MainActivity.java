package com.chromocam.chromocam;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.chromocam.chromocam.util.ChromoServer;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.HttpResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements EventListTab.OnEventSelectionListener, ArchiveListTab.OnEventSelectionListener, View.OnClickListener{

    //Tab manager
    ActionBar actionBar;
    ActionBar.Tab events, archives, livestream;
    ListFragment eventTab = new EventListTab();
    Fragment archiveTab = new ArchiveListTab();
    Fragment livestreamTab = new LiveStreamTab();
    String topLevelName = "activity_main";

    //Authorization
    ChromoServer chromoServer;
    Button registerButton;
    private Boolean deviceRegistered = false;
    private Bundle savedInstanceState;

    //Seems legit
    private String secretToken = "";
    private String targetDomain = "http://downyhouse.homenet.org:3000";
    private String password = "asdf123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;

        //Registration Phase
        if(!this.deviceRegistered){
            registerDevice();
        }

    }

    private void registerDevice() {
        setContentView(R.layout.registration);

        //set Button listener
        this.registerButton = (Button) findViewById(R.id.registerButton);
        this.registerButton.setOnClickListener(this);

        ((TextView) findViewById(R.id.targetDomain_key)).getEditableText().insert(0, this.targetDomain);
        ((TextView) findViewById(R.id.password_key)).getEditableText().insert(0, this.password);
    }

    public void onClick(View v) {
        Log.d("REGISTER_DEBUG", "Register Button Pressed");

        this.checkRegistration();

        if(this.deviceRegistered == true)
        {
            this.initChromocam();
        }
    }

    private boolean checkRegistration()
    {
        Log.d("REGISTER_DEBUG", "Checking Registration");

        //Get Target Domain
        TextView target_domain_key = (TextView)findViewById(R.id.targetDomain_key);
        String tDomain = target_domain_key.getEditableText().toString();
        Log.d("REGISTER_DEBUG", "Domain: " + tDomain);

        //this.chromoServer = new ChromoServer(tDomain, pws )


        //Get Password
        TextView password_key = (TextView)findViewById(R.id.password_key);
        String pws = password_key.getEditableText().toString();
        Log.d("REGISTER_DEBUG", "Password: " + pws);

        return false;
    }

    public void initChromocam(){
        setContentView(R.layout.activity_main);
        invalidateOptionsMenu();

        if (savedInstanceState == null){
            getFragmentManager().beginTransaction().add(R.id.fragment_container, new EventListTab()).commit();
        }


        actionBar = getActionBar();

        //Hide Actionbar Icon
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        //Set Tab Titles
        events = actionBar.newTab().setText("Events");
        archives = actionBar.newTab().setText("Archives");
        livestream = actionBar.newTab().setText("Livestream");

        //Set TabListeners
        events.setTabListener(new TabListener(eventTab));
        archives.setTabListener(new TabListener(archiveTab));
        livestream.setTabListener(new TabListener(livestreamTab));

        //Add Tabs
        actionBar.addTab(events);
        actionBar.addTab(archives);
        actionBar.addTab(livestream);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(this.deviceRegistered) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
        }
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
            this.actionBar.hide();
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, new PrefsFragment()).addToBackStack(this.topLevelName).commit();
            Log.d("PREFERENCES-DEBUG", "Preferences have been selected");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //Back Button Pressed
    @Override
    public void onBackPressed()
    {
        //If Top level fragment, show action bar
        super.onBackPressed();

        Boolean isTop = isFragmentVisible(R.id.fragment_container);

        if(isTop)
        {
            this.actionBar.show();
        }

        Log.d("BACK-PRESSED-DEBUG", "Back button Has been pressed");


        //this.actionBar.show();

    }

    //Gets the Current Fragment
    public boolean isFragmentVisible(int fragmentID)
    {
        //Find Fragment by ID
        Fragment currFrag = getFragmentManager().findFragmentById(fragmentID);

        if(currFrag instanceof Fragment)
        {
            return true;
        }

        return false;
    }

    //Preferences Fragment Class
    public static class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }
    @Override
    public void onEventSelection(EventContent.EventItem item) {

    }



}
