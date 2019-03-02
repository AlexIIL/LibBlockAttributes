package alexiil.mc.lib.attributes;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IAttributeCustomAdder<T> {

    void addAll(World world, BlockPos pos, BlockState state, AttributeList<T> to);
}
