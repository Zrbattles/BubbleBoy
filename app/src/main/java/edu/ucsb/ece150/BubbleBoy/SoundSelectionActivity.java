package edu.ucsb.ece150.BubbleBoy;

import android.app.Activity;
import android.os.Bundle;

public class SoundSelectionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noise_selection);
        //TODO set up so that noise selection can be made and sent back to our program

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //[TODO] edit so that when back button is pressed our code doesn't break
    }
}
