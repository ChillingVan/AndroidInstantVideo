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

package com.chillingvan.instantvideo.sample.test.audio;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.chillingvan.instantvideo.sample.R;

public class TestAudioEncoderActivity extends AppCompatActivity {

    private TestAudioEncoder testAudioEncoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_audio_encoder);

        testAudioEncoder = new TestAudioEncoder(getApplicationContext());
        testAudioEncoder.prepareEncoder();
    }




    @Override
    protected void onPause() {
        super.onPause();
        testAudioEncoder.stop();
    }

    public void clickStartTest(View view) {
        TextView textView = (TextView) view;
        if (testAudioEncoder.isStart()) {
            testAudioEncoder.stop();
            textView.setText("RECORD");
        } else {
            testAudioEncoder.prepareEncoder();
            testAudioEncoder.start();
            textView.setText("PAUSE");
        }
    }
}
