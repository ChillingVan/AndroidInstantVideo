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
import com.chillingvan.canvasgl.OffScreenCanvas;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.util.Loggers;
import com.chillingvan.lib.encoder.MediaCodecInputStream;
import com.chillingvan.lib.publisher.StreamPublisher;

import java.io.IOException;

/**
 * Data Stream:
 *
 * The texture of {@link H264Encoder#setSharedTexture} -> Surface of MediaCodec -> encode data(byte[])
 *
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


    public H264Encoder(StreamPublisher.StreamPublisherParam params) throws IOException {
        this(params, EglContextWrapper.EGL_NO_CONTEXT_WRAPPER);
    }


    /**
     *
     * @param eglCtx can be EGL10.EGL_NO_CONTEXT or outside context
     */
    public H264Encoder(final StreamPublisher.StreamPublisherParam params, final EglContextWrapper eglCtx) throws IOException {

//        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, params.width, params.height);
        MediaFormat format = params.createVideoMediaFormat();


        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, params.videoBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, params.frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, params.iframeInterval);
        mEncoder = MediaCodec.createEncoderByType(params.videoMIMEType);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();
        mediaCodecInputStream = new MediaCodecInputStream(mEncoder, new MediaCodecInputStream.MediaFormatCallback() {
            @Override
            public void onChangeMediaFormat(MediaFormat mediaFormat) {
                params.setVideoOutputMediaFormat(mediaFormat);
            }
        });

        offScreenCanvas = new EncoderCanvas(params.width, params.height, eglCtx);
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

    public void close() {
        if (!isStart) return;

        Loggers.d("H264Encoder", "close");
        offScreenCanvas.end();
        mediaCodecInputStream.close();
        synchronized (mEncoder) {
            mEncoder.stop();
            mEncoder.release();
        }
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
