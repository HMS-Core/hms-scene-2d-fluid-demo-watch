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
import android.content.res.Configuration;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.huawei.hms.scene.engine.iphysics.utils.CircleShape;
import com.huawei.hms.scene.engine.iphysics.utils.Color;
import com.huawei.hms.scene.engine.iphysics.utils.EdgeShape;
import com.huawei.hms.scene.engine.iphysics.utils.ParticleGroupInfo;
import com.huawei.hms.scene.engine.iphysics.utils.PolygonShape;
import com.huawei.hms.scene.engine.iphysics.utils.Shape;
import com.huawei.hms.scene.engine.iphysics.utils.Vector2;
import com.huawei.hms.scene.sdk.fluid.Body;
import com.huawei.hms.scene.sdk.fluid.ParticleSystem;
import com.huawei.hms.scene.sdk.fluid.World;
import com.huawei.scenekit.fluiddemo.shader.ProgramUtil;
import com.huawei.scenekit.fluiddemo.util.Config;
import com.huawei.scenekit.fluiddemo.util.WorldManager;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Description: Render
 *
 * @author HUAWEI
 * @since 2022-06-29
 */
public class Render implements GLSurfaceView.Renderer {
    /**
     * DIAL_KEY_NUM
     */
    public static final int DIAL_KEY_NUM = 10;

    /**
     * WATER
     */
    public static final int WATER = 1 << 0;

    /**
     * VISCOUS
     */
    public static final int VISCOUS = 1 << 1;

    /**
     * STRESSFUL
     */
    public static final int STRESSFUL = 1 << 2;

    /**
     * MIX_COLOR
     */
    public static final int MIX_COLOR = 1 << 3;

    /**
     * REPULSIVE
     */
    public static final int REPULSIVE = 1 << 4;

    /**
     * TENSILE
     */
    public static final int TENSILE = 1 << 5;

    /**
     * POWER
     */
    public static final int POWER = 1 << 6;

    /**
     * WALL
     */
    public static final int WALL = 1 << 7;

    /**
     * BARRIER
     */
    public static final int BARRIER = 1 << 8;

    /**
     * ZOMBIE
     */
    public static final int ZOMBIE = 1 << 9;

    private static Render instance = new Render();
    private static final int DTIME = 32;
    private static final float CIRCLE_PARTITION = 60;

    /**
     * instance if WorldManager
     */
    protected WorldManager worldManager;

    /**
     * instance if Activity
     */
    protected Activity activity = null;

    /**
     * instance if NodeRender
     */
    protected NodeRender nodeRender;

    /**
     * instance if CanvasRender
     */
    protected CanvasRender canvasRender;

    /**
     * instance if DrawShape
     */
    protected DrawShape drawShape;

    /**
     * instance if DebugDraw
     */
    protected DebugDraw debugDraw;

    /**
     * instance if Surface
     */
    protected Surface screenSurface;

    /**
     * List shapes
     */
    protected List<Shape> shapes = new ArrayList<>();

    /**
     * frame number
     */
    protected int frameNumber = 0;

    /**
     * system current time
     */
    protected long nowTime = System.currentTimeMillis();

    /**
     * system last time
     */
    protected long lastTime = System.currentTimeMillis();

    /**
     * fps time
     */
    protected long fpsTime = 0;

    /**
     * count time
     */
    protected long countTime = 0;

    private boolean update = false;
    private Body border = null;
    private Body pins = null;
    private Body[] circleDialKeyBody = null;
    private int second = 0;
    private float minite = 0;
    private float hour = 40;

    protected Render() {
        worldManager = new WorldManager();
        nodeRender = new NodeRender(this);
        canvasRender = new CanvasRender(this);
        drawShape = new DrawShape();
        debugDraw = new DebugDraw();
    }

    public static Render getInstance() {
        return instance;
    }

    /**
     * init activity
     *
     * @param activity the activity
     */
    public void init(Activity activity) {
        this.activity = activity;
        // Rebuild the world.
        resetWorld();
        // Rebuild borders.
        resetBorder();
        // Rebuid particles.
        resetNodes();
    }

    @Override
    protected void finalize() {
        deleteAll();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        // Load shaders.
        ProgramUtil.loadAllShaders(activity.getAssets());

        canvasRender.onSurfaceCreated(activity, Config.SurfaceViewId.Default);

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

        resetBorder();
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

        // If you want to limit the frame, please call the optimize(); function here
        nowTime = System.currentTimeMillis();
        long flagTime = nowTime - countTime;
        if (flagTime >= 1000) {
            second++;
            countTime = nowTime;
            if (second >= 60) {
                second = 0;
            }
            changeCircleBorder(second);
        }
    }

