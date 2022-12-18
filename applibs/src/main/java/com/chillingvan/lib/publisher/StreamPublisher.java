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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.util.Loggers;
import com.chillingvan.lib.encoder.MediaCodecInputStream;
import com.chillingvan.lib.encoder.audio.AACEncoder;
import com.chillingvan.lib.encoder.video.H264Encoder;
import com.chillingvan.lib.muxer.IMuxer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Stream: <br>
 * Video: <br>
 * ... Something that can draw things on Surface(Original Surface) - > The shared Surface Texture
 * -> The surface of MediaCodec -> encode data(byte[]) -> RTMPMuxer -> Server
 * Audio: <br>
 * MIC -> AudioRecord -> voice data(byte[]) -> MediaCodec -> encode data(byte[]) -> RTMPMuxer -> Server
 */
public class StreamPublisher {

    public static final int MSG_OPEN = 1;
    public static final int MSG_WRITE_VIDEO = 2;
    private EglContextWrapper eglCtx;
    private IMuxer muxer;
    private AACEncoder aacEncoder;
    private H264Encoder h264Encoder;
    private boolean isStart;

    private HandlerThread writeVideoHandlerThread;

    private Handler writeVideoHandler;
    private StreamPublisherParam param;
    private List<GLTexture> sharedTextureList = new ArrayList<>();

    public StreamPublisher(EglContextWrapper eglCtx, IMuxer muxer) {
        this.eglCtx = eglCtx;
        this.muxer = muxer;
    }


