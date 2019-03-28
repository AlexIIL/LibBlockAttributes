package alexiil.mc.lib.attributes;

import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class AttributeDefaulted<T> extends Attribute<T> {

    @Nonnull
    public final T defaultValue;

    public AttributeDefaulted(Class<T> clazz, @Nonnull T defaultValue) {
        super(clazz);
        this.defaultValue = defaultValue;
    }

    public AttributeDefaulted(Class<T> clazz, @Nonnull T defaultValue, IAttributeCustomAdder<T> customAdder) {
        super(clazz, customAdder);
        this.defaultValue = defaultValue;
    }

    /** @param searchParam The search parameters to use for accessing instances. Many blocks only offer attributes from
     *            a certain direction, which should be provided as a {@link SearchParamDirectional}. (However there is
     *            also {@link SearchParamInVoxel} and {@link SearchParamDirectionalVoxel} if you need to filter only
     *            attribute instances that are in a certain subspace of the block).Alternatively you can provide
     *            {@link SearchParameter#NONE} if you don't have a direction or voxel space to search in.
     * @return The first attribute instance (as obtained by {@link #getAll(World, BlockPos, SearchParameter)}), or the
     *         {@link #defaultValue} if the search didn't find any attribute instances at the specified position. */
    @Nonnull
    public final T getFirst(World world, BlockPos pos, SearchParameter searchParam) {
        VoxelShape blockShape = world.getBlockState(pos).getOutlineShape(world, pos);
        AttributeList<T> list = new AttributeList<>(this, searchParam, blockShape);
        addAll(world, pos, list);
        if (list.list.isEmpty()) {
            return defaultValue;
        } else {
            return list.get(0);
        }
    }
}
