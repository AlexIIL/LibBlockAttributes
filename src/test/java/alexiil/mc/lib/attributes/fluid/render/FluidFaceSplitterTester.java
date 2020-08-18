/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.render;

import java.util.Collections;

import org.junit.Test;

public class FluidFaceSplitterTester {

    @Test
    public void printFace() {
        double l = 0.25;
        double h = 0.75;
        FluidRenderFace face = FluidRenderFace.createFlatFaceZ(l, l, 0, h, h, 0, 1, true);
        System.out.println(face);

        System.out.println("SPLIT:");

        for (FluidRenderFace f : FluidFaceSplitter.splitFaces(Collections.singletonList(face))) {
            System.out.println(f);
        }
    }
}
