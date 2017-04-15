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

public class InstantVideoCamera {

    private Camera camera;
    private boolean isOpened;
    private int currentCamera;
    private int previewWidth;
    private int previewHeight;

    public void setPreview(SurfaceTexture surfaceTexture) {
        try {
            camera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void openCamera(int whichCamera, int previewWidth, int previewHeight) {
        this.currentCamera = whichCamera;
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == whichCamera) {
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

    public void switchCamera() {
        switchCamera(previewWidth, previewHeight);
    }

    public void switchCamera(int previewWidth, int previewHeight) {
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
        release();
        int which = currentCamera == Camera.CameraInfo.CAMERA_FACING_BACK ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
        openCamera(which, previewWidth, previewHeight);
    }

    public boolean isOpened() {
        return isOpened;
    }

    public void startPreview() {
        camera.startPreview();
    }

    public void stopPreview() {
        camera.stopPreview();
    }

    public Camera getCamera() {
        return camera;
    }

    public void release() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
            isOpened = false;
        }
    }


}
