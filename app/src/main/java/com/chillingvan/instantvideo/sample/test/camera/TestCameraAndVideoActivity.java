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

    public static final int MESSAGE_START = 1;
    public static final int MESSAGE_STOP = 2;
    private TestVideoEncoder testVideoEncoder;
    private CameraPreviewTextureView cameraPreviewTextureView;
    private InstantVideoCamera instantVideoCamera;
    private Handler handler;
    private HandlerThread inputHandlerThread;
    private HandlerThread outputVideoThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_and_video);
        cameraPreviewTextureView = (CameraPreviewTextureView) findViewById(R.id.camera_produce_view);
        instantVideoCamera = new InstantVideoCamera();


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
                if (msg.what == MESSAGE_START) {
                    if (!testVideoEncoder.isStart()) {
                        testVideoEncoder.prepareEncoder(new H264Encoder.OnDrawListener() {
                            @Override
                            public void onGLDraw(ICanvasGL canvasGL, SurfaceTexture producedSurfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {
                                canvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, 0, 0, outsideTexture.getWidth(), outsideTexture.getHeight());
                                Log.i("TestVideoEncoder", "gl draw");
                            }
                        });
                        testVideoEncoder.start();
                    }
                    testVideoEncoder.writeAFrame();
                    outputVideoHandler.sendEmptyMessage(0);

                    sendEmptyMessage(MESSAGE_START);
                } else if (msg.what == MESSAGE_STOP){
                    testVideoEncoder.stop();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        instantVideoCamera.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT, 1280, 720);
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
            handler.sendEmptyMessage(MESSAGE_STOP);
        }
        inputHandlerThread.quit();
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
            handler.sendEmptyMessage(MESSAGE_STOP);
            textView.setText("START");
        } else {
            handler.sendEmptyMessage(MESSAGE_START);
            textView.setText("STOP");
        }
    }
}
