package alexiil.mc.lib.attributes;

import java.util.function.Predicate;

import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public final class SearchOptionDirectionalVoxel<T> extends SearchOptionDirectional<T> {

    /** If true then the {@link AttributeList} will sort itself based on the direction and shape. */
    public final boolean ordered;
    public final VoxelShape shape;

    SearchOptionDirectionalVoxel(Direction direction, boolean ordered, VoxelShape shape) {
        super(direction);
        this.ordered = ordered;
        this.shape = shape;
    }

    SearchOptionDirectionalVoxel(Direction direction, boolean ordered, VoxelShape shape, Predicate<T> searchMatcher) {
        super(direction, searchMatcher);
        this.ordered = ordered;
        this.shape = shape;
    }

    @Override
    public VoxelShape getShape() {
        return shape;
    }
}
