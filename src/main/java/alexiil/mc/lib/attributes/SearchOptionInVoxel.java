package alexiil.mc.lib.attributes;

import java.util.function.Predicate;

import net.minecraft.util.shape.VoxelShape;

public final class SearchOptionInVoxel<T> extends SearchOption<T> {
    public final VoxelShape shape;

    SearchOptionInVoxel(VoxelShape shape) {
        this.shape = shape;
    }

    SearchOptionInVoxel(VoxelShape shape, Predicate<T> searchMatcher) {
        super(searchMatcher);
        this.shape = shape;
    }

    @Override
    public VoxelShape getShape() {
        return shape;
    }
}
