/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.Attributes;
import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.fluid.compat.silk.SilkFluidCompat;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFluidExtractable;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFluidInsertable;
import alexiil.mc.lib.attributes.fluid.impl.CombinedGroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.CombinedGroupedFluidInvView;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFluidExtractable;
import alexiil.mc.lib.attributes.fluid.impl.EmptyGroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.RejectingFluidInsertable;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

public final class FluidAttributes {
    private FluidAttributes() {}

    public static final CombinableAttribute<FixedFluidInvView> FIXED_INV_VIEW;
    public static final CombinableAttribute<FixedFluidInv> FIXED_INV;
    public static final CombinableAttribute<GroupedFluidInvView> GROUPED_INV_VIEW;
    public static final CombinableAttribute<GroupedFluidInv> GROUPED_INV;
    public static final CombinableAttribute<FluidInsertable> INSERTABLE;
    public static final CombinableAttribute<FluidExtractable> EXTRACTABLE;

    static {
        FIXED_INV_VIEW = Attributes.createCombinable(FixedFluidInvView.class, EmptyFixedFluidInv.INSTANCE,
            CombinedFixedFluidInvView::new);
        FIXED_INV =
            Attributes.createCombinable(FixedFluidInv.class, EmptyFixedFluidInv.INSTANCE, CombinedFixedFluidInv::new);
        GROUPED_INV_VIEW = Attributes.createCombinable(GroupedFluidInvView.class, EmptyGroupedFluidInv.INSTANCE,
            list -> new CombinedGroupedFluidInvView(list));
        GROUPED_INV = Attributes.createCombinable(GroupedFluidInv.class, EmptyGroupedFluidInv.INSTANCE,
            list -> new CombinedGroupedFluidInv(list));
        INSERTABLE = Attributes.createCombinable(FluidInsertable.class, RejectingFluidInsertable.NULL,
            list -> new CombinedFluidInsertable(list));
        EXTRACTABLE = Attributes.createCombinable(FluidExtractable.class, EmptyFluidExtractable.NULL,
            list -> new CombinedFluidExtractable(list));

        try {
            Class.forName("io.github.prospector.silk.fluid.FluidContainerProvider");
            LibBlockAttributes.LOGGER.info("Silk found, loading compatibility for fluids.");
            SilkFluidCompat.load();
        } catch (ClassNotFoundException cnfe) {
            LibBlockAttributes.LOGGER.info("Silk not found, not loading compatibility for fluids.");
        }
    }
}
