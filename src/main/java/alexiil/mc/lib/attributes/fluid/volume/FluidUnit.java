package alexiil.mc.lib.attributes.fluid.volume;

import java.util.IllegalFormatException;

import net.minecraft.util.Language;

public final class FluidUnit {

    public static final FluidUnit BUCKET = new FluidUnit(FluidVolume.BUCKET, "bucket");
    public static final FluidUnit BOTTLE = new FluidUnit(FluidVolume.BOTTLE, "bottle");

    /* package-private */ static final String KEY_AMOUNT = "libblockattributes.fluid.amount";
    /* package-private */ static final String KEY_FLOW_RATE = "libblockattributes.fluid.flow_rate";
    /* package-private */ static final String KEY_TANK_EMPTY = "libblockattributes.fluid.tank_empty";
    /* package-private */ static final String KEY_TANK_PARTIAL = "libblockattributes.fluid.tank_partial";
    /* package-private */ static final String KEY_TANK_FULL = "libblockattributes.fluid.tank_full";

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
        return localize(KEY_AMOUNT, amount == unitAmount, amount);
    }

    public String localizeEmptyTank(int capacity) {
        return localize(KEY_TANK_EMPTY, true, capacity);
    }

    public String localizeFullTank(int capacity) {
        return localize(KEY_TANK_FULL, true, capacity);
    }

    public String localizeTank(int amount, int capacity) {
        if (amount == 0) {
            return localizeEmptyTank(capacity);
        } else if (amount == capacity) {
            return localizeFullTank(capacity);
        }
        return localize(KEY_TANK_PARTIAL, true, amount, capacity);
    }

    public String localizeFlowRate(int amountPerTick) {
        double rate = amountPerTick / (double) timeGap;
        String translatedUnit = translateUnit(rate == unitAmount);
        String format = format(rate);
        String translatedKey = Language.getInstance().translate(KEY_FLOW_RATE);
        String translatedtime = Language.getInstance().translate(keyTime);
        try {
            return String.format(translatedKey, translatedUnit, translatedtime, format);
        } catch (IllegalFormatException ife) {
            return KEY_FLOW_RATE + " [" + format + "] " + ife.getMessage();
        }
    }

    /* package-private */ String localize(String key, boolean isSingular, int number) {
        String translatedUnit = translateUnit(isSingular);
        String format = format(number);
        String translatedKey = Language.getInstance().translate(key);
        try {
            return String.format(translatedKey, translatedUnit, format);
        } catch (IllegalFormatException ife) {
            return key + " [" + format + "] " + ife.getMessage();
        }
    }

    private String translateUnit(boolean isSingular) {
        return Language.getInstance().translate(isSingular ? keySingular : keyPlural);
    }

    /* package-private */ String localize(String key, boolean isSingular, int number1, int number2) {
        String translatedUnit = translateUnit(isSingular);
        String format1 = format(number1);
        String format2 = format(number2);
        String translatedKey = Language.getInstance().translate(key);
        try {
            return String.format(translatedKey, translatedUnit, format1, format2);
        } catch (IllegalFormatException ife) {
            return key + " [" + format1 + ", " + format2 + "] " + ife.getMessage();
        }
    }

    private String format(int number) {
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
}
