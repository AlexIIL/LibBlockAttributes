package alexiil.mc.lib.attributes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A {@link Block} that contains attributes. */
public interface IAttributeBlock {

    /** Adds every instance of the given attribute in this block to the resulting list. Note that this must never add
     * wrapped versions of an attribute to the resulting list as the caller is responsible for doing that instead. */
    <T> void addAllAttributes(World world, BlockPos pos, BlockState state, AttributeList<T> to);
}
