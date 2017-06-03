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

/**
 * Created by Chilling on 2017/5/29.
 */

public class RTMPStreamMuxer implements IMuxer {


    private String filename = "";
    private RTMPMuxer rtmpMuxer;

    private long lastVideoTime = -1;
    private long lastAudioTime = -1;

    public RTMPStreamMuxer() {
        this("");
    }

    public RTMPStreamMuxer(String filename) {
        this.filename = filename;
        rtmpMuxer = new RTMPMuxer();
    }

    @Override
    public int open(String url, int videoWidth, int videoHeight) {
        Loggers.d("RTMPStreamMuxer", String.format("open: %s", url));
        if (!"".equals(filename)) {
            rtmpMuxer.file_open(filename);
            rtmpMuxer.write_flv_header(true, true);
        }

        int open = rtmpMuxer.open(url, videoWidth, videoHeight);
        Loggers.d("RTMPStreamMuxer", String.format("open: open: %d", open));
        int connected = rtmpMuxer.isConnected();
        Loggers.d("RTMPStreamMuxer", String.format("open: isConnected: %d", connected));
        return connected;
    }

    @Override
    public void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        Loggers.d("RTMPStreamMuxer", String.format("writeVideo: "));
        if (lastVideoTime <= 0) {
            lastVideoTime = bufferInfo.presentationTimeUs;
        }

        int delta = (int) (bufferInfo.presentationTimeUs - lastVideoTime);
        lastVideoTime = bufferInfo.presentationTimeUs;

        rtmpMuxer.writeVideo(buffer, offset, length, Math.abs(delta/1000));
    }

    @Override
    public void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        Loggers.d("RTMPStreamMuxer", String.format("writeAudio: "));
        if (lastAudioTime <= 0) {
            lastAudioTime = bufferInfo.presentationTimeUs;
        }

        int delta = (int) (bufferInfo.presentationTimeUs - lastAudioTime);
        lastAudioTime = bufferInfo.presentationTimeUs;
        rtmpMuxer.writeAudio(buffer, offset, length,  Math.abs(delta/1000));
    }

    @Override
    public int close() {
        if (!"".equals(filename)) {
            rtmpMuxer.file_close();
        }
        return rtmpMuxer.close();
//        return 0;
    }
}
