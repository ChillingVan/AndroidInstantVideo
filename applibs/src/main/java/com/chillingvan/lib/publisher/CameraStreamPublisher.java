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

package com.chillingvan.lib.publisher;

import android.graphics.SurfaceTexture;

import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.chillingvan.lib.camera.CameraInterface;
import com.chillingvan.lib.encoder.video.H264Encoder;
import com.chillingvan.lib.muxer.IMuxer;

import java.io.IOException;

/**
 * Created by Chilling on 2017/5/28.
 */

public class CameraStreamPublisher {

    private StreamPublisher streamPublisher;
    private IMuxer muxer;
    private GLSurfaceTextureProducerView cameraPreviewTextureView;
    private CameraInterface instantVideoCamera;

    public CameraStreamPublisher(IMuxer muxer, GLSurfaceTextureProducerView cameraPreviewTextureView, CameraInterface instantVideoCamera) {
        this.muxer = muxer;
        this.cameraPreviewTextureView = cameraPreviewTextureView;
        this.instantVideoCamera = instantVideoCamera;
    }

    private void initCameraTexture() {
        cameraPreviewTextureView.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
            @Override
            public void onCreate(EglContextWrapper eglContext) {
                streamPublisher = new StreamPublisher(eglContext, muxer);
            }
        });
        cameraPreviewTextureView.setOnSurfaceTextureSet(new GLSurfaceTextureProducerView.OnSurfaceTextureSet() {
            @Override
            public void onSet(SurfaceTexture surfaceTexture, RawTexture rawTexture) {
                streamPublisher.setSharedTexture(rawTexture, surfaceTexture);
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        cameraPreviewTextureView.requestRenderAndWait();
                        streamPublisher.drawAFrame();
                    }
                });

                instantVideoCamera.setPreview(surfaceTexture);
                instantVideoCamera.startPreview();
            }
        });
    }

    public void prepareEncoder(StreamPublisher.StreamPublisherParam param, H264Encoder.OnDrawListener onDrawListener) {
        streamPublisher.prepareEncoder(param, onDrawListener);
    }

    public void resumeCamera() {
        instantVideoCamera.openCamera();
        initCameraTexture();
        cameraPreviewTextureView.onResume();
    }

    public boolean isStart() {
        return streamPublisher.isStart();
    }

    public void pauseCamera() {
        instantVideoCamera.release();
        cameraPreviewTextureView.onPause();
    }

    public void startPublish(String url, int width, int height) throws IOException {
        streamPublisher.start(url, width, height);
    }

    public void stopPublish() {
        streamPublisher.stop();
    }

    public void closeAll() {
        streamPublisher.close();
        pauseCamera();
    }
}
