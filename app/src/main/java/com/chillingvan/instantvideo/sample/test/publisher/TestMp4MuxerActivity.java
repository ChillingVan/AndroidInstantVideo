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

package com.chillingvan.instantvideo.sample.test.publisher;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.textureFilter.BasicTextureFilter;
import com.chillingvan.canvasgl.textureFilter.HueFilter;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;
import com.chillingvan.canvasgl.util.Loggers;
import com.chillingvan.instantvideo.sample.R;
import com.chillingvan.instantvideo.sample.test.camera.CameraPreviewTextureView;
import com.chillingvan.lib.camera.InstantVideoCamera;
import com.chillingvan.lib.encoder.video.H264Encoder;
import com.chillingvan.lib.muxer.MP4Muxer;
import com.chillingvan.lib.publisher.CameraStreamPublisher;
import com.chillingvan.lib.publisher.StreamPublisher;

import java.io.IOException;
import java.util.List;

public class TestMp4MuxerActivity extends AppCompatActivity {

    private CameraStreamPublisher streamPublisher;
    private CameraPreviewTextureView cameraPreviewTextureView;
    private InstantVideoCamera instantVideoCamera;
    private Handler handler;
    private HandlerThread handlerThread;
    private TextView outDirTxt;
    private String outputDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        outputDir = getExternalFilesDir(null) + "/test_mp4_encode.mp4";
        setContentView(R.layout.activity_test_mp4_muxer);
        cameraPreviewTextureView = findViewById(R.id.camera_produce_view);
        cameraPreviewTextureView.setOnDrawListener(new H264Encoder.OnDrawListener() {
            @Override
            public void onGLDraw(ICanvasGL canvasGL, List<GLTexture> producedTextures, List<GLTexture> consumedTextures) {
                GLTexture texture = producedTextures.get(0);
                drawVideoFrame(canvasGL, texture.getSurfaceTexture(), texture.getRawTexture());
            }

        });
        outDirTxt = (TextView) findViewById(R.id.output_dir_txt);
        outDirTxt.setText(outputDir);


        instantVideoCamera = new InstantVideoCamera(Camera.CameraInfo.CAMERA_FACING_FRONT, 640, 480);
//        instantVideoCamera = new InstantVideoCamera(Camera.CameraInfo.CAMERA_FACING_FRONT, 1280, 720);

        handlerThread = new HandlerThread("StreamPublisherOpen");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
//                StreamPublisher.StreamPublisherParam streamPublisherParam = new StreamPublisher.StreamPublisherParam();
//                StreamPublisher.StreamPublisherParam streamPublisherParam = new StreamPublisher.StreamPublisherParam(1080, 640, 9500 * 1000, 30, 1, 44100, 19200);
                StreamPublisher.StreamPublisherParam streamPublisherParam = new StreamPublisher.StreamPublisherParam(540, 750, 1500 * 1000, 30, 1, 44100, 19200);
                streamPublisherParam.outputFilePath = outputDir;
//                streamPublisherParam.outputFilePath = getExternalFilesDir(null) + "/test_mp4_encode.mp4";
                streamPublisher.prepareEncoder(streamPublisherParam, new H264Encoder.OnDrawListener() {
                    @Override
                    public void onGLDraw(ICanvasGL canvasGL, List<GLTexture> producedTextures, List<GLTexture> consumedTextures) {
                        GLTexture texture = producedTextures.get(0);
                        drawVideoFrame(canvasGL, texture.getSurfaceTexture(), texture.getRawTexture());
                        Loggers.i("DEBUG", "gl draw");
                    }

                });
                try {
                    streamPublisher.startPublish();
                } catch (IOException e) {
                    e.printStackTrace();
                    ((TextView)findViewById(R.id.test_camera_button)).setText("START");
                }
            }
        };

        streamPublisher = new CameraStreamPublisher(new MP4Muxer(), cameraPreviewTextureView, instantVideoCamera);
    }

    private void drawVideoFrame(ICanvasGL canvasGL, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {
        // Here you can do video process
        // 此处可以视频处理，例如加水印等等
        TextureFilter textureFilterLT = new BasicTextureFilter();
        TextureFilter textureFilterRT = new HueFilter(180);
        int width = outsideTexture.getWidth();
        int height = outsideTexture.getHeight();
        canvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, 0, 0, width /2, height /2, textureFilterLT);
        canvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, 0, height/2, width/2, height, textureFilterRT);

    }

    @Override
    protected void onResume() {
        super.onResume();
        streamPublisher.resumeCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        streamPublisher.pauseCamera();
        if (streamPublisher.isStart()) {
            streamPublisher.closeAll();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handlerThread.quitSafely();
    }

    public void clickStartTest(View view) {
        TextView textView = (TextView) view;
        if (streamPublisher.isStart()) {
            streamPublisher.closeAll();
            textView.setText("START");
        } else {
            streamPublisher.resumeCamera();
            handler.sendEmptyMessage(1);
            textView.setText("STOP");
        }
    }
}
