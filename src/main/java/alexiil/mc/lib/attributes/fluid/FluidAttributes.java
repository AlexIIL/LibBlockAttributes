package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.Attributes;
import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.fluid.compat.silk.SilkFluidCompat;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFluidExtractable;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFluidInsertable;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFluidInvStats;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFluidExtractable;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFluidInvStats;
import alexiil.mc.lib.attributes.fluid.impl.RejectingFluidInsertable;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

public enum FluidAttributes {
    ;

    public static final CombinableAttribute<FixedFluidInvView> FIXED_INV_VIEW;
    public static final CombinableAttribute<FixedFluidInv> FIXED_INV;
    public static final CombinableAttribute<FluidInvStats> INV_STATS;
    public static final CombinableAttribute<FluidInsertable> INSERTABLE;
    public static final CombinableAttribute<FluidExtractable> EXTRACTABLE;

    static {
        FIXED_INV_VIEW = Attributes.createCombinable(FixedFluidInvView.class, EmptyFixedFluidInv.INSTANCE,
            CombinedFixedFluidInvView::new);
        FIXED_INV =
            Attributes.createCombinable(FixedFluidInv.class, EmptyFixedFluidInv.INSTANCE, CombinedFixedFluidInv::new);

        // For some reason the java compiler can't work out what <T> should be for these three
        // So instead we create a lambda, which somehow gives it enough space to work out what it is.
        // (and yet eclipse had no problems with it :/ )
        INV_STATS = Attributes.createCombinable(FluidInvStats.class, EmptyFluidInvStats.INSTANCE,
            list -> new CombinedFluidInvStats(list));
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
