package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

@FunctionalInterface
public interface FluidInvAmountChangeListener {

    /** @param inv The inventory that changed
     * @param fluid The {@link FluidKey} whose amount changed.
     * @param previous The previous {@link FluidVolume}.
     * @param current The new {@link FluidVolume}. The {@link FluidVolume#getFluidKey()} will either be the empty fluid
     *            key, or equal to the passed {@link FluidKey} . */
    void onChange(GroupedFluidInvView inv, FluidKey fluid, int previous, int current);
}
