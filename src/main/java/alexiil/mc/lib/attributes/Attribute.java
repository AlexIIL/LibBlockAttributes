package alexiil.mc.lib.attributes;

import java.util.ArrayList;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class Attribute<T> {
    public final Class<T> clazz;

    private final ArrayList<IAttributeCustomAdder<T>> customAdders = new ArrayList<>();

    protected Attribute(Class<T> clazz) {
        this.clazz = clazz;
    }

    protected Attribute(Class<T> clazz, IAttributeCustomAdder<T> customAdder) {
        this.clazz = clazz;
        customAdders.add(customAdder);
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

    /** Appends a single {@link IAttributeCustomAdder} to the list of custom adders. These are called in order for
     * blocks that don't implement {@link IAttributeBlock}. */
    public final void appendCustomAdder(IAttributeCustomAdder<T> customAdder) {
        customAdders.add(customAdder);
    }

    final void addAll(World world, BlockPos pos, AttributeList<T> list) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof IAttributeBlock) {
            IAttributeBlock attributeBlock = (IAttributeBlock) block;
            attributeBlock.addAllAttributes(world, pos, state, list);
        } else {
            for (IAttributeCustomAdder<T> custom : customAdders) {
                custom.addAll(world, pos, state, list);
            }
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
