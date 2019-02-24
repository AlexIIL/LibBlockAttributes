package alexiil.mc.lib.attributes;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** An alternative to {@link IAttributeBlock} where all of it's attributes are held by different
 * {@link IAttributeProvider} objects instead of being embedded directly. */
public interface IDelegatingAttributeBlock {
    Iterable<IAttributeProvider> getAttributeProviders(World world, BlockPos pos, BlockState state);
}
