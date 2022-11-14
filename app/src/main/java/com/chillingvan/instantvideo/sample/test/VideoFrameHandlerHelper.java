package com.chillingvan.instantvideo.sample.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.androidCanvas.IAndroidCanvasHelper;
import com.chillingvan.instantvideo.sample.util.ScreenUtil;

/**
 * Created by Chilling on 2022/11/14.
 */

public class VideoFrameHandlerHelper {

    private final Context context;

    public VideoFrameHandlerHelper(Context context) {
        this.context = context;
    }

    private IAndroidCanvasHelper drawTextHelper = IAndroidCanvasHelper.Factory.createAndroidCanvasHelper(IAndroidCanvasHelper.MODE.MODE_ASYNC);
    private Paint textPaint = new Paint();

    {
        initTextPaint();
    }

    private void initTextPaint() {
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(ScreenUtil.dpToPx(context, 15));
    }

    public void initDrawHelper(int width, int height) {
        drawTextHelper.init(width, height);
    }

    public void drawText(ICanvasGL canvasGL) {
        drawTextHelper.draw(new IAndroidCanvasHelper.CanvasPainter() {
            @Override
            public void draw(Canvas androidCanvas, Bitmap drawingBitmap) {
                androidCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                androidCanvas.drawText("白色, White", 100, 100, textPaint);
            }
        });
        Bitmap outputBitmap = drawTextHelper.getOutputBitmap();
        canvasGL.invalidateTextureContent(outputBitmap);
        canvasGL.drawBitmap(outputBitmap, 0, 0);
    }
}
