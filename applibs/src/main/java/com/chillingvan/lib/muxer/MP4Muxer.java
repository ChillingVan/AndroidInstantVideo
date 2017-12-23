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
import android.media.MediaMuxer;

import com.chillingvan.canvasgl.Loggers;
import com.chillingvan.lib.publisher.StreamPublisher;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Chilling on 2017/12/17.
 */

public class MP4Muxer extends BaseMuxer {
    private static final String TAG = "MP4Muxer";

    private MediaMuxer mMuxer;
    private int videoTrackIndex;
    private int audioTrackIndex;
    private FrameSender frameSender;



    public MP4Muxer() {
        super();
    }

    @Override
    public int open(StreamPublisher.StreamPublisherParam params) {
        super.open(params);
        try {
            mMuxer = new MediaMuxer(params.outputFilePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        videoTrackIndex = mMuxer.addTrack(params.createVideoMediaFormat());
        audioTrackIndex = mMuxer.addTrack(params.createAudioMediaFormat());
        mMuxer.start();

        frameSender = new FrameSender(new FrameSender.FrameSenderCallback(){


            @Override
            public void onSendVideo(FramePool.Frame sendFrame) {

                mMuxer.writeSampleData(videoTrackIndex, ByteBuffer.wrap(sendFrame.data, 0, sendFrame.length), sendFrame.bufferInfo.getBufferInfo());
            }

            @Override
            public void onSendAudio(FramePool.Frame sendFrame) {
                mMuxer.writeSampleData(audioTrackIndex, ByteBuffer.wrap(sendFrame.data, 0, sendFrame.length), sendFrame.bufferInfo.getBufferInfo());
            }

            @Override
            public void close() {

                if (mMuxer != null) {
                    mMuxer.stop();
                    mMuxer.release();
                    mMuxer = null;
                }
            }
        });


        return 1;
    }

    @Override
    public void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        super.writeVideo(buffer, offset, length, bufferInfo);

        Loggers.d(TAG, "writeVideo: " + " offset:" + offset + " length:" + length);
        frameSender.sendAddFrameMessage(buffer, offset, length, new BufferInfoEx(bufferInfo, videoTimeIndexCounter.getTimeIndex()), FramePool.Frame.TYPE_VIDEO);
    }

    @Override
    public void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        super.writeAudio(buffer, offset, length, bufferInfo);
        Loggers.d(TAG, "writeAudio: ");
        frameSender.sendAddFrameMessage(buffer, offset, length, new BufferInfoEx(bufferInfo, audioTimeIndexCounter.getTimeIndex()), FramePool.Frame.TYPE_AUDIO);
    }

    @Override
    public int close() {
        if (frameSender != null) {
            frameSender.sendCloseMessage();
        }
        return 0;
    }
}
