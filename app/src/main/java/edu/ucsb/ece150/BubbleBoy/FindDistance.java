/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.ucsb.ece150.BubbleBoy;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import edu.ucsb.ece150.BubbleBoy.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FindDistance extends GraphicOverlay.Graphic {
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;
    private int mMaskIndex;
    private boolean mTooClose = false;

    FindDistance(GraphicOverlay overlay) {
        super(overlay);

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        int selectedColor = COLOR_CHOICES[2];
        if(mTooClose == true) {
            selectedColor = COLOR_CHOICES[4];
        }

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(Color.BLUE);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    void setId(int id) {
        mFaceId = id;
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    void updateMask(int maskIndex) {
        mMaskIndex = maskIndex;
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    void updateTooClose(boolean TooClose) {mTooClose = TooClose; }
    
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }
        if(mTooClose == true){
            mBoxPaint.setColor(Color.RED);
        }
        else {
            mBoxPaint.setColor(Color.GREEN);
        }



        switch (mMaskIndex) {
            default:
                float left = translateX(face.getPosition().x);
                float right = translateX(face.getPosition().x + face.getWidth());
                float bottom = translateY(face.getPosition().y + face.getHeight());
                float top = translateY(face.getPosition().y);



                mFaceId = face.getId();
                //Draw square around each persons face
                canvas.drawRect(left, top, right, bottom, mBoxPaint);
                break;
        }
    }
}
