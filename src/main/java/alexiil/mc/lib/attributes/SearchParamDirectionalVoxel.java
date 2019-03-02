package alexiil.mc.lib.attributes;

import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public final class SearchParamDirectionalVoxel extends SearchParamDirectional {

    /** If true then the {@link AttributeList} will sort itself based on the direction and shape. */
    public final boolean ordered;
    public final VoxelShape shape;

    private SearchParamDirectionalVoxel(Direction direction, boolean ordered, VoxelShape shape) {
        super(direction);
        this.ordered = ordered;
        this.shape = shape;
    }

    /** Creates a search parameter which will make an {@link AttributeList} order the results by the order that a cuboid
     * would if it moved from from the start of the shape to the end of it in the given direction. */
    public static SearchParamDirectionalVoxel createOrdered(Direction direction, VoxelShape shape) {
        return new SearchParamDirectionalVoxel(direction, true, shape);
    }

    public static SearchParamDirectionalVoxel createNormal(Direction direction, VoxelShape shape) {
        return new SearchParamDirectionalVoxel(direction, false, shape);
    }
}
