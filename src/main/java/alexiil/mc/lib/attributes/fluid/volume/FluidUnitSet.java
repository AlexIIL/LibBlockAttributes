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

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

public final class FluidUnitSet {

    /* package-private */ final NavigableSet<FluidUnit> units;

    /* package-private */ FluidUnitSet() {
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

    /** @deprecated Replaced by {@link #localizeAmount(FluidAmount)}. */
    @Deprecated
    public String localizeAmount(int amount) {
        return localizeAmount(FluidAmount.of1620(amount));
    }

    public String localizeAmount(FluidAmount amount) {
        return localizeAmount(amount, false);
    }

    /** @deprecated Replaced by {@link #localizeAmount(FluidAmount, boolean)}. */
    @Deprecated
    public String localizeAmount(int amount, boolean forceLastSingular) {
        return localizeAmount(FluidAmount.of1620(amount), forceLastSingular);
    }

    public String localizeAmount(FluidAmount amount, boolean forceLastSingular) {
        if (units.isEmpty()) {
            // Default to buckets
            return FluidUnit.BUCKET.localizeAmount(amount, forceLastSingular);
        } else if (amount.isZero()) {
            return getSmallestUnit().localizeAmount(amount, forceLastSingular);
        }
        int unitCount = units.size();
        if (unitCount == 1) {
            return getSmallestUnit().localizeAmount(amount, forceLastSingular);
        }

        int usedCount = 0;
        FluidUnit[] usedUnits = new FluidUnit[unitCount];
        String[] perUnitAmounts = new String[unitCount];
        boolean[] isUnitSingular = new boolean[unitCount];

        for (FluidUnit unit : units) {
            if (unit == units.last()) {
                usedUnits[usedCount] = unit;
                perUnitAmounts[usedCount] = unit.format(amount);
                isUnitSingular[usedCount] = amount == unit.unitAmount;
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
                return "0";
            }
            case 1: {
                return FluidUnit.localizeDirect(
                    FluidUnit.KEY_AMOUNT, perUnitAmounts[0], usedUnits[0].translateUnit(isUnitSingular[0])
                );
            }
            case 2: {
                String a1 = perUnitAmounts[0];
                String u1 = usedUnits[0].translateUnit(isUnitSingular[0]);
                String a2 = perUnitAmounts[1];
                String u2 = usedUnits[1].translateUnit(isUnitSingular[1]);
                return FluidUnit.localizeDirect(FluidUnit.KEY_MULTI_UNIT_2, a1, u1, a2, u2);
            }
            case 3: {
                String a1 = perUnitAmounts[0];
                String u1 = usedUnits[0].translateUnit(isUnitSingular[0]);
                String a2 = perUnitAmounts[1];
                String u2 = usedUnits[1].translateUnit(isUnitSingular[1]);
                String a3 = perUnitAmounts[2];
                String u3 = usedUnits[2].translateUnit(isUnitSingular[2]);
                return FluidUnit.localizeDirect(FluidUnit.KEY_MULTI_UNIT_3, a1, u1, a2, u2, a3, u3);
            }
            case 4: {
                String a1 = perUnitAmounts[0];
                String u1 = usedUnits[0].translateUnit(isUnitSingular[0]);
                String a2 = perUnitAmounts[1];
                String u2 = usedUnits[1].translateUnit(isUnitSingular[1]);
                String a3 = perUnitAmounts[2];
                String u3 = usedUnits[2].translateUnit(isUnitSingular[2]);
                String a4 = perUnitAmounts[3];
                String u4 = usedUnits[3].translateUnit(isUnitSingular[3]);
                return FluidUnit.localizeDirect(FluidUnit.KEY_MULTI_UNIT_4, a1, u1, a2, u2, a3, u3, a4, u4);
            }
            default: {
                assert usedCount > 4;

                String soFar = FluidUnit.localizeDirect(
                    FluidUnit.KEY_AMOUNT, perUnitAmounts[0], usedUnits[0].translateUnit(isUnitSingular[0])
                );
                int end = usedCount - 1;

                for (int i = 1; i < end; i++) {
                    String a = perUnitAmounts[i];
                    String u = usedUnits[i].translateUnit(isUnitSingular[i]);
                    soFar = FluidUnit.localizeDirect(FluidUnit.KEY_MULTI_UNIT_COMBINER, soFar, a, u);
                }

                String endAmount = perUnitAmounts[end];
                String endUnit = usedUnits[end].translateUnit(isUnitSingular[end]);
                return FluidUnit.localizeDirect(FluidUnit.KEY_MULTI_UNIT_END, soFar, endAmount, endUnit);
            }
        }
    }

    /** @deprecated Replaced by {@link #localizeEmptyTank(FluidAmount)}. */
    @Deprecated
    public String localizeEmptyTank(int capacity) {
        return localizeEmptyTank(FluidAmount.of1620(capacity));
    }

    public String localizeEmptyTank(FluidAmount capacity) {
        return FluidUnit.localizeDirect(FluidUnit.KEY_TANK_EMPTY, localizeAmount(capacity, true));
    }

    /** @deprecated Replaced by {@link #localizeFullTank(FluidAmount)}. */
    @Deprecated
    public String localizeFullTank(int capacity) {
        return localizeFullTank(FluidAmount.of1620(capacity));
    }

    public String localizeFullTank(FluidAmount capacity) {
        return FluidUnit.localizeDirect(FluidUnit.KEY_TANK_FULL, localizeAmount(capacity, true));
    }

    /** @deprecated Replaced by {@link #localizeTank(FluidAmount, FluidAmount)} */
    @Deprecated
    public String localizeTank(int amount, int capacity) {
        return localizeTank(FluidAmount.of1620(amount), FluidAmount.of1620(capacity));
    }

    public String localizeTank(FluidAmount amount, FluidAmount capacity) {
        if (units.isEmpty()) {
            // Default to buckets
            return FluidUnit.BUCKET.localizeTank(amount, capacity);
        } else if ((amount.isZero() && capacity.isZero()) || units.size() == 1) {
            return getSmallestUnit().localizeTank(amount, capacity);
        }

        if (amount.isZero()) {
            return localizeEmptyTank(capacity);
        } else if (amount.equals(capacity)) {
            return localizeFullTank(capacity);
        }

        return FluidUnit
            .localizeDirect(FluidUnit.KEY_TANK_MULTI_UNIT, localizeAmount(amount), localizeAmount(capacity, true));
    }

    /** @deprecated Replaced by {@link #localizeFlowRate(FluidAmount)}. */
    @Deprecated
    public String localizeFlowRate(int amountPerTick) {
        return localizeFlowRate(FluidAmount.of1620(amountPerTick));
    }

    public String localizeFlowRate(FluidAmount amountPerTick) {
        // TODO: Replace this with proper!
        return getSmallestUnit().localizeFlowRate(amountPerTick);
    }
}
