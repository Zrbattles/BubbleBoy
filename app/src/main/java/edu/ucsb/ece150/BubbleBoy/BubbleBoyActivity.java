//Created by Zach Battles and Chris Chan
package edu.ucsb.ece150.BubbleBoy;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.List;

import edu.ucsb.ece150.BubbleBoy.camera.CameraSourcePreview;
import edu.ucsb.ece150.BubbleBoy.camera.GraphicOverlay;

/**
 * Activity for the BubbleBoy app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public class BubbleBoyActivity extends AppCompatActivity {
    private static final String TAG = "BubbleBoy";
    static final String PREFS_NAME = "BubbleBoySaves";

    private int current_mask_index = 0;

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2; // Request code for Camera Permission
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 5;

    private Button mTestButton;
    private Button mtiltButton;
    private Button mInfoButton;

    Snackbar infoNotify;

    //set up accelerometer
    private SensorManager sensMan;
    private float timestamp;

    // set up private variables for sound
    private static final int RINGTONE_PICK_REQUEST_CODE = 11;
    private MediaPlayer mPlayer = null;
    private Uri mAlertSound = null;
    private CountDownTimer mTimer = null;
    private CountDownTimer mTimer2 = null;
    private int mMaxSoundDuration = 1000;
    private String[] mAlertSounds = {"Notification Sounds", "Alarm Sounds", "Ringtone Sounds"};
    private String[] mSoundSettings = {"5 Seconds", "3 Seconds", "1 Second"};
    private Boolean mMuteFlag = false;
    private Button mMuteButton;

    private Boolean mTooClose = false;
    private Boolean mTooCloseAlert = true;
    private Boolean mSoundPrepared = false;
    private Boolean mPlayLaterFlag = false;
    private int mTimer2Duration = 2000;
    private Boolean mTiltUp = false;
    private Boolean mTiltDown = false;
    private Boolean mTiltOption = true;
    private Boolean mTestingFlag = false;

    // set up private variables for haptics
    private Vibrator mVibe;
    private Boolean mHapticsFlag = true;
    private int mHapticsDuration = 100;
    private String[] mHapticSettings = {"Long", "Medium", "Short", "Disable Haptics"};

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bubble_boy);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.appToolbar);
        setSupportActionBar(myToolbar);

        //initialize gyroscope
        sensMan = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = sensMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        // pull saved data either from a bundle if rotated or shared prefs
        if (savedInstanceState == null) {
            SharedPreferences myPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            mAlertSound = Uri.parse(myPreferences.getString("alert_sound", "default"));
            if (mAlertSound.toString() == "default") {
                mAlertSound = null;
            }
            mMaxSoundDuration = myPreferences.getInt("max_sound_duration", 1000);
            mHapticsFlag = myPreferences.getBoolean("haptics_flag", true);
            mHapticsDuration = myPreferences.getInt("haptics_duration", 100);
            mMuteFlag = myPreferences.getBoolean("mute_flag", false);
            mTiltOption = myPreferences.getBoolean("tilt_flag", true);
        } else {
            mAlertSound = Uri.parse(savedInstanceState.getString("alert_sound"));
            mMaxSoundDuration = savedInstanceState.getInt("max_sound_duration");
            mHapticsFlag = savedInstanceState.getBoolean("haptics_flag");
            mHapticsDuration = savedInstanceState.getInt("haptics_duration");
            mMuteFlag = savedInstanceState.getBoolean("mute_flag");
            mTiltOption = savedInstanceState.getBoolean("tilt_flag");
        }

        mTestButton = (Button) findViewById(R.id.testButton);
        initAlertSoundPlayer();
        mTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mTestingFlag) {
                    mTestButton.setBackgroundColor(Color.RED);
                    mTestingFlag = true;
                }
                playAlertSound();
            }
        });

        mInfoButton = (Button) findViewById(R.id.infoButton);
        mInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                infoNotify = Snackbar.make(view, "Visit BubbleBoy Website?", Snackbar.LENGTH_LONG);
                infoNotify.setAction("CONFIRM", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://ucsbece150s20bubbleboy.wordpress.com/help/"));
                        startActivity(browserIntent);
                    }
                }).show();
            }
        });

        mtiltButton = (Button) findViewById(R.id.tiltButton);
        if (mTiltOption) {
            mtiltButton.setText("DISABLE TILT ASSIST");
        } else {
            mtiltButton.setText("ENABLE TILT ASSIST");
        }
        mtiltButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mTiltOption) {
                    mTiltOption = false;
                    mGraphicOverlay.clear();
                    mtiltButton.setText("ENABLE TILT ASSIST");
                    Toast.makeText(getApplicationContext(), "Tilt Assist Disabled! Press Again to Enable.", Toast.LENGTH_SHORT).show();
                }
                else{
                    mTiltOption = true;
                    mtiltButton.setText("DISABLE TILT ASSIST");
                    Toast.makeText(getApplicationContext(), "Tilt Assist Enabled! Press Again to Disable.", Toast.LENGTH_SHORT).show();
                }
                GraphicOverlay.updateTiltOption(mTiltOption);

            }
        });



        mMuteButton = findViewById(R.id.muteButton);
        if (mMuteFlag) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mMuteButton.setForeground(this.getDrawable(R.drawable.sound_off_foreground));
            } else {
                mMuteButton.setBackgroundColor(Color.RED);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mMuteButton.setForeground(this.getDrawable(R.drawable.sound_on_foreground));
            }
            else {
                mMuteButton.setBackgroundColor(Color.BLACK);
            }
        }
        mMuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMuteFlag) {
                    mMuteFlag = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mMuteButton.setForeground(getApplicationContext().getDrawable(R.drawable.sound_on_foreground));
                    } else {
                        mMuteButton.setBackgroundColor(Color.BLACK);
                    }
                    Toast.makeText(getApplicationContext(), "Alert Sounds Enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    mMuteFlag = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mMuteButton.setForeground(getApplicationContext().getDrawable(R.drawable.sound_off_foreground));
                    } else {
                        mMuteButton.setBackgroundColor(Color.RED);
                    }
                    Toast.makeText(getApplicationContext(), "Alert Sounds Muted!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //set up gyroscope listener
        SensorEventListener accelerometerSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event){
                int orientation = getResources().getConfiguration().orientation;
                // X axis should be the angle we are looking for axisX should return in radians
                if(timestamp != 0) {
                    float dt = event.timestamp - timestamp;
                    float axisX = event.values[0];
                    float axisY = event.values[1];
                    float axisZ = event.values[2];
                    //Log.i("AxisY","ay: " + Float.toString(axisY));
                    if(orientation == Configuration.ORIENTATION_PORTRAIT) {
                        if ((axisY < 9.5) || axisY > 10.5) {
                            if (axisZ > 2) {
                                mTiltUp = true;
                            } else {
                                mTiltDown = true;
                            }
                        } else {
                            mTiltUp = false;
                            mTiltDown = false;
                        }
                    }
                    else{
                        if((axisX < 9.5) || (axisX > 10.5)){
                            if (axisZ > 0) {
                                mTiltUp = true;
                            } else {
                                mTiltDown = true;
                            }
                        }
                        else{
                            mTiltUp = false;
                            mTiltDown = false;
                        }
                    }
                    GraphicOverlay.updateTilt(mTiltUp,mTiltDown);

                }
                timestamp = event.timestamp;
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i){
            }
        };
        sensMan.registerListener(accelerometerSensorListener,accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mTimer2 = new CountDownTimer(mTimer2Duration, mTimer2Duration) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                mTooCloseAlert = true;
            }
        };

        // Check for permissions before accessing the camera. If the permission is not yet granted,
        // request permission from the user.
        int cameraPermissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(cameraPermissionGranted == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

    }

    /**
     * Creates the menu on the Action Bar for selecting resetting gyroscope.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Toolbar myToolbar = (Toolbar) findViewById(R.id.appToolbar);
        myToolbar.inflateMenu(R.menu.masks);
        myToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener () {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
        return true;
    }

    /**
     * Handler function that determines what happens when an option is pressed in the Action Bar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.changeSound:
                final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alert Sound");
                if (mAlertSound != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mAlertSound);
                } else {
                    setDefAlertSound();
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mAlertSound);
                }
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Alert Sound Libraries")
                        .setItems(mAlertSounds, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0:
                                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                                        break;
                                    case 1:
                                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                                        break;
                                    case 2:
                                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
                                        break;
                                    default:
                                        break;
                                }
                                startActivityForResult(intent, RINGTONE_PICK_REQUEST_CODE);
                            }
                        });
                // Create the AlertDialog object and return it
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                break;

            case R.id.soundSettings:
                AlertDialog.Builder builder0 = new AlertDialog.Builder(this);
                builder0.setTitle("Alert Sound Max Duration")
                        .setItems(mSoundSettings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0:
                                        mMaxSoundDuration = 5000;
                                        initAlertSoundPlayer();
                                        Toast.makeText(getApplicationContext(), "Alert Sound Max Duration set to 5 Seconds.", Toast.LENGTH_SHORT).show();
                                        break;
                                    case 1:
                                        mMaxSoundDuration = 3000;
                                        initAlertSoundPlayer();
                                        Toast.makeText(getApplicationContext(), "Alert Sound Max Duration set to 3 Seconds.", Toast.LENGTH_SHORT).show();
                                        break;
                                    case 2:
                                        mMaxSoundDuration = 1000;
                                        initAlertSoundPlayer();
                                        Toast.makeText(getApplicationContext(), "Alert Sound Max Duration set to 1 Second.", Toast.LENGTH_SHORT).show();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });
                // Create the AlertDialog object and return it
                AlertDialog alertDialog0 = builder0.create();
                alertDialog0.show();
                break;
            case R.id.hapticSettings:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setTitle("Haptic Feedback Duration")
                        .setItems(mHapticSettings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0:
                                        mHapticsFlag = true;
                                        mHapticsDuration = 150;
                                        Toast.makeText(getApplicationContext(), "Haptics set to long.", Toast.LENGTH_SHORT).show();
                                        break;
                                    case 1:
                                        mHapticsFlag = true;
                                        mHapticsDuration = 100;
                                        Toast.makeText(getApplicationContext(), "Haptics set to medium.", Toast.LENGTH_SHORT).show();
                                        break;
                                    case 2:
                                        mHapticsFlag = true;
                                        mHapticsDuration = 50;
                                        Toast.makeText(getApplicationContext(), "Haptics set to short.", Toast.LENGTH_SHORT).show();
                                        break;
                                    case 3:
                                        mHapticsFlag = false;
                                        Toast.makeText(getApplicationContext(), "Haptics disabled.", Toast.LENGTH_SHORT).show();
                                        break;
                                    default:
                                        mHapticsFlag = true;
                                        break;
                                }
                            }
                        });
                // Create the AlertDialog object and return it
                AlertDialog alertDialog1 = builder1.create();
                alertDialog1.show();
                break;
            case R.id.setDefaults:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                builder2.setMessage("Reset all BubbleBoy settings to default values?")
                        .setTitle("Set Default Settings")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mPlayer = null;
                                mAlertSound = null;
                                initAlertSoundPlayer();
                                mHapticsFlag = true;
                                mHapticsDuration = 125;
                                mMaxSoundDuration = 1000;
                                mMuteFlag = false;
                                initAlertSoundPlayer();
                                if (mMuteFlag) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        mMuteButton.setForeground(getApplicationContext().getDrawable(R.drawable.sound_off_foreground));
                                    } else {
                                        mMuteButton.setBackgroundColor(Color.RED);
                                    }
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        mMuteButton.setForeground(getApplicationContext().getDrawable(R.drawable.sound_on_foreground));
                                    }
                                    else {
                                        mMuteButton.setBackgroundColor(Color.BLACK);
                                    }
                                }
                                Toast.makeText(getApplicationContext(), "Settings reset to Default!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Toast.makeText(getApplicationContext(), "Settings unchanged.", Toast.LENGTH_SHORT).show();
                            }
                        });
                // Create the AlertDialog object and return it
                AlertDialog alertDialog2 = builder2.create();
                alertDialog2.show();
                break;
            default:
                return false;
        }
        return true;
    }


    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission.");

        final String[] permissions = new String[] {Manifest.permission.CAMERA};

        if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;
        Snackbar.make(mGraphicOverlay, R.string.permissionCameraRationale, Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(thisActivity,
                                permissions, RC_HANDLE_CAMERA_PERM);
                    }
                }).show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {
        // 1. Get the application's context
        Context faceDetectorContext = getApplicationContext();
        // 2. Create a FaceDetector object for real time detection
        //    Ref: https://developers.google.com/vision/android/face-tracker-tutorial
        FaceDetector realTimeDetector = new FaceDetector.Builder(faceDetectorContext)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode (FaceDetector.FAST_MODE)
                .build();
        // 4. Create a GraphicFaceTrackerFactory
        GraphicFaceTrackerFactory detectorFactory = new GraphicFaceTrackerFactory();
        // 5. Pass the GraphicFaceTrackerFactory to
        //    a MultiProcessor.Builder to create a MultiProcessor
        MultiProcessor pipeline = new MultiProcessor.Builder<Face>(detectorFactory).build();
        // 6. Associate the MultiProcessor with the real time detector
        realTimeDetector.setProcessor(pipeline);
        // 7. Check if the real time detector is operational
        // 8. Create a camera source to capture video images from the camera,
        //    and continuously stream those images into the detector and
        //    its associated MultiProcessor
        mCameraSource = new CameraSource.Builder(faceDetectorContext, realTimeDetector)
                .setRequestedPreviewSize(960,720)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build();
    }

    // sets the alert sound to either the default ringtone or user choice
    private void setDefAlertSound() {
        if (mAlertSound == null) {
            mAlertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (RingtoneManager.getRingtone(this, mAlertSound)== null) {
                mAlertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (RingtoneManager.getRingtone(this, mAlertSound) == null) {
                    mAlertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
            }
        }
    }

    // creates the alert sound and sets behavior for on completion
    private void initAlertSoundPlayer() {
        // check if we have to first set the alert sound
        if (mAlertSound == null) {
            setDefAlertSound();
        }
        mTimer = new CountDownTimer(mMaxSoundDuration, mMaxSoundDuration) {
            @Override
            public void onTick(long l) {
            }
            @Override
            public void onFinish() {
                if (mTestButton != null) {
                    if (mTestingFlag) {
                        mTestingFlag = false;
                        mTestButton.setBackgroundColor(getResources().getColor(R.color.common_google_signin_btn_text_dark_focused));
                    }
                }
                if (mPlayer != null) {
                    if (mPlayer.isPlaying()) {
                        mPlayer.stop();
                        try {
                            mPlayer.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                            mPlayer.reset();
                            mPlayer.release();
                            mPlayer = null;
                        }
                    }
                }
            }
        };
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
        mPlayer = MediaPlayer.create(this, mAlertSound);
        mSoundPrepared = true;
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (mTestButton != null) {
                    if (mTestingFlag) {
                        mTestingFlag = false;
                        mTestButton.setBackgroundColor(getResources().getColor(R.color.common_google_signin_btn_text_dark_focused));
                    }
                }
                mTimer.cancel();
                mPlayer.stop();
                try {
                    mPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                    mPlayer.reset();
                    mPlayer.release();
                    mPlayer = null;
                }
            }
        });
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mSoundPrepared = true;
                if (!mMuteFlag && mPlayLaterFlag) {
                    mPlayer.start();
                    mTimer.start();
                    mSoundPrepared = false;
                    mPlayLaterFlag = false;
                }
            }
        });
    }

    // plays alert sound and handles if an alert is already playing
    public void playAlertSound() {
        if (mPlayer == null) {
            initAlertSoundPlayer();
        }
        if (mPlayer.isPlaying()) {
            mPlayer.stop();
            mTimer.cancel();
            try {
                mPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                mPlayer.reset();
                mPlayer.release();
                mPlayer = null;
                initAlertSoundPlayer();
            }
        }
        if (!mMuteFlag && mSoundPrepared) {
            mPlayer.start();
            mTimer.start();
            mSoundPrepared = false;
        } else {
            mPlayLaterFlag = true;
        }
        if (mHapticsFlag) {
            mVibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibe.vibrate(VibrationEffect.createOneShot(mHapticsDuration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                mVibe.vibrate(mHapticsDuration);
            }
        }
        if (mMuteFlag) {
            mTestingFlag = false;
            mTestButton.setBackgroundColor(getResources().getColor(R.color.common_google_signin_btn_text_dark_focused));
        }
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();

    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
        SharedPreferences myPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = myPreferences.edit();
        if (mAlertSound != null) {
            editor.putString("alert_sound", mAlertSound.toString());
        } else {
            setDefAlertSound();
            editor.putString("alert_sound", mAlertSound.toString());
        }
        editor.putInt("max_sound_duration", mMaxSoundDuration);
        editor.putBoolean("haptics_flag", mHapticsFlag);
        editor.putInt("haptics_duration", mHapticsDuration);
        editor.putBoolean("mute_flag", mMuteFlag);
        editor.putBoolean("tilt_flag", mTiltOption);
        editor.apply();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Long timeStampRaw = System.currentTimeMillis() / 1000;
                }
                break;
            case RC_HANDLE_CAMERA_PERM:
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted - initialize the camera source");
                    // We have permission, so create the camera source
                    createCameraSource();
                    return;
                }

                Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                        " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

                finish();
            default:
                Log.d(TAG, "Got unexpected permission result: " + requestCode);
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
        if (mAlertSound != null) {
            savedInstanceState.putString("alert_sound", mAlertSound.toString());
        } else {
            setDefAlertSound();
            savedInstanceState.putString("alert_sound", mAlertSound.toString());
        }
        savedInstanceState.putInt("max_sound_duration", mMaxSoundDuration);
        savedInstanceState.putBoolean("haptics_flag", mHapticsFlag);
        savedInstanceState.putInt("haptics_duration", mHapticsDuration);
        savedInstanceState.putBoolean("mute_flag", mMuteFlag);
        savedInstanceState.putBoolean("tilt_flag", mTiltOption);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RINGTONE_PICK_REQUEST_CODE) {
            mAlertSound = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            initAlertSoundPlayer();
        }
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // Check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FindDistance mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FindDistance(overlay);
            mFaceGraphic.updateMask(current_mask_index);

        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.updateMask(current_mask_index);
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            List<Landmark> landmarks = face.getLandmarks();
            Landmark nose = landmarks.get(2);
            Landmark mouth = landmarks.get(3);
            mTooClose = tooClose(nose,mouth);
            if(mTooClose && mTooCloseAlert){
                mTooCloseAlert = false;
                mTimer2.start();
                playAlertSound();
            }
            mFaceGraphic.updateTooClose(mTooClose);
            mFaceGraphic.updateFace(face);
            mFaceGraphic.updateMask(current_mask_index);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }

        // function to tell if someone is too close
        public boolean tooClose(Landmark nose, Landmark mouth){
            PointF npoint = nose.getPosition();
            PointF mpoint = mouth.getPosition();
            float distance = (float) Math.sqrt((npoint.x-mpoint.x)*(npoint.x-mpoint.x) + (npoint.y-mpoint.y)*(npoint.y-mpoint.y));
            if(distance <= 16.5)
                return false;
            else
                return true;

        }
    }

}
