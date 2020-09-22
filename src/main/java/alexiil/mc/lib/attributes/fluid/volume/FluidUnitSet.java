/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.NavigableSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

import net.minecraft.text.Text;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/** Multiple {@link FluidUnit}s. This can localise a single amount into multiple different units, for example a
 * FluidAmount of "4 + 1/3" would localize a string like "4 Buckets and 1 Bottle" if the two available units were
 * {@link FluidUnit#BUCKET} and {@link FluidUnit#BOTTLE}.
 * <p>
 * While it is possible to construct a custom {@link FluidUnitSet} it's recommended that you use the one that's built in
 * to every {@link FluidKey}, via {@link FluidKey#unitSet}. */
public final class FluidUnitSet extends FluidUnitBase {

    /* package-private */ final NavigableSet<FluidUnit> units;

    public FluidUnitSet() {
        this.units = new TreeSet<>();
    }

    public FluidUnitSet copy() {
        FluidUnitSet copy = new FluidUnitSet();
        copy.units.addAll(units);
        return copy;
    }

    public void copyFrom(FluidUnitSet other) {
        units.addAll(other.units);
    }

    /** @return True if the given unit was added, or false if another unit was already present with an amount equal to
     *         the given one. */
    public boolean addUnit(FluidUnit unit) {
        return units.add(unit);
    }

    /** @return The largest unit in this set. For example if this was [buckets, bottles] then this would return
     *         bottles. */
    public FluidUnit getSmallestUnit() {
        return units.last();
    }

    /** @return The largest unit in this set. For example if this was [buckets, bottles] then this would return
     *         buckets. */
    public FluidUnit getLargestUnit() {
        return units.first();
    }

    @Override
    public String localizeAmount(
        FluidAmount amount, boolean forceLastSingular, @Nullable Text fluidName, FluidTooltipContext ctx
    ) {
        if (units.isEmpty()) {
            // Default to buckets
            return FluidUnit.BUCKET.localizeAmount(amount, forceLastSingular, fluidName, ctx);
        } else if (amount.isZero()) {
            return getSmallestUnit().localizeAmount(amount, forceLastSingular, fluidName, ctx);
        }
        int unitCount = units.size();
        if (unitCount == 1) {
            return getSmallestUnit().localizeAmount(amount, forceLastSingular, fluidName, ctx);
        }
        String amt
            = localizeAmountInner(amount, forceLastSingular, ctx, FluidUnit::localizeUnit, FluidUnit::localizeDirect);
        if (fluidName != null && ctx.shouldJoinNameWithAmount()) {
            return FluidUnit.localizeDirect(FluidUnit.KEY_NAME, amt, FluidUnit.textToString(fluidName));
        } else {
            return amt;
        }
    }

    @Override
    public Text getAmount(
        FluidAmount amount, boolean forceLastSingular, @Nullable Text fluidName, FluidTooltipContext ctx
    ) {
        if (units.isEmpty()) {
            // Default to buckets
            return FluidUnit.BUCKET.getAmount(amount, forceLastSingular, fluidName, ctx);
        } else if (amount.isZero()) {
            return getSmallestUnit().getAmount(amount, forceLastSingular, fluidName, ctx);
        }
        int unitCount = units.size();
        if (unitCount == 1) {
            return getSmallestUnit().getAmount(amount, forceLastSingular, fluidName, ctx);
        }
        Text amt = localizeAmountInner(amount, forceLastSingular, ctx, FluidUnit::getUnit, FluidUnit::getDirect);
        if (fluidName != null && ctx.shouldJoinNameWithAmount()) {
            return FluidUnit.getDirect(FluidUnit.KEY_NAME, amt, ctx.stripFluidColours(fluidName));
        } else {
            return amt;
        }
    }

    private interface UnitLocalizer<T> {
        T localize(FluidUnit unit, boolean isSingular, FluidTooltipContext ctx);
    }

    private interface CombiningLocalizer<T> {
        T localize(String key, Object... args);
    }

