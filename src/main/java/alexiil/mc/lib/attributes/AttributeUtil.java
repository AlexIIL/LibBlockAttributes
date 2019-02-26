package alexiil.mc.lib.attributes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AttributeUtil {

    /** General getter method for any attribute.
     * 
     * @param world
     * @param pos
     * @param attribute
     * @return */
    public static <T> List<T> getAttribute(World world, BlockPos pos, Attribute<T> attribute) {

        // For safety - as otherwise someone might call this accidently
        // if (attribute == IFixedItemInvView.ATTRIBUTE_FIXED_INV_VIEW) {
        // return (T) ItemInvUtil.getFixedInvView(world, pos);
        // }
        // if (attribute == IFixedItemInv.ATTRIBUTE_FIXED_ITEM_INV) {
        // return (T) ItemInvUtil.getFixedInv(world, pos);
        // }
        // if (attribute == IItemInvStats.ATTRIBUTE_STATS) {
        // return (T) ItemInvUtil.getItemInvStats(world, pos);
        // }

        // Note that these don't work properly as they need the direction!
        // if (attribute == IItemInsertable.ATTRIBUTE_INSERTABLE) {
        // return (T) ItemInvUtil.getInsertable(world, pos);
        // }
        // if (attribute == IItemInsertable.ATTRIBUTE_INSERTABLE) {
        // return (T) ItemInvUtil.getInsertable(world, pos);
        // }

        List<T> list = new ArrayList<>();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Normal
        if (block instanceof IAttributeBlock) {
            IAttributeBlock attributeBlock = (IAttributeBlock) block;
            attributeBlock.addAllAttributes(world, pos, state, attribute, list);
        } else if (block instanceof IDelegatingAttributeBlock) {

            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                T obj = provider.getAttribute(attribute);
                // This doesn't work for any CombinableAttribute...
                if (obj != null) {
                    list.add(obj);
                }
            }
        }

        return list;
    }

    /** General getter method for any combinable attribute.
     * 
     * @param world
     * @param pos
     * @param attribute
     * @return */
    public static <T> T getAttribute(World world, BlockPos pos, CombinableAttribute<T> attribute) {

        // For safety - as otherwise someone might call this accidently
        // if (attribute == IFixedItemInvView.ATTRIBUTE_FIXED_INV_VIEW) {
        // return (T) ItemInvUtil.getFixedInvView(world, pos);
        // }
        // if (attribute == IFixedItemInv.ATTRIBUTE_FIXED_ITEM_INV) {
        // return (T) ItemInvUtil.getFixedInv(world, pos);
        // }
        // if (attribute == IItemInvStats.ATTRIBUTE_STATS) {
        // return (T) ItemInvUtil.getItemInvStats(world, pos);
        // }

        // Note that these don't work properly as they need the direction!
        // if (attribute == IItemInsertable.ATTRIBUTE_INSERTABLE) {
        // return (T) ItemInvUtil.getInsertable(world, pos);
        // }
        // if (attribute == IItemInsertable.ATTRIBUTE_INSERTABLE) {
        // return (T) ItemInvUtil.getInsertable(world, pos);
        // }

        List<T> list = new ArrayList<>();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Normal
        if (block instanceof IAttributeBlock) {
            IAttributeBlock attributeBlock = (IAttributeBlock) block;
            attributeBlock.addAllAttributes(world, pos, state, attribute, list);
        } else if (block instanceof IDelegatingAttributeBlock) {

            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                T obj = provider.getAttribute(attribute);
                if (obj != null) {
                    list.add(obj);
                }
            }
        }

        return attribute.combine(list);
    }
}
