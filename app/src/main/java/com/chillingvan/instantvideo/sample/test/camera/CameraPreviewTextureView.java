package com.chillingvan.instantvideo.sample.test.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.canvasgl.textureFilter.BasicTextureFilter;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;

/**
 * Created by Leon on 2017/4/19.
 */

public class CameraPreviewTextureView extends GLSurfaceTextureProducerView {

    private TextureFilter textureFilter = new BasicTextureFilter();

    public CameraPreviewTextureView(Context context) {
        super(context);
    }

    public CameraPreviewTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraPreviewTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void setTextureFilter(TextureFilter textureFilter) {
        this.textureFilter = textureFilter;
    }

    @Override
    protected void onGLDraw(ICanvasGL canvas, SurfaceTexture producedSurfaceTexture, RawTexture producedRawTexture, @Nullable SurfaceTexture sharedSurfaceTexture, @Nullable BasicTexture sharedTexture) {
        canvas.drawSurfaceTexture(producedRawTexture, producedSurfaceTexture, 0, 0, producedRawTexture.getWidth(), producedRawTexture.getHeight(), textureFilter);
    }
}
