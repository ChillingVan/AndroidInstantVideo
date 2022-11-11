package com.chillingvan.instantvideo.sample.test.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.androidCanvas.IAndroidCanvasHelper;
import com.chillingvan.canvasgl.glview.texture.GLMultiTexProducerView;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.lib.encoder.video.H264Encoder;

import java.util.List;

/**
 * Created by Leon on 2017/4/19.
 */

public class CameraPreviewTextureView extends GLMultiTexProducerView {

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
    protected int getInitialTexCount() {
        return 2;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        super.onSurfaceTextureAvailable(surface, width, height);
        if (mSharedEglContext == null) {
            setSharedEglContext(EglContextWrapper.EGL_NO_CONTEXT_WRAPPER);
        }
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        super.onSurfaceChanged(width, height);
        drawTextHelper.init(width, height);
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp2px(15));
    }

    @Override
    protected void onGLDraw(ICanvasGL canvas, List<GLTexture> producedTextures, List<GLTexture> consumedTextures) {
        onDrawListener.onGLDraw(canvas, producedTextures, consumedTextures);
        drawTextHelper.draw(new IAndroidCanvasHelper.CanvasPainter() {
            @Override
            public void draw(Canvas androidCanvas, Bitmap drawingBitmap) {
                androidCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                androidCanvas.drawText("白色, White", 100, 100, textPaint);
            }
        });
        Bitmap outputBitmap = drawTextHelper.getOutputBitmap();
        canvas.invalidateTextureContent(outputBitmap);
        canvas.drawBitmap(outputBitmap, 0, 0);
    }

    private float dp2px(int dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    public void setOnDrawListener(H264Encoder.OnDrawListener l) {
        onDrawListener = l;
    }
}
