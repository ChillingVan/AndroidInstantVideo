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

package com.chillingvan.lib.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.io.IOException;

/**
 * Created by Chilling on 2016/12/10.
 */

public class InstantVideoCamera implements CameraInterface {

    private Camera camera;
    private boolean isOpened;
    private int currentCamera;
    private int previewWidth;
    private int previewHeight;

    @Override
    public void setPreview(SurfaceTexture surfaceTexture) {
        try {
            camera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InstantVideoCamera(int currentCamera, int previewWidth, int previewHeight) {
        this.currentCamera = currentCamera;
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
    }

    @Override
    public void openCamera() {
        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == currentCamera) {
                camera = Camera.open(i);
                break;
            }
        }
        if (camera == null) {
            camera = Camera.open();
        }

        Camera.Parameters parms = camera.getParameters();

        CameraUtils.choosePreviewSize(parms, previewWidth, previewHeight);
        isOpened = true;
    }

    @Override
    public void switchCamera() {
        switchCamera(previewWidth, previewHeight);
    }

    @Override
    public void switchCamera(int previewWidth, int previewHeight) {
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
        release();
        currentCamera = currentCamera == Camera.CameraInfo.CAMERA_FACING_BACK ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
        openCamera();
    }

    @Override
    public boolean isOpened() {
        return isOpened;
    }

    @Override
    public void startPreview() {
        camera.startPreview();
    }

    @Override
    public void stopPreview() {
        camera.stopPreview();
    }

    @Override
    public Camera getCamera() {
        return camera;
    }

    @Override
    public void release() {
        if (isOpened) {
            camera.stopPreview();
            camera.release();
            camera = null;
            isOpened = false;
        }
    }


}
