/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/** Search result for block attributes. */
public class AttributeList<T> extends AbstractAttributeList<T> {
    public final SearchOption<? super T> searchParam;

    @Nonnull
    final VoxelShape defaultShape;

    final DefaultedList<CacheInfo> cacheList = DefaultedList.of();

    /** May contain null elements if the caller didn't provide one (and so the actual shape should be taken from the
     * block). */
    final List<VoxelShape> shapeList = new ArrayList<>();
    final List<VoxelShape> combinedShapeList;

    /** Only used if the {@link #getSearchDirection()} is a non-null value (as otherwise it's impossible to know which
     * direction should be blocked from). */
    VoxelShape obstructingShape;

    /** The number of calls to {@link #add(Object)}. */
    private int offeredCount;

    AttributeList(Attribute<T> attribute, @Nullable SearchOption<? super T> searchOption, VoxelShape defaultShape) {
        super(attribute);

        if (defaultShape == null) {
            throw new NullPointerException("defaultShape");
        }

        if (searchOption == null) {
            searchOption = SearchOptions.ALL;
        }

        this.searchParam = searchOption;
        this.defaultShape = defaultShape;

        if (getSearchDirection() != null) {
            this.obstructingShape = VoxelShapes.empty();
        } else {
            this.obstructingShape = null;
        }

        if (this.searchParam.getShape() != VoxelShapes.fullCube() || obstructingShape != null) {
            this.combinedShapeList = new ArrayList<>();
        } else {
            this.combinedShapeList = null;
        }
    }

    /** @return The {@link Direction} that the search is moving in, or null if the {@link #searchParam} doesn't supply
     *         one. */
    @Nullable
    public Direction getSearchDirection() {
        if (searchParam instanceof SearchOptionDirectional) {
            return ((SearchOptionDirectional<?>) searchParam).direction;
        } else {
            return null;
        }
    }

    /** @return The side of the target block that the search is approaching. If the search option is directional then
     *         this will be {@link Direction#getOpposite() opposite} of {@link #getSearchDirection()}, otherwise this
     *         will be null. */
    @Nullable
    public Direction getTargetSide() {
        Direction dir = getSearchDirection();
        return dir == null ? null : dir.getOpposite();
    }

    // Adders (used by attribute providers)

    /** Directly adds the given object to this list.
     * 
     * @param object The object to add. */
    public void add(T object) {
        add(object, null, null);
    }

    /** Directly adds the given object to this list.
     * 
     * @param object The object to add.
     * @param cacheInfo The caching information associated with the given object. (As caching hasn't been implemented
     *            yet this can always be null). */
    public void add(T object, @Nullable CacheInfo cacheInfo) {
        add(object, cacheInfo, null);
    }

    /** Directly adds the given object to this list.
     * 
     * @param object The object to add.
     * @param shape The shape of the given object. If null (or empty) then this will default to the shape of the block
     *            that is being checked. */
    public void add(T object, @Nullable VoxelShape shape) {
        add(object, null, shape);
    }

    /** Directly adds the given object to this list.
     * 
     * @param object The object to add.
     * @param cacheInfo The caching information associated with the given object. (As caching hasn't been implemented
     *            yet this can always be null).
     * @param shape The shape of the given object. If null (or empty) then this will default to the shape of the block
     *            that is being checked. */
    public void add(T object, @Nullable CacheInfo cacheInfo, @Nullable VoxelShape shape) {
        assertAdding();
        if (cacheInfo == null) {
            cacheInfo = CacheInfo.NOT_CACHABLE;
        }
        if (shape == null) {
            shape = defaultShape;
        }
        offeredCount++;
        if (!searchParam.matches(object)) {
            return;
        }
        VoxelShape searchShape = searchParam.getShape();
        if (combinedShapeList != null) {
            VoxelShape combined = VoxelShapes.combine(shape, searchShape, BooleanBiFunction.AND);
            if (combined.isEmpty()) {
                return;
            }
            combinedShapeList.add(combined);
        }
        list.add(object);
        cacheList.add(cacheInfo);
        shapeList.add(shape);
    }

    /** Offers the given object to this list. If the given object is not an instance of the current {@link #attribute}
     * (and it cannot be {@link Convertible#convertTo(Class) converted} into it) then this will not affect this list.
     * 
     * @param object The object to offer, which may implement {@link Convertible} if it can be converted into many
     *            different forms. */
    public void offer(Object object) {
        offer(object, null, null);
    }

