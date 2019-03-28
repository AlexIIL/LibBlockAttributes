package alexiil.mc.lib.attributes.item;

import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.Attributes;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInvView;
import alexiil.mc.lib.attributes.item.impl.CombinedItemExtractable;
import alexiil.mc.lib.attributes.item.impl.CombinedItemInsertable;
import alexiil.mc.lib.attributes.item.impl.CombinedItemInvStats;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;
import alexiil.mc.lib.attributes.item.impl.EmptyItemInvStats;
import alexiil.mc.lib.attributes.item.impl.FixedInventoryVanillaWrapper;
import alexiil.mc.lib.attributes.item.impl.FixedInventoryViewVanillaWrapper;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;

public enum ItemAttributes {
    ;

    public static final CombinableAttribute<FixedItemInvView> FIXED_INV_VIEW;
    public static final CombinableAttribute<FixedItemInv> FIXED_INV;
    public static final CombinableAttribute<ItemInvStats> INV_STATS;
    public static final CombinableAttribute<ItemInsertable> INSERTABLE;
    public static final CombinableAttribute<ItemExtractable> EXTRACTABLE;

    static {
        FIXED_INV_VIEW = Attributes.createCombinable(FixedItemInvView.class, EmptyFixedItemInv.INSTANCE,
            CombinedFixedItemInvView::new, (world, pos, state, list) -> {
                Block block = state.getBlock();

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
                        // Special case chests here, rather than through a mixin because it just simplifies
                        // everything

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
            });
        FIXED_INV = Attributes.createCombinable(FixedItemInv.class, EmptyFixedItemInv.INSTANCE,
            CombinedFixedItemInv::new, (world, pos, state, list) -> {
                Block block = state.getBlock();

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
                        // Special case chests here, rather than through a mixin because it just simplifies
                        // everything

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
            });

        // For some reason the java compiler can't work out what <T> should be for these three
        // So instead we create a lambda, which somehow gives it enough space to work out what it is.
        // (and yet eclipse had no problems with it :/ )
        INV_STATS = Attributes.createCombinable(ItemInvStats.class, EmptyItemInvStats.INSTANCE,
            list -> new CombinedItemInvStats(list), (world, pos, state, list) -> {

                Block block = state.getBlock();
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
                        // Special case chests here, rather than through a mixin because it just simplifies
                        // everything

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
            });
        INSERTABLE = Attributes.createCombinable(ItemInsertable.class, RejectingItemInsertable.NULL,
            list -> new CombinedItemInsertable(list), (world, pos, state, list) -> {
                Block block = state.getBlock();
                Direction direction = list.getSearchDirection();

                // Vanilla wrappers
                if (block instanceof HopperBlock && state.get(HopperBlock.FACING).getOpposite() == direction) {
                    // Explicitly don't add an insertable
                } else if (block instanceof InventoryProvider) {
                    InventoryProvider provider = (InventoryProvider) block;
                    SidedInventory inventory = provider.getInventory(state, world, pos);
                    if (inventory != null) {
                        if (inventory.getInvSize() > 0) {
                            FixedInventoryVanillaWrapper wrapper = new FixedInventoryVanillaWrapper(inventory);
                            if (direction == null) {
                                list.add(wrapper.getInsertable());
                            } else {
                                list.add(
                                    wrapper.getInsertable(inventory.getInvAvailableSlots(direction.getOpposite())));
                            }
                        } else {
                            list.add(RejectingItemInsertable.NULL);
                        }
                    }
                } else if (block.hasBlockEntity()) {
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof ChestBlockEntity) {
                        // Special case chests here, rather than through a mixin because it just simplifies
                        // everything

                        // method_17458: something like "get chest inventory"
                        Inventory chestInv = ChestBlock.method_17458(state, world, pos,
                            /* Check if the top is blocked by a solid block or a cat */false);
                        if (chestInv != null) {
                            list.add(new FixedInventoryVanillaWrapper(chestInv).getInsertable());
                        }
                    } else if (be instanceof SidedInventory) {
                        SidedInventory sidedInv = (SidedInventory) be;
                        if (direction != null) {
                            int[] slots = sidedInv.getInvAvailableSlots(direction.getOpposite());
                            list.add(new FixedInventoryVanillaWrapper(sidedInv).getInsertable(slots));
                        } else {
                            list.add(new FixedInventoryVanillaWrapper(sidedInv).getInsertable());
                        }
                    } else if (be instanceof Inventory) {
                        list.add(new FixedInventoryVanillaWrapper((Inventory) be).getInsertable());
                    }
                }
            });
        EXTRACTABLE = Attributes.createCombinable(ItemExtractable.class, EmptyItemExtractable.NULL,
            list -> new CombinedItemExtractable(list), (world, pos, state, list) -> {
                Block block = state.getBlock();
                Direction direction = list.getSearchDirection();
                // Vanilla wrappers
                if (block instanceof HopperBlock && state.get(HopperBlock.FACING) == direction) {
                    list.add(EmptyItemExtractable.SUPPLIER);
                } else if (block instanceof InventoryProvider) {
                    InventoryProvider provider = (InventoryProvider) block;
                    SidedInventory inventory = provider.getInventory(state, world, pos);
                    if (inventory != null) {
                        if (inventory.getInvSize() > 0) {
                            FixedInventoryVanillaWrapper wrapper = new FixedInventoryVanillaWrapper(inventory);
                            if (direction != null) {
                                list.add(
                                    wrapper.getExtractable(inventory.getInvAvailableSlots(direction.getOpposite())));
                            } else {
                                list.add(wrapper.getExtractable());
                            }
                        } else {
                            list.add(EmptyItemExtractable.NULL);
                        }
                    }
                } else if (block.hasBlockEntity()) {
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof ChestBlockEntity) {
                        // Special case chests here, rather than through a mixin because it just simplifies
                        // everything

                        // method_17458: something like "get chest inventory"
                        Inventory chestInv = ChestBlock.method_17458(state, world, pos,
                            /* Check if the top is blocked by a solid block or a cat */false);
                        if (chestInv != null) {
                            list.add(new FixedInventoryVanillaWrapper(chestInv).getExtractable());
                        }
                    } else if (be instanceof SidedInventory) {
                        SidedInventory sidedInv = (SidedInventory) be;
                        FixedInventoryVanillaWrapper fixedInv = new FixedInventoryVanillaWrapper(sidedInv);
                        if (direction != null) {
                            int[] slots = sidedInv.getInvAvailableSlots(direction.getOpposite());
                            list.add(fixedInv.getExtractable(slots));
                        } else {
                            list.add(fixedInv.getExtractable());
                        }
                    } else if (be instanceof Inventory) {
                        list.add(new FixedInventoryVanillaWrapper((Inventory) be).getExtractable());
                    }
                }
            });
    }
}
