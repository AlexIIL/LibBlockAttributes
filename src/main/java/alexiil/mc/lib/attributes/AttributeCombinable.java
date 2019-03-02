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

    /** @return Either the default value, an instance, or the {@link #combiner combined} instance. */
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
