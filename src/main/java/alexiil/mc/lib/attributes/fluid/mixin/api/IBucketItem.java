package alexiil.mc.lib.attributes.fluid.mixin.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** General interface for any bucket-like {@link Item} to be exposed by default as a {@link GroupedFluidInv} in
 * {@link FluidAttributes}. (Although unlike the vanilla bucket it can store fluids in NBT or be stackable). */
public interface IBucketItem {

    boolean libblockattributes__shouldExposeFluid();

    FluidKey libblockattributes__getFluid(ItemStack stack);

    /** @return Either the {@link ItemStack} filled with the given fluid, or {@link ItemStack#EMPTY} if the given fluid
     *         is unsupported. */
    ItemStack libblockattributes__withFluid(FluidKey fluid);

    /** Similar to {@link #libblockattributes__withFluid(FluidKey)}, but can return an empty stack if this bucket should
     * be lost when drained by a machine that doesn't have special tooling/mechanisms for keeping the container around.
     * (Unlike withFluid this is assumed to always be a valid result). */
    default ItemStack libblockattributes__drainedOfFluid(ItemStack stack) {
        // Most containers use the same logic for both
        return libblockattributes__withFluid(FluidKeys.EMPTY);
    }

    /** @return A value such as {@link FluidVolume#BUCKET}. */
    int libblockattributes__getFluidVolumeAmount();
}