    /**
     * optimize frame
     */
    public void optimize() {
        frameNumber++;
        nowTime = System.currentTimeMillis();
        long totaltTime = nowTime - fpsTime;

        if (totaltTime >= 1000) {
            Log.d("FLUID", "fluiddemo fps:" + (1000L * frameNumber / totaltTime));
            frameNumber = 0;
            fpsTime = nowTime;
        }

        long dt = nowTime - lastTime;
        if (dt < DTIME) {
            try {
                Thread.sleep(DTIME - dt);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lastTime = System.currentTimeMillis();
    }

    /**
     * Create surface
     *
     * @param width the surface width
     * @param height the surface height
     */
    public void createSurface(int width, int height) {
        screenSurface = new Surface(width, height);
        screenSurface.setClearColor(Config.CLEAR_COLOR);
    }

    /**
     * Get ScreenSurface
     *
     * @return screen surface
     */
    public Surface getScreenSurface() {
        return screenSurface;
    }

    /**
     * Stop simulation.
     */
    public void pause() {
        update = false;
    }

    /**
     * Start simulation.
     */
    public void start() {
        update = true;
    }

    /**
     * Obtains the world management class.
     *
     * @return WorldManager
     */
    public WorldManager getManager() {
        return worldManager;
    }

    /**
     * Increase the water volume.
     */
    public void addWater() {
        worldManager.acquire();
        try {
            ParticleGroupInfo info = new ParticleGroupInfo(
                ParticleGroupInfo.ParticleFlag.WATER | ParticleGroupInfo.ParticleFlag.MIX_COLOR);

            // Add particles to an area.
            CircleShape shape = new CircleShape();
            shape.setRadius(0.5f);
            shape.setPosition(new Vector2(Config.WORLD_HEIGHT / 2, Config.WORLD_HEIGHT / 2));
            info.setShape(shape);

            Color color = new Color(30, 144, 255, 220);
            info.setColor(color);

            ParticleSystem system = worldManager.getParticleSystem();

            if (system.getParticleCount() <= Config.MAX_NODE_COUNT_WATCH) {
                system.addParticles(info);
            }
        } finally {
            worldManager.release();
        }
    }

    /**
     * Decrease the water volume.
     */
    public void deleteWater() {
        worldManager.acquire();
        try {
            ParticleSystem system = worldManager.getParticleSystem();
            system.deleteParticles(100);
        } finally {
            worldManager.release();
        }
    }

    /**
     * Cyclically perform simulation.
     */
    protected void simulate() {
        if (!update) {
            return;
        }

        World world = worldManager.acquire();
        try {
            world.singleStep(Config.TIME_INTERVAL);
        } finally {
            worldManager.release();
        }
    }

    /**
     * Cyclically perform rendering.
     */
    protected void draw() {
        synchronized (Render.class) {
            // Draw a background.
            canvasRender.draw();
            // Draw particles.
            nodeRender.draw();
            // Special Draw
            drawShape.draw(shapes);
        }
    }

    /**
     * delete all
     */
    protected void deleteAll() {
        World world = worldManager.acquire();
        try {
            if (border != null) {
                world.destroyBody(border);
                border = null;
            }
            deleteDialKey(world);
            worldManager.deleteWorld();
        } finally {
            worldManager.release();
        }
    }

    private void deleteDialKey(World world) {
        for (int i = 0; i < DIAL_KEY_NUM; i++) {
            if (circleDialKeyBody != null) {
                if (circleDialKeyBody[i] != null) {
                    world.destroyBody(circleDialKeyBody[i]);
                    circleDialKeyBody[i] = null;
                }
            }
        }
    }

    /**
     * Reset the world.
     */
    protected void resetWorld() {
        worldManager.acquire();
        try {
            worldManager.init();
        } finally {
            worldManager.release();
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

            float x = Config.WORLD_WIDTH / 2;
            float y = Config.WORLD_HEIGHT / 2;
            float radius = x;
            border.addCircleBorder(x, y, radius);
        } finally {
            worldManager.release();
        }
    }

    private void resetCircleBorder() {
        float x = Config.WORLD_WIDTH / 2;
        float y = Config.WORLD_HEIGHT / 2;
        float radius = x;

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
            border.addCircleBorder(x, y, radius);
        } finally {
            worldManager.release();
        }
    }

    /**
     * change circle border
     *
     * @param second the time second
     */
    protected void changeCircleBorder(int second) {
        shapes.clear();

        World world = worldManager.acquire();
        try {
            // Clear body information.
            if (pins != null) {
                world.destroyBody(pins);
            }

            pins = world.createBody(World.BodyType.STATIC_BODY);
            if (pins == null) {
                Log.e("Render", "resetBorder: border is null");
                return;
            }

            float radius = Config.WORLD_WIDTH / 2;
            float thickSecond = Config.THICKNESS / 80;
            float thickMinite = Config.THICKNESS / 30;
            float thickHour = Config.THICKNESS / 30;

            // clock second
            float angleSecond = (float) (2 * Math.PI * (60 - second) / CIRCLE_PARTITION);
            PolygonShape pinSecond = new PolygonShape(thickSecond, radius * 0.35f,
                new Vector2((float)(radius - radius * 0.38f * Math.sin(angleSecond)),
                    (float)(radius + radius * 0.38f * Math.cos(angleSecond))), angleSecond);
            pins.addPolygonShape(pinSecond);

            minite += 0.016666667f;
            if (minite >= 60) {
                minite = 0;
            }
            // clock minite
            float angleMinite = (float) (2 * Math.PI * (60 - minite) / CIRCLE_PARTITION);
            PolygonShape pinMinite = new PolygonShape(thickMinite, radius * 0.27f,
                new Vector2((float)(radius - radius * 0.38f * Math.sin(angleMinite)),
                    (float)(radius + radius * 0.38f * Math.cos(angleMinite))), angleMinite);
            pins.addPolygonShape(pinMinite);

            hour += 0.001388889f;
            if (hour >= 60) {
                hour = 0;
            }
            // clock hour
            float angleHour = (float) (2 * Math.PI * (60 - hour) / CIRCLE_PARTITION);
            PolygonShape pinHour = new PolygonShape(thickHour, radius * 0.20f,
                new Vector2((float)(radius - radius * 0.38f * Math.sin(angleHour)),
                    (float)(radius + radius * 0.38f * Math.cos(angleHour))), angleHour);
            pins.addPolygonShape(pinHour);
            shapes.addAll(pins.getShapes());
        } finally {
            worldManager.release();
        }
    }

    // Create dial keys.
    private void resetDialKey() {
        World world = worldManager.acquire();
        circleDialKeyBody = new Body[DIAL_KEY_NUM];
        int ori = activity.getResources().getConfiguration().orientation;

        try {
            int dialKeyCount = 0;
            final int keys = 3;
            for (int i = 0; i < keys; i++) {
                for (int j = 0; j < keys; j++) {
                    if (circleDialKeyBody[dialKeyCount] != null) {
                        world.destroyBody(circleDialKeyBody[dialKeyCount]);
                    }

                    circleDialKeyBody[dialKeyCount] = world.createBody(World.BodyType.STATIC_BODY);

                    CircleShape circleShape = new CircleShape();
                    if (ori == Configuration.ORIENTATION_LANDSCAPE) {
                        circleShape.setPosition(new Vector2(Config.WORLD_HEIGHT * 0.75f + 0.8f + 0.8f * i,
                            (Config.DEFAULT_WORLD_HEIGHT / 4) * (1 + j)));
                    } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
                        circleShape.setPosition(new Vector2((Config.WORLD_WIDTH / 4) * (1 + i),
                            (Config.DEFAULT_WORLD_HEIGHT / 5) * (2 + j)));
                    } else {
                        circleShape.setPosition(new Vector2(0, 0));
                    }
                    circleShape.setRadius(0.2f);
                    circleDialKeyBody[dialKeyCount].addCircleShape(circleShape);
                    ++dialKeyCount;
                }
            }

            {
                CircleShape circleShape = new CircleShape();
                if (ori == Configuration.ORIENTATION_LANDSCAPE) {
                    circleShape.setPosition(new Vector2(Config.WORLD_HEIGHT * 0.75f + 0.8f + 0.8f * 2 + 0.8f,
                        (Config.DEFAULT_WORLD_HEIGHT / 4) * (1 + 1)));
                } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
                    circleShape.setPosition(new Vector2(Config.WORLD_WIDTH / 4 * 2,
                        (Config.DEFAULT_WORLD_HEIGHT / 5)));
                } else {
                    circleShape.setPosition(new Vector2(0, 0));
                }
                circleShape.setRadius(0.2f);
                circleDialKeyBody[dialKeyCount] = world.createBody(World.BodyType.STATIC_BODY);
                circleDialKeyBody[dialKeyCount].addCircleShape(circleShape);
            }
        } finally {
            worldManager.release();
        }
    }

    private void resetNodes() {
        worldManager.acquire();
        try {
                ParticleGroupInfo groupDef = new ParticleGroupInfo(WATER | MIX_COLOR);
                groupDef.setColor(Config.DEFAULT_COLOR);

                // Set the shape of a particle group.
                PolygonShape shape = new PolygonShape(0.f, 0.f, new Vector2(0.f, 0.f), 0);
                shape.setBox(Config.DEFAULT_WORLD_HEIGHT * 0.38f, Config.DEFAULT_WORLD_HEIGHT * 0.38f,
                    new Vector2(Config.DEFAULT_WORLD_HEIGHT / 2, Config.DEFAULT_WORLD_HEIGHT / 2), 0);
                groupDef.setShape(shape);

                ParticleSystem system = worldManager.getParticleSystem();
                system.addParticles(groupDef);
        } finally {
            worldManager.release();
        }
    }

    /**
     * Adjust the view size.
     *
     * @param width the view width
     * @param height the view height
     */
    protected void changeViewSize(int width, int height) {
        if (height > width) {
            // portrait
            Config.setWorldWidth(Config.DEFAULT_WORLD_HEIGHT);
            Config.setWorldHeight(height * Config.DEFAULT_WORLD_HEIGHT / width);
        } else {
            // landspace
            Config.setWorldHeight(Config.DEFAULT_WORLD_HEIGHT);
            Config.setWorldWidth(width * Config.DEFAULT_WORLD_HEIGHT / height);
        }
    }
}
