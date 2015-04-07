package com.chromocam.chromocam.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import com.chromocam.chromocam.R;

public class DisplayPictureActivity extends Activity {
    private int type = 1;
    private Menu menu;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_picture);
        //get intent data
        byte[] b = getIntent().getByteArrayExtra("image");
        type = getIntent().getIntExtra("calling", 1);
        Bitmap image = BitmapFactory.decodeByteArray(b, 0, b.length);
        ImageView imageView = (ImageView) findViewById(R.id.full_image_view);
        imageView.setImageBitmap(image);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu=menu;
        getMenuInflater().inflate(R.menu.menu_display_picture, menu);
        if(type==1){ //if the calling fragment is the Event fragment
            hideOption(R.id.action_unarchive);
            showOption(R.id.action_archive);
        } else if(type ==2 ){ //else if it's the Archive fragment
            showOption(R.id.action_unarchive);
            hideOption(R.id.action_archive);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void hideOption(int id){
        MenuItem item = menu.findItem(id);
        item.setVisible(false);
    }

    private void showOption(int id){
        MenuItem item = menu.findItem(id);
        item.setVisible(true);
    }
}
