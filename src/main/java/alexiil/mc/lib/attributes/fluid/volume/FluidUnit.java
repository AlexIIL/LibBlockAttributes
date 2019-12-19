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

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

public final class FluidUnit implements Comparable<FluidUnit> {

    public static final FluidUnit BUCKET = new FluidUnit(FluidAmount.BUCKET, "bucket");
    public static final FluidUnit BOTTLE = new FluidUnit(FluidAmount.BOTTLE, "bottle");

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

    /* package-private */ final FluidAmount unitAmount;
    // FIXME: This only works with singular vs plural languages!
    /* package-private */ final String keySingular, keyPlural;

    /** @deprecated Replaced by {@link #FluidUnit(FluidAmount, String)} */
    @Deprecated
    public FluidUnit(int unitAmount, String key) {
        this(FluidAmount.of1620(unitAmount), key);
    }

    public FluidUnit(FluidAmount unitAmount, String key) {
        if (!unitAmount.isPositive()) {
            throw new IllegalArgumentException("Unit Amount must be a positive number!");
        }
        this.unitAmount = unitAmount;
        this.keySingular = "libblockattributes.fluid_unit." + key + ".singular";
        this.keyPlural = "libblockattributes.fluid_unit." + key + ".plural";
    }

    /** @deprecated Replaced by {@link #localizeAmount(FluidAmount)} */
    @Deprecated
    public String localizeAmount(int amount) {
        return localizeAmount(FluidAmount.of1620(amount));
    }

    public String localizeAmount(FluidAmount amount) {
        return localizeAmount(amount, false);
    }

    /** @deprecated Replaced by {@link #localizeAmount(FluidAmount, boolean)}. */
    @Deprecated
    public String localizeAmount(int amount, boolean forceSingular) {
        return localizeAmount(FluidAmount.of1620(amount), forceSingular);
    }

    public String localizeAmount(FluidAmount amount, boolean forceSingular) {
        return localize(KEY_AMOUNT, forceSingular ? true : amount.equals(unitAmount), amount);
    }

    /** @deprecated Replaced by {@link #localizeEmptyTank(FluidAmount)}. */
    @Deprecated
    public String localizeEmptyTank(int capacity) {
        return localizeEmptyTank(FluidAmount.of1620(capacity));
    }

    public String localizeEmptyTank(FluidAmount capacity) {
        return localizeDirect(KEY_TANK_EMPTY, localizeAmount(capacity, true));
    }

    /** @deprecated Replaced by {@link #localizeFullTank(FluidAmount)}. */
    @Deprecated
    public String localizeFullTank(int capacity) {
        return localizeFullTank(FluidAmount.of1620(capacity));
    }

    public String localizeFullTank(FluidAmount capacity) {
        return localizeDirect(KEY_TANK_FULL, localizeAmount(capacity, true));
    }

    /** @deprecated Replaced by {@link #localizeTank(FluidAmount, FluidAmount)}. */
    @Deprecated
    public String localizeTank(int amount, int capacity) {
        return localizeTank(FluidAmount.of1620(amount), FluidAmount.of1620(capacity));
    }

    public String localizeTank(FluidAmount amount, FluidAmount capacity) {
        if (amount.isZero()) {
            return localizeEmptyTank(capacity);
        } else if (amount.equals(capacity)) {
            return localizeFullTank(capacity);
        }
        return localizeDirect(KEY_TANK_PARTIAL, format(amount), format(capacity), translateUnit(true));
    }

    /** @deprecated Replaced by {@link #localizeFlowRate(FluidAmount)}. */
    @Deprecated
    public String localizeFlowRate(int amountPerTick) {
        return localizeFlowRate(FluidAmount.of1620(amountPerTick));
    }

    public String localizeFlowRate(FluidAmount amountPerTick) {
        FluidAmount rate = amountPerTick.roundedDiv(timeGap);
        String translatedUnit = translateUnit(rate.equals(unitAmount));
        String format = format(rate);
        String translatedtime = Language.getInstance().translate(keyTime);
        return localizeDirect(KEY_FLOW_RATE, localizeDirect(KEY_AMOUNT, format, translatedUnit), translatedtime);
    }

    /* package-private */ String localize(String key, boolean isSingular, FluidAmount number) {
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

    /* package-private */ String format(FluidAmount number) {
        return number.roundedDiv(unitAmount).toDisplayString();
    }

    @Override
    public int compareTo(FluidUnit o) {
        // So that buckets are sorted *before* bottles.
        return o.unitAmount.compareTo(unitAmount);
    }
}
