package alexiil.mc.lib.attributes;

import javax.annotation.Nonnull;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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
        return getAll(world, pos, searchParam).getFirst(this);
    }

    /** Shorter method call for the common case of:</br>
     * BlockEntity be = ...;</br>
     * Direction dir = ...;</br>
     * Attribute&lt;T&gt; attr = ...;</br>
     * AttributeList&lt;T&gt; list = attr.{@link #getFirst(World, BlockPos, SearchOption) getAll}(be.getWorld(),
     * be.getPos().offset(dir), {@link SearchOptions#inDirection(Direction) SearchOptions.inDirection}(dir)); </br>
     */
    @Nonnull
    public final T getFirstFromNeighbour(BlockEntity be, Direction dir) {
        return getFirst(be.getWorld(), be.getPos().offset(dir), SearchOptions.inDirection(dir));
    }
}
