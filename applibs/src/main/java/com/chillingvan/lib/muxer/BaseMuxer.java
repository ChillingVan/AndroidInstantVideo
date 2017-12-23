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

import com.chillingvan.lib.publisher.StreamPublisher;

/**
 * Created by Chilling on 2017/12/23.
 */

public abstract class BaseMuxer implements IMuxer {

    protected TimeIndexCounter videoTimeIndexCounter = new TimeIndexCounter();
    protected TimeIndexCounter audioTimeIndexCounter = new TimeIndexCounter();

    @Override
    public int open(StreamPublisher.StreamPublisherParam params) {
        videoTimeIndexCounter.reset();
        audioTimeIndexCounter.reset();
        return 0;
    }

    @Override
    public void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        videoTimeIndexCounter.calcTotalTime(bufferInfo.presentationTimeUs);
    }

    @Override
    public void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        audioTimeIndexCounter.calcTotalTime(bufferInfo.presentationTimeUs);
    }
}
