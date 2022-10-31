/*
 * Copyright 2022 Huawei Technologies Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.scenekit.fluiddemo;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import com.huawei.scenekit.fluiddemo.render.MultiInstanceRender;
import com.huawei.scenekit.fluiddemo.util.Config;
import com.huawei.scenekit.fluiddemo.util.SensorManager;

/**
 * Description: MultiInstanceActivity
 *
 * @author HUAWEI
 * @since 2022-08-29
 */
public class MultiInstanceActivity extends Activity {
    private GLSurfaceView mainView;
    private GLSurfaceView mainView2;
    private ImageButton add;
    private ImageButton del;
    private ImageButton add2;
    private ImageButton del2;
    private SensorManager sensorManager;
    private SensorManager sensorManager2;
    private MultiInstanceRender render;
    private MultiInstanceRender render2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_instance);

        // Initialize the renderer.
        render = new MultiInstanceRender(Config.SurfaceViewId.One);
        render2 = new MultiInstanceRender(Config.SurfaceViewId.Two);
        render.init(this);
        render2.init(this);

        sensorManager = new SensorManager(this, render);
        sensorManager2 = new SensorManager(this, render2);
        // Initialize mainView.
        initMainView();
        initMainView2();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.onResume();
        sensorManager2.onResume();
        mainView.onResume();
        mainView2.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.onPause();
        sensorManager2.onPause();
        mainView.onPause();
        mainView2.onPause();
    }

    // Initialize mainView.
    private void initMainView() {
        mainView = findViewById(R.id.world);
        if (mainView == null) {
            Log.e("DemoActivity", "initMainView: mainView is null");
            return;
        }
        mainView.setEGLContextClientVersion(Config.EGL_CONTEXT_VERSION);
        mainView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mainView.setPreserveEGLContextOnPause(true);
        mainView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mainView.setRenderer(render);

        // Start rendering.
        render.start();

        add = findViewById(R.id.addWater);
        if (add != null) {
            add.setOnClickListener(view -> render.addWater());
        }
        del = findViewById(R.id.delWater);
        if (del != null) {
            del.setOnClickListener(view -> render.deleteWater());
        }
    }

    private void initMainView2() {
        mainView2 = findViewById(R.id.world2);
        if (mainView2 == null) {
            Log.e("DemoActivity", "initMainView: mainView is null");
            return;
        }
        mainView2.setEGLContextClientVersion(Config.EGL_CONTEXT_VERSION);
        mainView2.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mainView2.setPreserveEGLContextOnPause(true);
        mainView2.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mainView2.setRenderer(render2);

        // Start rendering.
        render2.start();

        add2 = findViewById(R.id.addWater2);
        if (add2 != null) {
            add2.setOnClickListener(view -> render2.addWater());
        }
        del2 = findViewById(R.id.delWater2);
        if (del2 != null) {
            del2.setOnClickListener(view -> render2.deleteWater());
        }
    }
}

