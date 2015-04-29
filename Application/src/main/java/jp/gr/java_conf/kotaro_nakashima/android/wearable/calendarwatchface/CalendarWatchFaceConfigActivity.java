/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.gr.java_conf.kotaro_nakashima.android.wearable.calendarwatchface;

import android.app.Activity;
import android.content.ComponentName;
import android.os.Bundle;
import android.support.wearable.companion.WatchFaceCompanion;
import android.widget.TextView;

import jp.gr.java_conf.kotaro_nakashima.android.wearable.calendarwatchface.R;
// TODO: [2015-04-29 12:43]
// プロジェクトのパッケージ名をgresregですべて変更したら、
// 何故かRが認識しなくなったので、インポート文を追加した。

public class CalendarWatchFaceConfigActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_watch_face_config);

        ComponentName name =
                getIntent().getParcelableExtra(WatchFaceCompanion.EXTRA_WATCH_FACE_COMPONENT);
        TextView label = (TextView) findViewById(R.id.label);
        //label.setText(label.getText() + " (" + name.getClassName() + ")");
        label.setText(label.getText());
    }
}
