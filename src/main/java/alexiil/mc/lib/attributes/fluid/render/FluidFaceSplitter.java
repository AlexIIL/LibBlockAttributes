package alexiil.mc.lib.attributes.fluid.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import alexiil.mc.lib.attributes.fluid.render.FluidVolumeRenderer.ComponentRenderFaces;

/** The implementation for {@link FluidVolumeRenderer#splitFaces(List)} and
 * {@link FluidVolumeRenderer#splitFacesComponent(List)}. */
/* package-private */ final class FluidFaceSplitter {

    public static void main(String[] args) {
        double l = 0.25;
        double h = 0.75;
        FluidRenderFace face = FluidRenderFace.createFlatFaceZ(l, l, 0, h, h, 0, 16, true);
        System.out.println(face);

        System.out.println("SPLIT:");

        for (FluidRenderFace f : splitFaces(Collections.singletonList(face))) {
            System.out.println(f);
        }
    }

    static List<FluidRenderFace> splitFaces(List<FluidRenderFace> faces) {
        return splitFacesComponent(faces).split;
    }

    static ComponentRenderFaces splitFacesComponent(List<FluidRenderFace> faces) {
        List<FluidRenderFace> splitFull = new ArrayList<>();
        List<FluidRenderFace> splitTex = new ArrayList<>();

        splitFull.addAll(faces);
        splitTex.addAll(faces);

        separateFaces(true, splitFull, splitTex);
        separateFaces(false, splitFull, splitTex);

        // Separates the given faces into distinct renderable faces that all have UV bounds between 0 and 1.
        return new ComponentRenderFaces(splitFull, splitTex);
    }

    private static void separateFaces(boolean u, List<FluidRenderFace> splitFull, List<FluidRenderFace> splitTex) {
        FluidRenderFace[] inputFull = splitFull.toArray(new FluidRenderFace[0]);
        FluidRenderFace[] inputTex = splitTex.toArray(new FluidRenderFace[0]);
        splitFull.clear();
        splitTex.clear();

        for (int i = 0; i < inputFull.length; i++) {
            FluidRenderFace face = inputFull[i];
            FluidRenderFace faceT = inputTex[i];

            // OPTIMISATION!
            // double lowest = q.lowest(u);
            // double highest = q.highest(u);
            // if (!doesCrossTextureBound(lowest, highest)) {
            // splitFull.add(q.toRounded(u));
            // splitTex.add(faceT);
            // }

            new Quad(face, faceT).split(u, splitFull, splitTex);
        }
    }

    private static boolean doesCrossTextureBound(double a, double b) {
        if (a == b) {
            return false;
        }
        if (a > b) {
            double t = a;
            a = b;
            b = t;
        }
        int ra = (int) Math.floor(a);
        int rb = (int) Math.floor(b);
        if (rb == b) {
            // Upper bound
            return rb - 1 != ra;
        }
        return rb == ra;
    }

    static final class Vertex {
        double x, y, z;
        /** U, V rounded co-ords */
        double uR, vR;
        /** U, V normal (not rounded) co-ords */
        double uN, vN;
        Line l0, l1;

        Vertex() {}

        Vertex(double x, double y, double z, double uR, double vR, double uN, double vN) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.uR = uR;
            this.vR = vR;
            this.uN = uN;
            this.vN = vN;
        }

        public Vertex(Vertex from) {
            this(from.x, from.y, from.z, from.uR, from.vR, from.uN, from.vN);
        }

        void set(FluidRenderFace rounded, FluidRenderFace normal, int i) {
            final FluidRenderFace f = rounded;
            final FluidRenderFace n = normal;
            x = i == 0 ? f.x0 : i == 1 ? f.x1 : i == 2 ? f.x2 : f.x3;
            y = i == 0 ? f.y0 : i == 1 ? f.y1 : i == 2 ? f.y2 : f.y3;
            z = i == 0 ? f.z0 : i == 1 ? f.z1 : i == 2 ? f.z2 : f.z3;
            uR = (i == 0 ? f.u0 : i == 1 ? f.u1 : i == 2 ? f.u2 : f.u3) / 16;
            vR = (i == 0 ? f.v0 : i == 1 ? f.v1 : i == 2 ? f.v2 : f.v3) / 16;
            uN = (i == 0 ? n.u0 : i == 1 ? n.u1 : i == 2 ? n.u2 : n.u3) / 16;
            vN = (i == 0 ? n.v0 : i == 1 ? n.v1 : i == 2 ? n.v2 : n.v3) / 16;
        }

        double texN(boolean _u) {
            return _u ? uN : vN;
        }

        double texR(boolean _u) {
            return _u ? uR : vR;
        }

        @Override
        public String toString() {
            return x + " " + y + " " + z + " " + uN + " " + vN;
        }
    }

    static final class Line {
        final Vertex v0, v1;

        Line(Vertex v0, Vertex v1) {
            this.v0 = v0;
            this.v1 = v1;
            v0.l1 = this;
            v1.l0 = this;
        }
    }

    static final class Quad {
        final Vertex v0, v1, v2, v3;
        final Line l0, l1, l2, l3;

        Quad() {
            this(new Vertex(), new Vertex(), new Vertex(), new Vertex());
        }

        Quad(FluidRenderFace rounded, FluidRenderFace normal) {
            this();
            set(rounded, normal);
        }

        public Quad(Vertex v0, Vertex v1, Vertex v2, Vertex v3) {
            this.v0 = v0.l0 == null && v0.l1 == null ? v0 : new Vertex(v0);
            this.v1 = v1.l0 == null && v1.l1 == null ? v1 : new Vertex(v1);
            this.v2 = v2.l0 == null && v2.l1 == null ? v2 : new Vertex(v2);
            this.v3 = v3.l0 == null && v3.l1 == null ? v3 : new Vertex(v3);

            l0 = new Line(v0, v1);
            l1 = new Line(v1, v2);
            l2 = new Line(v2, v3);
            l3 = new Line(v3, v0);
        }

        void set(FluidRenderFace rounded, FluidRenderFace normal) {
            v0.set(rounded, normal, 0);
            v1.set(rounded, normal, 1);
            v2.set(rounded, normal, 2);
            v3.set(rounded, normal, 3);
        }

        void split(boolean u, List<FluidRenderFace> splitFull, List<FluidRenderFace> splitTex) {
            // double min = lowest(u);
            // double max = highest(u);
            // if (!doesCrossTextureBound(min, max)) {
            // splitFull.add(this.toRounded(u));
            // splitTex.add(nonSplit);
            // return;
            // }

            Vertex lowestVertex = v0;
            Vertex highestVertex = v0;

            Vertex[] searchVertices = new Vertex[] { v1, v2, v3 };
            double[] searchPoints = new double[] { v1.texN(u), v2.texN(u), v3.texN(u) };
            for (int i = 0; i < searchVertices.length; i++) {
                Vertex v = searchVertices[i];
                double point = searchPoints[i];
                if (point < lowestVertex.texN(u)) {
                    lowestVertex = v;
                }
                if (point >= highestVertex.texN(u)) {
                    highestVertex = v;
                }
            }

            if (lowestVertex == highestVertex) {
                splitFull.add(this.toRounded(u));
                splitTex.add(this.toFace());
                return;
            }

            List<Vertex> lowToHigh0 = new ArrayList<>(4);
            List<Vertex> lowToHigh1 = new ArrayList<>(4);

            lowToHigh0.add(lowestVertex);
            lowToHigh1.add(lowestVertex);

            Vertex h = lowestVertex;
            do {
                h = h.l1.v1;
                if (h == lowestVertex) {
                    throw new IllegalStateException("h == lowestVertex");
                }
                lowToHigh0.add(h);
            } while (h != highestVertex);

            h = lowestVertex;
            do {
                h = h.l0.v0;
                if (h == lowestVertex) {
                    throw new IllegalStateException("h == lowestVertex");
                }

                lowToHigh1.add(h);
            } while (h != highestVertex);

            List<BucketedVertexList> separated0 = separateVertices(u, lowToHigh0);
            List<BucketedVertexList> separated1 = separateVertices(u, lowToHigh1);
            assert separated0.size() == separated1.size();

            // Now we've got two lists of lists of the texture co-ords, we can create quads from both
            for (int i = 0; i < separated0.size(); i++) {
                BucketedVertexList bucket0 = separated0.get(i);
                BucketedVertexList bucket1 = separated1.get(i);
                assert bucket0.texValue == bucket1.texValue;
                List<Vertex> list0 = bucket0.list;
                List<Vertex> list1 = bucket1.list;

                while (true) {
                    final int size0 = list0.size();
                    final int size1 = list1.size();
                    if (size0 > 1 && size1 > 1) {
                        Vertex vl0 = list0.remove(0);
                        Vertex vl1 = list0.get(0);
                        Vertex vr0 = list1.remove(0);
                        Vertex vr1 = list1.get(0);
                        Quad quad = new Quad(vl0, vl1, vr1, vr0);
                        splitFull.add(quad.toRounded(u, bucket0.texValue));
                        splitTex.add(quad.toFace());
                    } else if (size0 > 2 && size1 > 0) {
                        Vertex vl0 = list0.remove(0);
                        Vertex vl1 = list0.remove(0);
                        Vertex vl2 = list0.get(0);
                        Vertex vr0 = list1.get(0);
                        Quad quad = new Quad(vl0, vl1, vl2, vr0);
                        splitFull.add(quad.toRounded(u, bucket0.texValue));
                        splitTex.add(quad.toFace());
                    } else if (size0 > 0 && size1 > 2) {
                        Vertex vl0 = list0.get(0);
                        Vertex vr0 = list1.remove(0);
                        Vertex vr1 = list1.remove(0);
                        Vertex vr2 = list1.get(0);
                        Quad quad = new Quad(vl0, vr2, vr1, vr0);
                        splitFull.add(quad.toRounded(u, bucket0.texValue));
                        splitTex.add(quad.toFace());
                    } else if (size0 > 1 && size1 > 0) {
                        Vertex vl0 = list0.remove(0);
                        Vertex vl1 = list0.get(0);
                        Vertex vr0 = list1.get(0);
                        Quad quad = new Quad(vl0, vl1, vr0, vr0);
                        splitFull.add(quad.toRounded(u, bucket0.texValue));
                        splitTex.add(quad.toFace());
                    } else if (size0 > 0 && size1 > 1) {
                        Vertex vl0 = list0.get(0);
                        Vertex vr0 = list1.remove(0);
                        Vertex vr1 = list1.get(0);
                        Quad quad = new Quad(vl0, vr1, vr0, vl0);
                        splitFull.add(quad.toRounded(u, bucket0.texValue));
                        splitTex.add(quad.toFace());
                    } else if (size0 > 2) {
                        Vertex vl0 = list0.remove(0);
                        Vertex vl1 = list0.remove(0);
                        Vertex vl2 = list0.get(0);
                        Quad quad = new Quad(vl0, vl1, vl2, vl0);
                        splitFull.add(quad.toRounded(u, bucket0.texValue));
                        splitTex.add(quad.toFace());
                    } else if (size1 > 2) {
                        Vertex vr0 = list1.remove(0);
                        Vertex vr1 = list1.remove(0);
                        Vertex vr2 = list1.get(0);
                        Quad quad = new Quad(vr2, vr1, vr0, vr2);
                        splitFull.add(quad.toRounded(u, bucket0.texValue));
                        splitTex.add(quad.toFace());
                    } else if (size0 + size1 > 2) {
                        throw new IllegalStateException("Unhandled size: [ " + size0 + ", " + size1 + " ]");
                    } else {
                        break;
                    }
                }
            }
        }

        private static List<BucketedVertexList> separateVertices(boolean u, List<Vertex> in) {
            List<BucketedVertexList> out = new ArrayList<>();
            BucketedVertexList prevList = null;
            for (Vertex v : in) {
                double tex_d = v.texN(u);
                int tex_i = (int) Math.floor(tex_d);
                if (prevList == null) /* First vertex */ {
                    if (tex_d == tex_i) /* Right on the bounds */ {
                        // Add one for the lower ones
                        out.add(prevList = new BucketedVertexList(tex_i - 1));
                        prevList.list.add(v);
                        // Add one for the higher ones
                        out.add(prevList = new BucketedVertexList(tex_i));
                        prevList.list.add(v);
                    } else /* Somewhere inside the bounds */ {
                        out.add(prevList = new BucketedVertexList(tex_i));
                        prevList.list.add(v);
                    }
                } else {

                    if (prevList.texValue == tex_i) /* We are in the same bound */ {
                        prevList.list.add(v);
                    } else /* We are on a different bound */ {

                        // Generate vertices between the previous one and this one

                        Vertex vPrevious = prevList.list.get(prevList.list.size() - 1);

                        // Generate a single extra vertex to complete the prevList
                        prevList.list.add(interp(u, vPrevious, v, prevList.texValue + 1));

                        for (int i = prevList.texValue + 1; i < tex_i; i++) {
                            // Generate two vertices for every other texture index
                            out.add(prevList = new BucketedVertexList(i));
                            prevList.list.add(interp(u, vPrevious, v, i));
                            prevList.list.add(interp(u, vPrevious, v, i + 1));
                        }

                        if (tex_d == tex_i) /* Right on the bounds */ {
                            // Add a single one for the higher ones
                            out.add(prevList = new BucketedVertexList(tex_i));
                            prevList.list.add(v);
                        } else /* Somewhere on a higher bound */ {
                            // Generate two vertices for every other texture index
                            out.add(prevList = new BucketedVertexList(tex_i));
                            prevList.list.add(interp(u, vPrevious, v, tex_i));
                            prevList.list.add(v);
                        }
                    }
                }
            }
            return out;
        }

        private static Vertex interp(boolean u, Vertex v0, Vertex v1, int newTextureCoord) {
            double t0 = v0.texN(u);
            double t1 = v1.texN(u);
            if (t0 == newTextureCoord) {
                return v0;
            } else if (t1 == newTextureCoord) {
                return v1;
            }
            double interpValue = (newTextureCoord - t0) / (t1 - t0);
            return new Vertex(
                //
                interp(v0.x, v1.x, interpValue), //
                interp(v0.y, v1.y, interpValue), //
                interp(v0.z, v1.z, interpValue), //
                interp(v0.uR, v1.uR, interpValue), //
                interp(v0.vR, v1.vR, interpValue), //
                interp(v0.uN, v1.uN, interpValue), //
                interp(v0.vN, v1.vN, interpValue)//
            );
        }

        private static double interp(double a, double b, double interpValue) {
            return a * (1 - interpValue) + b * interpValue;
        }

        static class BucketedVertexList {
            final int texValue;
            final List<Vertex> list = new ArrayList<>();

            public BucketedVertexList(int texValue) {
                this.texValue = texValue;
            }
        }

        FluidRenderFace toRounded(boolean u) {
            return toRounded(u, (int) Math.floor(lowestR(u)));
        }

        FluidRenderFace toRounded(boolean u, int min) {
            double _u0 = round(u, min, v0.uR) * 16;
            double _u1 = round(u, min, v1.uR) * 16;
            double _u2 = round(u, min, v2.uR) * 16;
            double _u3 = round(u, min, v3.uR) * 16;

            double _v0 = round(!u, min, v0.vR) * 16;
            double _v1 = round(!u, min, v1.vR) * 16;
            double _v2 = round(!u, min, v2.vR) * 16;
            double _v3 = round(!u, min, v3.vR) * 16;

            return new FluidRenderFace(
                v0.x, v0.y, v0.z, _u0, _v0, //
                v1.x, v1.y, v1.z, _u1, _v1, //
                v2.x, v2.y, v2.z, _u2, _v2, //
                v3.x, v3.y, v3.z, _u3, _v3//
            );
        }

        FluidRenderFace toFace() {
            return new FluidRenderFace(
                v0.x, v0.y, v0.z, v0.uN * 16, v0.vN * 16, //
                v1.x, v1.y, v1.z, v1.uN * 16, v1.vN * 16, //
                v2.x, v2.y, v2.z, v2.uN * 16, v2.vN * 16, //
                v3.x, v3.y, v3.z, v3.uN * 16, v3.vN * 16//
            );
        }

        private double lowestR(boolean u) {
            return Math.min(Math.min(v0.texR(u), v1.texR(u)), Math.min(v2.texR(u), v3.texR(u)));
        }

        private static double round(boolean should, int min, double value) {
            return should ? value - min : value;
        }

        @Override
        public String toString() {
            return "Quad {\n  " + v0 + "\n  " + v1 + "\n  " + v2 + "\n  " + v3 + "\n}";
        }
    }
}
