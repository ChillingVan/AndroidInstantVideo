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

package com.chillingvan.instantvideo.sample.test.audio;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaRecorder;

import com.chillingvan.canvasgl.util.Loggers;
import com.chillingvan.lib.encoder.MediaCodecInputStream;
import com.chillingvan.lib.encoder.audio.AACEncoder;
import com.chillingvan.lib.publisher.StreamPublisher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Chilling on 2017/1/23.
 */

public class TestAudioEncoder {


    private AACEncoder aacEncoder;
    private byte[] writeBuffer = new byte[1024 * 64];
    private OutputStream os;
    private boolean isStart;

    public TestAudioEncoder(Context ctx) {

        try {
            os = new FileOutputStream(ctx.getExternalFilesDir(null) + File.separator + "test_aac_encode.aac");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void prepareEncoder() {
        try {
            StreamPublisher.StreamPublisherParam.Builder builder = new StreamPublisher.StreamPublisherParam.Builder();
            builder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            StreamPublisher.StreamPublisherParam streamPublisherParam = builder.createStreamPublisherParam();
            aacEncoder = new AACEncoder(streamPublisherParam);
            aacEncoder.setOnDataComingCallback(new AACEncoder.OnDataComingCallback() {
                @Override
                public void onComing() {
                    write();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (!isStart) {
            aacEncoder.start();
            isStart = true;
        }
    }


    public void stop() {
        isStart = false;
        if (aacEncoder != null) {
            aacEncoder.close();
            aacEncoder = null;
        }
    }

    public boolean isStart() {
        return isStart;
    }

    public void write() {
        MediaCodecInputStream mediaCodecInputStream = aacEncoder.getMediaCodecInputStream();
        MediaCodecInputStream.readAll(mediaCodecInputStream, writeBuffer, new MediaCodecInputStream.OnReadAllCallback() {
            boolean shouldAddPacketHeader = true;
            byte[] header = new byte[7];
            @Override
            public void onReadOnce(byte[] buffer, int readSize, MediaCodec.BufferInfo bufferInfo) {
                if (readSize <= 0) {
                    return;
                }
                try {
                    Loggers.d("TestAudioEncoder", String.format("onReadOnce: readSize:%d, bufferInfo:%d", readSize, bufferInfo.size));
                    if (shouldAddPacketHeader) {
                        Loggers.d("TestAudioEncoder", String.format("onReadOnce: add packet header"));
                        AACEncoder.addADTStoPacket(header, 7 + bufferInfo.size);
                        os.write(header);
                    }
                    os.write(buffer, 0, readSize);
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                shouldAddPacketHeader = readSize >= bufferInfo.size;
            }
        });
    }

}
