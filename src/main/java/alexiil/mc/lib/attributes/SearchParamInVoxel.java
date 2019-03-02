package alexiil.mc.lib.attributes;

import net.minecraft.util.shape.VoxelShape;

public final class SearchParamInVoxel extends SearchParameter {
    public final VoxelShape shape;

    public SearchParamInVoxel(VoxelShape shape) {
        this.shape = shape;
    }
}
