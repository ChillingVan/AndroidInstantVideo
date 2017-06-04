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

package com.chillingvan.lib.muxer;

import android.media.MediaCodec;

import com.chillingvan.canvasgl.Loggers;

import net.butterflytv.rtmp_client.RTMPMuxer;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Chilling on 2017/5/29.
 */

public class RTMPStreamMuxer implements IMuxer {


    public static final int KEEP_COUNT = 30;
    private String filename = "";
    private RTMPMuxer rtmpMuxer;

    private long lastVideoTime;
    private long lastAudioTime;
    private int totalVideoTime;
    private int totalAudioTime;


    private List<Frame> frameQueue = new LinkedList<>();

    public RTMPStreamMuxer() {
        this("");
    }

    public RTMPStreamMuxer(String filename) {
        this.filename = filename;
        rtmpMuxer = new RTMPMuxer();
    }

    @Override
    public synchronized int open(String url, int videoWidth, int videoHeight) {
        lastVideoTime = -1;
        lastAudioTime = -1;
        totalVideoTime = 0;
        totalAudioTime = 0;

        int open = rtmpMuxer.open(url, videoWidth, videoHeight);
        Loggers.d("RTMPStreamMuxer", String.format(Locale.CHINA, "open: open: %d", open));
        int connected = rtmpMuxer.isConnected();
        Loggers.d("RTMPStreamMuxer", String.format(Locale.CHINA, "open: isConnected: %d", connected));

        Loggers.d("RTMPStreamMuxer", String.format("open: %s", url));
        if (!"".equals(filename)) {
            rtmpMuxer.file_open(filename);
            rtmpMuxer.write_flv_header(true, true);
        }
        return connected;
    }

    @Override
    public synchronized void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        Loggers.d("RTMPStreamMuxer", "writeVideo: ");
        if (lastVideoTime <= 0) {
            lastVideoTime = bufferInfo.presentationTimeUs;
        }

        int delta = (int) (bufferInfo.presentationTimeUs - lastVideoTime);
        lastVideoTime = bufferInfo.presentationTimeUs;

        totalVideoTime += Math.abs(delta/1000);
        addFrame(buffer, offset, length, totalVideoTime, Frame.TYPE_VIDEO);
        sendFrame(KEEP_COUNT);
    }

    private void addFrame(byte[] buffer, int offset, int length, int time, int type) {
        byte[] frameData = new byte[length];
        System.arraycopy(buffer, offset, frameData, 0, length);
        frameQueue.add(new Frame(frameData, time, type));
        Frame.sortFrame(frameQueue);
    }

    private void sendFrame(int keepCount) {
        while (frameQueue.size() >= keepCount) {
            Frame sendFrame = frameQueue.remove(0);
            Loggers.i("RTMPStreamMuxer", String.format(Locale.CHINA, "sendFrame: size:%d time:%d, type:%d", sendFrame.data.length, sendFrame.timeStampMs, sendFrame.type));
            byte[] array = sendFrame.data;
            if (sendFrame.type == Frame.TYPE_VIDEO) {
                rtmpMuxer.writeVideo(array, 0, array.length, sendFrame.timeStampMs);
            } else {
                rtmpMuxer.writeAudio(array, 0, array.length, sendFrame.timeStampMs);
            }
        }
    }

    @Override
    public synchronized void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        Loggers.d("RTMPStreamMuxer", "writeAudio: ");
        if (lastAudioTime <= 0) {
            lastAudioTime = bufferInfo.presentationTimeUs;
        }

        int delta = (int) (bufferInfo.presentationTimeUs - lastAudioTime);
        lastAudioTime = bufferInfo.presentationTimeUs;
        totalAudioTime += Math.abs(delta/1000);
        addFrame(buffer, offset, length, totalAudioTime, Frame.TYPE_AUDIO);
        sendFrame(KEEP_COUNT);
    }

    @Override
    public synchronized int close() {
        sendFrame(0);
        if (!"".equals(filename)) {
            rtmpMuxer.file_close();
        }
        return rtmpMuxer.close();
    }

    private static class Frame {
        public byte[] data;
        public int timeStampMs;
        public int type;
        public static final int TYPE_VIDEO = 1;
        public static final int TYPE_AUDIO = 2;

        public Frame(byte[] data, int timeStampMs, int type) {
            this.data = data;
            this.timeStampMs = timeStampMs;
            this.type = type;
        }

        static void sortFrame(List<Frame> frameQueue) {
            Collections.sort(frameQueue, new Comparator<Frame>() {
                @Override
                public int compare(Frame left, Frame right) {
                    if (left.timeStampMs < right.timeStampMs) {
                        return -1;
                    } else if (left.timeStampMs == right.timeStampMs) {
                        return 0;
                    } else {
                        return 1;
                    }
                }
            });
        }
    }
}
