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

import android.opengl.GLES20;
import android.util.Log;

import com.huawei.hms.scene.engine.iphysics.utils.CircleShape;
import com.huawei.hms.scene.engine.iphysics.utils.EdgeShape;
import com.huawei.hms.scene.engine.iphysics.utils.PolygonShape;
import com.huawei.hms.scene.engine.iphysics.utils.Shape;
import com.huawei.hms.scene.engine.iphysics.utils.Vector2;
import com.huawei.scenekit.fluiddemo.shader.Material;
import com.huawei.scenekit.fluiddemo.shader.Program;
import com.huawei.scenekit.fluiddemo.shader.ProgramUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

/**
 * Description: DebugDraw
 *
 * @author HUAWEI
 * @since 2022-9-27
 */
public class DebugDraw {
    private static final int CIRCLE_PARTITION = 32;
    private Material debugDrawMaterial;
    private float[] worldTransform;

    public DebugDraw() {
    }

    /**
     * Set world transform
     *
     * @param worldTransform the world transform
     */
    public void setWorldTransform(float[] worldTransform) {
        this.worldTransform = worldTransform;
    }

    /**
     * onSurfaceCreated override
     */
    public void onSurfaceCreated() {
        debugDrawMaterial = new Material(new Program(ProgramUtil.Shader.DEBUG));
        debugDrawMaterial.addAttribute("position", 2, ProgramUtil.FLOAT, 4, false);
    }

    /**
     * draw shapes
     *
     * @param shapes list of shape
     */
    public void draw(List<Shape> shapes) {
        for (int i = 0; i < shapes.size(); i++) {
            if (shapes.get(i).getType() == Shape.Type.POLYGON) {
                PolygonShape pgn = (PolygonShape) shapes.get(i);
                float[] points = {pgn.getVertices()[1].x, pgn.getVertices()[1].y,
                    pgn.getVertices()[0].x, pgn.getVertices()[0].y,
                    pgn.getVertices()[3].x, pgn.getVertices()[3].y,
                    pgn.getVertices()[2].x, pgn.getVertices()[2].y};

                FloatBuffer vertexBuffer = createBuffer(points);
                vertexBuffer.position(0);
                renderMaterial(GLES20.GL_TRIANGLE_FAN, 0, 4, vertexBuffer);
            } else if (shapes.get(i).getType() == Shape.Type.EDGE) {
                EdgeShape edge = (EdgeShape) shapes.get(i);
                float[] points = {edge.getVertex()[0].x, edge.getVertex()[0].y,
                    edge.getVertex()[1].x, edge.getVertex()[1].y};
                FloatBuffer vertexBuffer = createBuffer(points);
                vertexBuffer.position(0);
                renderMaterial(GLES20.GL_LINES, 0, 2, vertexBuffer);
            } else if (shapes.get(i).getType() == Shape.Type.CIRCLE) {
                CircleShape circle = (CircleShape) shapes.get(i);
                float radius = circle.getRadius();
                Vector2 center = circle.getPosition();
                Vector2 v1 = new Vector2(center.x - radius, center.y);
                Vector2 v2 = new Vector2();
                for (int j = 0; j <= CIRCLE_PARTITION; j++) {
                    float angle = (float) (2 * Math.PI * j / CIRCLE_PARTITION);
                    v2.setValue(center.x - (float) Math.cos(angle) * radius, center.y
                        + (float) Math.sin(angle) * radius);

                    float[] points = {v1.x, v1.y, v2.x, v2.y, center.x, center.y};
                    FloatBuffer vertexBuffer = createBuffer(points);
                    vertexBuffer.position(0);

                    renderMaterial(GLES20.GL_TRIANGLES, 0, 3, vertexBuffer);
                    v1.setValue(v2.x, v2.y);
                }
            } else {
                Log.d("DebugDraw", "draw: shape is invalid type");
            }
        }
    }

    private void renderMaterial(int type, int offset, int count, FloatBuffer vertexBuffer) {
        debugDrawMaterial.startRender();
        debugDrawMaterial.setVertexBuffer("position", vertexBuffer, 0, 0);
        debugDrawMaterial.updateUniform("mvp", worldTransform);
        debugDrawMaterial.draw(type, offset, count);
        setLineAttributes();
        debugDrawMaterial.endRender();
    }

    private void setLineAttributes() {
        GLES20.glEnable(GL10.GL_SMOOTH);
        GLES20.glLineWidth(4);
    }

    private FloatBuffer createBuffer(float[] points) {
        return ByteBuffer.allocateDirect(points.length * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().put(points);
    }
}
