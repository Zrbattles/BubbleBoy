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
import android.graphics.PointF;

import edu.ucsb.ece150.BubbleBoy.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.util.List;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FindDistance extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;



    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;
    private int mMaskIndex;
    private float mFaceHappiness;

    FindDistance(GraphicOverlay overlay) {
        super(overlay);

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
    //[TODO] change not to draw masks but tell distance from user to person
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        switch (mMaskIndex) {
            case 0:
                float x = translateX(face.getPosition().x + face.getWidth() / 2);
                float y = translateY(face.getPosition().y + face.getHeight() / 2);

                float left = translateX(face.getPosition().x);
                float right = translateX(face.getPosition().x + face.getWidth());
                float bottom = translateY(face.getPosition().y + face.getHeight());
                float top = translateY(face.getPosition().y);

                float id_x = x + ID_X_OFFSET;
                float id_y = y + ID_Y_OFFSET;

                //float hap_x = x - ID_X_OFFSET;
                //float hap_y = y - ID_Y_OFFSET;

                mFaceId = face.getId();
                //mFaceHappiness = face.getIsSmilingProbability();

                // [TODO] Draw real time masks for a single face
                canvas.drawCircle(x,y, FACE_POSITION_RADIUS, mFacePositionPaint);
                canvas.drawRect(left, top, right, bottom, mBoxPaint);
                canvas.drawText("id: " + Integer.toString(mFaceId), id_x, id_y, mIdPaint);
                //canvas.drawText("happiness: "+ Float.toString(mFaceHappiness), hap_x, hap_y, mIdPaint);
                break;
            case 1:
                List<Landmark> landmarks = face.getLandmarks();
                for (int j = 0; j < landmarks.size(); j++) {
                    Landmark mark = landmarks.get(j);
                    PointF point = mark.getPosition();
                    float x2 = translateX(point.x);
                    float y2 = translateY(point.y);
                    canvas.drawCircle(x2,y2, 5f, mFacePositionPaint);
                }
                break;
            default:
                break;
        }
    }
}
