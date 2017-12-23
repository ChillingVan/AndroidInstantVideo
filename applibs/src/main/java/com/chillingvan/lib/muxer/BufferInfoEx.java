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

/**
 * Created by Chilling on 2017/12/23.
 */

public class BufferInfoEx {
    private MediaCodec.BufferInfo bufferInfo;
    private int totalTime;

    public BufferInfoEx(MediaCodec.BufferInfo bufferInfo, int totalTime) {
        this.bufferInfo = bufferInfo;
        this.totalTime = totalTime;
    }

    public MediaCodec.BufferInfo getBufferInfo() {
        return bufferInfo;
    }

    public int getTotalTime() {
        return totalTime;
    }
}
