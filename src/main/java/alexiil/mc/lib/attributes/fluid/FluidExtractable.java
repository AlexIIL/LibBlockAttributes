/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.lang.reflect.Method;
import java.math.RoundingMode;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.AttributeUtil;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.FilteredFluidExtractable;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.item.ItemStackUtil;

/** Defines an object that can have items extracted from it.
 * <p>
 * As of 0.6 implementations must override either {@link #attemptExtraction(FluidFilter, FluidAmount, Simulation)} or
 * {@link #attemptExtraction(FluidFilter, int, Simulation)}, as both implementations call each other by default. */
public interface FluidExtractable {

    /** Attempt to extract *any* {@link FluidVolume} from this that {@link FluidFilter#matches(FluidKey) matches} the
     * given {@link FluidFilter}.
     * 
     * @param filter
     * @param maxAmount The maximum amount of fluid that can be extracted. Negative numbers throw an exception.
     * @param simulation If {@link Simulation#SIMULATE} then this should return the same result that the exact same call
     *            with {@link Simulation#ACTION} would do, except that the filter can be made more specific between
     *            calls if the previously simulated extracted fluid is used as a filter.
     *            <p>
     *            For example the following code snippet should never throw an exception:
     *            <p>
     *            {@link FluidExtractable} from = // Some extractable <br>
     *            {@link FluidVolume} attempted = from.{@link FluidExtractable#attemptAnyExtraction
     *            attemptAnyExtraction}({@link Integer#MAX_VALUE Integer.MAX_VALUE}, {@link Simulation#SIMULATE
     *            Simulation.SIMULATE}); <br>
     *            if (attempted.{@link FluidVolume#isEmpty() isEmpty()}) return;<br>
     *            {@link FluidVolume} extracted = from.{@link FluidExtractable#extract(FluidKey, int)
     *            extract}(attempted.{@link FluidVolume#getFluidKey() getFluidKey()},
     *            attempted.{@link FluidVolume#getAmount() getAmount()}); <br>
     *            assert !extracted.{@link FluidVolume#isEmpty() isEmpty};<br>
     *            assert attempted.{@link FluidVolume#getAmount() getAmount()} ==
     *            extracted.{@link FluidVolume#getAmount() getAmount()}; <br>
     *            assert attempted.{@link FluidVolume#getFluidKey() getFluidKet()} ==
     *            extracted.{@link FluidVolume#getFluidKey() getFluidKet()}; <br>
     *            assert attempted.{@link Object#equals(Object) equals}(extracted);<br>
     * @return A new, independent {@link FluidVolume} that was extracted.
     * @deprecated Replaced by {@link #attemptExtraction(FluidFilter, FluidAmount, Simulation)}. */
    default FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
        if (AttributeUtil.EXPENSIVE_DEBUG_CHECKS) {
            validateFluidExtractable(this);
        }
        return attemptExtraction(filter, FluidAmount.of1620(maxAmount), simulation);
    }

    /** Attempt to extract *any* {@link FluidVolume} from this that {@link FluidFilter#matches(FluidKey) matches} the
     * given {@link FluidFilter}.
     * 
     * @param filter
     * @param maxAmount The maximum amount of fluid that can be extracted. Negative numbers throw an exception.
     * @param simulation If {@link Simulation#SIMULATE} then this should return the same result that the exact same call
     *            with {@link Simulation#ACTION} would do, except that the filter can be made more specific between
     *            calls if the previously simulated extracted fluid is used as a filter.
     *            <p>
     *            For example the following code snippet should never throw an exception:
     *            <p>
     *            {@link FluidExtractable} from = // Some extractable <br>
     *            {@link FluidVolume} attempted = from.{@link FluidExtractable#attemptAnyExtraction
     *            attemptAnyExtraction}({@link Integer#MAX_VALUE Integer.MAX_VALUE}, {@link Simulation#SIMULATE
     *            Simulation.SIMULATE}); <br>
     *            if (attempted.{@link FluidVolume#isEmpty() isEmpty()}) return;<br>
     *            {@link FluidVolume} extracted = from.{@link FluidExtractable#extract(FluidKey, int)
     *            extract}(attempted.{@link FluidVolume#getFluidKey() getFluidKey()},
     *            attempted.{@link FluidVolume#getAmount() getAmount()}); <br>
     *            assert !extracted.{@link FluidVolume#isEmpty() isEmpty};<br>
     *            assert attempted.{@link FluidVolume#getAmount() getAmount()} ==
     *            extracted.{@link FluidVolume#getAmount() getAmount()}; <br>
     *            assert attempted.{@link FluidVolume#getFluidKey() getFluidKet()} ==
     *            extracted.{@link FluidVolume#getFluidKey() getFluidKet()}; <br>
     *            assert attempted.{@link Object#equals(Object) equals}(extracted);<br>
     * @return A new, independent {@link FluidVolume} that was extracted. */
    default FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        if (AttributeUtil.EXPENSIVE_DEBUG_CHECKS) {
            validateFluidExtractable(this);
        }
        return attemptExtraction(filter, maxAmount.asInt(1620, RoundingMode.DOWN), simulation);
    }

    /** Calls {@link #attemptExtraction(FluidFilter, int, Simulation) attemptExtraction()} with an {@link FluidFilter}
     * of {@link ConstantFluidFilter#ANYTHING}.
     * 
     * @deprecated Replaced by {@link #attemptAnyExtraction(FluidAmount, Simulation)} */
    @Deprecated
    default FluidVolume attemptAnyExtraction(int maxAmount, Simulation simulation) {
        return attemptExtraction(ConstantFluidFilter.ANYTHING, maxAmount, simulation);
    }

    /** Calls {@link #attemptExtraction(FluidFilter, FluidAmount, Simulation) attemptExtraction()} with an
     * {@link FluidFilter} of {@link ConstantFluidFilter#ANYTHING}. */
    default FluidVolume attemptAnyExtraction(FluidAmount maxAmount, Simulation simulation) {
        return attemptExtraction(ConstantFluidFilter.ANYTHING, maxAmount, simulation);
    }

    /** Attempt to extract *any* {@link FluidVolume} from this that {@link FluidFilter#matches(FluidKey) matches} the
     * given {@link FluidFilter}.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(FluidFilter, int, Simulation)} with a {@link Simulation}
     * parameter of {@link Simulation#ACTION ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted.
     * @deprecated Replaced by {@link #extract(FluidFilter, FluidAmount)}. */
    @Deprecated
    default FluidVolume extract(FluidFilter filter, int maxAmount) {
        return attemptExtraction(filter, maxAmount, Simulation.ACTION);
    }

    /** Attempt to extract *any* {@link FluidVolume} from this that {@link FluidFilter#matches(FluidKey) matches} the
     * given {@link FluidFilter}.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(FluidFilter, int, Simulation)} with a {@link Simulation}
     * parameter of {@link Simulation#ACTION ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted. */
    default FluidVolume extract(FluidFilter filter, FluidAmount maxAmount) {
        return attemptExtraction(filter, maxAmount, Simulation.ACTION);
    }

    /** Attempt to extract *any* {@link FluidVolume} from this that is
     * {@link ItemStackUtil#areEqualIgnoreAmounts(ItemStack, ItemStack) equal}to the given {@link ItemStack}.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(FluidFilter, int, Simulation)} with an
     * {@link FluidFilter} parameter of {@link ExactFluidFilter} and a {@link Simulation} parameter of
     * {@link Simulation#ACTION ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted.
     * @deprecated Replaced by {@link #extract(FluidKey, FluidAmount)} */
    @Deprecated
    default FluidVolume extract(FluidKey filter, int maxAmount) {
        return attemptExtraction(new ExactFluidFilter(filter), maxAmount, Simulation.ACTION);
    }

    /** Attempt to extract *any* {@link FluidVolume} from this that is
     * {@link ItemStackUtil#areEqualIgnoreAmounts(ItemStack, ItemStack) equal}to the given {@link ItemStack}.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(FluidFilter, int, Simulation)} with an
     * {@link FluidFilter} parameter of {@link ExactFluidFilter} and a {@link Simulation} parameter of
     * {@link Simulation#ACTION ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted. */
    default FluidVolume extract(FluidKey filter, FluidAmount maxAmount) {
        return attemptExtraction(new ExactFluidFilter(filter), maxAmount, Simulation.ACTION);
    }

    /** Attempt to extract *any* {@link FluidVolume} from this.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(FluidFilter, int, Simulation)} with an
     * {@link FluidFilter} parameter of {@link ConstantFluidFilter#ANYTHING} and a {@link Simulation} parameter of
     * {@link Simulation#ACTION ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted.
     * @deprecated Replaced by {@link #extract(FluidAmount)} */
    @Deprecated
    default FluidVolume extract(int maxAmount) {
        return attemptExtraction(ConstantFluidFilter.ANYTHING, maxAmount, Simulation.ACTION);
    }

    /** Attempt to extract *any* {@link FluidVolume} from this.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(FluidFilter, int, Simulation)} with an
     * {@link FluidFilter} parameter of {@link ConstantFluidFilter#ANYTHING} and a {@link Simulation} parameter of
     * {@link Simulation#ACTION ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted. */
    default FluidVolume extract(FluidAmount maxAmount) {
        return attemptExtraction(ConstantFluidFilter.ANYTHING, maxAmount, Simulation.ACTION);
    }

    /** @return True if {@link #attemptAnyExtraction(FluidAmount, Simulation) attemptAnyExtraction}(FluidAmount.ONE,
     *         SIMULATE) returns a non-empty {@link FluidVolume}. */
    default boolean couldExtractAnything() {
        return !attemptAnyExtraction(FluidAmount.ONE, Simulation.SIMULATE).isEmpty();
    }

    public static void validateFluidExtractable(FluidExtractable instance) {
        Class<?> c = instance.getClass();
        try {
            Method m0 = c.getMethod("attemptExtraction", FluidFilter.class, int.class, Simulation.class);
            Method m1 = c.getMethod("attemptExtraction", FluidFilter.class, FluidAmount.class, Simulation.class);
            if (m0.getDeclaringClass() == FluidExtractable.class) {
                if (m1.getDeclaringClass() == FluidExtractable.class) {
                    throw new Error(
                        "The " + c + " needs to override either attemptExtraction(FluidFilter, int, Simulation) "
                            + "or attemptExtraction(FluidFilter, FluidAmount, Simulation)!"
                    );
                }
            }
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        }
    }

    /** @return A new {@link FluidExtractable} that has an additional filter applied to limit the fluid extracted from
     *         it. */
    default FluidExtractable filtered(FluidFilter filter) {
        return new FilteredFluidExtractable(this, filter);
    }

    /** @return An object that only implements {@link FluidExtractable}, and does not expose any of the other
     *         modification methods that sibling or subclasses offer (like {@link FluidInsertable} or
     *         {@link GroupedFluidInv}. */
    default FluidExtractable getPureExtractable() {
        FluidExtractable delegate = this;
        return new FluidExtractable() {
            @Override
            @Deprecated
            public FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
                return delegate.attemptExtraction(filter, maxAmount, simulation);
            }

            @Override
            public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
                return delegate.attemptExtraction(filter, maxAmount, simulation);
            }

            @Override
            @Deprecated
            public FluidVolume attemptAnyExtraction(int maxAmount, Simulation simulation) {
                return delegate.attemptAnyExtraction(maxAmount, simulation);
            }

            @Override
            public FluidVolume attemptAnyExtraction(FluidAmount maxAmount, Simulation simulation) {
                return delegate.attemptAnyExtraction(maxAmount, simulation);
            }
        };
    }
}
