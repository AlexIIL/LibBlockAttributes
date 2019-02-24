package alexiil.mc.lib.attributes.hooks;

import java.util.List;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.item.IFixedItemInv;
import alexiil.mc.lib.attributes.item.IFixedItemInvView;
import alexiil.mc.lib.attributes.item.IItemExtractable;
import alexiil.mc.lib.attributes.item.IItemInsertable;

public class ItemInvViewGetterHooks {

    public static void addItemInvViews(World world, BlockPos pos, List<IFixedItemInvView> list) {

    }

    public static void addItemInventories(World world, BlockPos pos, List<IFixedItemInv> list) {

    }

    public static void addItemInsertables(World world, BlockPos pos, List<IItemInsertable> list) {

    }

    public static void addItemExtractables(World world, BlockPos pos, List<IItemExtractable> list) {

    }
}
