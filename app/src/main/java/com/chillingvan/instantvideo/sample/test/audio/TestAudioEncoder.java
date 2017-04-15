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

import com.chillingvan.canvasgl.Loggers;
import com.chillingvan.lib.encoder.MediaCodecInputStream;
import com.chillingvan.lib.encoder.audio.AACEncoder;

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
            aacEncoder = new AACEncoder(44100, 192000);
            aacEncoder.setOnDataComingCallback(new AACEncoder.OnDataComingCallback() {
                @Override
                public void onComing() {
                    write();
                }
            });
            isStart = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void stop() {
        isStart = false;
        if (aacEncoder != null) {
            aacEncoder.stop();
            aacEncoder = null;
        }
    }

    public boolean isStart() {
        return isStart;
    }

    public void write() {
        MediaCodecInputStream mediaCodecInputStream = aacEncoder.getMediaCodecInputStream();
        MediaCodecInputStream.readAll(mediaCodecInputStream, writeBuffer, 0, new MediaCodecInputStream.OnReadAllCallback() {
            boolean shouldAddPacketHeader = true;
            byte[] header = new byte[7];
            @Override
            public void onReadOnce(byte[] buffer, int readSize, int mediaBufferSize) {
                if (readSize <= 0) {
                    return;
                }
                try {
                    Loggers.d("TestAudioEncoder", String.format("onReadOnce: readSize:%d, mediaBufferSize:%d", readSize, mediaBufferSize));
                    if (shouldAddPacketHeader) {
                        Loggers.d("TestAudioEncoder", String.format("onReadOnce: add packet header"));
                        addADTStoPacket(header, 7 + mediaBufferSize);
                        os.write(header);
                    }
                    os.write(buffer, 0, readSize);
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                shouldAddPacketHeader = readSize >= mediaBufferSize;
            }
        });
    }

    private static void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE


        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
