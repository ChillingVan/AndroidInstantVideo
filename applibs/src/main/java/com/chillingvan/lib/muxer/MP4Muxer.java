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

import com.chillingvan.lib.publisher.StreamPublisher;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Chilling on 2017/12/17.
 */

public class MP4Muxer implements IMuxer {

    private MediaMuxer mMuxer;
    private int videoTrackIndex;
    private int audioTrackIndex;

    public MP4Muxer() {
    }

    @Override
    public int open(StreamPublisher.StreamPublisherParam params) {
        try {
            mMuxer = new MediaMuxer(params.outputFilePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        videoTrackIndex = mMuxer.addTrack(params.createVideoMediaFormat());
        audioTrackIndex = mMuxer.addTrack(params.createAudioMediaFormat());
        mMuxer.start();
        return 0;
    }

    @Override
    public void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        mMuxer.writeSampleData(videoTrackIndex, ByteBuffer.wrap(buffer, offset, length), bufferInfo);
    }

    @Override
    public void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        mMuxer.writeSampleData(audioTrackIndex, ByteBuffer.wrap(buffer, offset, length), bufferInfo);
    }

    @Override
    public int close() {
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
        return 0;
    }
}
