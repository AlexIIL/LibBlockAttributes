/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.render;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

public final class FluidRenderFace {
    public final double x0, y0, z0, u0, v0;
    public final double x1, y1, z1, u1, v1;
    public final double x2, y2, z2, u2, v2;
    public final double x3, y3, z3, u3, v3;

    /** If true then the renderer should use the centre 16x of the flowing sprite (assuming the texture is 32x),
     * otherwise it should use the still sprite. */
    public final boolean flowing;

    public FluidRenderFace(
        double _x0, double _y0, double _z0, double _u0, double _v0, //
        double _x1, double _y1, double _z1, double _u1, double _v1, //
        double _x2, double _y2, double _z2, double _u2, double _v2, //
        double _x3, double _y3, double _z3, double _u3, double _v3 //
    ) {
        this(
            _x0, _y0, _z0, _u0, _v0, //
            _x1, _y1, _z1, _u1, _v1, //
            _x2, _y2, _z2, _u2, _v2, //
            _x3, _y3, _z3, _u3, _v3, //
            false//
        );
    }

    public FluidRenderFace(
        double _x0, double _y0, double _z0, double _u0, double _v0, //
        double _x1, double _y1, double _z1, double _u1, double _v1, //
        double _x2, double _y2, double _z2, double _u2, double _v2, //
        double _x3, double _y3, double _z3, double _u3, double _v3, //
        boolean flowing//
    ) {
        x0 = _x0;
        y0 = _y0;
        z0 = _z0;
        u0 = _u0;
        v0 = _v0;

        x1 = _x1;
        y1 = _y1;
        z1 = _z1;
        u1 = _u1;
        v1 = _v1;

        x2 = _x2;
        y2 = _y2;
        z2 = _z2;
        u2 = _u2;
        v2 = _v2;

        x3 = _x3;
        y3 = _y3;
        z3 = _z3;
        u3 = _u3;
        v3 = _v3;

        this.flowing = flowing;
    }

    public static void appendCuboid(
        double x0, double y0, double z0, double x1, double y1, double z1, double textureScale, EnumSet<Direction> faces,
        List<FluidRenderFace> to
    ) {
        appendCuboid(x0, y0, z0, x1, y1, z1, textureScale, faces, to, false);
    }

    public static void appendCuboid(
        double x0, double y0, double z0, double x1, double y1, double z1, double textureScale, EnumSet<Direction> faces,
        List<FluidRenderFace> to, boolean flowing
    ) {
        for (Direction face : faces) {
            to.add(createFlatFace(x0, y0, z0, x1, y1, z1, textureScale, face, flowing));
        }
    }

    public static FluidRenderFace createFlatFaceX(
        double x0, double y0, double z0, double x1, double y1, double z1, double textureScale, boolean positive
    ) {
        return createFlatFaceX(x0, y0, z0, x1, y1, z1, textureScale, positive, false);
    }

    public static FluidRenderFace createFlatFaceX(
        double x0, double y0, double z0, double x1, double y1, double z1, double textureScale, boolean positive,
        boolean flowing
    ) {
        final double s = textureScale;
        if (positive) {
            return new FluidRenderFace(
                x1, y0, z0, z0 * s, y0 * s, //
                x1, y1, z0, z1 * s, y0 * s, //
                x1, y1, z1, z1 * s, y1 * s, //
                x1, y0, z1, z0 * s, y1 * s, //
                flowing
            );
        } else {
            return new FluidRenderFace(
                x0, y0, z0, z0 * s, y0 * s, //
                x0, y0, z1, z0 * s, y1 * s, //
                x0, y1, z1, z1 * s, y1 * s, //
                x0, y1, z0, z1 * s, y0 * s, //
                flowing
            );
        }
    }

    public static FluidRenderFace createFlatFaceY(
        double x0, double y0, double z0, double x1, double y1, double z1, double textureScale, boolean positive
    ) {
        return createFlatFaceY(x0, y0, z0, x1, y1, z1, textureScale, positive, false);
    }

