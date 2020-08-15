/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.util.function.Predicate;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;

/** Base interface to add attributes for {@link BlockEntity}s that you haven't defined.
 * <p>
 * There is an additional {@link FunctionalInterface} variant of this: {@link BlockEntityAttributeAdderFN}.
 * Implementations must be registered to {@link Attribute} in order to be used, in any of these methods:
 * <ul>
 * <li>
 * {@link Attribute#addBlockEntityPredicateAdder(AttributeSourceType, boolean, Predicate, BlockEntityAttributeAdderFN)}.
 * </li>
 * <li>{@link Attribute#putBlockEntityClassAdder(AttributeSourceType, Class, boolean, BlockEntityAttributeAdderFN)}</li>
 * <li>{@link Attribute#setBlockEntityAdder(AttributeSourceType, BlockEntityType, BlockEntityAttributeAdder)}</li>
 * <li>
 * {@link Attribute#setBlockEntityAdder(AttributeSourceType, BlockEntityType, Class, BlockEntityAttributeAdderFN)}</li>
 * <li>{@link Attribute#setBlockEntityAdderFN(AttributeSourceType, BlockEntityType, BlockEntityAttributeAdderFN)}</li>
 * </ul>
 *
 * @param <AT> The attribute type.
 * @param <BE> The BlockEntity target type. (This doesn't extend {@link BlockEntity} so you can target interfaces that
 *            block entities may implement). */
public interface BlockEntityAttributeAdder<AT, BE> {
    /* Note that we do have the type parameter (unlike AttributeProvider) because instances are registered to a specific
     * Attribute so it's actually useful for implementors. */

    /** Adds every attribute instance to the given list that the block entity itself cannot be expected to add support
     * for. */
    void addAll(BE blockEntity, AttributeList<AT> to);

    /** @return The target {@link Class} for the {@link BlockEntity}. This doesn't extend {@link BlockEntity} so you can
     *         target interfaces that block entities may implement). */
    Class<BE> getBlockEntityClass();

    /** {@link FunctionalInterface} version of {@link BlockEntityAttributeAdder}.
     * <p>
     * You can convert instances of this into the full {@link BlockEntityAttributeAdder} via
     * {@link BlockEntityAttributeAdder#ofBasic(BlockEntityAttributeAdderFN)} or
     * {@link BlockEntityAttributeAdder#ofTyped(Class, BlockEntityAttributeAdderFN)} . */
    @FunctionalInterface
    public interface BlockEntityAttributeAdderFN<AT, BE> {

        /** Adds every attribute instance to the given list that the block entity itself cannot be expected to add
         * support for. */
        void addAll(BE blockEntity, AttributeList<AT> to);
    }

    /** Creates a full {@link BlockEntityAttributeAdder} from the {@link FunctionalInterface} variant, but only allows
     * targeting {@link BlockEntity} directly. */
    public static <AT> BlockEntityAttributeAdder<AT, BlockEntity> ofBasic(
        BlockEntityAttributeAdderFN<AT, BlockEntity> fn
    ) {
        return new BeAdderBase<>(BlockEntity.class, fn);
    }

    /** Creates a full {@link BlockEntityAttributeAdder} from the {@link FunctionalInterface} variant, and a
     * {@link Class} to target. */
    public static <AT, BE> BlockEntityAttributeAdder<AT, BE> ofTyped(
        Class<BE> clazz, BlockEntityAttributeAdderFN<AT, BE> fn
    ) {
        return new BeAdderBase<>(clazz, fn);
    }

    /** Base class for {@link BlockEntityAttributeAdderFN} conversions. Most of the time you are recommened to use
     * {@link BlockEntityAttributeAdder#ofBasic(BlockEntityAttributeAdderFN)} or
     * {@link BlockEntityAttributeAdder#ofTyped(Class, BlockEntityAttributeAdderFN)} instead of this directly. */
    public static class BeAdderBase<AT, BE> implements BlockEntityAttributeAdder<AT, BE> {
        private final Class<BE> clazz;
        private final BlockEntityAttributeAdderFN<AT, BE> fn;

        public BeAdderBase(Class<BE> clazz, BlockEntityAttributeAdderFN<AT, BE> fn) {
            this.clazz = clazz;
            this.fn = fn;
        }

        @Override
        public final Class<BE> getBlockEntityClass() {
            return clazz;
        }

        @Override
        public final void addAll(BE blockEntity, AttributeList<AT> to) {
            fn.addAll(blockEntity, to);
        }

        @Override
        public String toString() {
            return "{" + clazz + ": " + fn + "}";
        }
    }
}
