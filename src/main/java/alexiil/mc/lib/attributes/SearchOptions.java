package alexiil.mc.lib.attributes;

import java.util.function.Predicate;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

/** Various methods for creating {@link SearchOption} instances. */
public final class SearchOptions {

    private SearchOptions() {}

    /** A {@link SearchOption} that has no constraints, and will get every attribute instance that is obtainable without
     * a direction. Note that you should never need to use this directly as every attribute getter method (like
     * {@link Attribute#getAll(World, BlockPos, SearchOption)}) will use this if null is passed. */
    public static final SearchOption<Object> ALL = SearchOption.ALL;

    /** @return A {@link SearchOption} that will only match attribute instances that pass the given {@link Predicate}
     *         {@link Predicate#test(Object) test}. */
    public static <T> SearchOption<T> matching(Predicate<T> matcher) {
        return new SearchOption<>(matcher);
    }

    /** @param direction The direction to search in - in other words the direction from the block that is searching to
     *            the block that is being searched.
     * @return A {@link SearchOption} that will only match attribute instances that offer themselves in the specified
     *         direction. */
    public static SearchOptionDirectional<Object> inDirection(Direction direction) {
        return SearchOptionDirectional.of(direction);
    }

    /** @param direction The direction to search in - in other words the direction from the block that is searching to
     *            the block that is being searched.
     * @return A {@link SearchOption} that will only match attribute instances that offer themselves in the specified
     *         direction, and which pass the given {@link Predicate} {@link Predicate#test(Object) test}. */
    public static <T> SearchOptionDirectional<T> inDirectionMatching(Direction direction, Predicate<T> matcher) {
        return new SearchOptionDirectional<>(direction, matcher);
    }

    /** @return A {@link SearchOption} that will only match attribute instances that
     *         {@link VoxelShapes#union(VoxelShape, VoxelShape) intersect} with the given {@link VoxelShape}. */
    public static SearchOptionInVoxel<Object> inVoxel(VoxelShape shape) {
        return new SearchOptionInVoxel<>(shape);
    }

    /** @return A {@link SearchOption} that will only match attribute instances that
     *         {@link VoxelShapes#union(VoxelShape, VoxelShape) intersect} with the given {@link VoxelShape}, and and
     *         which pass the given {@link Predicate} {@link Predicate#test(Object) test}. */
    public static <T> SearchOptionInVoxel<T> inVoxelMatching(VoxelShape shape, Predicate<T> matcher) {
        return new SearchOptionInVoxel<>(shape, matcher);
    }

    /** @param direction The direction to search in - in other words the direction from the block that is searching to
     *            the block that is being searched.
     * @return A {@link SearchOption} that will only match attribute instances that offer themselves in the specified
     *         direction, and which {@link VoxelShapes#union(VoxelShape, VoxelShape) intersect} with the given
     *         {@link VoxelShape}, and and which pass the given {@link Predicate} {@link Predicate#test(Object)
     *         test}. */
    public static SearchOptionDirectionalVoxel<Object> inDirectionalVoxel(Direction direction, VoxelShape shape) {
        return new SearchOptionDirectionalVoxel<>(direction, false, shape);
    }

    /** @param direction The direction to search in - in other words the direction from the block that is searching to
     *            the block that is being searched.
     * @return A {@link SearchOption} that will only match attribute instances that:
     *         <ul>
     *         <li>Offer themselves in the specified direction
     *         <li>{@link VoxelShapes#union(VoxelShape, VoxelShape) Intersect} with the given {@link VoxelShape}</li>
     *         <li>Pass the given {@link Predicate} {@link Predicate#test(Object) test}, and which</li>
     *         </ul>
     */
    public static <T> SearchOptionDirectionalVoxel<T> inDirectionalVoxelMatching(Direction direction, VoxelShape shape,
        Predicate<T> matcher) {
        return new SearchOptionDirectionalVoxel<>(direction, false, shape, matcher);
    }

    /** @param direction The direction to search in - in other words the direction from the block that is searching to
     *            the block that is being searched.
     * @return A {@link SearchOption} that will only match attribute instances that offer themselves in the specified
     *         direction, and which {@link VoxelShapes#union(VoxelShape, VoxelShape) intersect} with the given
     *         {@link VoxelShape}, and and which pass the given {@link Predicate} {@link Predicate#test(Object) test}.
     *         <p>
     *         In addition this will sort the resulting {@link AttributeList} that are returned from methods like
     *         {@link Attribute#getAll(World, BlockPos, SearchOption)} in the order that their {@link VoxelShape} will
     *         be encountered by the directional search. */
    public static SearchOptionDirectionalVoxel<Object> inOrderedDirectionalVoxel(Direction direction,
        VoxelShape shape) {
        return new SearchOptionDirectionalVoxel<>(direction, true, shape);
    }

    /** @param direction The direction to search in - in other words the direction from the block that is searching to
     *            the block that is being searched.
     * @return A {@link SearchOption} that will only match attribute instances that:
     *         <ul>
     *         <li>Offer themselves in the specified direction
     *         <li>{@link VoxelShapes#union(VoxelShape, VoxelShape) Intersect} with the given {@link VoxelShape}</li>
     *         <li>Pass the given {@link Predicate} {@link Predicate#test(Object) test}, and which</li>
     *         </ul>
     *         In addition this will sort the resulting {@link AttributeList} that are returned from methods like
     *         {@link Attribute#getAll(World, BlockPos, SearchOption)} in the order that their {@link VoxelShape} will
     *         be encountered by the search. */
    public static <T> SearchOptionDirectionalVoxel<T> inOrderedDirectionalVoxelMatching(Direction direction,
        VoxelShape shape, Predicate<T> matcher) {
        return new SearchOptionDirectionalVoxel<>(direction, true, shape, matcher);
    }
}