    public static FluidRenderFace createFlatFaceY(
        double x0, double y0, double z0, double x1, double y1, double z1, double textureScale, boolean positive,
        boolean flowing
    ) {
        final double s = textureScale;
        if (positive) {
            return new FluidRenderFace(
                x0, y1, z0, x0 * s, z0 * s, //
                x0, y1, z1, x0 * s, z1 * s, //
                x1, y1, z1, x1 * s, z1 * s, //
                x1, y1, z0, x1 * s, z0 * s, //
                flowing
            );
        } else {
            return new FluidRenderFace(
                x0, y0, z0, x0 * s, z0 * s, //
                x1, y0, z0, x1 * s, z0 * s, //
                x1, y0, z1, x1 * s, z1 * s, //
                x0, y0, z1, x0 * s, z1 * s, //
                flowing
            );
        }
    }

    public static FluidRenderFace createFlatFaceZ(
        double x0, double y0, double z0, double x1, double y1, double z1, double textureScale, boolean positive
    ) {
        return createFlatFaceZ(x0, y0, z0, x1, y1, z1, textureScale, positive, false);
    }

    public static FluidRenderFace createFlatFaceZ(
        double x0, double y0, double z0, double x1, double y1, double z1, double textureScale, boolean positive,
        boolean flowing
    ) {
        final double s = textureScale;
        if (positive) {
            return new FluidRenderFace(
                x0, y0, z1, x0 * s, y0 * s, //
                x1, y0, z1, x1 * s, y0 * s, //
                x1, y1, z1, x1 * s, y1 * s, //
                x0, y1, z1, x0 * s, y1 * s, //
                flowing
            );
        } else {
            return new FluidRenderFace(
                x0, y0, z0, x0 * s, y0 * s, //
                x0, y1, z0, x0 * s, y1 * s, //
                x1, y1, z0, x1 * s, y1 * s, //
                x1, y0, z0, x1 * s, y0 * s, //
                flowing
            );
        }
    }

    public static FluidRenderFace createFlatFace(
        double x0, double y0, double z0, double x1, double y1, double z1, double textureScale, Direction face
    ) {
        return createFlatFace(x0, y0, z0, x1, y1, z1, textureScale, face, false);
    }

    public static FluidRenderFace createFlatFace(
        double x0, double y0, double z0, double x1, double y1, double z1, double textureScale, Direction face,
        boolean flowing
    ) {
        switch (face) {
            case DOWN:
                return createFlatFaceY(x0, y0, z0, x1, y1, z1, textureScale, false, flowing);
            case UP:
                return createFlatFaceY(x0, y0, z0, x1, y1, z1, textureScale, true, flowing);
            case NORTH:
                return createFlatFaceZ(x0, y0, z0, x1, y1, z1, textureScale, false, flowing);
            case SOUTH:
                return createFlatFaceZ(x0, y0, z0, x1, y1, z1, textureScale, true, flowing);
            case WEST:
                return createFlatFaceX(x0, y0, z0, x1, y1, z1, textureScale, false, flowing);
            case EAST:
                return createFlatFaceX(x0, y0, z0, x1, y1, z1, textureScale, true, flowing);
            default: {
                throw new IllegalStateException("Unknown Direction " + face);
            }
        }
    }

    @Override
    public String toString() {
        return "FluidRenderFace { " + (flowing ? "flowing" : "still")//
            + "\n  " + x0 + " " + y0 + " " + z0 + " " + u0 + " " + v0//
            + "\n  " + x1 + " " + y1 + " " + z1 + " " + u1 + " " + v1//
            + "\n  " + x2 + " " + y2 + " " + z2 + " " + u2 + " " + v2//
            + "\n  " + x3 + " " + y3 + " " + z3 + " " + u3 + " " + v3//
            + "\n}"//
        ;
    }

    public float getU(Sprite still, Sprite flowing, double u) {
        if (this.flowing) {
            return flowing.getU(4 + u / 2);
        } else {
            return still.getU(u);
        }
    }

    public float getV(Sprite still, Sprite flowing, double v) {
        if (this.flowing) {
            return flowing.getV(4 + v / 2);
        } else {
            return still.getV(v);
        }
    }
}
