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

package com.chillingvan.lib.encoder.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.chillingvan.canvasgl.Loggers;
import com.chillingvan.lib.encoder.MediaCodecInputStream;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Chilling on 2017/1/11.
 */

public class AACEncoder {

    private static final String TAG = "AACEncoder";

    private AudioRecord mAudioRecord;
    private final MediaCodec mMediaCodec;
    private final MediaCodecInputStream mediaCodecInputStream;
    private final Thread mThread;
    private OnDataComingCallback onDataComingCallback;
    private long startWhen;

    public AACEncoder(final int samplingRate, int bitRate) throws IOException {

        final int bufferSize = AudioRecord.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 2;


        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, samplingRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        mMediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
        MediaFormat format = MediaFormat.createAudioFormat("audio/mp4a-latm", samplingRate, 2);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mAudioRecord.startRecording();
        mMediaCodec.start();
        startWhen = System.nanoTime();

        mediaCodecInputStream = new MediaCodecInputStream(mMediaCodec);
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();


        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int len = 0, bufferIndex = 0;
                try {
                    while (!Thread.interrupted()) {
                        bufferIndex = mMediaCodec.dequeueInputBuffer(10000);
                        if (bufferIndex >= 0) {
                            inputBuffers[bufferIndex].clear();
                            long presentationTimeNs = System.nanoTime();
                            len = mAudioRecord.read(inputBuffers[bufferIndex], bufferSize);
                            presentationTimeNs -= (len / samplingRate ) / 1000000000;
                            Loggers.i(TAG, "Index: " + bufferIndex + " len: " + len + " buffer_capacity: " + inputBuffers[bufferIndex].capacity());
                            long presentationTimeUs = (presentationTimeNs - startWhen) / 1000;
                            if (len == AudioRecord.ERROR_INVALID_OPERATION || len == AudioRecord.ERROR_BAD_VALUE) {
                                Log.e(TAG, "An error occured with the AudioRecord API !");
                            } else {
                                mMediaCodec.queueInputBuffer(bufferIndex, 0, len, presentationTimeUs, 0);
                                if (onDataComingCallback != null) {
                                    onDataComingCallback.onComing();
                                }
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        });

        mThread.start();
    }

    public void setOnDataComingCallback(OnDataComingCallback onDataComingCallback) {
        this.onDataComingCallback = onDataComingCallback;
    }

    public interface OnDataComingCallback {
        void onComing();
    }


    public MediaCodecInputStream getMediaCodecInputStream() {
        return mediaCodecInputStream;
    }


    public synchronized void stop() {
        Loggers.d(TAG, "Interrupting threads...");
        mThread.interrupt();
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
    }
}
