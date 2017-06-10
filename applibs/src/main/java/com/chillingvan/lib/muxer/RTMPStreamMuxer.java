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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.chillingvan.canvasgl.Loggers;

import net.butterflytv.rtmp_client.RTMPMuxer;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Chilling on 2017/5/29.
 */

public class RTMPStreamMuxer implements IMuxer {


    public static final int KEEP_COUNT = 30;
    public static final int MESSAGE_READY_TO_CLOSE = 4;
    public static final int MSG_ADD_FRAME = 3;
    private String filename = "";
    private RTMPMuxer rtmpMuxer;

    private long lastVideoTime;
    private long lastAudioTime;
    private int totalVideoTime;
    private int totalAudioTime;


    private List<FramePool.Frame> frameQueue = new LinkedList<>();
    private FramePool framePool = new FramePool(KEEP_COUNT + 10);
    private Handler sendHandler;

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

        final HandlerThread sendHandlerThread = new HandlerThread("send_thread");
        sendHandlerThread.start();
        sendHandler = new Handler(sendHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.obj != null) {
                    addFrame((FramePool.Frame) msg.obj);
                }
                sendFrame(msg.arg1);

                if (msg.what == MESSAGE_READY_TO_CLOSE) {
                    if (!"".equals(filename)) {
                        rtmpMuxer.file_close();
                    }
                    rtmpMuxer.close();
                    sendHandlerThread.quitSafely();
                }
            }
        };
        return connected;
    }

    @Override
    public void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        Loggers.d("RTMPStreamMuxer", "writeVideo: ");
        if (lastVideoTime <= 0) {
            lastVideoTime = bufferInfo.presentationTimeUs;
        }

        int delta = (int) (bufferInfo.presentationTimeUs - lastVideoTime);
        lastVideoTime = bufferInfo.presentationTimeUs;

        totalVideoTime += Math.abs(delta/1000);
        sendAddFrameMessage(sendHandler, framePool.obtain(buffer, offset, length, totalVideoTime, FramePool.Frame.TYPE_VIDEO));
    }

    private static void sendAddFrameMessage(Handler sendHandler, FramePool.Frame frame) {
        Message message = Message.obtain();
        message.what = MSG_ADD_FRAME;
        message.obj = frame;
        message.arg1 = KEEP_COUNT;
        sendHandler.sendMessage(message);
    }

    private void addFrame(FramePool.Frame frame) {
        frameQueue.add(frame);
        FramePool.Frame.sortFrame(frameQueue);
    }

    private void sendFrame(int keepCount) {
        while (frameQueue.size() > keepCount) {
            FramePool.Frame sendFrame = frameQueue.remove(0);
            Loggers.i("RTMPStreamMuxer", String.format(Locale.CHINA, "sendFrame: size:%d time:%d, type:%d", sendFrame.data.length, sendFrame.timeStampMs, sendFrame.type));
            byte[] array = sendFrame.data;
            if (sendFrame.type == FramePool.Frame.TYPE_VIDEO) {
                rtmpMuxer.writeVideo(array, 0, sendFrame.length, sendFrame.timeStampMs);
            } else {
                rtmpMuxer.writeAudio(array, 0, sendFrame.length, sendFrame.timeStampMs);
            }
            framePool.release(sendFrame);
        }
    }

    @Override
    public void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        Loggers.d("RTMPStreamMuxer", "writeAudio: ");
        if (lastAudioTime <= 0) {
            lastAudioTime = bufferInfo.presentationTimeUs;
        }

        int delta = (int) (bufferInfo.presentationTimeUs - lastAudioTime);
        lastAudioTime = bufferInfo.presentationTimeUs;
        totalAudioTime += Math.abs(delta/1000);
        sendAddFrameMessage(sendHandler, framePool.obtain(buffer, offset, length, totalAudioTime, FramePool.Frame.TYPE_AUDIO));
    }

    @Override
    public synchronized int close() {
        sendCloseMessage(sendHandler);

        return 0;
    }

    private static void sendCloseMessage(Handler sendHandler) {
        Message message = Message.obtain();
        message.arg1 = 0;
        message.what = MESSAGE_READY_TO_CLOSE;
        sendHandler.sendMessage(message);
    }

}
