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

package com.chillingvan.instantvideo.sample.test.video;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.util.Log;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glcanvas.GLPaint;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.util.Loggers;
import com.chillingvan.lib.encoder.MediaCodecInputStream;
import com.chillingvan.lib.encoder.video.H264Encoder;
import com.chillingvan.lib.publisher.StreamPublisher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chilling on 2016/12/10.
 */

public class TestVideoEncoder {

    private H264Encoder h264Encoder;
    private byte[] writeBuffer = new byte[1024 * 64];
    private Context ctx;
    private EglContextWrapper eglCtx;
    public static int drawCnt;
    private OutputStream os;
    private List<GLTexture> glTextureList = new ArrayList<>();

    public TestVideoEncoder(Context ctx, final EglContextWrapper eglCtx) {
        this.ctx = ctx;
        this.eglCtx = eglCtx;

        try {
            os = new FileOutputStream(ctx.getExternalFilesDir(null) + File.separator + "test_h264_encode.h264");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void prepareEncoder(H264Encoder.OnDrawListener onDrawListener) {
        try {
            h264Encoder = new H264Encoder(new StreamPublisher.StreamPublisherParam.Builder().createStreamPublisherParam(), eglCtx);
            for (GLTexture texture : glTextureList) {
                h264Encoder.addSharedTexture(texture);
            }
            h264Encoder.setOnDrawListener(onDrawListener);
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void addSharedTexture(RawTexture outsideTexture, SurfaceTexture outsideSurfaceTexture) {
        glTextureList.add(new GLTexture(outsideTexture, outsideSurfaceTexture));
    }

    public static void drawRect(ICanvasGL canvasGL, int drawCnt) {
        GLPaint glPaint = new GLPaint();
        glPaint.setColor(Color.BLUE);
        canvasGL.drawRect(new Rect(10 * drawCnt - 20, 50, 10 * drawCnt, 100), glPaint);
    }


    public void start() {
        Log.d("TestVideoEncoder", "start: ");
        h264Encoder.start();
    }

    public void stop() {
        if (h264Encoder == null) {
            return;
        }
        h264Encoder.close();
    }

    public boolean isStart() {
        return h264Encoder != null && h264Encoder.isStart();
    }

    public void destroy() {
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (h264Encoder == null) {
            return;
        }
        h264Encoder.close();
    }

    public boolean encodeAFrame() {
        if (isStart()) {
            Loggers.d("TestVideoEncoder", "writeAFrame");
            h264Encoder.requestRenderAndWait();
            return true;
        }
        return false;
    }

    public void write() {
        MediaCodecInputStream mediaCodecInputStream = h264Encoder.getMediaCodecInputStream();
        MediaCodecInputStream.readAll(mediaCodecInputStream, writeBuffer, new MediaCodecInputStream.OnReadAllCallback() {
            @Override
            public void onReadOnce(byte[] buffer, int readSize, MediaCodec.BufferInfo bufferInfo) {
                Loggers.d("TestVideoEncoder", String.format("onReadOnce: readSize:%d", readSize));
                if (readSize <= 0) {
                    return;
                }
                try {
                    os.write(buffer, 0, readSize);
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
