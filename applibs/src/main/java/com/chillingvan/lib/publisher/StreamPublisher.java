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

package com.chillingvan.lib.publisher;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.chillingvan.canvasgl.Loggers;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.lib.encoder.MediaCodecInputStream;
import com.chillingvan.lib.encoder.audio.AACEncoder;
import com.chillingvan.lib.encoder.video.H264Encoder;
import com.chillingvan.lib.muxer.IMuxer;

import java.io.IOException;

/**
 * Created by Chilling on 2017/5/21.
 */

public class StreamPublisher {

    public static final int MSG_OPEN = 1;
    public static final int MSG_WRITE_VIDEO = 2;
    private BasicTexture outsideTexture;
    private EglContextWrapper eglCtx;
    private IMuxer muxer;
    private SurfaceTexture outsideSurfaceTexture;
    private AACEncoder aacEncoder;
    private H264Encoder h264Encoder;
    private boolean isStart;

    private HandlerThread writeVideoHandlerThread;

    private Handler writeVideoHandler;
    private StreamPublisherParam param = new StreamPublisherParam();

    {
    }

    public StreamPublisher(EglContextWrapper eglCtx, IMuxer muxer) {
        this.eglCtx = eglCtx;
        this.muxer = muxer;
    }


    public void prepareEncoder(final StreamPublisherParam param, H264Encoder.OnDrawListener onDrawListener) {
        this.param = param;

        try {
            h264Encoder = new H264Encoder(param.width, param.height, param.videoBitRate, param.frameRate, param.iframeInterval, eglCtx);
            h264Encoder.setSharedTexture(outsideTexture, outsideSurfaceTexture);
            h264Encoder.setOnDrawListener(onDrawListener);
            aacEncoder = new AACEncoder(param.samplingRate, param.audioBitRate);
            aacEncoder.setOnDataComingCallback(new AACEncoder.OnDataComingCallback() {
                private byte[] writeBuffer = new byte[param.audioBitRate/8];
                @Override
                public void onComing() {
                    MediaCodecInputStream mediaCodecInputStream = aacEncoder.getMediaCodecInputStream();
                    MediaCodecInputStream.readAll(mediaCodecInputStream, writeBuffer, 0, new MediaCodecInputStream.OnReadAllCallback() {
                        @Override
                        public void onReadOnce(byte[] buffer, int readSize, MediaCodec.BufferInfo bufferInfo) {
                            if (readSize <= 0) {
                                return;
                            }
                            muxer.writeAudio(buffer, 0, readSize, bufferInfo);
                        }
                    });
                }
            });

        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }

        writeVideoHandlerThread = new HandlerThread("WriteVideoHandlerThread");
        writeVideoHandlerThread.start();
        writeVideoHandler = new Handler(writeVideoHandlerThread.getLooper()) {
            private byte[] writeBuffer = new byte[param.videoBitRate/8];
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MSG_WRITE_VIDEO) {
                    MediaCodecInputStream mediaCodecInputStream = h264Encoder.getMediaCodecInputStream();
                    MediaCodecInputStream.readAll(mediaCodecInputStream, writeBuffer, 0, new MediaCodecInputStream.OnReadAllCallback() {
                        @Override
                        public void onReadOnce(byte[] buffer, int readSize, MediaCodec.BufferInfo bufferInfo) {
                            if (readSize <= 0) {
                                return;
                            }
                            muxer.writeVideo(buffer, 0, readSize, bufferInfo);
                        }
                    });
                }
            }
        };
    }

    public void setSharedTexture(BasicTexture outsideTexture, SurfaceTexture outsideSurfaceTexture) {
        this.outsideTexture = outsideTexture;
        this.outsideSurfaceTexture = outsideSurfaceTexture;
    }


    public void start(String url, int width, int height) throws IOException {
        if (!isStart) {
            if (muxer.open(url, width, height) < 0) {
                Loggers.e("StreamPublisher", "muxer open fail");
                throw new IOException("muxer open fail");
            }
            h264Encoder.start();
            aacEncoder.start();
            isStart = true;
        }

    }

    public void close() {
        isStart = false;
        if (h264Encoder != null) {
            h264Encoder.close();
        }

        if (aacEncoder != null) {
            aacEncoder.close();
        }
        if (writeVideoHandlerThread != null) {
            writeVideoHandlerThread.quitSafely();
        }
        if (muxer != null) {
            muxer.close();
        }
    }

    public boolean isStart() {
        return isStart;
    }


    public boolean drawAFrame() {
        if (isStart) {
            h264Encoder.requestRenderAndWait();
            writeVideoHandler.sendEmptyMessage(MSG_WRITE_VIDEO);
            return true;
        }
        return false;
    }

    public static class StreamPublisherParam {
        public int width = 640;
        public int height = 480;
        public int videoBitRate = 2949120;
        public int frameRate = 30;
        public int iframeInterval = 5;
        public int samplingRate = 44100;
        public int audioBitRate = 192000;

        public StreamPublisherParam() {
        }

        public StreamPublisherParam(int width, int height, int videoBitRate, int frameRate, int iframeInterval, int samplingRate, int audioBitRate) {
            this.width = width;
            this.height = height;
            this.videoBitRate = videoBitRate;
            this.frameRate = frameRate;
            this.iframeInterval = iframeInterval;
            this.samplingRate = samplingRate;
            this.audioBitRate = audioBitRate;
        }
    }

}
