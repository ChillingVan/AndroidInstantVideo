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

package com.chillingvan.instantvideo.sample.test.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.canvasgl.textureFilter.BasicTextureFilter;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;
import com.chillingvan.instantvideo.sample.R;

import static com.chillingvan.instantvideo.sample.test.video.TestVideoEncoder.drawCnt;

/**
 * Created by Chilling on 2016/11/3.
 */

public class ProduceTextureView extends GLSurfaceTextureProducerView {

    private TextureFilter textureFilter = new BasicTextureFilter();
    private Bitmap bitmap;

    public ProduceTextureView(Context context) {
        super(context);
    }

    public ProduceTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProduceTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void onSurfaceCreated() {
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.lenna);
        setProducedTextureTarget(GLES20.GL_TEXTURE_2D);
        super.onSurfaceCreated();
    }

    public void setTextureFilter(TextureFilter textureFilter) {
        this.textureFilter = textureFilter;
    }


    @Override
    protected void onGLDraw(ICanvasGL canvas, SurfaceTexture producedSurfaceTexture, RawTexture producedRawTexture, @Nullable SurfaceTexture sharedSurfaceTexture, @Nullable BasicTexture sharedTexture) {
        if (drawCnt == 19 || drawCnt == 39) {
            canvas.drawBitmap(bitmap, 0, 0);
        }
        TestVideoEncoder.drawRect(canvas, drawCnt);
    }
}
