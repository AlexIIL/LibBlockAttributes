package alexiil.mc.lib.attributes;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class Attribute<T> {
    public final Class<T> clazz;

    @Nullable
    final IAttributeCustomAdder<T> customAdder;

    protected Attribute(Class<T> clazz) {
        this(clazz, null);
    }

    protected Attribute(Class<T> clazz, IAttributeCustomAdder<T> customAdder) {
        this.clazz = clazz;
        this.customAdder = customAdder;
    }

    /** Checks to see if the given object is an {@link Class#isInstance(Object)} of this attribute. */
    public final boolean isInstance(Object obj) {
        return clazz.isInstance(obj);
    }

    /** {@link Class#cast(Object) Casts} The given object to type of this attribute. */
    public final T cast(Object obj) {
        return clazz.cast(obj);
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    final void addAll(World world, BlockPos pos, AttributeList<T> list) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof IAttributeBlock) {
            IAttributeBlock attributeBlock = (IAttributeBlock) block;
            attributeBlock.addAllAttributes(world, pos, state, list);
        } else if (block instanceof IDelegatingAttributeBlock) {
            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                provider.addAllAttributes(list);
            }
        } else if (customAdder != null) {
            customAdder.addAll(world, pos, state, list);
        }
    }

    public final AttributeList<T> getAll(World world, BlockPos pos, SearchParameter searchParam) {
        VoxelShape blockShape = world.getBlockState(pos).getOutlineShape(world, pos);
        AttributeList<T> list = new AttributeList<>(this, searchParam, blockShape);
        addAll(world, pos, list);
        list.finishAdding();
        return list;
    }

    @Nullable
    public final T getFirstOrNull(World world, BlockPos pos, SearchParameter searchParam) {
        AttributeList<T> list = getAll(world, pos, searchParam);
        if (list.list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }
}
