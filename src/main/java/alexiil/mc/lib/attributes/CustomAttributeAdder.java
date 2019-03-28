package alexiil.mc.lib.attributes;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@FunctionalInterface
public interface CustomAttributeAdder<T> {
    /* Note that we do have the type parameter (unlike IAttributeBlock) because instances are registered to a specific
     * Attribute so it's actually useful for implementors. */

    /** Adds every attribute instance to the given list that the block itself cannot be expected to add support for. */
    void addAll(World world, BlockPos pos, BlockState state, AttributeList<T> to);
}
