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

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.view.Surface;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.MultiTexOffScreenCanvas;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.util.Loggers;
import com.chillingvan.lib.encoder.MediaCodecInputStream;
import com.chillingvan.lib.publisher.StreamPublisher;

import java.io.IOException;
import java.util.List;

/**
 * Data Stream:
 *
 * The texture of {@link H264Encoder#addSharedTexture} -> Surface of MediaCodec -> encode data(byte[])
 *
 */
public class H264Encoder {

    private final Surface mInputSurface;
    private final MediaCodecInputStream mediaCodecInputStream;
    MediaCodec mEncoder;

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 5;           // 5 seconds between I-frames
    protected final EncoderCanvas offScreenCanvas;
    private OnDrawListener onDrawListener;
    private boolean isStart;
    private int initialTextureCount = 1;


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

        this.initialTextureCount = params.getInitialTextureCount();
        offScreenCanvas = new EncoderCanvas(params.width, params.height, eglCtx);
    }

    /**
     * If called, should be called before start() called.
     */
    public void addSharedTexture(GLTexture texture) {
        offScreenCanvas.addConsumeGLTexture(texture);
    }


    public Surface getInputSurface() {
        return mInputSurface;
    }

    public MediaCodecInputStream getMediaCodecInputStream() {
        return mediaCodecInputStream;
    }

    /**
     *
     * @param initialTextureCount Default is 1
     */
    public void setInitialTextureCount(int initialTextureCount) {
        if (initialTextureCount < 1) {
            throw new IllegalArgumentException("initialTextureCount must >= 1");
        }
        this.initialTextureCount = initialTextureCount;
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
        /**
         * Called when a frame is ready to be drawn.
         * @param canvasGL The gl canvas
         * @param producedTextures The textures produced by internal. These can be used for camera or video decoder to render.
         * @param consumedTextures See {@link #addSharedTexture(GLTexture)}. The textures you set from outside. Then you can draw the textures render by other Views of OffscreenCanvas.
         */
        void onGLDraw(ICanvasGL canvasGL, List<GLTexture> producedTextures, List<GLTexture> consumedTextures);
    }

    private class EncoderCanvas extends MultiTexOffScreenCanvas {
        public EncoderCanvas(int width, int height, EglContextWrapper eglCtx) {
            super(width, height, eglCtx, H264Encoder.this.mInputSurface);
        }

        @Override
        protected void onGLDraw(ICanvasGL canvas, List<GLTexture> producedTextures, List<GLTexture> consumedTextures) {
            if (onDrawListener != null) {
                onDrawListener.onGLDraw(canvas, producedTextures, consumedTextures);
            }
        }

        @Override
        protected int getInitialTexCount() {
            return initialTextureCount;
        }
    }
}
