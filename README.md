# Show the Video Stream
展现Android硬编码下的视频数据流动，可以对视频做处理，例如加滤镜，加水印等。

本项目主要是为了展现Android使用硬编码下的视频数据流动，目前只完成了H264和AAC编码以及对视频帧的图像处理，欢迎Fork和Pull Request。

### 功能：

* 硬编码H264格式视频 + 对视频帧的图像处理
  
  具体看例子里的 [TestVideoEncoder](https://github.com/ChillingVan/Android_ShowVideoStream/blob/master/app/src/main/java/com/chillingvan/instantvideo/sample/test/video/TestVideoEncoder.java)
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

关于Camera，我已经封装好了 [InstantVideoCamera](https://github.com/ChillingVan/Android_ShowVideoStream/blob/master/applibs/src/main/java/com/chillingvan/lib/camera/InstantVideoCamera.java)


不过没写sample，以后补上。

* 编码产生aac格式音频

  直接使用手机进行录音。
  具体看例子里的 [TestAudioEncoder](https://github.com/ChillingVan/Android_ShowVideoStream/blob/master/app/src/main/java/com/chillingvan/instantvideo/sample/test/audio/TestAudioEncoder.java)
  

  例子中生成的aac文件在/storage/sdcard/Android/data/com.chillingvan.instantvideo.sample/files/test_aac_encode.aac"，可以在代码里修改输出路径
  对于生成的文件，可以在PC上使用 [PotPlayer](http://potplayer.daum.net/?lang=zh_CN) 播放


### TODO
本项目是个未完成的项目，目前可以添加的功能如下：

1. 使用Android的muxer将h264和aac结合生成mp4文件

2. 使用 librtmp 将h264和aac变成RTMP流

3. 变成RTSP流

### 关于 Pull Request

欢迎Fork!
添加了功能发出 Pull Request 的话，希望能在sample的module里添加相应的测试代码，最好在文件开头加上自己的license注释。


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
