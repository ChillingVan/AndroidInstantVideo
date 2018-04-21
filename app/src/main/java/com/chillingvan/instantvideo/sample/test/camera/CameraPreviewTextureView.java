package com.chillingvan.instantvideo.sample.test.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.androidCanvas.IAndroidCanvasHelper;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.lib.encoder.video.H264Encoder;

/**
 * Created by Leon on 2017/4/19.
 */

public class CameraPreviewTextureView extends GLSurfaceTextureProducerView {

    private H264Encoder.OnDrawListener onDrawListener;
    private IAndroidCanvasHelper drawTextHelper = IAndroidCanvasHelper.Factory.createAndroidCanvasHelper(IAndroidCanvasHelper.MODE.MODE_ASYNC);
    private Paint textPaint;

    public CameraPreviewTextureView(Context context) {
        super(context);
    }

    public CameraPreviewTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraPreviewTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        super.onSurfaceChanged(width, height);
        drawTextHelper.init(width, height);
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp2px(15));
    }

    private float dp2px(int dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onGLDraw(final ICanvasGL canvas, SurfaceTexture producedSurfaceTexture, RawTexture producedRawTexture, @Nullable SurfaceTexture sharedSurfaceTexture, @Nullable BasicTexture sharedTexture) {
        onDrawListener.onGLDraw(canvas, producedSurfaceTexture, producedRawTexture, sharedSurfaceTexture, sharedTexture);
        drawTextHelper.draw(new IAndroidCanvasHelper.CanvasPainter() {
            @Override
            public void draw(Canvas androidCanvas) {
                androidCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                androidCanvas.drawText("白色, White", 100, 100, textPaint);
            }
        });
        Bitmap outputBitmap = drawTextHelper.getOutputBitmap();
        canvas.invalidateTextureContent(outputBitmap);
        canvas.drawBitmap(outputBitmap, 0, 0);
    }

    public void setOnDrawListener(H264Encoder.OnDrawListener l) {
        onDrawListener = l;
    }
}
