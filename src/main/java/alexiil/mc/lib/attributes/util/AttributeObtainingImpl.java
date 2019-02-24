package alexiil.mc.lib.attributes.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
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
import alexiil.mc.lib.attributes.item.ItemInvUtil;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;
import alexiil.mc.lib.attributes.item.impl.FixedInventoryVanillaWrapper;
import alexiil.mc.lib.attributes.item.impl.FixedInventoryViewVanillaWrapper;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;

/** Various plumbing methods for {@link ItemInvUtil} amongst others. it's generally recommended that you don't use this
 * class directly, and instead rely on the many other abstractions. */
public class AttributeObtainingImpl {

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
            attributeBlock.addAllAttributes(world, pos, state, IFixedItemInvView.ATTRIBUTE_FIXED_INV_VIEW, list);
        } else if (block instanceof IDelegatingAttributeBlock) {

            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                IFixedItemInvView inv = provider.getAttribute(IFixedItemInvView.ATTRIBUTE_FIXED_INV_VIEW);
                if (inv != null && inv != EmptyFixedItemInv.INSTANCE) {
                    list.add(inv);
                }
            }
        } else
        // Vanilla wrappers
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

        return IFixedItemInvView.ATTRIBUTE_FIXED_INV_VIEW.combine(list);
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
            attributeBlock.addAllAttributes(world, pos, state, IFixedItemInv.ATTRIBUTE_FIXED_ITEM_INV, list);
        } else if (block instanceof IDelegatingAttributeBlock) {

            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                IFixedItemInv inv = provider.getAttribute(IFixedItemInv.ATTRIBUTE_FIXED_ITEM_INV);
                if (inv != null && inv != EmptyFixedItemInv.INSTANCE) {
                    list.add(inv);
                }
            }

        } else
        // Vanilla wrappers
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

        return IFixedItemInv.ATTRIBUTE_FIXED_ITEM_INV.combine(list);
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
            attributeBlock.addAllAttributesFromDirection(world, pos, state, IItemInsertable.ATTRIBUTE_INSERTABLE, list,
                direction);
        } else if (block instanceof IDelegatingAttributeBlock) {

            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                IItemInsertable inv = provider.getAttribute(IItemInsertable.ATTRIBUTE_INSERTABLE);
                if (inv != null && inv != RejectingItemInsertable.NULL_INSERTABLE) {
                    list.add(inv);
                }
            }

        } else
        // Vanilla wrappers
        if (block instanceof InventoryProvider) {
            InventoryProvider provider = (InventoryProvider) block;
            SidedInventory inventory = provider.getInventory(state, world, pos);
            if (inventory != null) {
                FixedInventoryVanillaWrapper wrapper = new FixedInventoryVanillaWrapper(inventory);
                list.add(wrapper.getInsertable(inventory.getInvAvailableSlots(direction.getOpposite())));
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

        return IItemInsertable.ATTRIBUTE_INSERTABLE.combine(list);
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
            attributeBlock.addAllAttributesFromDirection(world, pos, state, IItemExtractable.ATTRIBUTE_EXTRACTABLE,
                list, direction);
        } else if (block instanceof IDelegatingAttributeBlock) {

            for (IAttributeProvider provider : ((IDelegatingAttributeBlock) block).getAttributeProviders(world, pos,
                state)) {
                IItemExtractable inv = provider.getAttribute(IItemExtractable.ATTRIBUTE_EXTRACTABLE);
                if (inv != null && inv != EmptyItemExtractable.NULL_EXTRACTABLE) {
                    list.add(inv);
                }
            }

        } else
        // Vanilla wrappers
        if (block instanceof InventoryProvider) {
            InventoryProvider provider = (InventoryProvider) block;
            SidedInventory inventory = provider.getInventory(state, world, pos);
            if (inventory != null) {
                FixedInventoryVanillaWrapper wrapper = new FixedInventoryVanillaWrapper(inventory);
                list.add(wrapper.getExtractable(inventory.getInvAvailableSlots(direction.getOpposite())));
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

        return IItemExtractable.ATTRIBUTE_EXTRACTABLE.combine(list);
    }

    // #######################
    // Internals
    // #######################

}
