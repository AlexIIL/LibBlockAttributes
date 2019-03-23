package alexiil.mc.lib.attributes;

import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class AttributeCombinable<T> extends AttributeDefaulted<T> {

    public final IAttributeCombiner<T> combiner;

    AttributeCombinable(Class<T> clazz, @Nonnull T defaultValue, IAttributeCombiner<T> combiner) {
        super(clazz, defaultValue);
        this.combiner = combiner;
    }

    AttributeCombinable(Class<T> clazz, @Nonnull T defaultValue, IAttributeCombiner<T> combiner,
        IAttributeCustomAdder<T> customAdder) {
        super(clazz, defaultValue, customAdder);
        this.combiner = combiner;
    }

    /** @param searchParam The search parameters to use for accessing instances. Many blocks only offer attributes from
     *            a certain direction, which should be provided as a {@link SearchParamDirectional}. (However there is
     *            also {@link SearchParamInVoxel} and {@link SearchParamDirectionalVoxel} if you need to filter only
     *            attribute instances that are in a certain subspace of the block).Alternatively you can provide
     *            {@link SearchParameter#NONE} if you don't have a direction or voxel space to search in.
     * @return Either the {@link AttributeDefaulted #defaultValue defaultValue}, a single instance, or a
     *         {@link #combiner combined} instance depending on how many attribute instances could be found. */
    @Nonnull
    public final T get(World world, BlockPos pos, SearchParameter searchParam) {
        AttributeList<T> list = getAll(world, pos, searchParam);
        switch (list.list.size()) {
            case 0: {
                return defaultValue;
            }
            case 1: {
                return list.get(0);
            }
            default: {
                return combiner.combine(list.list);
            }
        }
    }
}
