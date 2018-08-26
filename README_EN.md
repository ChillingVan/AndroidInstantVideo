# AndroidInstantVideo
Show the stream of Android video hardware encode, including video processing and video publishing.

1. Use MediaCodec for H264 encoding.
2. Use MediaCodec for AAC encoding.
3. Use RTMP for video publishing.


**Thanks for these projects**

[LibRtmp-Client-for-Android](https://github.com/ant-media/LibRtmp-Client-for-Android)


## Requirement
* Android API >= 18 


### Feature

* MediaCodec Hardware encoding for H264 + Video Processing
  
  Sample: [TestVideoEncoder](https://github.com/ChillingVan/AndroidInstantVideo/blob/master/app/src/main/java/com/chillingvan/instantvideo/sample/test/video/TestVideoEncoder.java)
  The Video Processing Part：
```java
public class TestVideoEncoder {
//...
    public void prepareEncoder() {
    //...
        h264Encoder.setOnDrawListener(new H264Encoder.OnDrawListener() {
            @Override
            public void onGLDraw(ICanvasGL canvasGL, SurfaceTexture producedSurfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {
                // 此处可以使用canvasGL的drawTexture, drawBitmap等方法实现对视频帧的处理.
                // 
                // 
            }
        });
    //...
    }
//...
}
```
  The key for video frame processing : [android-opengl-canvas](https://github.com/ChillingVan/android-openGL-canvas)

  H264 test file: /storage/sdcard/Android/data/com.chillingvan.instantvideo.sample/files/test_h264_encode.h264，可以在代码里修改输出路径
```java
public class TestVideoEncoder {
    public TestVideoEncoder(Context ctx, final EglContextWrapper eglCtx) {
        try {
            os = new FileOutputStream(ctx.getExternalFilesDir(null) + File.separator + "test_h264_encode.h264");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
```

You can use [PotPlayer](http://potplayer.daum.net/?lang=zh_CN) to play the test files.


* Camera

  I wrap a class for simple usage [InstantVideoCamera](https://github.com/ChillingVan/AndroidInstantVideo/blob/master/applibs/src/main/java/com/chillingvan/lib/camera/InstantVideoCamera.java)
  Not work well at my 4.4 phone but fine on my 6.0 phone.



* AAC Audio

  Sample: [TestAudioEncoder](https://github.com/ChillingVan/AndroidInstantVideo/blob/master/app/src/main/java/com/chillingvan/instantvideo/sample/test/audio/TestAudioEncoder.java)
  

  Test file directory:/storage/sdcard/Android/data/com.chillingvan.instantvideo.sample/files/test_aac_encode.aac"，可以在代码里修改输出路径
  You can use [PotPlayer](http://potplayer.daum.net/?lang=zh_CN) to play the test files.


* [LibRtmp](https://github.com/ant-media/LibRtmp-Client-for-Android) for RTMP Stream

  Use [nginx-rtmp-module](https://github.com/arut/nginx-rtmp-module) for your Nginx test local server.
  Use [OBS](https://obsproject.com/) for comparing effect. And player can be VLC，PotPlayer，ffplay. Make sure you use player open stream first because of the simple test server.
  
  Sample: [TestCameraPublisherActivity](https://github.com/ChillingVan/AndroidInstantVideo/blob/master/app/src/main/java/com/chillingvan/instantvideo/sample/test/publisher/TestCameraPublisherActivity.java)
```java

public class TestCameraPublisherActivity extends AppCompatActivity {
    ...
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ...
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                StreamPublisher.StreamPublisherParam streamPublisherParam = new StreamPublisher.StreamPublisherParam();
                streamPublisher.prepareEncoder(streamPublisherParam, new H264Encoder.OnDrawListener() {
                    @Override
                    public void onGLDraw(ICanvasGL canvasGL, SurfaceTexture surfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {

                        // Here you can do video process
                        // 此处可以视频处理，例如加水印等等
                        canvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, 0, 0, outsideTexture.getWidth(), outsideTexture.getHeight());
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
    ...
}
```

### Latest Update:
1. Add Mp4Muxer(Use Android MediaMuxer). Sample: TestMp4MuxerActivity
2. Update Interface Imuxer. Add more parameters to StreamPublisherParam. 

### TODO

1. RTSP Stream.

###  Pull Request

Welcome.
Please add sample code to sample module. Add your license comment.



## License
    Copyright 2017 ChillingVan.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
