package alexiil.mc.lib.attributes;

import java.util.function.Predicate;

import net.minecraft.util.shape.VoxelShape;

public final class SearchOptionInVoxel<T> extends SearchOption<T> {
    public final VoxelShape shape;

    public SearchOptionInVoxel(VoxelShape shape) {
        this.shape = shape;
    }

    public SearchOptionInVoxel(VoxelShape shape, Predicate<T> searchMatcher) {
        super(searchMatcher);
        this.shape = shape;
    }
}
