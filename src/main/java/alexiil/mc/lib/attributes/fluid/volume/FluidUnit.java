/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.Arrays;
import java.util.IllegalFormatException;

import net.minecraft.util.Language;

public final class FluidUnit implements Comparable<FluidUnit> {

    public static final FluidUnit BUCKET = new FluidUnit(FluidVolume.BUCKET, "bucket");
    public static final FluidUnit BOTTLE = new FluidUnit(FluidVolume.BOTTLE, "bottle");

    /** Amount, Unit */
    /* package-private */ static final String KEY_AMOUNT = "libblockattributes.fluid.amount";

    /** (Amount+Unit), Time */
    /* package-private */ static final String KEY_FLOW_RATE = "libblockattributes.fluid.flow_rate";

    /** (Amount+Unit) */
    /* package-private */ static final String KEY_TANK_EMPTY = "libblockattributes.fluid.tank_empty";

    /** (Amount+Unit) */
    /* package-private */ static final String KEY_TANK_FULL = "libblockattributes.fluid.tank_full";

    /** Amount, Capacity, Unit */
    /* package-private */ static final String KEY_TANK_PARTIAL = "libblockattributes.fluid.tank_partial";

    /** Amount, Capacity */
    /* package-private */ static final String KEY_TANK_MULTI_UNIT = "libblockattributes.fluid.tank_multi_unit";

    /** Amount1, Unit1, Amount2, Unit2 */
    /* package-private */ static final String KEY_MULTI_UNIT_2 = "libblockattributes.fluid.multi_unit.2";

    /** Amount1, Unit1, Amount2, Unit2, Amount3, Unit3 */
    /* package-private */ static final String KEY_MULTI_UNIT_3 = "libblockattributes.fluid.multi_unit.3";

    /** Amount1, Unit1, Amount2, Unit2, Amount3, Unit3, Amount4, Unit4 */
    /* package-private */ static final String KEY_MULTI_UNIT_4 = "libblockattributes.fluid.multi_unit.4";

    /** Amount1, Unit1, Amount2, Unit2 */
    /* package-private */ static final String KEY_MULTI_UNIT_COMBINER = "libblockattributes.fluid.multi_unit.combiner";

    /** ((Amount+Unit)[1...N-1]), Amount_N, Unit_N */
    /* package-private */ static final String KEY_MULTI_UNIT_END = "libblockattributes.fluid.multi_unit.end";

    /* package-private */ static final String KEY_SECOND = "libblockattributes.time_unit.second";
    /* package-private */ static final String KEY_TICK = "libblockattributes.time_unit.tick";

    /* package-private */ static final String keyTime = KEY_SECOND;
    /* package-private */ static final int timeGap = 20;

    /* package-private */ final int unitAmount, decimalPlaces, roundingValue;
    // FIXME: This only works with singular vs plural languages!
    /* package-private */ final String keySingular, keyPlural;

    public FluidUnit(int unitAmount, String key) {
        if (unitAmount < 0) {
            throw new IllegalArgumentException("Unit Amount must be a positive number!");
        }
        this.unitAmount = unitAmount;
        this.decimalPlaces = unitAmount == 1 ? 0 : Integer.toString(unitAmount - 1).length();
        this.roundingValue = (int) Math.pow(10, decimalPlaces);
        this.keySingular = "libblockattributes.fluid_unit." + key + ".singular";
        this.keyPlural = "libblockattributes.fluid_unit." + key + ".plural";
    }

    public String localizeAmount(int amount) {
        return localizeAmount(amount, false);
    }

    public String localizeAmount(int amount, boolean forceSingular) {
        return localize(KEY_AMOUNT, forceSingular ? true : amount == unitAmount, amount);
    }

    public String localizeEmptyTank(int capacity) {
        return localizeDirect(KEY_TANK_EMPTY, localizeAmount(capacity, true));
    }

    public String localizeFullTank(int capacity) {
        return localizeDirect(KEY_TANK_FULL, localizeAmount(capacity, true));
    }

    public String localizeTank(int amount, int capacity) {
        if (amount == 0) {
            return localizeEmptyTank(capacity);
        } else if (amount == capacity) {
            return localizeFullTank(capacity);
        }
        return localizeDirect(KEY_TANK_PARTIAL, format(amount), format(capacity), translateUnit(true));
    }

    public String localizeFlowRate(int amountPerTick) {
        double rate = amountPerTick / (double) timeGap;
        String translatedUnit = translateUnit(rate == unitAmount);
        String format = format(rate);
        String translatedtime = Language.getInstance().translate(keyTime);
        return localizeDirect(KEY_FLOW_RATE, localizeDirect(KEY_AMOUNT, format, translatedUnit), translatedtime);
    }

    /* package-private */ String localize(String key, boolean isSingular, int number) {
        String translatedUnit = translateUnit(isSingular);
        String format = format(number);
        return localizeDirect(key, format, translatedUnit);
    }

    /* package-private */ String translateUnit(boolean isSingular) {
        return Language.getInstance().translate(isSingular ? keySingular : keyPlural);
    }

    /* package-private */ static String localizeDirect(String localeKey, Object... args) {
        String translatedKey = Language.getInstance().translate(localeKey);
        if (translatedKey == localeKey) {
            return localeKey + " " + Arrays.toString(args);
        }
        try {
            return String.format(translatedKey, args);
        } catch (IllegalFormatException ife) {
            return localeKey + " " + Arrays.toString(args) + " " + ife.getMessage();
        }
    }

    /* package-private */ String format(int number) {
        if (unitAmount == 1) {
            return Integer.toString(number);
        }
        int div = number / unitAmount;
        int rem = number % unitAmount;
        if (rem == 0) {
            return Integer.toString(div);
        }
        double fraction = (number / (double) unitAmount) - div;
        String fractionStr = Integer.toString((int) (fraction * roundingValue));
        while (fractionStr.length() < decimalPlaces) {
            fractionStr = "0" + fractionStr;
        }
        // FIXME: This '.' isn't locale aware!
        return Integer.toString(div) + "." + fractionStr;
    }

    private static String format(double number) {
        if (number == (int) number) {
            return Integer.toString((int) number);
        }
        return Double.toString(number);
    }

    @Override
    public int compareTo(FluidUnit o) {
        // So that buckets are sorted *before* bottles.
        return Integer.compare(o.unitAmount, unitAmount);
    }
}
