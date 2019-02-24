package alexiil.mc.lib.attributes;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** A {@link Block} that contains attributes. */
public interface IAttributeBlock {

    /** Adds every instance of the given attribute in this block to the resulting list. Note that this must never add
     * wrapped versions of an attribute to the resulting list as the caller is responsible for doing that instead. */
    default <T> void addAllAttributes(World world, BlockPos pos, BlockState state, Attribute<T> attribute,
        List<T> resultingList) {

    }

    /** Adds every instance of the given attribute in this block that can be accessed from the specified direction to
     * the resulting list. Note that this must never add wrapped versions of an attribute to the resulting list as the
     * caller is responsible for doing that instead.
     * 
     * @param attribute
     * @param resultingList
     * @param searchDirection */
    // FIXME: the search direction param is technically confusing both to callers and implementors :/
    default <T> void addAllAttributesFromDirection(World world, BlockPos pos, BlockState state, Attribute<T> attribute,
        List<T> resultingList, @Nonnull Direction searchDirection) {

    }
}
