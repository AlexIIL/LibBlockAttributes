/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.GlassBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil.FluidTankInteraction;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil.ItemContainerStatus;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFluidExtractable;
import alexiil.mc.lib.attributes.fluid.impl.RejectingFluidInsertable;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.NullVariant;
import alexiil.mc.lib.attributes.misc.PlayerInvUtil;
import alexiil.mc.lib.attributes.misc.Reference;

/** 4 base methods for interacting a {@link Reference} of an {@link ItemStack} with a {@link FixedFluidInv},
 * {@link FluidTransferable}, or {@link FluidInsertable}&{@link FluidExtractable} pair.
 * <p>
 * The 4 main methods are:
 * <ol>
 * <li>{@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)
 * interactItemWithTank(...)}: which is the base method for interacting anything with a tank (and doesn't do anything
 * special)</li>
 * <li>{@link #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount)
 * interactWithTank(...)}: Which uses interactItemWithTank internally, but adds excess items to the player's inventory
 * as well as playing a sound.</li>
 * <li>{@link #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount)
 * interactHandWithTank(...)}: Which uses interactWithTank internally, and interacts with the player's current held
 * item. (This is the first method that doesn't require the caller to create a {@link Reference} themselves).</li>
 * <li>{@link #interactCursorWithTank(FluidInsertable, FluidExtractable, ServerPlayerEntity, FluidFilter, FluidAmount)
 * interactCursorWithTank(...)}: Which uses interactWithTank internally, and interacts with the player's cursor stack
 * (what the player has while they have a {@link HandledScreen} open). (This is the second method that doesn't require
 * the caller to create a {@link Reference} themselves).</li>
 * </ol>
 */
public final class FluidInvUtil {
    private FluidInvUtil() {}

    private static final Consumer<ItemStack> ITEM_VOID = item -> {
        // Drop the stack
    };

    /*
     * What follows is just 4 methods, with 12 variants fully multiplied out.
     *
     * If a future maintainer needs to modify these methods, or invalidate
     * some of the javadoc then I would recommend either updating *everything*
     * or removing all javadoc except for 4 methods that take all of the args
     * (but leaving the "@see" sections to the main method).
     */

    // ###########################
    // interactHandWithTank
    // ###########################
    // inv = (FixedFluidInv) | (FluidTransferable) | (FluidInsertable+FluidExtractable)
    // ref = (Reference<ItemStack>)
    // player = (PlayerEntity)
    // hand = (Hand)
    // filter = () | (FluidFilter)
    // maximum = () | (FluidAmount)
    // ###########################

    // #############
    // FixedFluidInv
    // #############

