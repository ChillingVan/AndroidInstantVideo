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

package com.chillingvan.lib.encoder;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.chillingvan.canvasgl.util.Loggers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@SuppressLint("NewApi")
public class MediaCodecInputStream extends InputStream {

    public final String TAG = "MediaCodecInputStream";

    private MediaCodec mMediaCodec = null;
    private MediaFormatCallback mediaFormatCallback;
    private BufferInfo mBufferInfo = new BufferInfo();
    private ByteBuffer mBuffer = null;
    private boolean mClosed = false;

    public MediaFormat mMediaFormat;
    private ByteBuffer[] encoderOutputBuffers;

    public MediaCodecInputStream(MediaCodec mediaCodec, MediaFormatCallback mediaFormatCallback) {
        mMediaCodec = mediaCodec;
        this.mediaFormatCallback = mediaFormatCallback;
    }

    @Override
    public void close() {
        mClosed = true;
    }

    @Deprecated
    @Override
    public int read() throws IOException {
        return 0;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int readLength;
        int encoderStatus = -1;

        if (mBuffer == null) {
            while (!Thread.interrupted() && !mClosed) {
                synchronized (mMediaCodec) {
                    if (mClosed) return 0;
                    // timeout should not bigger than 0 for clear voice
                    encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
                    Loggers.d(TAG, "Index: " + encoderStatus + " Time: " + mBufferInfo.presentationTimeUs + " size: " + mBufferInfo.size);
                    if (encoderStatus >= 0) {
                        if (Build.VERSION.SDK_INT >= 21) {
                            mBuffer = mMediaCodec.getOutputBuffer(encoderStatus);
                        } else {
                            mBuffer = mMediaCodec.getOutputBuffers()[encoderStatus];
                        }
                        mBuffer.position(mBufferInfo.offset);
                        mBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                        break;
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mMediaFormat = mMediaCodec.getOutputFormat();
                        if (mediaFormatCallback != null) {
                            mediaFormatCallback.onChangeMediaFormat(mMediaFormat);
                        }
                        Log.i(TAG, mMediaFormat.toString());
                    } else if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        Log.v(TAG, "No buffer available...");
                        return 0;
                    } else {
                        Log.e(TAG, "Message: " + encoderStatus);
                        return 0;
                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        return 0;
                    }
                }
            }
        }


        if (mClosed) throw new IOException("This InputStream was closed");

        readLength = length < mBufferInfo.size - mBuffer.position() ? length :
                mBufferInfo.size - mBuffer.position();
        mBuffer.get(buffer, offset, readLength);
        if (mBuffer.position() >= mBufferInfo.size) {
            mMediaCodec.releaseOutputBuffer(encoderStatus, false);
            mBuffer = null;
        }

        return readLength;
    }

    public int available() {
        if (mBuffer != null)
            return mBufferInfo.size - mBuffer.position();
        else
            return 0;
    }

    public BufferInfo getLastBufferInfo() {
        return mBufferInfo;
    }

    public static void readAll(MediaCodecInputStream is, byte[] buffer, @NonNull OnReadAllCallback onReadAllCallback) {
        byte[] readBuf = buffer;

        int readSize = 0;
        do {
            try {
                readSize = is.read(readBuf, 0, readBuf.length);
                onReadAllCallback.onReadOnce(readBuf, readSize, copyBufferInfo(is.getLastBufferInfo()));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } while (readSize > 0);
    }

    @NonNull
    private static BufferInfo copyBufferInfo(BufferInfo lastBufferInfo) {
        BufferInfo bufferInfo = new BufferInfo();
        bufferInfo.presentationTimeUs = lastBufferInfo.presentationTimeUs;
        bufferInfo.flags = lastBufferInfo.flags;
        bufferInfo.offset = lastBufferInfo.offset;
        bufferInfo.size = lastBufferInfo.size;
        return bufferInfo;
    }

    public interface OnReadAllCallback {
        void onReadOnce(byte[] buffer, int readSize, BufferInfo mediaBufferSize);
    }

    public interface MediaFormatCallback {
        void onChangeMediaFormat(MediaFormat mediaFormat);
    }
}
