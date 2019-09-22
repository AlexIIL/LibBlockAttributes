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

public final class FluidUnitSet {

    /* package-private */ final NavigableSet<FluidUnit> units;

    /* package-private */ FluidUnitSet() {
        this.units = new TreeSet<>();
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

    public String localizeAmount(int amount) {
        if (units.isEmpty()) {
            // Default to buckets
            return FluidUnit.BUCKET.localizeAmount(amount);
        } else if (amount == 0) {
            return getSmallestUnit().localizeAmount(amount);
        }
        int unitCount = units.size();
        if (unitCount == 1) {
            return getSmallestUnit().localizeAmount(amount);
        }

        int usedCount = 0;
        FluidUnit[] usedUnits = new FluidUnit[unitCount];
        String[] perUnitAmounts = new String[unitCount];
        boolean[] isUnitSingular = new boolean[unitCount];

        for (FluidUnit unit : units) {
            int div = amount / unit.unitAmount;
            int rem = amount % unit.unitAmount;

            if (unit == units.last()) {
                usedUnits[usedCount] = unit;
                perUnitAmounts[usedCount] = unit.format(amount);
                isUnitSingular[usedCount] = amount == unit.unitAmount;
                usedCount++;
                break;
            }
            if (div == 0) {
                continue;
            }
            usedUnits[usedCount] = unit;
            perUnitAmounts[usedCount] = Integer.toString(div);
            isUnitSingular[usedCount] = div == 1;
            amount -= div * unit.unitAmount;
            usedCount++;
            if (rem == 0) {
                break;
            }
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

    public String localizeEmptyTank(int capacity) {
        return FluidUnit.localizeDirect(FluidUnit.KEY_TANK_EMPTY, localizeAmount(capacity));
    }

    public String localizeFullTank(int capacity) {
        return FluidUnit.localizeDirect(FluidUnit.KEY_TANK_FULL, localizeAmount(capacity));
    }

    public String localizeTank(int amount, int capacity) {
        if (units.isEmpty()) {
            // Default to buckets
            return FluidUnit.BUCKET.localizeTank(amount, capacity);
        } else if ((amount == 0 && capacity == 0) || units.size() == 1) {
            return getSmallestUnit().localizeTank(amount, capacity);
        }

        if (amount == 0) {
            return localizeEmptyTank(capacity);
        } else if (amount == capacity) {
            return localizeFullTank(capacity);
        }

        return FluidUnit.localizeDirect(
            FluidUnit.KEY_TANK_MULTI_UNIT, localizeAmount(amount), localizeAmount(capacity)
        );
    }

    public String localizeFlowRate(int amountPerTick) {
        // TODO: Replace this with proper!
        return getSmallestUnit().localizeFlowRate(amountPerTick);
    }
}
