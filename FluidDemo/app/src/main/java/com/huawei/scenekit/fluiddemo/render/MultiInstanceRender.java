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

package com.huawei.scenekit.fluiddemo.render;

import android.app.Activity;
import android.opengl.GLES20;
import android.util.Log;

import com.huawei.hms.scene.engine.iphysics.utils.ParticleGroupInfo;
import com.huawei.hms.scene.engine.iphysics.utils.PolygonShape;
import com.huawei.hms.scene.engine.iphysics.utils.Vector2;
import com.huawei.hms.scene.sdk.fluid.Body;
import com.huawei.hms.scene.sdk.fluid.ParticleSystem;
import com.huawei.hms.scene.sdk.fluid.World;
import com.huawei.scenekit.fluiddemo.shader.ProgramUtil;
import com.huawei.scenekit.fluiddemo.util.Config;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Description: MultiInstanceRender
 *
 * @author HUAWEI
 * @since 2022-06-29
 */
public class MultiInstanceRender extends Render {
    private Body border = null;
    private Config.SurfaceViewId surfaceViewId = Config.SurfaceViewId.One;

    public MultiInstanceRender(Config.SurfaceViewId id) {
        super();
        surfaceViewId = id;
    }

    /**
     * init activity
     *
     * @param activity the activity
     */
    public void init(Activity activity) {
        super.init(activity);

        resetNodes2();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        // Load shaders.
        ProgramUtil.loadAllShaders(activity.getAssets());

        canvasRender.onSurfaceCreated(activity, surfaceViewId);

        nodeRender.onSurfaceCreated(activity);

        drawShape.onSurfaceCreated(activity);
        debugDraw.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        // Set the viewport.
        GLES20.glViewport(0, 0, width, height);
        // Adjust the view size.
        changeViewSize(width, height);
        // Rebuild the bounding box.
        if (surfaceViewId == Config.SurfaceViewId.One) {
            resetBorder();
        }
        if (surfaceViewId == Config.SurfaceViewId.Two) {
            resetBorder2();
        }
        createSurface(width, height);
        // Adjust NodeRender.
        nodeRender.onSurfaceChanged(width, height);

        debugDraw.setWorldTransform(nodeRender.getWorldTransform());
        drawShape.setWorldTransform(nodeRender.getWorldTransform());
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        // Simulate particles.
        simulate();

        // Draw all particles.
        draw();

        frameNumber++;
        nowTime = System.currentTimeMillis();
        long totaltTime = nowTime - fpsTime;

        if (totaltTime >= 1000) {
            frameNumber = 0;
            fpsTime = nowTime;
        }
    }

    // Reset borders.
    private void resetBorder() {
        World world = worldManager.acquire();
        try {
            // Clear body information.
            if (border != null) {
                world.destroyBody(border);
            }

            border = world.createBody(World.BodyType.STATIC_BODY);
            if (border == null) {
                Log.e("Render", "resetBorder: border is null");
                return;
            }
            PolygonShape borderShape = new PolygonShape(
                0.f, 0.f, new Vector2(0.f, 0.f), 0.f);

            float width = Config.WORLD_WIDTH;
            float height = Config.WORLD_HEIGHT;
            float thick = Config.THICKNESS;

            // Create the top border.
            borderShape.setBox(width, thick, new Vector2(width / 2, height + thick), 0.f);
            border.addPolygonShape(borderShape);
            // Create the bottom border.
            borderShape.setBox(width, thick, new Vector2(width / 2, -thick), 0.f);
            border.addPolygonShape(borderShape);
            // Create the left border.
            borderShape.setBox(thick, height, new Vector2(-thick, height / 2), 0.f);
            border.addPolygonShape(borderShape);
            // Create the right border.
            borderShape.setBox(thick, height, new Vector2(width + thick, height / 2), 0.f);
            border.addPolygonShape(borderShape);
        } finally {
            worldManager.release();
        }
    }

    // Reset borders.
    private void resetBorder2() {
        float x = Config.WORLD_WIDTH;
        float y = Config.WORLD_HEIGHT;

        World world = worldManager.acquire();
        try {
            // Clear body information.
            if (border != null) {
                world.destroyBody(border);
            }

            border = world.createBody(World.BodyType.STATIC_BODY);
            if (border == null) {
                Log.e("Render", "resetBorder: border is null");
                return;
            }

            border.addRoundRectBorder(x / 8, y / 8, x * 0.6f, y * 0.6f, y / 8);
        } finally {
            worldManager.release();
        }
    }

    private void resetNodes() {
        worldManager.acquire();
        try {
            ParticleGroupInfo groupDef = new ParticleGroupInfo(WATER | MIX_COLOR | VISCOUS);
            groupDef.setColor(Config.DEFAULT_COLOR);

            // Set the shape of a particle group.
            PolygonShape shape = new PolygonShape(0.f, 0.f, new Vector2(0.f, 0.f), 0);
            shape.setBox(Config.DEFAULT_WORLD_HEIGHT * 0.36f, Config.DEFAULT_WORLD_HEIGHT * 0.36f,
                new Vector2(Config.DEFAULT_WORLD_HEIGHT / 2, Config.DEFAULT_WORLD_HEIGHT / 2), 0);
            groupDef.setShape(shape);

            ParticleSystem system = worldManager.getParticleSystem();
            system.addParticles(groupDef);
        } finally {
            worldManager.release();
        }
    }

    private void resetNodes2() {
        worldManager.acquire();
        try {
            ParticleGroupInfo groupDef = new ParticleGroupInfo(WATER | MIX_COLOR );
            groupDef.setColor(Config.DEFAULT_COLOR);

            // Set the shape of a particle group.
            PolygonShape shape = new PolygonShape(0.f, 0.f, new Vector2(0.f, 0.f), 0);
            shape.setBox(Config.DEFAULT_WORLD_HEIGHT * 0.2f, Config.DEFAULT_WORLD_HEIGHT * 0.2f,
                    new Vector2(Config.DEFAULT_WORLD_HEIGHT / 2, Config.DEFAULT_WORLD_HEIGHT / 2), 0);
            groupDef.setShape(shape);

            ParticleSystem system = worldManager.getParticleSystem();
            system.addParticles(groupDef);
        } finally {
            worldManager.release();
        }
    }
}
