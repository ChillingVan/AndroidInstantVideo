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
import android.text.TextUtils;

import com.chillingvan.canvasgl.Loggers;
import com.chillingvan.lib.publisher.StreamPublisher;

import net.butterflytv.rtmp_client.RTMPMuxer;

import java.util.Locale;

/**
 * Created by Chilling on 2017/5/29.
 */

public class RTMPStreamMuxer extends BaseMuxer {
    private RTMPMuxer rtmpMuxer;

    private FrameSender frameSender;


    public RTMPStreamMuxer() {
        super();
    }


    /**
     *
     * @return 1 if it is connected
     * 0 if it is not connected
     */
    @Override
    public synchronized int open(final StreamPublisher.StreamPublisherParam params) {
        super.open(params);

        if (TextUtils.isEmpty(params.outputUrl)) {
            throw new IllegalArgumentException("Param outputUrl is empty");
        }

        rtmpMuxer = new RTMPMuxer();
        // -2 Url format error; -3 Connect error.
        int open = rtmpMuxer.open(params.outputUrl, params.width, params.height);
        Loggers.d("RTMPStreamMuxer", String.format(Locale.CHINA, "open: open: %d", open));
        int connected = rtmpMuxer.isConnected();
        Loggers.d("RTMPStreamMuxer", String.format(Locale.CHINA, "open: isConnected: %d", connected));

        Loggers.d("RTMPStreamMuxer", String.format("open: %s", params.outputUrl));
        if (!TextUtils.isEmpty(params.outputFilePath)) {
            rtmpMuxer.file_open(params.outputFilePath);
            rtmpMuxer.write_flv_header(true, true);
        }

        frameSender = new FrameSender(new FrameSender.FrameSenderCallback() {
            @Override
            public void onSendVideo(FramePool.Frame sendFrame) {

                rtmpMuxer.writeVideo(sendFrame.data, 0, sendFrame.length, sendFrame.bufferInfo.getTotalTime());
            }

            @Override
            public void onSendAudio(FramePool.Frame sendFrame) {

                rtmpMuxer.writeAudio(sendFrame.data, 0, sendFrame.length, sendFrame.bufferInfo.getTotalTime());
            }

            @Override
            public void close() {
                if (rtmpMuxer != null) {
                    if (!TextUtils.isEmpty(params.outputFilePath)) {
                        rtmpMuxer.file_close();
                    }
                    rtmpMuxer.close();
                    rtmpMuxer = null;
                }

            }
        });

        return connected;
    }

    @Override
    public void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        super.writeVideo(buffer, offset, length, bufferInfo);
        Loggers.d("RTMPStreamMuxer", "writeVideo: " + " time:" + videoTimeIndexCounter.getTimeIndex() + " offset:" + offset + " length:" + length);
        frameSender.sendAddFrameMessage(buffer, offset, length, new BufferInfoEx(bufferInfo, videoTimeIndexCounter.getTimeIndex()), FramePool.Frame.TYPE_VIDEO);
    }



    @Override
    public void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        super.writeAudio(buffer, offset, length, bufferInfo);
        Loggers.d("RTMPStreamMuxer", "writeAudio: ");
        frameSender.sendAddFrameMessage(buffer, offset, length, new BufferInfoEx(bufferInfo, audioTimeIndexCounter.getTimeIndex()), FramePool.Frame.TYPE_AUDIO);
    }

    @Override
    public synchronized int close() {
        if (frameSender != null) {
            frameSender.sendCloseMessage();
        }

        return 0;
    }


}