    /** This is the "interactHandWithTank" variant that takes a single {@link FixedFluidInv} and doesn't limit what
     * fluid is moved, or how much fluid is moved.
     * <p>
     * Attempts to either fill the inventory from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or drain
     * the inventory to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactHandWithTank(FixedFluidInv inv, PlayerEntity player, Hand hand) {
        return interactHandWithTank(inv, player, hand, null, null);
    }

    /** This is the "interactHandWithTank" variant that takes a single {@link FixedFluidInv} and doesn't limit what
     * fluid is moved.
     * <p>
     * Attempts to either fill the inventory from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or drain
     * the inventory to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactHandWithTank(
        FixedFluidInv inv, PlayerEntity player, Hand hand, FluidAmount maximum
    ) {
        return interactHandWithTank(inv, player, hand, null, maximum);
    }

    /** This is the "interactHandWithTank" variant that takes a single {@link FixedFluidInv} and doesn't limit the
     * maximum amount of fluid moved.
     * <p>
     * Attempts to either fill the inventory from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or drain
     * the inventory to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactHandWithTank(
        FixedFluidInv inv, PlayerEntity player, Hand hand, FluidFilter filter
    ) {
        return interactHandWithTank(inv, player, hand, filter, null);
    }

    /** This is the "interactHandWithTank" variant that takes a single {@link FixedFluidInv}.
     * <p>
     * Attempts to either fill the inventory from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or drain
     * the inventory to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactHandWithTank(
        FixedFluidInv inv, PlayerEntity player, Hand hand, FluidFilter filter, FluidAmount maximum
    ) {
        return interactHandWithTank(inv.getTransferable(), player, hand, filter, maximum);
    }

    // #################
    // FluidTransferable
    // #################

    /** This is the "interactHandWithTank" variant that takes a single {@link FluidTransferable} and doesn't limit what
     * fluid is moved, or how much fluid is moved.
     * <p>
     * Attempts to either fill the transferable from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or
     * drain the transferable to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactHandWithTank(FluidTransferable inv, PlayerEntity player, Hand hand) {
        return interactHandWithTank(inv, inv, player, hand, null, null);
    }

    /** This is the "interactHandWithTank" variant that takes a single {@link FluidTransferable} and doesn't limit what
     * fluid is moved.
     * <p>
     * Attempts to either fill the transferable from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or
     * drain the transferable to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactHandWithTank(
        FluidTransferable inv, PlayerEntity player, Hand hand, FluidAmount maximum
    ) {
        return interactHandWithTank(inv, inv, player, hand, null, maximum);
    }

    /** This is the "interactHandWithTank" variant that takes a single {@link FluidTransferable} and doesn't limit the
     * maximum amount of fluid moved.
     * <p>
     * Attempts to either fill the transferable from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or
     * drain the transferable to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactHandWithTank(
        FluidTransferable inv, PlayerEntity player, Hand hand, FluidFilter filter
    ) {
        return interactHandWithTank(inv, inv, player, hand, filter, null);
    }

    /** This is the "interactHandWithTank" variant that takes a single {@link FluidTransferable}.
     * <p>
     * Attempts to either fill the transferable from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or
     * drain the transferable to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactHandWithTank(
        FluidTransferable inv, PlayerEntity player, Hand hand, FluidFilter filter, FluidAmount maximum
    ) {
        return interactHandWithTank(inv, inv, player, hand, filter, maximum);
    }

    // ##################
    // FluidInsertable
    // + FluidExtractable
    // ##################

    /** This is the "interactHandWithTank" variant that doesn't limit what fluid is moved, or how much fluid is moved.
     * <p>
     * Attempts to either fill the insertable from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or drain
     * the extractable to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactHandWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, PlayerEntity player, Hand hand
    ) {
        return interactHandWithTank(invInsert, invExtract, player, hand, null, null);
    }

    /** This is the "interactHandWithTank" variant that doesn't limit what fluid is moved.
     * <p>
     * Attempts to either fill the insertable from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or drain
     * the extractable to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactHandWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, PlayerEntity player, Hand hand,
        FluidAmount maximum
    ) {
        return interactHandWithTank(invInsert, invExtract, player, hand, null, maximum);
    }

    /** This is the "interactHandWithTank" variant that doesn't limit the maximum amount of fluid moved.
     * <p>
     * Attempts to either fill the insertable from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or drain
     * the extractable to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactHandWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, PlayerEntity player, Hand hand,
        FluidFilter filter
    ) {
        return interactHandWithTank(invInsert, invExtract, player, hand, filter, null);
    }

    /** Attempts to either fill the insertable from the player's {@link PlayerEntity#getStackInHand(Hand) hand}, or
     * drain the extractable to the hand. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods. */
    public static FluidTankInteraction interactHandWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, PlayerEntity player, Hand hand,
        FluidFilter filter, FluidAmount maximum
    ) {
        Reference<ItemStack> stack = PlayerInvUtil.referenceHand(player, hand);
        return interactWithTank(invInsert, invExtract, player, stack, filter, maximum);
    }

    // ###########################
    // interactCursorWithTank
    // ###########################
    // inv = (FixedFluidInv) | (FluidTransferable) | (FluidInsertable+FluidExtractable)
    // player = (ServerPlayerEntity)
    // filter = () | (FluidFilter)
    // maximum = () | (FluidAmount)
    // ###########################

    // #############
    // FixedFluidInv
    // #############

    /** This is the "interactCursorWithTank" variant that takes a single {@link FixedFluidInv} and doesn't limit what
     * fluid is moved, or how much fluid is moved.
     * <p>
     * Attempts to either fill the inventory from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * inventory to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods. */
    public static FluidTankInteraction interactCursorWithTank(FixedFluidInv inv, ServerPlayerEntity player) {
        return interactCursorWithTank(inv, player, null, null);
    }

    /** This is the "interactCursorWithTank" variant that takes a single {@link FixedFluidInv} and doesn't limit what
     * fluid is moved.
     * <p>
     * Attempts to either fill the inventory from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * inventory to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods. */
    public static FluidTankInteraction interactCursorWithTank(
        FixedFluidInv inv, ServerPlayerEntity player, FluidAmount maximum
    ) {
        return interactCursorWithTank(inv, player, null, maximum);
    }

    /** This is the "interactCursorWithTank" variant that takes a single {@link FixedFluidInv} and doesn't limit the
     * maximum amount of fluid moved.
     * <p>
     * Attempts to either fill the inventory from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * inventory to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactCursorWithTank(FluidInsertable, FluidExtractable, ServerPlayerEntity, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactCursorWithTank(
        FixedFluidInv inv, ServerPlayerEntity player, FluidFilter filter
    ) {
        return interactCursorWithTank(inv, player, filter, null);
    }

    /** This is the "interactCursorWithTank" variant that takes a single {@link FixedFluidInv}.
     * <p>
     * Attempts to either fill the inventory from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * inventory to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactCursorWithTank(FluidInsertable, FluidExtractable, ServerPlayerEntity, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactCursorWithTank(
        FixedFluidInv inv, ServerPlayerEntity player, FluidFilter filter, FluidAmount maximum
    ) {
        return interactCursorWithTank(inv.getTransferable(), player, filter, maximum);
    }

    // #################
    // FluidTransferable
    // #################

    /** This is the "interactCursorWithTank" variant that takes a single {@link FluidTransferable} and doesn't limit
     * what fluid is moved, or how much fluid is moved.
     * <p>
     * Attempts to either fill the insertable from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * extractable to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods. */
    public static FluidTankInteraction interactCursorWithTank(FluidTransferable inv, ServerPlayerEntity player) {
        return interactCursorWithTank(inv, inv, player, null, null);
    }

    /** This is the "interactCursorWithTank" variant that takes a single {@link FluidTransferable} and doesn't limit
     * what fluid is moved.
     * <p>
     * Attempts to either fill the transferable from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * transferable to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods. */
    public static FluidTankInteraction interactCursorWithTank(
        FluidTransferable inv, ServerPlayerEntity player, FluidAmount maximum
    ) {
        return interactCursorWithTank(inv, inv, player, null, maximum);
    }

    /** This is the "interactCursorWithTank" variant that takes a single {@link FluidTransferable} and doesn't limit the
     * maximum amount of fluid moved.
     * <p>
     * Attempts to either fill the insertable from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * extractable to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactCursorWithTank(FluidInsertable, FluidExtractable, ServerPlayerEntity, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactCursorWithTank(
        FluidTransferable inv, ServerPlayerEntity player, FluidFilter filter
    ) {
        return interactCursorWithTank(inv, inv, player, filter, null);
    }

    /** This is the "interactCursorWithTank" variant that takes a single {@link FluidTransferable}.
     * <p>
     * Attempts to either fill the insertable from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * extractable to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactCursorWithTank(FluidInsertable, FluidExtractable, ServerPlayerEntity, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactCursorWithTank(
        FluidTransferable inv, ServerPlayerEntity player, FluidFilter filter, FluidAmount maximum
    ) {
        return interactCursorWithTank(inv, inv, player, filter, maximum);
    }

    // ##################
    // FluidInsertable
    // + FluidExtractable
    // ##################

    /** This is the "interactCursorWithTank" variant that doesn't limit what fluid is moved, or how much fluid is moved.
     * <p>
     * Attempts to either fill the insertable from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * extractable to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods. */
    public static FluidTankInteraction interactCursorWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, ServerPlayerEntity player
    ) {
        return interactCursorWithTank(invInsert, invExtract, player, null, null);
    }

    /** This is the "interactCursorWithTank" variant that doesn't limit what fluid is moved.
     * <p>
     * Attempts to either fill the insertable from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * extractable to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods. */
    public static FluidTankInteraction interactCursorWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, ServerPlayerEntity player,
        FluidAmount maximum
    ) {
        return interactCursorWithTank(invInsert, invExtract, player, null, maximum);
    }

    /** This is the "interactCursorWithTank" variant that doesn't limit the maximum amount of fluid moved.
     * <p>
     * Attempts to either fill the insertable from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * extractable to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactCursorWithTank(FluidInsertable, FluidExtractable, ServerPlayerEntity, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactCursorWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, ServerPlayerEntity player,
        FluidFilter filter
    ) {
        return interactCursorWithTank(invInsert, invExtract, player, filter, null);
    }

    /** Attempts to either fill the insertable from the player's {@link PlayerInventory#getCursorStack()}, or drain the
     * extractable to the cursor stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the cursor stack won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods. */
    public static FluidTankInteraction interactCursorWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, ServerPlayerEntity player,
        FluidFilter filter, FluidAmount maximum
    ) {
        Reference<ItemStack> stack = PlayerInvUtil.referenceGuiCursor(player);
        return interactWithTank(invInsert, invExtract, player, stack, filter, maximum);
    }

    // ###########################
    // interactWithTank
    // ###########################
    // inv = (FixedFluidInv) | (FluidTransferable) | (FluidInsertable+FluidExtractable)
    // ref = (Reference<ItemStack>)
    // excess = (LimitedConsumer)
    // player = (PlayerEntity)
    // filter = () | (FluidFilter)
    // maximum = () | (FluidAmount)
    // ###########################

    // #############
    // FixedFluidInv
    // #############

    /** This is the "interactWithTank" variant that takes a single {@link FixedFluidInv}, and doesn't limit what fluid
     * is moved, or the amount of fluid moved.
     * <p>
     * Attempts to either fill the inventory from the provided stack reference, or drain the inventory to the provided
     * stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactWithTank(
        FixedFluidInv inv, PlayerEntity player, Reference<ItemStack> stack
    ) {
        return interactWithTank(inv, player, stack, null, null);
    }

    /** This is the "interactWithTank" variant that takes a single {@link FixedFluidInv}, and doesn't limit what fluid
     * is moved.
     * <p>
     * Attempts to either fill the inventory from the provided stack reference, or drain the inventory to the provided
     * stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactWithTank(
        FixedFluidInv inv, PlayerEntity player, Reference<ItemStack> stack, FluidAmount maximum
    ) {
        return interactWithTank(inv, player, stack, null, maximum);
    }

    /** This is the "interactWithTank" variant that takes a single {@link FixedFluidInv}, and doesn't limit the amount
     * of fluid moved.
     * <p>
     * Attempts to either fill the inventory from the provided stack reference, or drain the inventory to the provided
     * stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactWithTank(
        FixedFluidInv inv, PlayerEntity player, Reference<ItemStack> stack, FluidFilter filter
    ) {
        return interactWithTank(inv, player, stack, filter, null);
    }

    /** This is the "interactWithTank" variant that takes a single {@link FixedFluidInv}.
     * <p>
     * Attempts to either fill the inventory from the provided stack reference, or drain the inventory to the provided
     * stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactWithTank(
        FixedFluidInv inv, PlayerEntity player, Reference<ItemStack> stack, FluidFilter filter, FluidAmount maximum
    ) {
        return interactWithTank(inv.getTransferable(), player, stack, filter, maximum);
    }

    // #################
    // FluidTransferable
    // #################

    /** This is the "interactWithTank" variant that takes a single {@link FluidTransferable}, and doesn't limit what
     * fluid is moved, or the amount of fluid moved.
     * <p>
     * Attempts to either fill the transferable from the provided stack reference, or drain the transferable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactWithTank(
        FluidTransferable inv, PlayerEntity player, Reference<ItemStack> stack
    ) {
        return interactWithTank(inv, inv, player, stack, null, null);
    }

    /** This is the "interactWithTank" variant that takes a single {@link FluidTransferable}, and doesn't limit what
     * fluid is moved.
     * <p>
     * Attempts to either fill the transferable from the provided stack reference, or drain the transferable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactWithTank(
        FluidTransferable inv, PlayerEntity player, Reference<ItemStack> stack, FluidAmount maximum
    ) {
        return interactWithTank(inv, inv, player, stack, null, maximum);
    }

    /** This is the "interactWithTank" variant that takes a single {@link FluidTransferable}, and doesn't limit the
     * amount of fluid moved.
     * <p>
     * Attempts to either fill the transferable from the provided stack reference, or drain the transferable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactWithTank(
        FluidTransferable inv, PlayerEntity player, Reference<ItemStack> stack, FluidFilter filter
    ) {
        return interactWithTank(inv, inv, player, stack, filter, null);
    }

    /** This is the "interactWithTank" variant that takes a single {@link FluidTransferable}.
     * <p>
     * Attempts to either fill the transferable from the provided stack reference, or drain the transferable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactWithTank(
        FluidTransferable inv, PlayerEntity player, Reference<ItemStack> stack, FluidFilter filter, FluidAmount maximum
    ) {
        return interactWithTank(inv, inv, player, stack, filter, maximum);
    }

    // ##################
    // FluidInsertable
    // + FluidExtractable
    // ##################

    /** This is the "interactWithTank" variant that doesn't limit what fluid is moved, or the amount of fluid moved.
     * <p>
     * Attempts to either fill the insertable from the provided stack reference, or drain the extractable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, PlayerEntity player,
        Reference<ItemStack> stack
    ) {
        return interactWithTank(invInsert, invExtract, player, stack, null, null);
    }

    /** This is the "interactWithTank" variant that doesn't limit the amount of fluid moved.
     * <p>
     * Attempts to either fill the insertable from the provided stack reference, or drain the extractable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, PlayerEntity player,
        Reference<ItemStack> stack, FluidFilter filter
    ) {
        return interactWithTank(invInsert, invExtract, player, stack, filter, null);
    }

    /** This is the "interactWithTank" variant that doesn't limit the fluid moved.
     * <p>
     * Attempts to either fill the insertable from the provided stack reference, or drain the extractable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference, FluidFilter, FluidAmount) */
    public static FluidTankInteraction interactWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, PlayerEntity player,
        Reference<ItemStack> stack, FluidAmount maximum
    ) {
        return interactWithTank(invInsert, invExtract, player, stack, null, maximum);
    }

    /** Attempts to either fill the insertable from the provided stack reference, or drain the extractable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * Unlike
     * {@link #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter, FluidAmount)}
     * this will:
     * <ul>
     * <li>Play a sound for filling/draining either a bottle or a bucket.</li>
     * <li>Add excess items directly to the player's inventory (or voids them if the player is in creative mode)</li>
     * <li>If the player is in creative mode then the stack reference given won't be modified.</li>
     * </ul>
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid. (Unless the player is in creative mode).
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods. */
    public static FluidTankInteraction interactWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, PlayerEntity player,
        Reference<ItemStack> stack, FluidFilter filter, FluidAmount maximum
    ) {
        ItemStack mainStack = stack.get();
        if (mainStack.isEmpty()) {
            return FluidTankInteraction.NONE;
        }
        boolean isCreative = player.abilities.creativeMode;
        Reference<ItemStack> realRef = stack;
        if (isCreative) {
            realRef = Reference.callable(stack::get, ITEM_VOID, s -> true);
        }
        Consumer<ItemStack> stackConsumer = isCreative ? ITEM_VOID : PlayerInvUtil.createPlayerInsertable(player);
        LimitedConsumer<ItemStack> excess = LimitedConsumer.fromConsumer(stackConsumer);
        FluidTankInteraction result = interactItemWithTank(invInsert, invExtract, realRef, excess, filter, maximum);
        if (!result.didMoveAny()) {
            return result;
        }
        final SoundEvent soundEvent;
        if (result.fluidMoved.fluidKey == FluidKeys.LAVA) {
            soundEvent = result.intoTank ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_FILL_LAVA;
        } else {
            Item item = mainStack.getItem();
            boolean isBottle = item instanceof GlassBottleItem || item instanceof PotionItem;
            if (isBottle) {
                soundEvent = result.intoTank ? SoundEvents.ITEM_BOTTLE_EMPTY : SoundEvents.ITEM_BOTTLE_FILL;
            } else {
                soundEvent = result.intoTank ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BUCKET_FILL;
            }
        }
        player.playSound(soundEvent, SoundCategory.BLOCKS, 1.0f, 1.0f);
        return result;
    }

    // ###########################
    // interactItemWithTank
    // ###########################
    // inv = (FixedFluidInv) | (FluidTransferable) | (FluidInsertable+FluidExtractable)
    // ref = (Reference<ItemStack>)
    // excess = (LimitedConsumer)
    // filter = () | (FluidFilter)
    // maximum = () | (FluidAmount)
    // ###########################

    // #############
    // FixedFluidInv
    // #############

    /** This is the "interactItemWithTank" variant that uses a single {@link FixedFluidInv} for the
     * insertable/extractable tanks, and doesn't limit the fluid moved, or the amount moved.
     * <p>
     * Attempts to either fill the inventory from the provided stack reference, or drain the inventory to the provided
     * stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter,
     *      FluidAmount) */
    public static FluidTankInteraction interactItemWithTank(
        FixedFluidInv inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks
    ) {
        return interactItemWithTank(inv, stack, excessStacks, null, null);
    }

    /** This is the "interactItemWithTank" variant that uses a single {@link FixedFluidInv} for the
     * insertable/extractable tanks, and doesn't limit the amount of fluid moved.
     * <p>
     * Attempts to either fill the inventory from the provided stack reference, or drain the inventory to the provided
     * stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter,
     *      FluidAmount) */
    public static FluidTankInteraction interactItemWithTank(
        FixedFluidInv inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks, FluidFilter filter
    ) {
        return interactItemWithTank(inv, stack, excessStacks, filter, null);
    }

    /** This is the "interactItemWithTank" variant that uses a single {@link FixedFluidInv} for the
     * insertable/extractable tanks, and doesn't limit what fluid is moved.
     * <p>
     * Attempts to either fill the inventory from the provided stack reference, or drain the inventory to the provided
     * stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter,
     *      FluidAmount) */
    public static FluidTankInteraction interactItemWithTank(
        FixedFluidInv inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks, FluidAmount maximum
    ) {
        return interactItemWithTank(inv, stack, excessStacks, null, maximum);
    }

    /** This is the "interactItemWithTank" variant that uses a single {@link FixedFluidInv} for the
     * insertable/extractable tanks.
     * <p>
     * Attempts to either fill the inventory from the provided stack reference, or drain the inventory to the provided
     * stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param inv The fluid inventory to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter,
     *      FluidAmount) */
    public static FluidTankInteraction interactItemWithTank(
        FixedFluidInv inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks, FluidFilter filter,
        FluidAmount maximum
    ) {
        return interactItemWithTank(inv.getTransferable(), stack, excessStacks, filter, maximum);
    }

    // #################
    // FluidTransferable
    // #################

    /** This is the "interactItemWithTank" variant that uses a single {@link FluidTransferable} for the
     * insertable/extractable tanks, and doesn't limit what fluid is moved, or how much is moved.
     * <p>
     * Attempts to either fill the transferable from the provided stack reference, or drain the transferable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter,
     *      FluidAmount) */
    public static FluidTankInteraction interactItemWithTank(
        FluidTransferable inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks
    ) {
        return interactItemWithTank(inv, inv, stack, excessStacks, null, null);
    }

    /** This is the "interactItemWithTank" variant that uses a single {@link FluidTransferable} for the
     * insertable/extractable tanks, and doesn't limit the amount moved.
     * <p>
     * Attempts to either fill the transferable from the provided stack reference, or drain the transferable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter,
     *      FluidAmount) */
    public static FluidTankInteraction interactItemWithTank(
        FluidTransferable inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks, FluidFilter filter
    ) {
        return interactItemWithTank(inv, inv, stack, excessStacks, filter, null);
    }

    /** This is the "interactItemWithTank" variant that uses a single {@link FluidTransferable} for the
     * insertable/extractable tanks, and doesn't filter what is moved.
     * <p>
     * Attempts to either fill the transferable from the provided stack reference, or drain the transferable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter,
     *      FluidAmount) */
    public static FluidTankInteraction interactItemWithTank(
        FluidTransferable inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks, FluidAmount maximum
    ) {
        return interactItemWithTank(inv, inv, stack, excessStacks, null, maximum);
    }

    /** This is the "interactItemWithTank" variant that uses a single {@link FluidTransferable} for the
     * insertable/extractable tanks.
     * <p>
     * Attempts to either fill the transferable from the provided stack reference, or drain the transferable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param inv The fluid transferable to interact with (referred to as "the tank").
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter,
     *      FluidAmount) */
    public static FluidTankInteraction interactItemWithTank(
        FluidTransferable inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks, FluidFilter filter,
        FluidAmount maximum
    ) {
        return interactItemWithTank(inv, inv, stack, excessStacks, filter, maximum);
    }

    // ##################
    // FluidInsertable
    // + FluidExtractable
    // ##################

    /** This is the "interactItemWithTank" variant that doesn't limit what fluid is moved, or the amount of fluid that
     * is moved.
     * <p>
     * Attempts to either fill the {inventory/transferable/insertable} from the provided stack reference, or drain the
     * {inventory/transferable/extractable} to the provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter,
     *      FluidAmount) */
    public static FluidTankInteraction interactItemWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, Reference<ItemStack> stack,
        LimitedConsumer<ItemStack> excessStacks
    ) {
        return interactItemWithTank(invInsert, invExtract, stack, excessStacks, null, null);
    }

    /** This is the "interactItemWithTank" variant that doesn't limit the amount of fluid moved.
     * <p>
     * Attempts to either fill the insertable from the provided stack reference, or drain the extractable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter,
     *      FluidAmount) */
    public static FluidTankInteraction interactItemWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, Reference<ItemStack> stack,
        LimitedConsumer<ItemStack> excessStacks, FluidFilter filter
    ) {
        return interactItemWithTank(invInsert, invExtract, stack, excessStacks, filter, null);
    }

    /** This is the "interactItemWithTank" variant that doesn't filter what is moved.
     * <p>
     * Attempts to either fill the insertable from the provided stack reference, or drain the extractable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods.
     * @see #interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidFilter,
     *      FluidAmount) */
    public static FluidTankInteraction interactItemWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, Reference<ItemStack> stack,
        LimitedConsumer<ItemStack> excessStacks, FluidAmount maximum
    ) {
        return interactItemWithTank(invInsert, invExtract, stack, excessStacks, null, maximum);
    }

    // ##############
    // Implementation
    // ##############

    /** Attempts to either fill the insertable from the provided stack reference, or drain the extractable to the
     * provided stack. Internally this uses
     * ({@link FluidAttributes#INSERTABLE}/{@link FluidAttributes#EXTRACTABLE}).{@link CombinableAttribute#get(Reference, LimitedConsumer)
     * get}(stack, excessStacks) to get the {@link FluidExtractable}/{@link FluidInsertable} from the item to extract
     * from or insert to.
     * <p>
     * For example, you could have a machine that fills it's internal fluid tank from a slot containing
     * {@link ItemStack}s. If the machine has both an inventory for incoming items that contain fluid and an inventory
     * for outgoing items then this could be called once for each item in the incoming inventory with the
     * {@link Reference} being the current {@link ItemStack} to drain from, and the {@link LimitedConsumer} being an
     * inserter (perhaps an ItemInsertable from LBA's item module) to the outgoing inventory. (This does make the
     * assumption that the incoming inventory filters the incoming items to only accept items which can have fluid
     * drained from them).
     * 
     * @param invInsert The fluid insertable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to extract from the item, and
     *            {@link FluidTankInteraction#fromTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param invExtract The fluid extractable to interact with (referred to as "the tank"). If this is null (or
     *            implements {@link NullVariant}) then this will not attempt to insert into the item, and
     *            {@link FluidTankInteraction#intoTankStatus} will be {@link ItemContainerStatus#NOT_CHECKED}.
     * @param stack The {@link Reference} holding an {@link ItemStack} to interact with. If the reference doesn't allow
     *            modification then this will always fail to move any fluid.
     * @param excessStacks The {@link LimitedConsumer} to take any excess {@link ItemStack}'s that can't go back into
     *            the provided reference.
     * @param filter A filter to limit what {@link FluidKey} may be moved.
     * @param maximum The maximum amount of fluid that may be moved.
     * @return A {@link FluidTankInteraction} with some information about what happened:
     *         <ul>
     *         <li>{@link FluidTankInteraction#fluidMoved} for a copy of the fluid moved.</li>
     *         <li>{@link FluidTankInteraction#intoTank} will be true if fluid was extracted from the item and inserted
     *         into the tank, and false otherwise.</li>
     *         <li>{@link FluidTankInteraction#intoTankStatus} will have the status of the item's
     *         {@link FluidExtractable}.</li>
     *         <li>{@link FluidTankInteraction#fromTankStatus} will have the status of the item's
     *         {@link FluidInsertable}.</li>
     *         </ul>
     *         The method {@link FluidTankInteraction#didMoveAny()} is recommended for checking to see if anything was
     *         moved.
     *         <p>
     *         The method {@link FluidTankInteraction#asActionResult()} is recommended for converting the result into an
     *         {@link ActionResult}, suitable for normal block or item "use" methods. */
    public static FluidTankInteraction interactItemWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, Reference<ItemStack> stack,
        LimitedConsumer<ItemStack> excessStacks, FluidFilter filter, FluidAmount maximum
    ) {
        // Even though neither FluidFilter nor FluidAmount are specified as @Nullable
        // they should still accept nulls to make calling them simpler

        if (invInsert instanceof NullVariant) {
            invInsert = null;
        }
        if (invExtract instanceof NullVariant) {
            invExtract = null;
        }
        ItemContainerStatus fromTankStatus = ItemContainerStatus.NOT_CHECKED;
        ItemContainerStatus intoTankStatus = ItemContainerStatus.NOT_CHECKED;
        if (invExtract != null) {
            FluidInsertable itemDest = FluidAttributes.INSERTABLE.get(stack, excessStacks);
            if (itemDest != RejectingFluidInsertable.NULL) {
                fromTankStatus = ItemContainerStatus.VALID;
                FluidVolume fluidMoved = FluidVolumeUtil.move(invExtract, itemDest, filter, maximum);
                if (!fluidMoved.isEmpty()) {
                    return new FluidTankInteraction(fluidMoved, false, intoTankStatus, fromTankStatus);
                }
            } else {
                fromTankStatus = ItemContainerStatus.INVALID;
            }
        }
        if (invInsert != null) {
            FluidExtractable itemSrc = FluidAttributes.EXTRACTABLE.get(stack, excessStacks);
            if (itemSrc != EmptyFluidExtractable.NULL) {
                intoTankStatus = ItemContainerStatus.VALID;
                FluidVolume fluidMoved = FluidVolumeUtil.move(itemSrc, invInsert, filter, maximum);
                if (fluidMoved != null) {
                    return new FluidTankInteraction(fluidMoved, true, intoTankStatus, fromTankStatus);
                }
            } else {
                intoTankStatus = ItemContainerStatus.INVALID;
            }
        }
        return FluidTankInteraction.none(intoTankStatus, fromTankStatus);
    }

    // #########################
    // End of interactItemWithTank()
    // #########################
}
