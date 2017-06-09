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
import android.widget.EditText;
import android.widget.TextView;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.Loggers;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.textureFilter.ContrastFilter;
import com.chillingvan.canvasgl.textureFilter.DirectionalSobelEdgeDetectionFilter;
import com.chillingvan.canvasgl.textureFilter.HueFilter;
import com.chillingvan.canvasgl.textureFilter.SaturationFilter;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;
import com.chillingvan.instantvideo.sample.R;
import com.chillingvan.instantvideo.sample.test.camera.CameraPreviewTextureView;
import com.chillingvan.lib.camera.InstantVideoCamera;
import com.chillingvan.lib.encoder.video.H264Encoder;
import com.chillingvan.lib.muxer.RTMPStreamMuxer;
import com.chillingvan.lib.publisher.CameraStreamPublisher;
import com.chillingvan.lib.publisher.StreamPublisher;

import java.io.IOException;

public class TestCameraPublisherActivity extends AppCompatActivity {

    private CameraStreamPublisher streamPublisher;
    private CameraPreviewTextureView cameraPreviewTextureView;
    private InstantVideoCamera instantVideoCamera;
    private Handler handler;
    private EditText addrEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_camera_publisher);
        cameraPreviewTextureView = (CameraPreviewTextureView) findViewById(R.id.camera_produce_view);
        cameraPreviewTextureView.setOnDrawListener(new H264Encoder.OnDrawListener() {
            @Override
            public void onGLDraw(ICanvasGL canvasGL, SurfaceTexture surfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {
                drawVideoFrame(canvasGL, surfaceTexture, rawTexture);
            }
        });
        addrEditText = (EditText) findViewById(R.id.ip_input_test);
        instantVideoCamera = new InstantVideoCamera(Camera.CameraInfo.CAMERA_FACING_FRONT, 1280, 720);

        HandlerThread handlerThread = new HandlerThread("StreamPublisherOpen");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                StreamPublisher.StreamPublisherParam streamPublisherParam = new StreamPublisher.StreamPublisherParam(1280, 720, 322560 * 4, 30, 5, 44100, 19200);
                streamPublisher.prepareEncoder(streamPublisherParam, new H264Encoder.OnDrawListener() {
                    @Override
                    public void onGLDraw(ICanvasGL canvasGL, SurfaceTexture surfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {
                        drawVideoFrame(canvasGL, outsideSurfaceTexture, outsideTexture);
                        Loggers.i("DEBUG", "gl draw");
                    }
                });
                try {
                    streamPublisher.startPublish(addrEditText.getText().toString(), streamPublisherParam.width, streamPublisherParam.height);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

//        streamPublisher = new CameraStreamPublisher(new RTMPStreamMuxer(), cameraPreviewTextureView, instantVideoCamera);
        String filename = getExternalFilesDir(null) + "/test_flv_encode.flv";
        streamPublisher = new CameraStreamPublisher(new RTMPStreamMuxer(filename), cameraPreviewTextureView, instantVideoCamera);
    }

    private void drawVideoFrame(ICanvasGL canvasGL, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {
        // Here you can do video process
        // 此处可以视频处理，例如加水印等等
        TextureFilter textureFilterLT = new ContrastFilter(2.0f);
        TextureFilter textureFilterRT = new HueFilter(180);
        TextureFilter textureFilterLB = new SaturationFilter(1.5f);
        TextureFilter textureFilterRB = new DirectionalSobelEdgeDetectionFilter(2.0f);
        canvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, 0, 0, outsideTexture.getWidth()/2, outsideTexture.getHeight()/2, textureFilterLT);
        canvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, outsideTexture.getWidth()/2, 0, outsideTexture.getWidth(), outsideTexture.getHeight()/2, textureFilterRT);
        canvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, 0, outsideTexture.getHeight()/2, outsideTexture.getWidth()/2, outsideTexture.getHeight(), textureFilterLB);
        canvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, outsideTexture.getWidth()/2, outsideTexture.getHeight()/2, outsideTexture.getWidth(), outsideTexture.getHeight(), textureFilterRB);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        streamPublisher.closeAll();
    }

    public void clickStartTest(View view) {
        TextView textView = (TextView) view;
        if (streamPublisher.isStart()) {
            streamPublisher.stopPublish();
            textView.setText("START");
        } else {
            handler.sendEmptyMessage(1);
            textView.setText("STOP");
        }
    }
}
