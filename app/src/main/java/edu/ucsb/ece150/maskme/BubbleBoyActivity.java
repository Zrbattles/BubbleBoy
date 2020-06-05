package edu.ucsb.ece150.maskme;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import edu.ucsb.ece150.maskme.camera.CameraSourcePreview;
import edu.ucsb.ece150.maskme.camera.GraphicOverlay;

/**
 * Activity for the BubbleBoy app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public class BubbleBoyActivity extends AppCompatActivity {
    private static final String TAG = "BubbleBoy";

    static final String PREFS_NAME = "MyPrefFile";
    private int current_mask_index = 0;

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2; // Request code for Camera Permission
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 5;

    private enum ButtonsMode {
        PREVIEW_CAPTURE, BACK_SAVE,
    }

    SparseArray<Face> mFaces = new SparseArray<>();

    private ButtonsMode buttonsMode = ButtonsMode.PREVIEW_CAPTURE;
    private MaskedImageView mImageView;
    private Bitmap mCapturedImage;
    private Button mLeftButton;
    private Button mCenterButton;
    private Button mRightButton;
    private Bitmap savedImage;

    private FaceDetector mStaticFaceDetector;

    private boolean previewButtonVisible = false;
    private int maskTypeDrawn = 0;

    private boolean inPreview = false;
    private boolean mImageViewCaptured = false;

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_tracker);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.appToolbar);
        setSupportActionBar(myToolbar);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        mCenterButton = (Button) findViewById(R.id.centerButton);
        mCenterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(buttonsMode) {
                    case PREVIEW_CAPTURE:
                        mLeftButton.setVisibility(View.VISIBLE);
                        previewButtonVisible = true;
                        if(mCameraSource != null) {
                            mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                                @Override
                                public void onPictureTaken(byte[] data) {
                                    mCapturedImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    // TODO - These lines can help with some trouble when rotating the camera. Uncomment and edit if necessary.
                                    int orientation = getResources().getConfiguration().orientation;
                                    if(orientation == Configuration.ORIENTATION_PORTRAIT){
                                       mCapturedImage = rotateImage(mCapturedImage, 90.0f);
                                    } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                        mCapturedImage = rotateImage(mCapturedImage, 180f);
                                    }
                                    mImageView.setImageBitmap(mCapturedImage);
                                    mImageViewCaptured = true;
                                }
                            });
                        }
                        break;
                    case BACK_SAVE:
                        // [TODO] remove save feature
                        savedImage = getBitmapFromImageView(mImageView);
                        savePrevImage();
                        break;
                    default:
                        break;
                }
            }
        });

        previewButtonVisible = false;
        maskTypeDrawn = 0;
        mLeftButton = (Button) findViewById(R.id.leftButton);
        mLeftButton.setVisibility(View.GONE);
        mLeftButton.setOnClickListener(new View.OnClickListener() {
            //[TODO] probably get rid of all of this
            @Override
            public void onClick(View view) {
                switch(buttonsMode) {
                    case PREVIEW_CAPTURE:
                        mRightButton.setVisibility(View.VISIBLE);
                        mPreview.addView(mImageView);
                        mPreview.bringChildToFront(mImageView);
                        maskTypeDrawn = 0;

                        mLeftButton.setText("Back");
                        mCenterButton.setText("Save");
                        mRightButton.setText("Clear");
                        buttonsMode = ButtonsMode.BACK_SAVE;
                        mImageView.clearPath();
                        inPreview = true;
                        break;
                    case BACK_SAVE:
                        mRightButton.setVisibility(View.GONE);
                        mPreview.removeView(mImageView);
                        mFaces.clear();
                        maskTypeDrawn = 0;

                        mLeftButton.setText("Preview");
                        mCenterButton.setText("Capture");
                        buttonsMode = ButtonsMode.PREVIEW_CAPTURE;
                        inPreview = false;
                        break;
                    default:
                        break;
                }
            }
        });

        maskTypeDrawn = 0;
        mRightButton = (Button) findViewById(R.id.rightButton);
        mRightButton.setVisibility(View.GONE);
        mRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(buttonsMode) {
                    case PREVIEW_CAPTURE:
                        // do nothing, should not be visible
                    case BACK_SAVE:
                        mPreview.removeView(mImageView);
                        mFaces.clear();
                        mImageView.clearPath();
                        mImageView.setImageBitmap(mCapturedImage);
                        maskTypeDrawn = 0;
                        mPreview.addView(mImageView);
                        mPreview.bringChildToFront(mImageView);
                        break;
                    default:
                        break;
                }
            }
        });

        mImageView = new MaskedImageView(getApplicationContext());
        mImageView.setScaleType(ImageView.ScaleType.FIT_XY);

        // Check for permissions before accessing the camera. If the permission is not yet granted,
        // request permission from the user.
        int cameraPermissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(cameraPermissionGranted == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
        //[TODO] update so that angle data can be passed
        if (savedInstanceState != null) {
            mImageViewCaptured = savedInstanceState.getBoolean("capFlag");
            inPreview = savedInstanceState.getBoolean("prevFlag");
            current_mask_index = savedInstanceState.getInt("mask");
            if (mImageViewCaptured) {
                mLeftButton.setVisibility(View.VISIBLE);
                byte[] savedCapImageData = savedInstanceState.getByteArray("capImage");
                mCapturedImage = BitmapFactory.decodeByteArray(savedCapImageData, 0, savedCapImageData.length);
                mLeftButton.setVisibility(View.VISIBLE);
                if (inPreview) {
                    byte[] savedPrevImageData = savedInstanceState.getByteArray("prevImage");
                    Bitmap prevImage = BitmapFactory.decodeByteArray(savedPrevImageData, 0, savedPrevImageData.length);
                    mImageView.setImageBitmap(prevImage);
                    mRightButton.setVisibility(View.VISIBLE);
                    mPreview.addView(mImageView);
                    mPreview.bringChildToFront(mImageView);
                    maskTypeDrawn = 0;

                    mLeftButton.setText("Back");
                    mCenterButton.setText("Save");
                    mRightButton.setText("Clear");
                    buttonsMode = ButtonsMode.BACK_SAVE;
                } else {
                    mImageView.setImageBitmap(mCapturedImage);
                    mLeftButton.setText("Preview");
                    mCenterButton.setText("Capture");
                    buttonsMode = ButtonsMode.PREVIEW_CAPTURE;
                }
            }
        }
    }

    /**
     * Rotates a Bitmap by a specified angle in degrees
     */
    public static Bitmap rotateImage(Bitmap source, float angle){
        Log.v("MyLogger", "Rotating bitmap " + angle + " degrees.");
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Creates the menu on the Action Bar for selecting masks.
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
        mImageView.setImageBitmap(mCapturedImage);
        switch(item.getItemId()) {
            // [TODO] Using this as an example, implement behavior when a mask option is pressed.
            case R.id.Reset:
                // [TODO] reset the angle calculation
                detectStaticFaces(mCapturedImage);
                mImageView.drawFirstMask(mFaces);
                current_mask_index = 0;

                break;
            /*case R.id.mask2:
                detectStaticFaces(mCapturedImage);
                mImageView.drawSecondMask(mFaces);
                current_mask_index = 1;
                break;*/
            default:
                break;
        }
        return true;
    }

    private void detectStaticFaces(Bitmap image) {
        if(image == null) return;

        Frame frame = new Frame.Builder().setBitmap(image).build();
        mFaces = mStaticFaceDetector.detect(frame);

        Log.i("NumberofFaces", String.valueOf(mFaces.size()));
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
        // [TODO] Create a face detector for real time face detection
        // 1. Get the application's context
        Context faceDetectorContext = getApplicationContext();
        // 2. Create a FaceDetector object for real time detection
        //    Ref: https://developers.google.com/vision/android/face-tracker-tutorial
        FaceDetector realTimeDetector = new FaceDetector.Builder(faceDetectorContext)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode (FaceDetector.FAST_MODE)
                .build();
        // 3. Create a FaceDetector object for detecting faces on a static photo
        mStaticFaceDetector = new FaceDetector.Builder(faceDetectorContext)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        // 4. Create a GraphicFaceTrackerFactory
        GraphicFaceTrackerFactory detectorFactory = new GraphicFaceTrackerFactory();
        // 5. Pass the GraphicFaceTrackerFactory to
        //    a MultiProcessor.Builder to create a MultiProcessor
        MultiProcessor pipeline = new MultiProcessor.Builder<Face>(detectorFactory).build();
        // 6. Associate the MultiProcessor with the real time detector
        realTimeDetector.setProcessor(pipeline);
        // 7. Check if the real time detector is operational
        //
        // 8. Create a camera source to capture video images from the camera,
        //    and continuously stream those images into the detector and
        //    its associated MultiProcessor
        mCameraSource = new CameraSource.Builder(faceDetectorContext, realTimeDetector)
                .setRequestedPreviewSize(1280,720)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build();
    }

    private Bitmap getBitmapFromImageView(MaskedImageView drawnImageView) {
        Bitmap newImage = Bitmap.createBitmap(drawnImageView.getWidth(), drawnImageView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newImage);
        drawnImageView.draw(canvas);
        return newImage;
    }

    private void savePrevImage() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
        } else {
            Long timeStampRaw = System.currentTimeMillis() / 1000;
            String fileName = "maskImage-" + timeStampRaw.toString() + ".jpg";
            MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), savedImage, fileName, "description stub");
            Toast.makeText(getApplicationContext(), "Saved " + fileName + " to Gallery", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
        SharedPreferences myPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        current_mask_index = myPreferences.getInt("preferredMaskIndex", 0);
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();

        mPreview.stop();
        SharedPreferences myPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = myPreferences.edit();
        editor.putInt("preferredMaskIndex", current_mask_index);
        editor.apply();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

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
                    String fileName = "maskImage-" + timeStampRaw.toString() + ".jpg";
                    MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), savedImage, fileName, "description stub");
                    Toast.makeText(getApplicationContext(), "Saved " + fileName + " to Gallery", Toast.LENGTH_LONG).show();
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
    //[TODO] update onSaveInstanceState probably need to fix angle readings
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("prevFlag", inPreview);
        savedInstanceState.putBoolean("capFlag", mImageViewCaptured);
        savedInstanceState.putInt("mask",current_mask_index);
        if (mImageViewCaptured) {
            Bitmap savedCapBitmap = mCapturedImage;
            ByteArrayOutputStream streamCap = new ByteArrayOutputStream();
            savedCapBitmap.compress(Bitmap.CompressFormat.PNG, 100, streamCap);
            byte[] imageCapData = streamCap.toByteArray();
            savedInstanceState.putByteArray("capImage", imageCapData);
            if (inPreview) {
                mImageView.buildDrawingCache();
                Bitmap savedPrevBitmap = getBitmapFromImageView(mImageView);
                ByteArrayOutputStream streamPrev = new ByteArrayOutputStream();
                savedPrevBitmap.compress(Bitmap.CompressFormat.PNG, 100, streamPrev);
                byte[] imagePrevData = streamPrev.toByteArray();
                savedInstanceState.putByteArray("prevImage", imagePrevData);
            }
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
    }
}
