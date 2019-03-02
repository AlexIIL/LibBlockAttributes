package alexiil.mc.lib.attributes;

import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class AttributeDefaulted<T> extends Attribute<T> {

    @Nonnull
    public final T defaultValue;

    public AttributeDefaulted(Class<T> clazz, @Nonnull T defaultValue) {
        super(clazz);
        this.defaultValue = defaultValue;
    }

    public AttributeDefaulted(Class<T> clazz, @Nonnull T defaultValue, IAttributeCustomAdder<T> customAdder) {
        super(clazz, customAdder);
        this.defaultValue = defaultValue;
    }

    @Nonnull
    public final T getFirst(World world, BlockPos pos, SearchParameter searchParam) {
        VoxelShape blockShape = world.getBlockState(pos).getOutlineShape(world, pos);
        AttributeList<T> list = new AttributeList<>(this, searchParam, blockShape);
        addAll(world, pos, list);
        if (list.list.isEmpty()) {
            return defaultValue;
        } else {
            return list.get(0);
        }
    }
}
