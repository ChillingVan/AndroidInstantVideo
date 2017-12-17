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
import android.media.MediaRecorder;
import android.util.Log;

import com.chillingvan.canvasgl.Loggers;
import com.chillingvan.lib.encoder.MediaCodecInputStream;
import com.chillingvan.lib.publisher.StreamPublisher;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Data Stream:
 * MIC -> AudioRecord -> voice data(byte[]) -> MediaCodec -> encode data(byte[])
 */

public class AACEncoder {

    private static final String TAG = "AACEncoder";

    private AudioRecord mAudioRecord;
    private final MediaCodec mMediaCodec;
    private final MediaCodecInputStream mediaCodecInputStream;
    private Thread mThread;
    private OnDataComingCallback onDataComingCallback;
    private int samplingRate;
    private final int bufferSize;
    private boolean isStart;

    public AACEncoder(StreamPublisher.StreamPublisherParam params) throws IOException {
        this.samplingRate = params.samplingRate;

        bufferSize = params.audioBufferSize;
        mMediaCodec = MediaCodec.createEncoderByType(params.audioMIME);
        mMediaCodec.configure(params.createAudioMediaFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mediaCodecInputStream = new MediaCodecInputStream(mMediaCodec);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, samplingRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

    }

    public void start() {
        mAudioRecord.startRecording();
        mMediaCodec.start();
        final long startWhen = System.nanoTime();
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int len, bufferIndex;
                while (isStart && !Thread.interrupted()) {
                    synchronized (mMediaCodec) {
                        if (!isStart) return;
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
                }
            }
        });

        mThread.start();
        isStart = true;
    }


    public static void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE


        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
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


    public synchronized void close() {
        if (!isStart) {
            return;
        }
        Loggers.d(TAG, "Interrupting threads...");
        isStart = false;
        mThread.interrupt();
        mediaCodecInputStream.close();
        synchronized (mMediaCodec) {
            mMediaCodec.stop();
            mMediaCodec.release();
        }
        mAudioRecord.stop();
        mAudioRecord.release();
    }

}
