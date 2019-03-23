package alexiil.mc.lib.attributes.fluid;

import io.github.prospector.silk.fluid.FluidContainer;
import io.github.prospector.silk.fluid.FluidContainerProvider;

import alexiil.mc.lib.attributes.AttributeCombinable;
import alexiil.mc.lib.attributes.Attributes;
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

    public static final AttributeCombinable<IFixedFluidInvView> FIXED_INV_VIEW;
    public static final AttributeCombinable<IFixedFluidInv> FIXED_INV;
    public static final AttributeCombinable<IFluidInvStats> INV_STATS;
    public static final AttributeCombinable<IFluidInsertable> INSERTABLE;
    public static final AttributeCombinable<IFluidExtractable> EXTRACTABLE;

    static {
        FIXED_INV_VIEW = Attributes.createCombinable(IFixedFluidInvView.class, EmptyFixedFluidInv.INSTANCE,
            CombinedFixedFluidInvView::new);
        FIXED_INV =
            Attributes.createCombinable(IFixedFluidInv.class, EmptyFixedFluidInv.INSTANCE, CombinedFixedFluidInv::new);

        // For some reason the java compiler can't work out what <T> should be for these three
        // So instead we create a lambda, which somehow gives it enough space to work out what it is.
        // (and yet eclipse had no problems with it :/ )
        INV_STATS = Attributes.createCombinable(IFluidInvStats.class, EmptyFluidInvStats.INSTANCE,
            list -> new CombinedFluidInvStats(list));
        INSERTABLE = Attributes.createCombinable(IFluidInsertable.class, RejectingFluidInsertable.NULL,
            list -> new CombinedFluidInsertable(list));
        EXTRACTABLE = Attributes.createCombinable(IFluidExtractable.class, EmptyFluidExtractable.NULL,
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
