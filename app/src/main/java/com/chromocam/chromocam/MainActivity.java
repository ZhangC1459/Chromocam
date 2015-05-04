package com.chromocam.chromocam;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chromocam.chromocam.util.ChromoComplete;
import com.chromocam.chromocam.util.ChromoServer;
import com.chromocam.chromocam.util.Payload;
import com.chromocam.chromocam.util.Purpose;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

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
import java.util.concurrent.atomic.AtomicInteger;


public class MainActivity extends Activity implements ChromoComplete, EventListTab.OnEventSelectionListener, ArchiveListTab.OnEventSelectionListener, View.OnClickListener{
   //Push Variables
   private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Tag used on log messages.
     */
    static final String TAG = "Chromocam";
    Context context;

    //Tab manager
    ActionBar actionBar;
    ActionBar.Tab events, archives, livestream;
    EventListTab eventTab = new EventListTab();
    ArchiveListTab archiveTab = new ArchiveListTab();
    Fragment livestreamTab = new LiveStreamTab();
    String topLevelName = "activity_main";

    //Authorization
    private ChromoServer chromoServer;
    Button registerButton;
    private Boolean deviceRegistered = false;
    private Bundle savedInstanceState;

    //Defaults
    private String secretToken = "";
    private String targetDomain = "https://chromocam.co:3000";
    private String password = "asdf123";
    private AlertDialog.Builder quitDialog;

    //kludgy as hell
    private Boolean settings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.savedInstanceState = savedInstanceState;
        this.context = getApplicationContext();

        this.chromoServer = new ChromoServer(this, this.getApplicationContext());




        quitDialog = new AlertDialog.Builder(this);
        quitDialog.setTitle("Quit Chromocam?");
        quitDialog.setMessage(R.string.are_you_sure);
        quitDialog.setPositiveButton(R.string.quit, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {



                Log.d("MainActivity Dialog", "Clicked quit button");
                MainActivity.this.finish();
                System.exit(0);
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.d("MainActivity Dialog", "Clicked cancel button");
            }
        });
        quitDialog.create();
        //Registration Phase
        if(!this.deviceRegistered){
            registerDevice();
        } else {
            this.initChromocam();
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


    }

    public ChromoServer getServ(){
        return this.chromoServer;
    }

    private void checkRegistration()
    {
        Log.d("REGISTER_DEBUG", "Checking Registration");



        //Get Target Domain
        TextView target_domain_key = (TextView)findViewById(R.id.targetDomain_key);
        String tDomain = target_domain_key.getEditableText().toString();
        Log.d("REGISTER_DEBUG", "Domain: " + tDomain);



        //Get Password
        TextView password_key = (TextView)findViewById(R.id.password_key);
        String pws = password_key.getEditableText().toString();
        Log.d("REGISTER_DEBUG", "Password: " + pws);

        if(tDomain.isEmpty() || pws.isEmpty())
        {
            Toast.makeText(this, "Fields cannot be empty!", Toast.LENGTH_LONG).show();
        }else if (!Patterns.WEB_URL.matcher(tDomain).matches()) {
            Toast.makeText(this, "You must input an URL!", Toast.LENGTH_LONG).show();
        }
        else
        {
            chromoServer.initChromoServer(tDomain, pws);
        }


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

        this.chromoServer.syncServerCurrentSettings();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }



    @Override
    protected void onResume() {
        super.onResume();
        // Check device for Play Services APK.
        checkPlayServices();
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
            this.chromoServer.syncServerCurrentSettings();
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, new PrefsFragment()).addToBackStack(this.topLevelName).commit();
            Log.d("PREFERENCES-DEBUG", "Preferences have been selected");
            settings = true;
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //Back Button Pressed
    @Override
    public void onBackPressed()
    {
        //If Top level fragment, show action bar

        if(!settings)
        {
             this.quitDialog.show();
        } else {
            settings = false;
            super.onBackPressed();
            this.actionBar.show();
        }

        Log.d("BACK-PRESSED-DEBUG", "Back button Has been pressed");



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

    @Override
    public void onTaskCompleted(Payload p) {
        deviceRegistered = p.getResult();
        Purpose taskPurpose = p.getPurpose();

        if(deviceRegistered && p.getPurpose() == Purpose.REGISTER){
            initChromocam();
        }
        else if(deviceRegistered && p.getPurpose() == Purpose.REGISTERED)
        {

        }
        else if(deviceRegistered && p.getPurpose() == Purpose.GET_FILE_LIST_E)
        {
            eventTab.loadListCallback(p.getContent());
        }
        else if(deviceRegistered && p.getPurpose() == Purpose.GET_FILE_LIST_A)
        {
            archiveTab.loadListCallback(p.getContent());
        }
        else {
            Log.d("REG DEBUG", "Registration failed ayyy");
        }
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

    //required method
    @Override
    public void onEventSelection(EventContent item) {

    }




}
