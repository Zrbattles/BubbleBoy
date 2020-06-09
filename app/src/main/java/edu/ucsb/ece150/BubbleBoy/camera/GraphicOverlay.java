/**
 * A view which renders a series of custom graphics to be overlayed on top of an associated preview
 * (i.e., the camera preview).  The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.<p>
 *
 * Supports scaling and mirroring of the graphics relative the camera's preview properties.  The
 * idea is that detection items are expressed in terms of a preview size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.<p>
 *
 * Associated {@link Graphic} items should use the following methods to convert to view coordinates
 * for the graphics that are drawn:
 * <ol>
 * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of the
 * supplied value from the preview scale to the view scale.</li>
 * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the coordinate
 * from the preview's coordinate system to the view coordinate system.</li>
 * </ol>
 */

package edu.ucsb.ece150.BubbleBoy.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.OrientationEventListener;
import android.view.View;

import com.google.android.gms.vision.CameraSource;

import java.util.HashSet;

/**
 * A view which renders a series of custom graphics to be overlayed on top of an associated preview
 * (i.e., the camera preview).  The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.<p>
 *
 * Supports scaling and mirroring of the graphics relative the camera's preview properties.  The
 * idea is that detection items are expressed in terms of a preview size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.<p>
 *
 * Associated {@link Graphic} items should use the following methods to convert to view coordinates
 * for the graphics that are drawn:
 * <ol>
 * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of the
 * supplied value from the preview scale to the view scale.</li>
 * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the coordinate
 * from the preview's coordinate system to the view coordinate system.</li>
 * </ol>
 */
public class GraphicOverlay extends View {
    private final Object mLock = new Object();

    private int mPreviewWidth;
    private int mPreviewHeight;
    private float mWidthScaleFactor = 1.0f;
    private float mHeightScaleFactor = 1.0f;
    private static boolean mTiltUp = false;
    private static boolean mTiltDown = false;
    private static boolean mTiltOption = true;

    private int mFacing = CameraSource.CAMERA_FACING_BACK;
    private HashSet<Graphic> mGraphics = new HashSet<>();

    public static void updateTilt(boolean tiltup, boolean tiltdown){
        mTiltUp = tiltup;
        mTiltDown = tiltdown;
    }
    public static void updateTiltOption(boolean tiltOption){ mTiltOption = tiltOption;}
    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay.  Subclass
     * this and implement the {@link Graphic#draw(Canvas)} method to define the
     * graphics element.  Add instances to the overlay using {@link GraphicOverlay#add(Graphic)}.
     */
    public static abstract class Graphic{
        private GraphicOverlay mOverlay;

        public Graphic(GraphicOverlay overlay) { mOverlay = overlay; }

        /**
         * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
         * convert to view coordinates for the graphics that are drawn:
         * <ol>
         * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of
         * the supplied value from the preview scale to the view scale.</li>
         * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
         * coordinate from the preview's coordinate system to the view coordinate system.</li>
         * </ol>
         *
         * @param canvas drawing canvas
         */
        public abstract void draw(Canvas canvas);

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view
         * scale.
         */
        public float scaleX(float horizontal) {
            return horizontal * mOverlay.mWidthScaleFactor;
        }

        /**
         * Adjusts a vertical value of the supplied value from the preview scale to the view scale.
         */
        public float scaleY(float vertical) {
            return vertical * mOverlay.mHeightScaleFactor;
        }

        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateX(float x) {
            if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                return mOverlay.getWidth() - scaleX(x);
            } else {
                return scaleX(x);
            }
        }

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateY(float y) {
            return scaleY(y);
        }

        public void postInvalidate() {
            mOverlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Removes all graphics from the overlay.
     */
    public void clear() {
        synchronized (mLock) {
            mGraphics.clear();
        }
        postInvalidate();
    }


    /**
     * Adds a graphic to the overlay.
     */
    public void add(Graphic graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
        }
        postInvalidate();
    }

    /**
     * Removes a graphic from the overlay.
     */
    public void remove(Graphic graphic) {
        synchronized (mLock) {
            mGraphics.remove(graphic);
        }
        postInvalidate();
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform
     * image coordinates later.
     */
    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            mFacing = facing;
        }
        postInvalidate();
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float ID_TEXT_SIZE = 30.0f;
        Paint mIdPaint;
        mIdPaint = new Paint();
        mIdPaint.setColor(Color.WHITE);
        mIdPaint.setTextSize(ID_TEXT_SIZE);
        int xPos = (9*canvas.getWidth()/32);
        int yPos = (canvas.getHeight()/4);
        if(mTiltUp == true && mTiltOption == true) {

            canvas.drawText("Tilt Camera Up", xPos, 3*yPos/4, mIdPaint);

        }
        if(mTiltDown == true && mTiltOption == true){
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                canvas.drawText("Tilt Camera Down", xPos, 15*yPos/4, mIdPaint);
            }
            else{
                canvas.drawText("Tilt Camera Down", xPos, 15*yPos/4, mIdPaint);
            }
        }
        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = (float) canvas.getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor = (float) canvas.getHeight() / (float) mPreviewHeight;
            }

            for (Graphic graphic : mGraphics) {
                graphic.draw(canvas);
            }
        }
    }
}
