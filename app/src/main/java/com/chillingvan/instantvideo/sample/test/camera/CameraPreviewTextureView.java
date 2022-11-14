package com.chillingvan.instantvideo.sample.test.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glview.texture.GLMultiTexProducerView;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.instantvideo.sample.test.VideoFrameHandlerHelper;
import com.chillingvan.lib.encoder.video.H264Encoder;

import java.util.List;

/**
 * Created by Leon on 2017/4/19.
 */

public class CameraPreviewTextureView extends GLMultiTexProducerView {

    private H264Encoder.OnDrawListener onDrawListener;
    private VideoFrameHandlerHelper videoFrameHandlerHelper = new VideoFrameHandlerHelper(getContext().getApplicationContext());

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
        videoFrameHandlerHelper.initDrawHelper(width, height);
    }

    @Override
    protected void onGLDraw(ICanvasGL canvas, List<GLTexture> producedTextures, List<GLTexture> consumedTextures) {
        onDrawListener.onGLDraw(canvas, producedTextures, consumedTextures);
        videoFrameHandlerHelper.drawText(canvas);
    }

    public void setOnDrawListener(H264Encoder.OnDrawListener l) {
        onDrawListener = l;
    }
}
