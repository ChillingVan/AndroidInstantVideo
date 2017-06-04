package com.chillingvan.instantvideo.sample.test.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.chillingvan.instantvideo.sample.R;
import com.chillingvan.instantvideo.sample.test.video.TestVideoEncoder;
import com.chillingvan.lib.camera.InstantVideoCamera;
import com.chillingvan.lib.encoder.video.H264Encoder;

public class TestCameraAndVideoActivity extends AppCompatActivity {

    private TestVideoEncoder testVideoEncoder;
    private CameraPreviewTextureView cameraPreviewTextureView;
    private InstantVideoCamera instantVideoCamera;
    private HandlerThread outputVideoThread;
    private Handler outputVideoHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_and_video);
        cameraPreviewTextureView = (CameraPreviewTextureView) findViewById(R.id.camera_produce_view);
        instantVideoCamera = new InstantVideoCamera(Camera.CameraInfo.CAMERA_FACING_FRONT, 1280, 720);

    }

    private void initWriteFileHandler() {
        outputVideoThread = new HandlerThread("outputVideoThread");
        outputVideoThread.start();

        outputVideoHandler = new Handler(outputVideoThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0) {
                    testVideoEncoder.write();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        initWriteFileHandler();
        instantVideoCamera.openCamera();
        initCameraTexture();
        cameraPreviewTextureView.onResume();
    }

    private void initCameraTexture() {
        cameraPreviewTextureView.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
            @Override
            public void onCreate(EglContextWrapper eglContext) {
                testVideoEncoder = new TestVideoEncoder(getApplicationContext(), eglContext);
            }
        });
        cameraPreviewTextureView.setOnSurfaceTextureSet(new GLSurfaceTextureProducerView.OnSurfaceTextureSet() {
            @Override
            public void onSet(SurfaceTexture surfaceTexture, RawTexture rawTexture) {
                testVideoEncoder.setSharedTexture(rawTexture, surfaceTexture);
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        cameraPreviewTextureView.requestRenderAndWait();
                        if (testVideoEncoder.encodeAFrame()) {
                            outputVideoHandler.sendEmptyMessage(0);
                        }
                    }
                });

                instantVideoCamera.setPreview(surfaceTexture);
                instantVideoCamera.startPreview();
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        instantVideoCamera.release();
        cameraPreviewTextureView.onPause();

        if (testVideoEncoder.isStart()) {
            testVideoEncoder.stop();
        }
        outputVideoThread.quit();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        testVideoEncoder.destroy();
    }
    public void clickStartTest(View view) {
        TextView textView = (TextView) view;
        if (testVideoEncoder.isStart()) {
            testVideoEncoder.stop();
            textView.setText("START");
        } else {
            testVideoEncoder.prepareEncoder(new H264Encoder.OnDrawListener() {
                @Override
                public void onGLDraw(ICanvasGL canvasGL, SurfaceTexture producedSurfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {
                    canvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, 0, 0, outsideTexture.getWidth(), outsideTexture.getHeight());
                    Log.i("TestVideoEncoder", "gl draw");
                }
            });
            testVideoEncoder.start();
            textView.setText("STOP");
        }
    }
}
