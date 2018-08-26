# AndroidInstantVideo
展现Android硬编码下的视频数据流动，可以对视频做处理，例如加滤镜，加水印等。

本项目主要是为了展现Android使用硬编码下的视频数据流动，目前完成了H264和AAC编码以及对视频帧的图像处理，以及RTMP直播推流。欢迎Fork和Pull Request。

[English README](https://github.com/ChillingVan/AndroidInstantVideo/blob/master/README_EN.md) 

**感谢以下项目**
[LibRtmp-Client-for-Android](https://github.com/ant-media/LibRtmp-Client-for-Android)


## 使用要求
* Android API 18 以上

## 用法


### 功能：

* 硬编码H264格式视频 + 对视频帧的图像处理
  
  具体看例子里的 [TestVideoEncoder](https://github.com/ChillingVan/AndroidInstantVideo/blob/master/app/src/main/java/com/chillingvan/instantvideo/sample/test/video/TestVideoEncoder.java)
  其中实现图片处理的部分：
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
  实现图像处理的关键是用了[android-opengl-canvas](https://github.com/ChillingVan/android-openGL-canvas)

  例子中生成的h264文件在/storage/sdcard/Android/data/com.chillingvan.instantvideo.sample/files/test_h264_encode.h264，可以在代码里修改输出路径
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

对于生成的文件，可以在PC上使用 [PotPlayer](http://potplayer.daum.net/?lang=zh_CN) 播放


* 使用 camera 录像并处理

  关于Camera，我已经封装好了 [InstantVideoCamera](https://github.com/ChillingVan/AndroidInstantVideo/blob/master/applibs/src/main/java/com/chillingvan/lib/camera/InstantVideoCamera.java)

  具体可以查看例子，目前例子在我的4.4手机上录下来的文件有一卡一卡的现象，在一部6.0的手机上没有。所以说其实MediaCodec的坑不少，看过不少sdk似乎都是使用ffmpeg作录制。


* 编码产生aac格式音频

  直接使用手机进行录音。
  具体看例子里的 [TestAudioEncoder](https://github.com/ChillingVan/AndroidInstantVideo/blob/master/app/src/main/java/com/chillingvan/instantvideo/sample/test/audio/TestAudioEncoder.java)
  

  例子中生成的aac文件在/storage/sdcard/Android/data/com.chillingvan.instantvideo.sample/files/test_aac_encode.aac"，可以在代码里修改输出路径
  对于生成的文件，可以在PC上使用 [PotPlayer](http://potplayer.daum.net/?lang=zh_CN) 播放


* 使用 [LibRtmp](https://github.com/ant-media/LibRtmp-Client-for-Android) 将h264和aac变成RTMP流 发送到服务器

  需要测试的话，请自行搭建RTMP服务器。我用的是自己搭建的Nginx服务器，用的Module是[nginx-rtmp-module](https://github.com/arut/nginx-rtmp-module)。搭建服务器不需要写代码，根据教程敲几行命令就行。
  可以用开源直播软件[OBS](https://obsproject.com/)对比播放效果。播放器用各种都行，VLC，PotPlayer，ffplay都可以。我用的是ffplay，注意，因为只是简单的服务器，所以要先开播放器连接后再开始启动推流。
  例如我使用的命令是：.\ffplay.exe "rtmp://localhost:19305/live/room live=1"
  
  可以看例子[TestCameraPublisherActivity](https://github.com/ChillingVan/AndroidInstantVideo/blob/master/app/src/main/java/com/chillingvan/instantvideo/sample/test/publisher/TestCameraPublisherActivity.java)
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

### 最近更新
1. 添加使用Android MediaMuxer的 Mp4Muxer，输出Mp4文件。例子 TestMp4MuxerActivity
2. 修改 IMuxer 接口，使之更通用。给StreamPublisherParam添加更多参数。

### TODO

1. RTSP流

### 关于 Pull Request

欢迎Fork!
添加了功能发出 Pull Request 的话，希望能在sample的module里添加相应的测试代码，最好在文件开头加上自己的license注释。


### 相关博文

[使用MediaCodec和RTMP做直播推流](http://www.jianshu.com/p/3c479c0f4876)

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
