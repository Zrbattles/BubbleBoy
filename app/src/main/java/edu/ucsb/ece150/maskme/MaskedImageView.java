package edu.ucsb.ece150.maskme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.SparseArray;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.util.List;

public class MaskedImageView extends AppCompatImageView {
    private enum MaskType {
        NOMASK, FIRST, SECOND
    }

    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
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

    private SparseArray<Face> faces = null;
    private MaskType maskType = MaskType.NOMASK;
    Paint mPaint = new Paint();
    Paint mPathPaint = new Paint();
    private Bitmap mBitmap;

    private Path mPath = new Path();
    private Path mPathSave = new Path();
    private float mX, mY;

    public MaskedImageView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mBitmap = ((BitmapDrawable) getDrawable()).getBitmap();
        if(mBitmap == null){
            return;
        }
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
        drawBitmap(canvas, scale);
        canvas.drawPath(mPathSave,mPathPaint);

        switch (maskType){
            case FIRST:
                drawFirstMaskOnCanvas(canvas, scale);
                break;
            case SECOND:
                drawSecondMaskOnCanvas(canvas, scale);
                break;
        }
    }

    protected void drawFirstMask(SparseArray<Face> faces){
        this.faces = faces;
        this.maskType = MaskType.FIRST;
        this.invalidate();
    }

    protected void drawSecondMask(SparseArray<Face> faces){
        this.faces = faces;
        this.maskType = MaskType.SECOND;
        this.invalidate();
    }

    private void drawBitmap(Canvas canvas, double scale) {
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();

        Rect destBounds = new Rect(0, 0, (int)(imageWidth * scale), (int)(imageHeight * scale));
        canvas.drawBitmap(mBitmap, null, destBounds, null);
    }

    private void drawFirstMaskOnCanvas(Canvas canvas, double scale) {

        // [TODO] Draw first type of mask on the static photo
        // 1. set properties of mPaint
        Paint mFacePositionPaint = new Paint();
        Paint mIdPaint = new Paint();
        Paint mBoxPaint = new Paint();
        mIdPaint.setTextSize(ID_TEXT_SIZE);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

        // 2. get positions of faces and draw masks on faces.
        for(int i = 0; i < faces.size(); i++) {
            final int selectedColor = COLOR_CHOICES[i % COLOR_CHOICES.length];
            mFacePositionPaint.setColor(selectedColor);
            mIdPaint.setColor(selectedColor);
            mBoxPaint.setColor(selectedColor);

            Face face = faces.get(faces.keyAt(i));
            float x = (face.getPosition().x + face.getWidth() / 2) * (float)scale;
            float y = (face.getPosition().y + face.getHeight() / 2) * (float)scale;

            float left = (face.getPosition().x) * (float)scale;
            float right = (face.getPosition().x + face.getWidth()) * (float)scale;
            float bottom = (face.getPosition().y + face.getHeight()) * (float)scale;
            float top = (face.getPosition().y) * (float)scale;

            float id_x = x + (ID_X_OFFSET * (float)scale);
            float id_y = y + (ID_Y_OFFSET * (float)scale);

            //float hap_x = x - (ID_X_OFFSET * (float)scale);
            //float hap_y = y - (ID_Y_OFFSET * (float)scale);

            int mFaceId = i;
            //float mFaceHappiness = face.getIsSmilingProbability();

            // [TODO] Draw real time masks for a single face
            canvas.drawCircle(x,y, FACE_POSITION_RADIUS, mFacePositionPaint);
            canvas.drawRect(left, top, right, bottom, mBoxPaint);
            canvas.drawText("id: " + Integer.toString(mFaceId), id_x, id_y, mIdPaint);
            //canvas.drawText("happiness: "+ Float.toString(mFaceHappiness), hap_x, hap_y, mIdPaint);
        }
    }

    private void drawSecondMaskOnCanvas( Canvas canvas, double scale ) {
        // [TODO] Draw second type of mask on the static photo
        // 1. set properties of mPaint
        mPaint.setStrokeWidth(1f);
        // 2. get positions of faces and draw masks on faces.
        if (faces == null) {
            return;
        }
        for(int i = 0; i < faces.size(); i++) {
            final int selectedColor = COLOR_CHOICES[i % COLOR_CHOICES.length];
            mPaint.setColor(selectedColor);
            Face face = faces.get(faces.keyAt(i));
            List<Landmark> landmarks = face.getLandmarks();
            for (int j = 0; j < landmarks.size(); j++) {
                Landmark mark = landmarks.get(j);
                PointF point = mark.getPosition();
                float x = point.x * (float)scale;
                float y = point.y * (float)scale;
                canvas.drawCircle(x,y, 5f, mPaint);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        mPathPaint.setDither(true);
        mPathPaint.setColor(Color.RED);
        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setStrokeJoin(Paint.Join.ROUND);
        mPathPaint.setStrokeCap(Paint.Cap.ROUND);
        mPathPaint.setStrokeWidth(20);

        canvas.drawPath(mPath, mPathPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPath.moveTo(x, y);
                mX = x;
                mY = y;
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(x - mX) >= 3 || Math.abs(y - mY) >= 3) {
                    mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                    mX = x;
                    mY = y;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                mPath.lineTo(mX, mY);
                mPathSave.addPath(mPath);
                mPath.reset();
                invalidate();
                break;
            default:
                invalidate();
                break;
        }
        return true;
    }

    public void noFaces() {
        faces = null;
    }

    public void clearPath() {
        mPathSave.reset();
    }

    public void reset() {
        faces = null;
        setImageBitmap(null);
    }
}
