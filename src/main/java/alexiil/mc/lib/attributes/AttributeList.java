package alexiil.mc.lib.attributes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class AttributeList<T> {
    public final Attribute<T> attribute;
    public final SearchOption<? super T> searchParam;

    @Nonnull
    final VoxelShape defaultShape;

    final DefaultedList<T> list = DefaultedList.create();
    final DefaultedList<CacheInfo> cacheList = DefaultedList.create();

    /** May contain null elements if the caller didn't provide one (and so the actual shape should be taken from the
     * block). */
    final List<VoxelShape> shapeList = new ArrayList<>();

    /** Only used if we need to sort the resulting list by the shapes. */
    final List<VoxelShape> combinedShapeList;

    /** Only used if the {@link #getSearchDirection()} is a non-null value (as otherwise it's impossible to know which
     * direction should be blocked from). */
    VoxelShape obstructingShape;

    private AttributeListMode mode = AttributeListMode.ADDING;

    AttributeList(Attribute<T> attribute, @Nullable SearchOption<? super T> searchOption, VoxelShape defaultShape) {
        if (defaultShape == null) {
            throw new NullPointerException("defaultShape");
        }
        if (searchOption == null) {
            searchOption = SearchOptions.ALL;
        }
        this.attribute = attribute;
        this.searchParam = searchOption;
        this.defaultShape = defaultShape;
        if (searchOption instanceof SearchOptionDirectionalVoxel
            && ((SearchOptionDirectionalVoxel<?>) searchOption).ordered) {
            this.combinedShapeList = new ArrayList<>();
        } else {
            this.combinedShapeList = null;
        }
        if (getSearchDirection() != null) {
            this.obstructingShape = VoxelShapes.empty();
        } else {
            this.obstructingShape = null;
        }
    }

    @Nullable
    public Direction getSearchDirection() {
        if (searchParam instanceof SearchOptionDirectional) {
            return ((SearchOptionDirectional<?>) searchParam).direction;
        } else {
            return null;
        }
    }

    // Adders (used by attribute providers)

    public void add(T obj) {
        add(obj, null, null);
    }

    public void add(T obj, @Nullable CacheInfo cacheInfo) {
        add(obj, cacheInfo, null);
    }

    public void add(T obj, @Nullable VoxelShape shape) {
        add(obj, null, shape);
    }

    public void add(T obj, @Nullable CacheInfo cacheInfo, @Nullable VoxelShape shape) {
        assertAdding();
        if (cacheInfo == null) {
            cacheInfo = CacheInfo.NOT_CACHABLE;
        }
        if (shape == null) {
            shape = defaultShape;
        }
        if (!searchParam.matches(obj)) {
            return;
        }
        VoxelShape searchShape = searchParam.getShape();
        if (searchParam instanceof SearchOptionDirectionalVoxel) {
            SearchOptionDirectionalVoxel<?> voxelSearch = (SearchOptionDirectionalVoxel<?>) searchParam;
            if (voxelSearch.ordered) {
                VoxelShape combined = VoxelShapes.union(shape, searchShape);
                if (combined.isEmpty()) {
                    return;
                }
                combinedShapeList.add(combined);
            }
        }
        if (/* Optimisation - most searches are going to be on a full cube, so there's no need to check them (as they
             * should always be in the cube) */
        searchShape != VoxelShapes.fullCube()
            /* Another optimisation - the above check (for SearchOptionDirectionalVoxel) will also check that they
             * intersect via the "VoxelShapes.union" call. */
            && combinedShapeList == null
            /* Finally, the real check! */
            && !VoxelShapes.matchesAnywhere(shape, searchShape, BooleanBiFunction.AND)) {
            return;
        }
        list.add(obj);
        cacheList.add(cacheInfo);
        shapeList.add(shape);
    }

    public void offer(Object object) {
        offer(object, null, null);
    }

    public void offer(Object object, @Nullable CacheInfo cacheInfo) {
        offer(object, cacheInfo, null);
    }

    public void offer(Object object, @Nullable VoxelShape shape) {
        offer(object, null, shape);
    }

    public void offer(Object object, @Nullable CacheInfo cacheInfo, @Nullable VoxelShape shape) {
        // Always check before to throw the error as early as possible
        assertAdding();
        if (attribute.isInstance(object)) {
            add(attribute.cast(object), cacheInfo, shape);
        }
    }

    /** Adds an obstruction to the current search. For example a buildcraft pipe plug would add a small
     * {@link VoxelShape} to prevent the neighbouring pipe connecting through it.
     * <p>
     * This only has an effect on the current search if {@link #getSearchDirection()} returns a non-null value. (as
     * otherwise it won't obstruct anything). */
    public void obstruct(VoxelShape shape) {
        if (obstructingShape != null) {
            obstructingShape = VoxelShapes.union(obstructingShape, extendShape(shape, getSearchDirection()));
        }
    }

    /** @return A new voxel shape that has been extended to infinity(?) in the given direction, and also moved forwards
     *         by half a voxel. */
    private static VoxelShape extendShape(VoxelShape shape, Direction direction) {
        if (direction == null) {
            return shape;
        }
        // TODO: Improve this algorithm!
        VoxelShape combined = VoxelShapes.empty();
        for (BoundingBox box : shape.getBoundingBoxes()) {
            // Offset it a tiny bit to allow an obstacle to return attributes (as otherwise it would block itself)
            box = box.offset(new Vec3d(direction.getVector()).multiply(1 / 32.0));
            double minX = box.minX;
            double minY = box.minY;
            double minZ = box.minZ;
            double maxX = box.maxX;
            double maxY = box.maxY;
            double maxZ = box.maxZ;
            switch (direction) {
                // @formatter:off
                case DOWN: minY = 0; break;
                case UP: maxY = 1; break;
                case NORTH: minZ = 0; break;
                case SOUTH: maxZ = 1; break;
                case WEST: minX = 0; break;
                case EAST: maxX = 1; break;
                // @formatter:on
                default:
                    throw new IllegalStateException("Unknown Direction " + direction);
            }
            combined = VoxelShapes.union(combined, VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ));
        }
        return combined;
    }

    // Accessors (used by attribute lookup functions)

    /** @return The number of attribute instances added to this list. */
    public int getCount() {
        assertUsing();
        return list.size();
    }

    @Nonnull
    public T get(int index) {
        assertUsing();
        return list.get(index);
    }

    public CacheInfo getCacheInfo(int index) {
        assertUsing();
        return cacheList.get(index);
    }

    @Nullable
    public VoxelShape getVoxelShape(int index) {
        assertUsing();
        return shapeList.get(index);
    }

    /** @return True if the {@link #obstruct(VoxelShape) obstructions} completely block the search shape at the
     *         <i>end</i> of the search.
     * @throws IllegalStateException if {@link #getSearchDirection()} is null. */
    public boolean doesSearchReachEnd() {
        assertUsing();
        Direction dir = getSearchDirection();
        if (dir == null) {
            throw new IllegalStateException("Didn't have a search shape!");
        }
        assert obstructingShape != null;
        VoxelShape searchShape = searchParam.getShape();

        VoxelShape leftover = VoxelShapes.combine(searchShape, obstructingShape, BooleanBiFunction.ONLY_FIRST);
        if (dir.getDirection() == AxisDirection.POSITIVE) {
            return searchShape.getMaximum(dir.getAxis()) == leftover.getMaximum(dir.getAxis());
        } else {
            return searchShape.getMinimum(dir.getAxis()) == leftover.getMinimum(dir.getAxis());
        }
    }

    @Nullable
    public T getFirstOrNull() {
        assertUsing();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Nonnull
    public T getFirst(DefaultedAttribute<T> attribute) {
        assertUsing();
        if (list.isEmpty()) {
            return attribute.defaultValue;
        }
        return list.get(0);
    }

    /** @return A combined version of this list, or the attribute's default value if this list is empty. */
    @Nonnull
    public T combine(CombinableAttribute<T> combinable) {
        assertUsing();
        return combinable.combine(list);
    }

    /** @return A combined version of this list and then the second given list, or the attribute's default value if both
     *         lists are empty. */
    @Nonnull
    public T combine(AttributeList<T> after, CombinableAttribute<T> combinable) {
        assertUsing();
        return combinable.combine(list, after.list);
    }

    // Internal

    private enum AttributeListMode {
        ADDING,
        USING;
    }

    void reset() {
        list.clear();
        cacheList.clear();
        shapeList.clear();
        if (combinedShapeList != null) {
            combinedShapeList.clear();
        }
        mode = AttributeListMode.ADDING;
    }

    void finishAdding() {
        assertAdding();
        mode = AttributeListMode.USING;
        if (searchParam instanceof SearchOptionDirectionalVoxel) {
            SearchOptionDirectionalVoxel<?> param = (SearchOptionDirectionalVoxel<?>) searchParam;
            if (param.ordered) {
                if (list.size() > 1) {
                    Swapper swapper = (a, b) -> {
                        swap(list, a, b);
                        swap(cacheList, a, b);
                        swap(shapeList, a, b);
                        swap(combinedShapeList, a, b);
                    };
                    IntComparator comparator = (a, b) -> {
                        VoxelShape shapeA = combinedShapeList.get(a);
                        VoxelShape shapeB = combinedShapeList.get(b);
                        if (param.direction.getDirection() == AxisDirection.POSITIVE) {
                            double minA = shapeA.getMinimum(param.direction.getAxis());
                            double minB = shapeB.getMinimum(param.direction.getAxis());
                            return Double.compare(minA, minB);
                        } else {
                            double maxA = shapeA.getMaximum(param.direction.getAxis());
                            double maxB = shapeB.getMaximum(param.direction.getAxis());
                            return Double.compare(maxB, maxA);
                        }
                    };
                    // Dammit fastutil why do you have to use the same name as java :(
                    it.unimi.dsi.fastutil.Arrays.quickSort(0, list.size(), comparator, swapper);
                }
            }
        }
        if (obstructingShape != null) {
            for (int i = list.size() - 1; i >= 0; i--) {
                VoxelShape attributeShape = (combinedShapeList != null ? combinedShapeList : shapeList).get(i);
                // Just in case right?
                attributeShape = VoxelShapes.union(attributeShape, VoxelShapes.fullCube());
                if (VoxelShapes.combine(obstructingShape, attributeShape, BooleanBiFunction.ONLY_SECOND).isEmpty()) {
                    list.remove(i);
                    cacheList.remove(i);
                    shapeList.remove(i);
                    if (combinedShapeList != null) {
                        combinedShapeList.remove(i);
                    }
                }
            }
        }
    }

    private static <T> void swap(List<T> list, int a, int b) {
        list.set(a, list.set(b, list.get(a)));
    }

    void assertAdding() {
        assert mode == AttributeListMode.ADDING;
    }

    void assertUsing() {
        assert mode == AttributeListMode.USING;
    }
}
