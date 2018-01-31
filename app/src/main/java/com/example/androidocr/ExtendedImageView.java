package com.example.androidocr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;

import java.util.List;

public class ExtendedImageView extends android.support.v7.widget.AppCompatImageView {

    private List<RectF> mRects;
    private Paint mPaint;
    private Matrix mMatrix;

    public ExtendedImageView(Context context) {
        super(context);
    }

    public ExtendedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mMatrix = new Matrix();
        mMatrix.postScale(0.246875f, 0.252206809583859f);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2.0f);
    }

    public ExtendedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mRects != null) {
            for (RectF rect : mRects) {
                getImageMatrix().mapRect(rect);
                //getImageMatrix().postTranslate(rect.left, rect.top);
                canvas.drawRect(rect, mPaint);
            }
        }
    }

    public void addRects(List<RectF> rects) {
        mRects = rects;
        postInvalidate();
    }
}