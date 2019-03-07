package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public interface IFluidInvTankChangeListener {

    /** @param inv The inventory that changed
     * @param tank The tank that changed
     * @param previous The previous {@link FluidVolume}.
     * @param current The new {@link FluidVolume} */
    void onChange(IFixedFluidInvView inv, int tank, FluidVolume previous, FluidVolume current);
}
