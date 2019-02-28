package alexiil.mc.lib.attributes.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.IAttributeBlock;
import alexiil.mc.lib.attributes.IAttributeProvider;
import alexiil.mc.lib.attributes.IDelegatingAttributeBlock;
import alexiil.mc.lib.attributes.hooks.ItemInvViewGetterHooks;
import alexiil.mc.lib.attributes.item.IFixedItemInv;
import alexiil.mc.lib.attributes.item.IFixedItemInvView;
import alexiil.mc.lib.attributes.item.IItemExtractable;
import alexiil.mc.lib.attributes.item.IItemInsertable;
import alexiil.mc.lib.attributes.item.IItemInvStats;
import alexiil.mc.lib.attributes.item.ItemAttributes;
import alexiil.mc.lib.attributes.item.ItemInvUtil;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;
import alexiil.mc.lib.attributes.item.impl.EmptyItemInvStats;
import alexiil.mc.lib.attributes.item.impl.FixedInventoryVanillaWrapper;
import alexiil.mc.lib.attributes.item.impl.FixedInventoryViewVanillaWrapper;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;

/** Various plumbing methods for {@link ItemInvUtil} amongst others. it's generally recommended that you don't use this
 * class directly, and instead rely on the many other abstractions. */
/* module-private */ public class AttributeObtainingImpl {

    // #######################
    // IFixedItemInvView
    // #######################
    // Entry points for ItemInvUtil,
    // caches (TODO) e t c
    // #######################

    /** @return An {@link IFixedItemInvView}, or {@link EmptyFixedItemInv} if one couldn't be found. */
    public static IFixedItemInvView getFixedInventoryView(World world, BlockPos pos) {

        List<IFixedItemInvView> list = new ArrayList<>();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Normal
        if (block instanceof IAttributeBlock) {
            IAttributeBlock attributeBlock = (IAttributeBlock) block;
            attributeBlock.addAllAttributes(world, pos, state, ItemAttributes.FIXED_INV_VIEW, list);
        } else if (block instanceof IDelegatingAttributeBlock) {

            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                IFixedItemInvView inv = provider.getAttribute(ItemAttributes.FIXED_INV_VIEW);
                if (inv != null && inv != EmptyFixedItemInv.INSTANCE) {
                    list.add(inv);
                }
            }
        } else
        // Vanilla wrappers
        // The hopper and composter don't need anything special.
        if (block instanceof InventoryProvider) {
            InventoryProvider provider = (InventoryProvider) block;
            SidedInventory inventory = provider.getInventory(state, world, pos);
            if (inventory != null) {
                list.add(FixedInventoryViewVanillaWrapper.wrapInventory(inventory));
            }
        } else if (block.hasBlockEntity()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity) {
                // Special case chests here, rather than through a mixin because it just simplifies everything

                // method_17458: something like "get chest inventory"
                Inventory chestInv = ChestBlock.method_17458(state, world, pos,
                    /* Check if the top is blocked by a solid block or a cat */false);
                if (chestInv != null) {
                    list.add(FixedInventoryViewVanillaWrapper.wrapInventory(chestInv));
                }
            } else if (be instanceof Inventory) {
                list.add(FixedInventoryViewVanillaWrapper.wrapInventory((Inventory) be));
            }
        }

        ItemInvViewGetterHooks.addItemInvViews(world, pos, list);

        return ItemAttributes.FIXED_INV_VIEW.combine(list);
    }

    // #######################
    // IFixedItemInv
    // #######################
    // Entry points for ItemInvUtil,
    // caches (TODO) e t c
    // #######################

    public static IFixedItemInv getFixedInventory(World world, BlockPos pos) {

        // As this is basically the same as "getFixedInventoryView" this is pretty much just a copy-paste
        // and as such every change to that should be copied into here

        List<IFixedItemInv> list = new ArrayList<>();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Normal
        if (block instanceof IAttributeBlock) {
            IAttributeBlock attributeBlock = (IAttributeBlock) block;
            attributeBlock.addAllAttributes(world, pos, state, ItemAttributes.FIXED_INV, list);
        } else if (block instanceof IDelegatingAttributeBlock) {

            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                IFixedItemInv inv = provider.getAttribute(ItemAttributes.FIXED_INV);
                if (inv != null && inv != EmptyFixedItemInv.INSTANCE) {
                    list.add(inv);
                }
            }

        } else
        // Vanilla wrappers
        // The hopper and composter don't need anything special.
        if (block instanceof InventoryProvider) {
            InventoryProvider provider = (InventoryProvider) block;
            SidedInventory inventory = provider.getInventory(state, world, pos);
            if (inventory != null) {
                list.add(new FixedInventoryVanillaWrapper(inventory));
            }
        } else if (block.hasBlockEntity()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity) {
                // Special case chests here, rather than through a mixin because it just simplifies everything

                // method_17458: something like "get chest inventory"
                Inventory chestInv = ChestBlock.method_17458(state, world, pos,
                    /* Check if the top is blocked by a solid block or a cat */false);
                if (chestInv != null) {
                    list.add(new FixedInventoryVanillaWrapper(chestInv));
                }
            } else if (be instanceof Inventory) {
                list.add(new FixedInventoryVanillaWrapper((Inventory) be));
            }
        }

        ItemInvViewGetterHooks.addItemInventories(world, pos, list);

        return ItemAttributes.FIXED_INV.combine(list);
    }

    // #######################
    // IItemInvStats
    // #######################
    // Entry points for ItemInvUtil,
    // caches (TODO) e t c
    // #######################

    public static IItemInvStats getItemInventoryStats(World world, BlockPos pos) {

        // As this is basically the same as "getFixedInventoryView" this is pretty much just a copy-paste
        // and as such every change to that should be copied into here

        List<IItemInvStats> list = new ArrayList<>();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Normal
        if (block instanceof IAttributeBlock) {
            IAttributeBlock attributeBlock = (IAttributeBlock) block;
            attributeBlock.addAllAttributes(world, pos, state, ItemAttributes.INV_STATS, list);
        } else if (block instanceof IDelegatingAttributeBlock) {

            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                IItemInvStats stats = provider.getAttribute(ItemAttributes.INV_STATS);
                if (stats != null && stats != EmptyItemInvStats.INSTANCE) {
                    list.add(stats);
                }
            }

        } else
        // Vanilla wrappers
        // The composter doesn't need any special support for inventories as it always has one.
        if (block instanceof InventoryProvider) {
            InventoryProvider provider = (InventoryProvider) block;
            SidedInventory inventory = provider.getInventory(state, world, pos);
            if (inventory != null) {
                list.add(new FixedInventoryVanillaWrapper(inventory).getStatistics());
            }
        } else if (block.hasBlockEntity()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity) {
                // Special case chests here, rather than through a mixin because it just simplifies everything

                // method_17458: something like "get chest inventory"
                Inventory chestInv = ChestBlock.method_17458(state, world, pos,
                    /* Check if the top is blocked by a solid block or a cat */false);
                if (chestInv != null) {
                    list.add(new FixedInventoryVanillaWrapper(chestInv).getStatistics());
                }
            } else if (be instanceof Inventory) {
                list.add(new FixedInventoryVanillaWrapper((Inventory) be).getStatistics());
            }
        }

        ItemInvViewGetterHooks.addItemInvStats(world, pos, list);

        return ItemAttributes.INV_STATS.combine(list);
    }

    // #######################
    // IItemInsertable
    // #######################
    // Entry points for ItemInvUtil,
    // caches (TODO) e t c
    // #######################

    public static IItemInsertable getInsertable(World world, BlockPos pos, @Nonnull Direction direction) {

        // As this is basically the same as "getFixedInventoryView" this is pretty much just a copy-paste
        // and as such every change to that should be copied into here

        List<IItemInsertable> list = new ArrayList<>();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Normal
        if (block instanceof IAttributeBlock) {
            IAttributeBlock attributeBlock = (IAttributeBlock) block;
            attributeBlock.addAllAttributesFromDirection(world, pos, state, ItemAttributes.INSERTABLE, list,
                direction);
        } else if (block instanceof IDelegatingAttributeBlock) {

            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                IItemInsertable inv = provider.getAttribute(ItemAttributes.INSERTABLE);
                if (inv != null && inv != RejectingItemInsertable.NULL) {
                    list.add(inv);
                }
            }

        } else
        // Vanilla wrappers
        if (block instanceof HopperBlock && state.get(HopperBlock.FACING) == direction.getOpposite()) {
            // Explicitly don't add an insertable
        } else if (block instanceof InventoryProvider) {
            InventoryProvider provider = (InventoryProvider) block;
            SidedInventory inventory = provider.getInventory(state, world, pos);
            if (inventory != null) {
                if (inventory.getInvSize() > 0) {
                    FixedInventoryVanillaWrapper wrapper = new FixedInventoryVanillaWrapper(inventory);
                    list.add(wrapper.getInsertable(inventory.getInvAvailableSlots(direction.getOpposite())));
                } else {
                    list.add(RejectingItemInsertable.NULL);
                }
            }
        } else if (block.hasBlockEntity()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity) {
                // Special case chests here, rather than through a mixin because it just simplifies everything

                // method_17458: something like "get chest inventory"
                Inventory chestInv = ChestBlock.method_17458(state, world, pos,
                    /* Check if the top is blocked by a solid block or a cat */false);
                if (chestInv != null) {
                    list.add(new FixedInventoryVanillaWrapper(chestInv).getInsertable());
                }
            } else if (be instanceof SidedInventory) {
                SidedInventory sidedInv = (SidedInventory) be;
                int[] slots = sidedInv.getInvAvailableSlots(direction.getOpposite());
                list.add(new FixedInventoryVanillaWrapper(sidedInv).getInsertable(slots));
            } else if (be instanceof Inventory) {
                list.add(new FixedInventoryVanillaWrapper((Inventory) be).getInsertable());
            }
        }

        ItemInvViewGetterHooks.addItemInsertables(world, pos, list);

        return ItemAttributes.INSERTABLE.combine(list);
    }
    // #######################
    // IItemExtractable
    // #######################
    // Entry points for ItemInvUtil,
    // caches (TODO) e t c
    // #######################

    public static IItemExtractable getExtractable(World world, BlockPos pos, @Nonnull Direction direction) {

        // As this is basically the same as "getFixedInventoryView" this is pretty much just a copy-paste
        // and as such every change to that should be copied into here

        List<IItemExtractable> list = new ArrayList<>();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Normal
        if (block instanceof IAttributeBlock) {
            IAttributeBlock attributeBlock = (IAttributeBlock) block;
            attributeBlock.addAllAttributesFromDirection(world, pos, state, ItemAttributes.EXTRACTABLE,
                list, direction);
        } else if (block instanceof IDelegatingAttributeBlock) {

            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                IItemExtractable inv = provider.getAttribute(ItemAttributes.EXTRACTABLE);
                if (inv != null && inv != EmptyItemExtractable.NULL) {
                    list.add(inv);
                }
            }

        } else
        // Vanilla wrappers
        if (block instanceof HopperBlock && state.get(HopperBlock.FACING) == direction) {
            list.add(EmptyItemExtractable.SUPPLIER);
        } else if (block instanceof InventoryProvider) {
            InventoryProvider provider = (InventoryProvider) block;
            SidedInventory inventory = provider.getInventory(state, world, pos);
            if (inventory != null) {
                if (inventory.getInvSize() > 0) {
                    FixedInventoryVanillaWrapper wrapper = new FixedInventoryVanillaWrapper(inventory);
                    list.add(wrapper.getExtractable(inventory.getInvAvailableSlots(direction.getOpposite())));
                } else {
                    list.add(EmptyItemExtractable.NULL);
                }
            }
        } else if (block.hasBlockEntity()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity) {
                // Special case chests here, rather than through a mixin because it just simplifies everything

                // method_17458: something like "get chest inventory"
                Inventory chestInv = ChestBlock.method_17458(state, world, pos,
                    /* Check if the top is blocked by a solid block or a cat */false);
                if (chestInv != null) {
                    list.add(new FixedInventoryVanillaWrapper(chestInv).getExtractable());
                }
            } else if (be instanceof SidedInventory) {
                SidedInventory sidedInv = (SidedInventory) be;
                int[] slots = sidedInv.getInvAvailableSlots(direction.getOpposite());
                list.add(new FixedInventoryVanillaWrapper(sidedInv).getExtractable(slots));
            } else if (be instanceof Inventory) {
                list.add(new FixedInventoryVanillaWrapper((Inventory) be).getExtractable());
            }
        }

        ItemInvViewGetterHooks.addItemExtractables(world, pos, list);

        return ItemAttributes.EXTRACTABLE.combine(list);
    }

    // #######################
    // Internals
    // #######################

}
