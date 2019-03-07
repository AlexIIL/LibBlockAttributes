package alexiil.mc.lib.attributes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class AttributeList<T> {
    public final Attribute<T> attribute;
    public final SearchParameter searchParam;

    @Nonnull
    final VoxelShape defaultShape;

    final DefaultedList<T> list = DefaultedList.create();
    final DefaultedList<CacheInfo> cacheList = DefaultedList.create();

    /** May contain null elements if the caller didn't provide one (and so the actual shape should be taken from the
     * block). */
    final List<VoxelShape> shapeList = new ArrayList<>();

    /** Only used if we need to sort the resulting list by the shapes. */
    final List<VoxelShape> combinedShapeList;

    private AttributeListMode mode = AttributeListMode.ADDING;

    AttributeList(Attribute<T> attribute, SearchParameter searchParam, VoxelShape defaultShape) {
        if (defaultShape == null) {
            throw new NullPointerException("defaultShape");
        }
        this.attribute = attribute;
        this.searchParam = searchParam;
        this.defaultShape = defaultShape;
        if (searchParam instanceof SearchParamDirectionalVoxel && ((SearchParamDirectionalVoxel) searchParam).ordered) {
            this.combinedShapeList = new ArrayList<>();
        } else {
            this.combinedShapeList = null;
        }
    }

    @Nullable
    public Direction getSearchDirection() {
        if (searchParam instanceof SearchParamDirectional) {
            return ((SearchParamDirectional) searchParam).direction;
        } else {
            return null;
        }
    }

    // Adders (used by attribute providers)

    public void add(T obj) {
        // FIXME: We default to null, but in most cases it's possible to get the shape
        // of the block anyway - as most normal blocks only have their normal shape.
        // perhaps we should default it to their whole shape if it's not given?
        add(obj, CacheInfo.NOT_CACHABLE, null);
    }

    public void add(T obj, CacheInfo cacheInfo) {
        add(obj, cacheInfo, null);
    }

    public void add(T obj, @Nullable VoxelShape shape) {
        add(obj, CacheInfo.NOT_CACHABLE, shape);
    }

    public void add(T obj, CacheInfo cacheInfo, @Nullable VoxelShape shape) {
        assertAdding();
        if (shape == null) {
            shape = defaultShape;
        }
        if (searchParam instanceof SearchParamInVoxel) {
            if (!VoxelShapes.compareShapes(shape, ((SearchParamInVoxel) searchParam).shape, BooleanBiFunction.AND)) {
                return;
            }
        } else if (searchParam instanceof SearchParamDirectionalVoxel) {
            SearchParamDirectionalVoxel voxelSearch = (SearchParamDirectionalVoxel) searchParam;
            if (voxelSearch.ordered) {
                VoxelShape combined = VoxelShapes.union(shape, voxelSearch.shape);
                if (combined.isEmpty()) {
                    return;
                }
                combinedShapeList.add(combined);
            } else {
                if (!VoxelShapes.compareShapes(shape, voxelSearch.shape, BooleanBiFunction.AND)) {
                    return;
                }
            }
        }
        list.add(obj);
        cacheList.add(cacheInfo);
        shapeList.add(shape);
    }

    public void offer(Object object) {
        offer(object, CacheInfo.NOT_CACHABLE, null);
    }

    public void offer(Object object, CacheInfo cacheInfo) {
        offer(object, cacheInfo, null);
    }

    public void offer(Object object, @Nullable VoxelShape shape) {
        offer(object, CacheInfo.NOT_CACHABLE, shape);
    }

    public void offer(Object object, CacheInfo cacheInfo, @Nullable VoxelShape shape) {
        // Always check before to throw the error as early as possible
        assertAdding();
        if (attribute.isInstance(object)) {
            add(attribute.cast(object), cacheInfo, shape);
        }
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
        if (mode != AttributeListMode.ADDING) {
            throw new IllegalStateException("Already finished adding!");
        }
        mode = AttributeListMode.USING;
        if (searchParam instanceof SearchParamDirectionalVoxel) {
            SearchParamDirectionalVoxel param = (SearchParamDirectionalVoxel) searchParam;
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
