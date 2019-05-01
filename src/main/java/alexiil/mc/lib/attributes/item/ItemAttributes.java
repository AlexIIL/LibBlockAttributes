package alexiil.mc.lib.attributes.item;

import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.attributes.Attributes;
import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.CustomAttributeAdder;
import alexiil.mc.lib.attributes.DefaultedAttribute;
import alexiil.mc.lib.attributes.item.compat.FixedInventoryVanillaWrapper;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInvView;
import alexiil.mc.lib.attributes.item.impl.CombinedGroupedItemInv;
import alexiil.mc.lib.attributes.item.impl.CombinedGroupedItemInvView;
import alexiil.mc.lib.attributes.item.impl.CombinedItemExtractable;
import alexiil.mc.lib.attributes.item.impl.CombinedItemInsertable;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyGroupedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;

public final class ItemAttributes {
    private ItemAttributes() {}

    public static final CombinableAttribute<FixedItemInvView> FIXED_INV_VIEW;
    public static final CombinableAttribute<FixedItemInv> FIXED_INV;
    public static final CombinableAttribute<GroupedItemInvView> GROUPED_INV_VIEW;
    public static final CombinableAttribute<GroupedItemInv> GROUPED_INV;
    public static final CombinableAttribute<ItemInsertable> INSERTABLE;
    public static final CombinableAttribute<ItemExtractable> EXTRACTABLE;

    static {
        FIXED_INV_VIEW = Attributes.createCombinable(//
            FixedItemInvView.class, //
            EmptyFixedItemInv.INSTANCE, //
            list -> new CombinedFixedItemInvView<>(list), //
            createFixedInvAdder(false, null, inv -> inv)//
        );
        FIXED_INV = Attributes.createCombinable(//
            FixedItemInv.class, //
            EmptyFixedItemInv.INSTANCE, //
            list -> new CombinedFixedItemInv<>(list), //
            createFixedInvAdder(false, null, Function.identity())//
        );
        GROUPED_INV_VIEW = Attributes.createCombinable(//
            GroupedItemInvView.class, //
            EmptyGroupedItemInv.INSTANCE, //
            list -> new CombinedGroupedItemInvView(list), //
            createFixedInvAdder(false, null, FixedItemInv::getGroupedInv)//
        );
        GROUPED_INV = Attributes.createCombinable(//
            GroupedItemInv.class, //
            EmptyGroupedItemInv.INSTANCE, //
            list -> new CombinedGroupedItemInv(list), //
            createFixedInvAdder(false, null, FixedItemInv::getGroupedInv)//
        );
        INSERTABLE = Attributes.createCombinable(//
            ItemInsertable.class, //
            RejectingItemInsertable.NULL, //
            list -> new CombinedItemInsertable(list), //
            createFixedInvAdder(true, null, FixedItemInv::getInsertable)//
        );
        EXTRACTABLE = Attributes.createCombinable(//
            ItemExtractable.class, //
            EmptyItemExtractable.NULL, //
            list -> new CombinedItemExtractable(list), //
            createFixedInvAdder(true, EmptyItemExtractable.SUPPLIER, FixedItemInv::getExtractable)//
        );
    }

    private static <T> CustomAttributeAdder<T> createFixedInvAdder(boolean specialCaseHoppers,
        @Nullable T hopperInstance, Function<FixedItemInv, T> getter) {
        return (world, pos, state, list) -> {
            Block block = state.getBlock();
            Direction direction = list.getSearchDirection();

            // Vanilla wrappers
            if (specialCaseHoppers && block instanceof HopperBlock && state.get(HopperBlock.FACING) == direction) {
                if (hopperInstance != null) {
                    list.add(hopperInstance);
                }
            } else if (block instanceof InventoryProvider) {
                InventoryProvider provider = (InventoryProvider) block;
                SidedInventory inventory = provider.getInventory(state, world, pos);
                if (inventory != null) {
                    if (inventory.getInvSize() > 0) {
                        FixedInventoryVanillaWrapper wrapper = new FixedInventoryVanillaWrapper(inventory);
                        if (direction != null) {
                            list.add(getter
                                .apply(wrapper.getMappedInv(inventory.getInvAvailableSlots(direction.getOpposite()))));
                        } else {
                            list.add(getter.apply(wrapper));
                        }
                    } else {
                        list.add(((DefaultedAttribute<T>) list.attribute).defaultValue);
                    }
                }
            } else if (block.hasBlockEntity()) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof ChestBlockEntity) {
                    // Special case chests here, rather than through a mixin because it just simplifies
                    // everything

                    Inventory chestInv = ChestBlock.getInventory(state, world, pos,
                        /* Check if the top is blocked by a solid block or a cat */false);
                    if (chestInv != null) {
                        list.add(getter.apply(new FixedInventoryVanillaWrapper(chestInv)));
                    }
                } else if (be instanceof SidedInventory) {
                    SidedInventory sidedInv = (SidedInventory) be;
                    FixedInventoryVanillaWrapper fixedInv = new FixedInventoryVanillaWrapper(sidedInv);
                    if (direction != null) {
                        int[] slots = sidedInv.getInvAvailableSlots(direction.getOpposite());
                        list.add(getter.apply(fixedInv.getMappedInv(slots)));
                    } else {
                        list.add(getter.apply(fixedInv));
                    }
                } else if (be instanceof Inventory) {
                    list.add(getter.apply(new FixedInventoryVanillaWrapper((Inventory) be)));
                }
            }
        };
    }
}
