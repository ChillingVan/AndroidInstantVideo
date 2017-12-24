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

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Chilling on 2017/12/17.
 */

public class FrameSender {

    private static final int KEEP_COUNT = 30;
    private static final int MESSAGE_READY_TO_CLOSE = 4;
    private static final int MSG_ADD_FRAME = 3;
    private static final int MSG_START = 2;

    private Handler sendHandler;
    private List<FramePool.Frame> frameQueue = new LinkedList<>();
    private FramePool framePool = new FramePool(KEEP_COUNT + 10);
    private FrameSenderCallback frameSenderCallback;


    public FrameSender(final FrameSenderCallback frameSenderCallback) {
        this.frameSenderCallback = frameSenderCallback;
        final HandlerThread sendHandlerThread = new HandlerThread("send_thread");
        sendHandlerThread.start();
        sendHandler = new Handler(sendHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.what == MESSAGE_READY_TO_CLOSE) {
                    if (msg.obj != null) {
                        addFrame((FramePool.Frame) msg.obj);
                    }
                    sendFrame(msg.arg1);

                    frameSenderCallback.close();
                    sendHandlerThread.quitSafely();
                } else if (msg.what == MSG_ADD_FRAME) {
                    if (msg.obj != null) {
                        addFrame((FramePool.Frame) msg.obj);
                    }
                    sendFrame(msg.arg1);
                } else if (msg.what == MSG_START) {
                    frameSenderCallback.onStart();
                }
            }
        };
    }


    private void addFrame(FramePool.Frame frame) {
        frameQueue.add(frame);
        FramePool.Frame.sortFrame(frameQueue);
    }

    private void sendFrame(int keepCount) {
        while (frameQueue.size() > keepCount) {
            FramePool.Frame sendFrame = frameQueue.remove(0);
            if (sendFrame.type == FramePool.Frame.TYPE_VIDEO) {
                frameSenderCallback.onSendVideo(sendFrame);
            } else if(sendFrame.type == FramePool.Frame.TYPE_AUDIO) {
                frameSenderCallback.onSendAudio(sendFrame);
            }
            framePool.release(sendFrame);
        }
    }

    public void sendStartMessage() {
        Message message = Message.obtain();
        message.what = MSG_START;
        sendHandler.sendMessage(message);
    }

    public void sendAddFrameMessage(byte[] data, int offset, int length, BufferInfoEx bufferInfo, int type) {
        FramePool.Frame frame = framePool.obtain(data, offset, length, bufferInfo, type);
        Message message = Message.obtain();
        message.what = MSG_ADD_FRAME;
        message.obj = frame;
        message.arg1 = KEEP_COUNT;
        sendHandler.sendMessage(message);
    }

    public void sendCloseMessage() {
        Message message = Message.obtain();
        message.arg1 = 0;
        message.what = MESSAGE_READY_TO_CLOSE;
        sendHandler.sendMessage(message);
    }

    public interface FrameSenderCallback {
        void onStart();
        void onSendVideo(FramePool.Frame sendFrame);
        void onSendAudio(FramePool.Frame sendFrame);
        void close();

    }
}
