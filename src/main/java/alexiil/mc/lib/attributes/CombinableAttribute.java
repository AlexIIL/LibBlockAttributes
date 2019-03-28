package alexiil.mc.lib.attributes;

import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class CombinableAttribute<T> extends DefaultedAttribute<T> {

    public final AttributeCombiner<T> combiner;

    CombinableAttribute(Class<T> clazz, @Nonnull T defaultValue, AttributeCombiner<T> combiner) {
        super(clazz, defaultValue);
        this.combiner = combiner;
    }

    CombinableAttribute(Class<T> clazz, @Nonnull T defaultValue, AttributeCombiner<T> combiner,
        CustomAttributeAdder<T> customAdder) {
        super(clazz, defaultValue, customAdder);
        this.combiner = combiner;
    }

    /** @return Either the {@link DefaultedAttribute #defaultValue defaultValue}, a single instance, or a
     *         {@link #combiner combined} instance depending on how many attribute instances could be found. */
    @Nonnull
    public final T get(World world, BlockPos pos) {
        return get(world, pos, null);
    }

    /** @param searchParam The search parameters to use for accessing instances. Many blocks only offer attributes from
     *            a certain direction, which should be provided as a {@link SearchOptionDirectional}. A full list of
     *            possible {@link SearchOption}'s is in {@link SearchOptions}.
     * @return Either the {@link DefaultedAttribute #defaultValue defaultValue}, a single instance, or a
     *         {@link #combiner combined} instance depending on how many attribute instances could be found. */
    @Nonnull
    public final T get(World world, BlockPos pos, SearchOption<? super T> searchParam) {
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
