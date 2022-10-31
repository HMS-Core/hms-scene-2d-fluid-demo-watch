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

import android.content.Context;
import android.opengl.GLES20;

import com.huawei.hms.scene.engine.iphysics.utils.PolygonShape;
import com.huawei.hms.scene.engine.iphysics.utils.Shape;
import com.huawei.scenekit.fluiddemo.shader.Material;
import com.huawei.scenekit.fluiddemo.shader.Program;
import com.huawei.scenekit.fluiddemo.shader.ProgramUtil;
import com.huawei.scenekit.fluiddemo.shader.Texture;
import com.huawei.scenekit.fluiddemo.util.Config;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * Description: DrawShape
 *
 * @author HUAWEI
 * @since 2022-9-27
 */
public class DrawShape {
    private Material rectMaterial;
    private float[] worldTransform;

    public DrawShape() {
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
     *
     * @param context the context
     */
    public void onSurfaceCreated(Context context) {
        rectMaterial = new Material(new Program(ProgramUtil.Shader.RECT));
        rectMaterial.addAttribute("position", 2, ProgramUtil.FLOAT, 4, false);
        rectMaterial.addAttribute("uv", 2, ProgramUtil.FLOAT, 4, false);
        rectMaterial.addSamplerTexture("texture", new Texture(context, Config.HANDS_TEXTURE_NAME));
    }

    /**
     * Draw shapes
     *
     * @param shapes list of shape
     */
    public void draw(List<Shape> shapes) {
        for (int i = 0; i < shapes.size(); i++) {
            if (shapes.get(i).getType() == Shape.Type.POLYGON) {
                PolygonShape pgn = (PolygonShape) shapes.get(i);
                float[] points = {
                    pgn.getVertices()[1].x, pgn.getVertices()[1].y,
                    pgn.getVertices()[0].x, pgn.getVertices()[0].y,
                    pgn.getVertices()[3].x, pgn.getVertices()[3].y,
                    pgn.getVertices()[2].x, pgn.getVertices()[2].y
                };

                FloatBuffer vertexBuffer = ByteBuffer
                    .allocateDirect(points.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(points);
                vertexBuffer.position(0);

                float[] data = new float[] {0, 1, 1, 1, 0, 0, 1, 0};
                FloatBuffer coordBuffer = ByteBuffer
                    .allocateDirect(data.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
                coordBuffer.put(data);

                rectMaterial.startRender();
                rectMaterial.setVertexBuffer("position", vertexBuffer, 0, 0);
                coordBuffer.rewind();
                rectMaterial.setVertexBuffer("uv", coordBuffer, 0, 0);
                rectMaterial.updateUniform("mvp", worldTransform);
                rectMaterial.draw(GLES20.GL_TRIANGLE_FAN, 0, 4);
                rectMaterial.endRender();
            }
        }
    }
}