    public void prepareEncoder(final StreamPublisherParam param, H264Encoder.OnDrawListener onDrawListener) {
        this.param = param;

        try {
            h264Encoder = new H264Encoder(param, eglCtx);
            for (GLTexture texture :sharedTextureList ) {
                h264Encoder.addSharedTexture(texture);
            }
            h264Encoder.setOnDrawListener(onDrawListener);
            aacEncoder = new AACEncoder(param);
            aacEncoder.setOnDataComingCallback(new AACEncoder.OnDataComingCallback() {
                private byte[] writeBuffer = new byte[param.audioBitRate / 8];

                @Override
                public void onComing() {
                    MediaCodecInputStream mediaCodecInputStream = aacEncoder.getMediaCodecInputStream();
                    MediaCodecInputStream.readAll(mediaCodecInputStream, writeBuffer, new MediaCodecInputStream.OnReadAllCallback() {
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
            private byte[] writeBuffer = new byte[param.videoBitRate / 8 / 2];

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MSG_WRITE_VIDEO) {
                    MediaCodecInputStream mediaCodecInputStream = h264Encoder.getMediaCodecInputStream();
                    MediaCodecInputStream.readAll(mediaCodecInputStream, writeBuffer, new MediaCodecInputStream.OnReadAllCallback() {
                        @Override
                        public void onReadOnce(byte[] buffer, int readSize, MediaCodec.BufferInfo bufferInfo) {
                            if (readSize <= 0) {
                                return;
                            }
                            Loggers.d("StreamPublisher", String.format("onReadOnce: %d", readSize));
                            muxer.writeVideo(buffer, 0, readSize, bufferInfo);
                        }
                    });
                }
            }
        };
    }

    public void clearTextures() {
        sharedTextureList.clear();
    }

    public void addSharedTexture(GLTexture outsideTexture) {
        sharedTextureList.add(outsideTexture);
    }


    public void start() throws IOException {
        if (!isStart) {
            if (muxer.open(param) <= 0) {
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
            h264Encoder.requestRender();
            writeVideoHandler.sendEmptyMessage(MSG_WRITE_VIDEO);
            return true;
        }
        return false;
    }

    public static class StreamPublisherParam {

        public static final int DEFAULT_CHANNEL_CNT = 1;

        public int width = 640;
        public int height = 480;
        public int videoBitRate = 2949120;
        public int frameRate = 30;
        public int iframeInterval = 5;
        public int samplingRate;
        public int audioBitRate;
        public int audioSource;
        public int channelCfg;
        public int channelCnt;

        public String videoMIMEType = "video/avc";
        public String audioMIME = "audio/mp4a-latm";
        public int audioBufferSize;

        public String outputFilePath;
        public String outputUrl;
        private MediaFormat videoOutputMediaFormat;
        private MediaFormat audioOutputMediaFormat;

        private int initialTextureCount = 1;

        public StreamPublisherParam() {
            this(640, 480, 2949120, 30, 5, 44100, 32000, MediaRecorder.AudioSource.VOICE_COMMUNICATION, AudioFormat.CHANNEL_IN_MONO, DEFAULT_CHANNEL_CNT);
        }

        private StreamPublisherParam(int width, int height, int videoBitRate, int frameRate,
                                    int iframeInterval, int samplingRate, int audioBitRate, int audioSource, int channelCfg, int channelCnt) {
            this.width = width;
            this.height = height;
            this.videoBitRate = videoBitRate;
            this.frameRate = frameRate;
            this.iframeInterval = iframeInterval;
            this.samplingRate = samplingRate;
            this.audioBitRate = audioBitRate;
            this.audioBufferSize = AudioRecord.getMinBufferSize(samplingRate, channelCfg, AudioFormat.ENCODING_PCM_16BIT) * 2;
            this.audioSource = audioSource;
            this.channelCfg = channelCfg;
            this.channelCnt = channelCnt;
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

        public int getInitialTextureCount() {
            return initialTextureCount;
        }

        public MediaFormat createVideoMediaFormat() {
            MediaFormat format = MediaFormat.createVideoFormat(videoMIMEType, width, height);

            // Set some properties.  Failing to specify some of these can cause the MediaCodec
            // configure() call to throw an unhelpful exception.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iframeInterval);
            return format;
        }

        public MediaFormat createAudioMediaFormat() {
            MediaFormat format = MediaFormat.createAudioFormat(audioMIME, samplingRate, channelCnt);
            format.setInteger(MediaFormat.KEY_BIT_RATE, audioBitRate);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioBufferSize);

            return format;
        }

        public void setVideoOutputMediaFormat(MediaFormat videoOutputMediaFormat) {
            this.videoOutputMediaFormat = videoOutputMediaFormat;
        }

        public void setAudioOutputMediaFormat(MediaFormat audioOutputMediaFormat) {
            this.audioOutputMediaFormat = audioOutputMediaFormat;
        }

        public MediaFormat getVideoOutputMediaFormat() {
            return videoOutputMediaFormat;
        }

        public MediaFormat getAudioOutputMediaFormat() {
            return audioOutputMediaFormat;
        }

        public static class Builder {
            private int width = 640;
            private int height = 480;
            private int videoBitRate = 2949120;
            private int frameRate = 30;
            private int iframeInterval = 5;
            private int samplingRate = 44100;
            private int audioBitRate = 32000;
            private int audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
            private int channelCfg = AudioFormat.CHANNEL_IN_MONO;
            private int channelCnt = 1;

            public Builder setWidth(int width) {
                this.width = width;
                return this;
            }

            public Builder setHeight(int height) {
                this.height = height;
                return this;
            }

            public Builder setVideoBitRate(int videoBitRate) {
                this.videoBitRate = videoBitRate;
                return this;
            }

            public Builder setFrameRate(int frameRate) {
                this.frameRate = frameRate;
                return this;
            }

            public Builder setIframeInterval(int iframeInterval) {
                this.iframeInterval = iframeInterval;
                return this;
            }

            public Builder setSamplingRate(int samplingRate) {
                this.samplingRate = samplingRate;
                return this;
            }

            public Builder setAudioBitRate(int audioBitRate) {
                this.audioBitRate = audioBitRate;
                return this;
            }

            public Builder setAudioSource(int audioSource) {
                this.audioSource = audioSource;
                return this;
            }

            public Builder setChannelCfg(int channelCfg) {
                this.channelCfg = channelCfg;
                return this;
            }

            public void setChannelCnt(int channelCnt) {
                this.channelCnt = channelCnt;
            }

            public StreamPublisherParam createStreamPublisherParam() {
                return new StreamPublisherParam(width, height, videoBitRate, frameRate, iframeInterval, samplingRate, audioBitRate, audioSource, channelCfg, channelCnt);
            }
        }
    }

}
