package alexiil.mc.lib.attributes;

import java.util.ArrayList;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class Attribute<T> {
    public final Class<T> clazz;

    private final ArrayList<CustomAttributeAdder<T>> customAdders = new ArrayList<>();

    protected Attribute(Class<T> clazz) {
        this.clazz = clazz;
    }

    protected Attribute(Class<T> clazz, CustomAttributeAdder<T> customAdder) {
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

    /** Appends a single {@link CustomAttributeAdder} to the list of custom adders. These are called only for blocks
     * that don't implement {@link AttributeProvider}. */
    public final void appendCustomAdder(CustomAttributeAdder<T> customAdder) {
        customAdders.add(customAdder);
    }

    final void addAll(World world, BlockPos pos, AttributeList<T> list) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof AttributeProvider) {
            AttributeProvider attributeBlock = (AttributeProvider) block;
            attributeBlock.addAllAttributes(world, pos, state, list);
        } else {
            for (CustomAttributeAdder<T> custom : customAdders) {
                custom.addAll(world, pos, state, list);
            }
        }
    }

    /** @return A complete {@link AttributeList} of every attribute instance that can be found. */
    public final AttributeList<T> getAll(World world, BlockPos pos) {
        return getAll(world, pos, null);
    }

    /** @param searchParam The search parameters to use for accessing instances. Many blocks only offer attributes from
     *            a certain direction, which should be provided as a {@link SearchOptionDirectional}. A full list of
     *            possible {@link SearchOption}'s is in {@link SearchOptions}.
     * @return A complete {@link AttributeList} of every attribute instance that can be found with the supplied search
     *         parameters. */
    public final AttributeList<T> getAll(World world, BlockPos pos, SearchOption<? super T> searchParam) {
        VoxelShape blockShape = world.getBlockState(pos).getOutlineShape(world, pos);
        AttributeList<T> list = new AttributeList<>(this, searchParam, blockShape);
        addAll(world, pos, list);
        list.finishAdding();
        return list;
    }

    /** Shorter method call for the common case of:</br>
     * BlockEntity be = ...;</br>
     * Direction dir = ...;</br>
     * Attribute&lt;T&gt; attr = ...;</br>
     * AttributeList&lt;T&gt; list = attr.{@link #getAll(World, BlockPos, SearchOption) getAll}(be.getWorld(),
     * be.getPos().offset(dir), {@link SearchOptions#inDirection(Direction) SearchOptions.inDirection}(dir)); </br>
     */
    public final AttributeList<T> getAllFromNeighbour(BlockEntity be, Direction dir) {
        return getAll(be.getWorld(), be.getPos().offset(dir), SearchOptions.inDirection(dir));
    }

    /** @return The first attribute instance (as obtained by {@link #getAll(World, BlockPos)}), or null if this didn't
     *         find any instances. */
    @Nullable
    public final T getFirstOrNull(World world, BlockPos pos) {
        return getFirstOrNull(world, pos, null);
    }

    /** @param searchParam The search parameters to use for accessing instances. Many blocks only offer attributes from
     *            a certain direction, which should be provided as a {@link SearchOptionDirectional}. A full list of
     *            possible {@link SearchOption}'s is in {@link SearchOptions}.
     * @return The first attribute instance (as obtained by {@link #getAll(World, BlockPos, SearchOption)}), or null if
     *         the search didn't find any attribute instances at the specified position. */
    @Nullable
    public final T getFirstOrNull(World world, BlockPos pos, @Nullable SearchOption<? super T> searchParam) {
        return getAll(world, pos, searchParam).getFirstOrNull();
    }

    /** Shorter method call for the common case of:</br>
     * BlockEntity be = ...;</br>
     * Direction dir = ...;</br>
     * Attribute&lt;T&gt; attr = ...;</br>
     * AttributeList&lt;T&gt; list = attr.{@link #getFirstOrNull(World, BlockPos, SearchOption) getAll}(be.getWorld(),
     * be.getPos().offset(dir), {@link SearchOptions#inDirection(Direction) SearchOptions.inDirection}(dir)); </br>
     */
    @Nullable
    public final T getFirstOrNullFromNeighbour(BlockEntity be, Direction dir) {
        return getFirstOrNull(be.getWorld(), be.getPos().offset(dir), SearchOptions.inDirection(dir));
    }
}
