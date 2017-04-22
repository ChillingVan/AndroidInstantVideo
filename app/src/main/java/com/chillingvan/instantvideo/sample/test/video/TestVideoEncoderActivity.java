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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.chillingvan.instantvideo.sample.R;
import com.chillingvan.lib.encoder.video.H264Encoder;


public class TestVideoEncoderActivity extends AppCompatActivity {

    private TestVideoEncoder testVideoEncoder;
    private ProduceTextureView produceTextureView;
    private Handler handler;
    private HandlerThread inputHandlerThread;
    private HandlerThread outputVideoThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_video_encoder);
        produceTextureView = (ProduceTextureView) findViewById(R.id.produce_view);


        produceTextureView.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
            @Override
            public void onCreate(EglContextWrapper eglContext) {
                testVideoEncoder = new TestVideoEncoder(getApplicationContext(), eglContext);
            }
        });
        produceTextureView.setOnSurfaceTextureSet(new GLSurfaceTextureProducerView.OnSurfaceTextureSet() {
            @Override
            public void onSet(SurfaceTexture surfaceTexture, RawTexture rawTexture) {
                testVideoEncoder.setSharedTexture(rawTexture, surfaceTexture);
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    }
                });
            }
        });


        outputVideoThread = new HandlerThread("outputVideoThread");
        outputVideoThread.start();

        final Handler outputVideoHandler = new Handler(outputVideoThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                testVideoEncoder.write();
            }
        };

        inputHandlerThread = new HandlerThread("encoder");
        inputHandlerThread.start();

        handler = new Handler(inputHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.lenna);
                    testVideoEncoder.prepareEncoder(new H264Encoder.OnDrawListener() {
                        @Override
                        public void onGLDraw(ICanvasGL canvasGL, SurfaceTexture producedSurfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {
                            TestVideoEncoder.drawCnt++;

                            if (TestVideoEncoder.drawCnt == 19 || TestVideoEncoder.drawCnt == 39) {
                                canvasGL.drawBitmap(bitmap, 0, 0);
                            }
                            TestVideoEncoder.drawRect(canvasGL, TestVideoEncoder.drawCnt);

                            if (TestVideoEncoder.drawCnt >= 60) {
                                TestVideoEncoder.drawCnt = 0;
                            }
                            Log.i("TestVideoEncoder", "gl draw");
                        }
                    });
                    testVideoEncoder.start();
                    for (int i = 0; i < 120; i++) {
                        produceTextureView.requestRenderAndWait();
                        testVideoEncoder.writeAFrame();
                        outputVideoHandler.sendEmptyMessage(0);
                        try {
                            Thread.sleep(16);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        testVideoEncoder.stop();
        inputHandlerThread.quit();
        outputVideoThread.quit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        testVideoEncoder.destroy();
    }

    public void clickStartTest(View view) {
        handler.sendEmptyMessage(1);

    }
}