    private <T> T localizeAmountInner(
        FluidAmount amount, boolean forceLastSingular, FluidTooltipContext ctx, UnitLocalizer<T> unitLocale,
        CombiningLocalizer<T> combiningLocale
    ) {
        int unitCount = units.size();
        int usedCount = 0;
        FluidUnit[] usedUnits = new FluidUnit[unitCount];
        String[] perUnitAmounts = new String[unitCount];
        boolean[] isUnitSingular = new boolean[unitCount];

        for (FluidUnit unit : units) {
            if (unit == units.last()) {
                usedUnits[usedCount] = unit;
                perUnitAmounts[usedCount] = unit.format(amount);
                isUnitSingular[usedCount] = amount.equals(unit.unitAmount);
                usedCount++;
                break;
            }

            FluidAmount sub = amount.roundedDiv(unit.unitAmount);
            if (sub.whole == 0) {
                continue;
            }
            usedUnits[usedCount] = unit;
            perUnitAmounts[usedCount] = Long.toString(sub.whole);
            isUnitSingular[usedCount] = sub.whole == 1;
            amount = FluidAmount.of(sub.numerator, sub.denominator).checkedMul(unit.unitAmount);
            usedCount++;
            if (sub.numerator == 0) {
                break;
            }
        }

        if (forceLastSingular) {
            isUnitSingular[usedCount - 1] = true;
        }

        switch (usedCount) {
            case 0: {
                assert false : "usedCount should never be zero!";
                return combiningLocale
                    .localize("ERROR: usedCount should never be zero! (for " + amount + " of " + this + ")");
            }
            case 1: {
                return combiningLocale.localize(
                    FluidUnit.KEY_AMOUNT, perUnitAmounts[0], unitLocale.localize(usedUnits[0], isUnitSingular[0], ctx)
                );
            }
            case 2: {
                String a1 = perUnitAmounts[0];
                T u1 = unitLocale.localize(usedUnits[0], isUnitSingular[0], ctx);
                String a2 = perUnitAmounts[1];
                T u2 = unitLocale.localize(usedUnits[1], isUnitSingular[1], ctx);
                return combiningLocale.localize(FluidUnit.KEY_MULTI_UNIT_2, a1, u1, a2, u2);
            }
            case 3: {
                String a1 = perUnitAmounts[0];
                T u1 = unitLocale.localize(usedUnits[0], isUnitSingular[0], ctx);
                String a2 = perUnitAmounts[1];
                T u2 = unitLocale.localize(usedUnits[1], isUnitSingular[1], ctx);
                String a3 = perUnitAmounts[2];
                T u3 = unitLocale.localize(usedUnits[2], isUnitSingular[2], ctx);
                return combiningLocale.localize(FluidUnit.KEY_MULTI_UNIT_3, a1, u1, a2, u2, a3, u3);
            }
            case 4: {
                String a1 = perUnitAmounts[0];
                T u1 = unitLocale.localize(usedUnits[0], isUnitSingular[0], ctx);
                String a2 = perUnitAmounts[1];
                T u2 = unitLocale.localize(usedUnits[1], isUnitSingular[1], ctx);
                String a3 = perUnitAmounts[2];
                T u3 = unitLocale.localize(usedUnits[2], isUnitSingular[2], ctx);
                String a4 = perUnitAmounts[3];
                T u4 = unitLocale.localize(usedUnits[3], isUnitSingular[3], ctx);
                return combiningLocale.localize(FluidUnit.KEY_MULTI_UNIT_4, a1, u1, a2, u2, a3, u3, a4, u4);
            }
            default: {
                assert usedCount > 4;

                T unit = unitLocale.localize(usedUnits[0], isUnitSingular[0], ctx);
                T soFar = combiningLocale.localize(FluidUnit.KEY_AMOUNT, perUnitAmounts[0], unit);
                int end = usedCount - 1;

                for (int i = 1; i < end; i++) {
                    String a = perUnitAmounts[i];
                    T u = unitLocale.localize(usedUnits[i], isUnitSingular[i], ctx);
                    soFar = combiningLocale.localize(FluidUnit.KEY_MULTI_UNIT_COMBINER, soFar, a, u);
                }

                String endAmount = perUnitAmounts[end];
                T endUnit = unitLocale.localize(usedUnits[end], isUnitSingular[end], ctx);
                return combiningLocale.localize(FluidUnit.KEY_MULTI_UNIT_END, soFar, endAmount, endUnit);
            }
        }
    }

    @Override
    public String localizeEmptyTank(FluidAmount capacity, FluidTooltipContext ctx) {
        return FluidUnit.localizeDirect(FluidUnit.KEY_TANK_EMPTY.get(ctx), localizeAmount(capacity, true, ctx));
    }

    @Override
    public Text getEmptyTank(FluidAmount capacity, FluidTooltipContext ctx) {
        return FluidUnit.getDirect(FluidUnit.KEY_TANK_EMPTY.get(ctx), getAmount(capacity, true, ctx));
    }

    @Override
    public String localizeFullTank(FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx) {
        return FluidUnit
            .localizeDirect(FluidUnit.KEY_TANK_FULL.get(ctx), localizeAmount(capacity, true, fluidName, ctx));
    }

    @Override
    public Text getFullTank(FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx) {
        return FluidUnit.getDirect(FluidUnit.KEY_TANK_FULL.get(ctx), getAmount(capacity, true, fluidName, ctx));
    }

    @Override
    public String localizePartialTank(
        FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx
    ) {
        if (units.isEmpty()) {
            // Default to buckets
            return FluidUnit.BUCKET.localizeTank(amount, capacity, ctx);
        } else if ((amount.isZero() && capacity.isZero()) || units.size() == 1) {
            return getSmallestUnit().localizeTank(amount, capacity, ctx);
        }

        String key = FluidUnit.KEY_TANK_MULTI_UNIT.get(ctx);
        return FluidUnit.localizeDirect(key, localizeAmount(amount, ctx), localizeAmount(capacity, true, ctx));
    }

    @Override
    public Text getPartialTank(
        FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx
    ) {
        if (units.isEmpty()) {
            // Default to buckets
            return FluidUnit.BUCKET.getPartialTank(amount, capacity, ctx);
        } else if ((amount.isZero() && capacity.isZero()) || units.size() == 1) {
            return getSmallestUnit().getPartialTank(amount, capacity, ctx);
        }

        String key = FluidUnit.KEY_TANK_MULTI_UNIT.get(ctx);
        return FluidUnit.getDirect(key, getAmount(amount, ctx), getAmount(capacity, true, ctx));
    }

    @Override
    public String localizeFlowRate(FluidAmount amountPerTick, @Nullable Text fluidName, FluidTooltipContext ctx) {
        // TODO: Replace this with proper!
        return getSmallestUnit().localizeFlowRate(amountPerTick, ctx);
    }

    @Override
    public Text getFlowRate(FluidAmount amountPerTick, @Nullable Text fluidName, FluidTooltipContext ctx) {
        // TODO: Replace this with proper!
        return getSmallestUnit().getFlowRate(amountPerTick, ctx);
    }
}
