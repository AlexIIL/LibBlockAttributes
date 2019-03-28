package alexiil.mc.lib.attributes;

import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DefaultedAttribute<T> extends Attribute<T> {

    @Nonnull
    public final T defaultValue;

    public DefaultedAttribute(Class<T> clazz, @Nonnull T defaultValue) {
        super(clazz);
        this.defaultValue = defaultValue;
    }

    public DefaultedAttribute(Class<T> clazz, @Nonnull T defaultValue, CustomAttributeAdder<T> customAdder) {
        super(clazz, customAdder);
        this.defaultValue = defaultValue;
    }

    /** @return The first attribute instance (as obtained by {@link #getAll(World, BlockPos, SearchOption)}), or the
     *         {@link #defaultValue} if none were found. */
    @Nonnull
    public final T getFirst(World world, BlockPos pos) {
        return getFirst(world, pos, null);
    }

    /** @param searchParam The search parameters to use for accessing instances. Many blocks only offer attributes from
     *            a certain direction, which should be provided as a {@link SearchOptionDirectional}. A full list of
     *            possible {@link SearchOption}'s is in {@link SearchOptions}.
     * @return The first attribute instance (as obtained by {@link #getAll(World, BlockPos, SearchOption)}), or the
     *         {@link #defaultValue} if the search didn't find any attribute instances at the specified position. */
    @Nonnull
    public final T getFirst(World world, BlockPos pos, SearchOption<? super T> searchParam) {
        AttributeList<T> list = getAll(world, pos, searchParam);
        if (list.list.isEmpty()) {
            return defaultValue;
        } else {
            return list.get(0);
        }
    }
}
