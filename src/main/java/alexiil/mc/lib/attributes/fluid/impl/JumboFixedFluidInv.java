/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.fluid.FluidExtractable;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInvView;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/** A {@link SimpleFixedFluidInv} with a few optimisations to make the {@link FluidInsertable},
 * {@link FluidExtractable}, and {@link GroupedFluidInvView} implementations much faster than a simple inventory search
 * for larger inventories. */
public class JumboFixedFluidInv extends SimpleFixedFluidInv {

    /** @deprecated Replaced by {@link #JumboFixedFluidInv(int, FluidAmount)}. */
    @Deprecated
    public JumboFixedFluidInv(int invSize, int tankCapacity) {
        super(invSize, tankCapacity);
    }

    public JumboFixedFluidInv(int invSize, FluidAmount tankCapacity) {
        super(invSize, tankCapacity);
    }

    // TODO: Optimisations!
}