    /** Offers the given object to this list. If the given object is not an instance of the current {@link #attribute}
     * (and it cannot be {@link Convertible#convertTo(Class) converted} into it) then this will not affect this list.
     * 
     * @param object The object to offer, which may implement {@link Convertible} if it can be converted into many
     *            different forms.
     * @param cacheInfo The caching information associated with the given object. (As caching hasn't been implemented
     *            yet this can always be null). */
    public void offer(Object object, @Nullable CacheInfo cacheInfo) {
        offer(object, cacheInfo, null);
    }

    /** Offers the given object to this list. If the given object is not an instance of the current {@link #attribute}
     * (and it cannot be {@link Convertible#convertTo(Class) converted} into it) then this will not affect this list.
     * 
     * @param object The object to offer, which may implement {@link Convertible} if it can be converted into many
     *            different forms.
     * @param shape The shape of the given object. If null (or empty) then this will default to the shape of the block
     *            that is being checked. */
    public void offer(Object object, @Nullable VoxelShape shape) {
        offer(object, null, shape);
    }

    /** Offers the given object to this list. If the given object is not an instance of the current {@link #attribute}
     * (and it cannot be {@link Convertible#convertTo(Class) converted} into it) then this will not affect this list.
     * 
     * @param object The object to offer, which may implement {@link Convertible} if it can be converted into many
     *            different forms.
     * @param cacheInfo The caching information associated with the given object. (As caching hasn't been implemented
     *            yet this can always be null).
     * @param shape The shape of the given object. If null (or empty) then this will default to the shape of the block
     *            that is being checked. */
    public void offer(Object object, @Nullable CacheInfo cacheInfo, @Nullable VoxelShape shape) {
        // Always check before to throw the error as early as possible
        assertAdding();
        T converted = Convertible.getAs(object, attribute.clazz);
        if (converted != null) {
            add(converted, cacheInfo, shape);
        }
    }

    /** Adds an obstruction to the current search. For example a buildcraft pipe plug would add a small
     * {@link VoxelShape} to prevent the neighbouring pipe connecting through it.
     * <p>
     * This only has an effect on the current search if {@link #getSearchDirection()} returns a non-null value. (as
     * otherwise it won't obstruct anything). */
    public void obstruct(VoxelShape shape) {
        if (obstructingShape != null) {
            shape = VoxelShapes.combine(shape, searchParam.getShape(), BooleanBiFunction.AND);
            if (!shape.isEmpty()) {
                obstructingShape = VoxelShapes.union(obstructingShape, extendShape(shape, getSearchDirection()));
            }
        }
    }

    @Override
    void finishAdding() {
        super.finishAdding();
        if (obstructingShape != null) {
            for (int i = 0; i < list.size(); i++) {
                VoxelShape cShape = combinedShapeList.get(i);
                VoxelShape visible = VoxelShapes.combine(obstructingShape, cShape, BooleanBiFunction.ONLY_SECOND);
                if (visible.isEmpty()) {
                    list.remove(i);
                    shapeList.remove(i);
                    combinedShapeList.remove(i);
                    i--;
                }
            }
        }
    }

    /** @return A new voxel shape that has been extended to infinity(?) in the given direction, and also moved forwards
     *         by half a voxel. */
    private static VoxelShape extendShape(VoxelShape shape, Direction direction) {
        if (direction == null) {
            return shape;
        }
        // TODO: Improve this algorithm!
        VoxelShape combined = null;
        for (Box box : shape.getBoundingBoxes()) {
            // Offset it a tiny bit to allow an obstacle to return attributes (as otherwise it would block itself)
            box = box.offset(Vec3d.of(direction.getVector()).multiply(1 / 32.0));
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
            VoxelShape thisBox = VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ);
            if (combined == null) {
                combined = thisBox;
            } else {
                combined = VoxelShapes.union(combined, thisBox);
            }
        }
        return combined == null ? VoxelShapes.empty() : combined;
    }

    // Accessors (used by attribute lookup functions)

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
            return searchShape.getMax(dir.getAxis()) == leftover.getMax(dir.getAxis());
        } else {
            return searchShape.getMin(dir.getAxis()) == leftover.getMin(dir.getAxis());
        }
    }

    /** @return True if {@link #getOfferedCount()} is greater than 0. */
    public boolean hasOfferedAny() {
        return offeredCount > 0;
    }

    /** @return The number of calls to {@link #add(Object, CacheInfo, VoxelShape)} (including calls to
     *         {@link #add(Object)} and it's variants). Note that this doesn't count calls to {@link #offer(Object)}
     *         (and it's variants) that don't add any attributes. */
    public int getOfferedCount() {
        return offeredCount;
    }

    /** @return A combined version of this list and then the second given list, or the attribute's default value if both
     *         lists are empty. */
    @Nonnull
    public T combine(AttributeList<T> after, CombinableAttribute<T> combinable) {
        assertUsing();
        return combinable.combine(list, after.list);
    }
}
