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

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Chilling on 2017/12/17.
 */

public class FrameSender {

    public static final int KEEP_COUNT = 30;

    private Handler sendHandler;
    private List<FramePool.Frame> frameQueue = new LinkedList<>();
    private FramePool framePool = new FramePool(KEEP_COUNT + 10);

    public FrameSender() {
    }
}
