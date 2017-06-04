/*
 *
 *  *
 *  *  * Copyright (C) 2017 ChillingVan
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package com.chillingvan.lib.encoder.video;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.support.annotation.Nullable;
import android.view.Surface;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.Loggers;
import com.chillingvan.canvasgl.OffScreenCanvas;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.lib.encoder.MediaCodecInputStream;

import java.io.IOException;

/**
 * Created by Chilling on 2016/12/11.
 */

public class H264Encoder {

    private final Surface mInputSurface;
    private final MediaCodecInputStream mediaCodecInputStream;
    MediaCodec mEncoder;

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 5;           // 5 seconds between I-frames
    private final EncoderCanvas offScreenCanvas;
    private OnDrawListener onDrawListener;
    private boolean isStart;


    public H264Encoder(int width, int height, int bitRate, int frameRate, int iframeInterval) throws IOException {
        this(width, height, bitRate, frameRate, iframeInterval, EglContextWrapper.EGL_NO_CONTEXT_WRAPPER);
    }


    /**
     *
     * @param width width
     * @param height height
     * @param bitRate bitRate default 500000
     * @param frameRate frameRate default 20
     * @param iframeInterval iframeInterval default 1
     * @param eglCtx can be EGL10.EGL_NO_CONTEXT or outside context
     * @throws IOException
     */
    public H264Encoder(int width, int height, int bitRate, int frameRate, int iframeInterval, final EglContextWrapper eglCtx) throws IOException {

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iframeInterval);
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();
        mediaCodecInputStream = new MediaCodecInputStream(mEncoder);

        offScreenCanvas = new EncoderCanvas(width, height, eglCtx);
    }

    /**
     * If called, should be called before start() called.
     */
    public void setSharedTexture(BasicTexture outsideTexture, SurfaceTexture outsideSurfaceTexture) {
        offScreenCanvas.setSharedTexture(outsideTexture, outsideSurfaceTexture);
    }


    public Surface getInputSurface() {
        return mInputSurface;
    }

    public MediaCodecInputStream getMediaCodecInputStream() {
        return mediaCodecInputStream;
    }

    public void start() {
        offScreenCanvas.start();
        isStart = true;
    }

    public void stop() {
        Loggers.d("H264Encoder", "stop: ");
        offScreenCanvas.onPause();
        mEncoder.stop();
        isStart = false;
    }

    public void release() {
        offScreenCanvas.end();
        mEncoder.stop();
        mEncoder.release();
        isStart = false;
    }

    public boolean isStart() {
        return isStart;
    }

    public void requestRender() {
        offScreenCanvas.requestRender();
    }


    public void requestRenderAndWait() {
        offScreenCanvas.requestRenderAndWait();
    }

    public void setOnDrawListener(OnDrawListener l) {
        this.onDrawListener = l;
    }

    public interface OnDrawListener {
        void onGLDraw(ICanvasGL canvasGL, SurfaceTexture surfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture);
    }

    private class EncoderCanvas extends OffScreenCanvas {
        public EncoderCanvas(int width, int height, EglContextWrapper eglCtx) {
            super(width, height, eglCtx, H264Encoder.this.mInputSurface);
        }



        @Override
        protected void onGLDraw(ICanvasGL iCanvasGL, SurfaceTexture surfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {
            if (onDrawListener != null) {
                onDrawListener.onGLDraw(iCanvasGL, surfaceTexture, rawTexture, outsideSurfaceTexture, outsideTexture);
            }
        }
    }
}
