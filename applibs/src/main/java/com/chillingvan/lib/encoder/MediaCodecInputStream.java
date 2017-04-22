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
import android.support.annotation.NonNull;
import android.util.Log;

import com.chillingvan.canvasgl.Loggers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * An InputStream that uses data from a MediaCodec.
 * The purpose of this class is to interface existing RTP packetizers of
 * libstreaming with the new MediaCodec API. This class is not thread safe !
 */
@SuppressLint("NewApi")
public class MediaCodecInputStream extends InputStream {

    public final String TAG = "MediaCodecInputStream";

    private MediaCodec mMediaCodec = null;
    private BufferInfo mBufferInfo = new BufferInfo();
    private ByteBuffer mBuffer = null;
    private boolean mClosed = false;

    public MediaFormat mMediaFormat;
    private ByteBuffer[] encoderOutputBuffers;

    public MediaCodecInputStream(MediaCodec mediaCodec) {
        mMediaCodec = mediaCodec;
        encoderOutputBuffers = mMediaCodec.getOutputBuffers();
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
        int readLength = 0;
        int encoderStatus = -1;

        try {
            if (mBuffer == null) {
                while (!Thread.interrupted() && !mClosed) {
                    encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 50000);
                    Loggers.d(TAG, "Index: " + encoderStatus + " Time: " + mBufferInfo.presentationTimeUs + " size: " + mBufferInfo.size);
                    if (encoderStatus >= 0) {
                        mBuffer = encoderOutputBuffers[encoderStatus];
                        mBuffer.position(0);
                        break;
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        encoderOutputBuffers = mMediaCodec.getOutputBuffers();
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mMediaFormat = mMediaCodec.getOutputFormat();
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


            if (mClosed) throw new IOException("This InputStream was closed");

            readLength = length < mBufferInfo.size - mBuffer.position() ? length :
                    mBufferInfo.size - mBuffer.position();
            mBuffer.get(buffer, offset, readLength);
            if (mBuffer.position() >= mBufferInfo.size) {
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                mBuffer = null;
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
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

    public static void readAll(MediaCodecInputStream is, byte[] buffer, int offset, @NonNull OnReadAllCallback onReadAllCallback) {
        int readSize = 0;
        if (is.available() <= 0) {
            return;
        }
        do {
            try {
                readSize = is.read(buffer, offset, buffer.length);
                onReadAllCallback.onReadOnce(buffer, readSize, is.getLastBufferInfo().size);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (readSize > 0);
    }

    public interface OnReadAllCallback {
        void onReadOnce(byte[] buffer, int readSize, int mediaBufferSize);
    }
}
