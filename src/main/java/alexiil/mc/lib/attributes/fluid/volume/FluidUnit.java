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
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Language;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.volume.FluidUnit.SpsLocaleKey;

/** A single unit that a {@link FluidVolume} may be expressed as, for example a bucket or a bottle.
 * <p>
 * Units are localised according to the key given, prefixed with {@link #LOCALE_KEY_PREFIX}
 * ("libblockattributes.fluid_unit."), and suffixed with either {@link #LOCALE_KEY_SUFFIX_SINGLE} (".singular"),
 * {@link #LOCALE_KEY_SUFFIX_MULTIPLE} (".plural"), or {@link #LOCALE_KEY_SUFFIX_SYMBOL} (".symbol").
 * <p>
 * Their are several methods for localising fluid amounts, summarised here:
 * <table border="1">
 * <tr>
 * <th>Name</th>
 * <th>Description</th>
 * <th>Example (long names)</th>
 * </tr>
 * <tr>
 * <td>{@link #localizeAmount(FluidAmount)}</td>
 * <td>Unit name and amount together. The plural name will be used if the amount doesn't equal the unit.</td>
 * <td>15 Buckets</td>
 * </tr>
 * <tr>
 * <td>{@link #localizeAmount(FluidAmount, boolean)}</td>
 * <td>Similar to the above, but allows you to force the usage of the singular name.</td>
 * <td>1 Buckets</td>
 * </tr>
 * <tr>
 * <td>{@link #localizeEmptyTank(FluidAmount)}</td>
 * <td>A tank with no fluid contained.</td>
 * <td>Empty 10 Bucket Tank</td>
 * </tr>
 * <tr>
 * <td>{@link #localizeFullTank(FluidAmount)}</td>
 * <td>A tank which is completely full.</td>
 * <td>Full 10 Bucket Tank</td>
 * </tr>
 * <tr>
 * <td>{@link #localizeTank(FluidAmount, FluidAmount)}</td>
 * <td>General-purpose method for all tanks. (If the amount is zero then this delegates to
 * {@link #localizeEmptyTank(FluidAmount)}, and if the amount is equal to the capacity then this delegates to
 * {@link #localizeFullTank(FluidAmount)}).</td>
 * <td>3 Buckets in a 10 Bucket Tank</td>
 * </tr>
 * <tr>
 * <td>{@link #localizeFlowRate(FluidAmount)}</td>
 * <td>Flow rate of a fluid, in ticks. (This may use seconds if this has been configured to do so, but the passed amount
 * should always be in ticks, or a 1/20 of a second).</td>
 * <td>3 Buckets per second</td>
 * </tr>
 * </table>
 * There is also a {@link Text} based version of every method above. (For each of the 2 method variants there's also one
 * for each which takes a 1620 fixed-fraction integer-based value (old LBA style)). */
public final class FluidUnit extends FluidUnitBase implements Comparable<FluidUnit> {

    public static final String LOCALE_KEY_PREFIX = "libblockattributes.fluid_unit.";
    public static final String LOCALE_KEY_SUFFIX_SINGLE = ".singular";
    public static final String LOCALE_KEY_SUFFIX_MULTIPLE = ".plural";
    public static final String LOCALE_KEY_SUFFIX_SYMBOL = ".symbol";

    public static final FluidUnit BUCKET = new FluidUnit(FluidAmount.BUCKET, "bucket");
    public static final FluidUnit BOTTLE = new FluidUnit(FluidAmount.BOTTLE, "bottle");

    /** Amount, Unit */
    /* package-private */ static final String KEY_AMOUNT = key("fluid.amount");

    /** Amount, Name */
    /* package-private */ static final String KEY_NAME = key("fluid.name");

    /** (Amount+Unit), Time */
    /* package-private */ static final LsLocaleKey KEY_FLOW_RATE = duel("fluid.flow_rate");

    /** (Amount+Unit) */
    /* package-private */ static final LsLocaleKey KEY_TANK_EMPTY = duel("fluid.tank_empty");

    /** (Amount+Unit) */
    /* package-private */ static final LsLocaleKey KEY_TANK_FULL = duel("fluid.tank_full");

    /** (Amount+Unit), (Capacity+Unit) */
    /* package-private */ static final LsLocaleKey KEY_TANK_PARTIAL = duel("fluid.tank_partial");

    /** Amount, Capacity */
    /* package-private */ static final LsLocaleKey KEY_TANK_MULTI_UNIT = duel("fluid.tank_multi_unit");

    /** Amount1, Unit1, Amount2, Unit2 */
    /* package-private */ static final String KEY_MULTI_UNIT_2 = key("fluid.multi_unit.2");

    /** Amount1, Unit1, Amount2, Unit2, Amount3, Unit3 */
    /* package-private */ static final String KEY_MULTI_UNIT_3 = key("fluid.multi_unit.3");

    /** Amount1, Unit1, Amount2, Unit2, Amount3, Unit3, Amount4, Unit4 */
    /* package-private */ static final String KEY_MULTI_UNIT_4 = key("fluid.multi_unit.4");

    /** Amount1, Unit1, Amount2, Unit2 */
    /* package-private */ static final String KEY_MULTI_UNIT_COMBINER = key("fluid.multi_unit.combiner");

    /** ((Amount+Unit)[1...N-1]), Amount_N, Unit_N */
    /* package-private */ static final String KEY_MULTI_UNIT_END = key("fluid.multi_unit.end");

    /* package-private */ static final SpsLocaleKey KEY_TICK = triple("time_unit.tick");
    /* package-private */ static final SpsLocaleKey KEY_SECOND = triple("time_unit.second");

    /* package-private */ final FluidAmount unitAmount;
    // FIXME: This only works with singular vs plural languages!
    /* package-private */ final SpsLocaleKey unitKey;

    /** @param key The name for this unit. (Please refer to {@link #FluidUnit(FluidAmount, String)} for the full
     *            description of what language keys you need to add).
     * @deprecated Replaced by {@link #FluidUnit(FluidAmount, String)}. */
    @Deprecated(since = "0.6.4", forRemoval = true)
    public FluidUnit(int unitAmount, String key) {
        this(FluidAmount.of1620(unitAmount), key);
    }

    /** @param key The name for this unit. You should add 3 language keys to your language file for each separate key
     *            you make:
     *            <table border="1">
     *            <tr>
     *            <th>Name</th>
     *            <th>Locale Key</th>
     *            <th>Example for Buckets</th>
     *            <th>Example for Ingots</th>
     *            </tr>
     *            <tr>
     *            <td>Singular</td>
     *            <td>"libblockattributes.fluid_unit.KEY.singular"</td>
     *            <td>Bucket</td>
     *            <td>Ingot</td>
     *            </tr>
     *            <tr>
     *            <td>Plural</td>
     *            <td>"libblockattributes.fluid_unit.KEY.plural"</td>
     *            <td>Buckets</td>
     *            <td>Ingots</td>
     *            </tr>
     *            <tr>
     *            <td>Singular</td>
     *            <td>"libblockattributes.fluid_unit.KEY.symbol"</td>
     *            <td>B</td>
     *            <td>I</td>
     *            </tr>
     *            </table>
     *            (But replace "KEY" with the string you pass in here). */
    public FluidUnit(FluidAmount unitAmount, String key) {
        if (!unitAmount.isPositive()) {
            throw new IllegalArgumentException("Unit Amount must be a positive number!");
        }
        this.unitAmount = unitAmount;
        this.unitKey = new SpsLocaleKey(LOCALE_KEY_PREFIX + key);
    }

    @Override
    public String localizeAmount(
        FluidAmount amount, boolean forceSingular, @Nullable Text fluidName, FluidTooltipContext ctx
    ) {
        String str = localize(KEY_AMOUNT, forceSingular ? true : amount.equals(unitAmount), amount, ctx);
        if (fluidName != null && ctx.shouldJoinNameWithAmount()) {
            str = localizeDirect(KEY_NAME, str, textToString(fluidName));
        }
        return str;
    }

    @Override
    public Text getAmount(
        FluidAmount amount, boolean forceSingular, @Nullable Text fluidName, FluidTooltipContext ctx
    ) {
        Text txt = get(KEY_AMOUNT, forceSingular || amount.equals(unitAmount), amount, ctx);
        if (fluidName != null && ctx.shouldJoinNameWithAmount()) {
            txt = getDirect(KEY_NAME, txt, ctx.stripFluidColours(fluidName));
        }
        return txt;
    }

    @Override
    public String localizeEmptyTank(FluidAmount capacity, FluidTooltipContext ctx) {
        return localizeDirect(KEY_TANK_EMPTY.get(ctx), localizeAmount(capacity, !ctx.shouldUseShortDescription(), ctx));
    }

    @Override
    public Text getEmptyTank(FluidAmount capacity, FluidTooltipContext ctx) {
        return getDirect(KEY_TANK_EMPTY.get(ctx), getAmount(capacity, !ctx.shouldUseShortDescription(), ctx));
    }

    @Override
    public String localizeFullTank(FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx) {
        return localizeDirect(KEY_TANK_FULL.get(ctx), localizeAmount(capacity, true, ctx));
    }

    @Override
    public Text getFullTank(FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx) {
        return getDirect(KEY_TANK_FULL.get(ctx), getAmount(capacity, true, ctx));
    }

    @Override
    public String localizePartialTank(
        FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx
    ) {
        String strAmount = localizeAmount(amount, fluidName, ctx);
        String strCapacity = localizeAmount(capacity, true, ctx);
        return localizeDirect(KEY_TANK_PARTIAL.get(ctx), strAmount, strCapacity);
    }

    @Override
    public Text getPartialTank(
        FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx
    ) {
        return getDirect(KEY_TANK_PARTIAL.get(ctx), getAmount(amount, fluidName, ctx), getAmount(capacity, true, ctx));
    }

    @Override
    public String localizeFlowRate(FluidAmount amountPerTick, @Nullable Text fluidName, FluidTooltipContext ctx) {
        FluidAmount rate = amountPerTick.roundedDiv(ctx.getTimeGap());
        String translatedUnit = localizeUnit(rate.equals(unitAmount), ctx);
        String format = format(rate);
        String timeKey = (ctx.shouldUseTicks() ? KEY_TICK : KEY_SECOND).get(false, ctx);
        String time = Language.getInstance().get(timeKey);
        return localizeDirect(KEY_FLOW_RATE.get(ctx), localizeDirect(KEY_AMOUNT, format, translatedUnit), time);
    }

    @Override
    public Text getFlowRate(FluidAmount amountPerTick, @Nullable Text fluidName, FluidTooltipContext ctx) {
        FluidAmount rate = amountPerTick.roundedDiv(ctx.getTimeGap());
        Text unit = getUnit(rate.equals(unitAmount), ctx);
        String format = format(rate);
        String timeKey = (ctx.shouldUseTicks() ? KEY_TICK : KEY_SECOND).get(false, ctx);
        Text time = toText(timeKey);
        return getDirect(KEY_FLOW_RATE.get(ctx), localizeDirect(KEY_AMOUNT, format, unit), time);
    }

    /* package-private */ String localize(String key, boolean isSingular, FluidAmount number, FluidTooltipContext ctx) {
        String translatedUnit = localizeUnit(isSingular, ctx);
        String format = format(number);
        return localizeDirect(key, format, translatedUnit);
    }

    /* package-private */ Text get(String key, boolean isSingular, FluidAmount number, FluidTooltipContext ctx) {
        Text unit = getUnit(isSingular, ctx);
        String format = format(number);
        return getDirect(key, format, unit);
    }

    /* package-private */ String localizeUnit(boolean isSingular, FluidTooltipContext ctx) {
        return Language.getInstance().get(unitKey.get(isSingular, ctx));
    }

    /* package-private */ Text getUnit(boolean isSingular, FluidTooltipContext ctx) {
        return toText(unitKey.get(isSingular, ctx));
    }

    /* package-private */ static String localizeDirect(String localeKey, Object... args) {
        String translatedKey = Language.getInstance().get(localeKey);
        if (translatedKey == localeKey) {
            return localeKey + " " + Arrays.toString(args);
        }
        boolean check = false;
        assert check = true;
        if (check) {
            for (int i = 0; i < args.length; i++) {
                Object[] arr = new Object[i];
                Arrays.fill(arr, "~ESC~");
                try {
                    String.format(translatedKey, arr);
                } catch (IllegalFormatException ignored) {
                    // This works correctly if this throws
                    continue;
                }
                throw new IllegalStateException(
                    "The key '" + localeKey + "' doesn't use all of the arguments " + Arrays.toString(args)
                );
            }
        }
        try {
            return String.format(translatedKey, args);
        } catch (IllegalFormatException ife) {
            return localeKey + " " + Arrays.toString(args) + " " + ife.getMessage();
        }
    }

    /* package-private */ static Text getDirect(String localeKey, Object... args) {
        return toText(localeKey, args);
    }

    /* package-private */ String format(FluidAmount number) {
        return number.roundedDiv(unitAmount).toDisplayString();
    }

    private static Text toText(String key, Object... args) {
        return new TranslatableText(key, args);
    }

    @Override
    public int compareTo(FluidUnit o) {
        // So that buckets are sorted *before* bottles.
        return o.unitAmount.compareTo(unitAmount);
    }

    private static String key(String suffix) {
        return "libblockattributes." + suffix;
    }

    private static LsLocaleKey duel(String suffix) {
        return new LsLocaleKey(key(suffix));
    }

    private static SpsLocaleKey triple(String suffix) {
        return new SpsLocaleKey(key(suffix));
    }

    /** Long&Short Locale Key */
    static final class LsLocaleKey {
        final String keyLong;
        final String keyShort;

        LsLocaleKey(String keyPrefix) {
            keyLong = keyPrefix + ".long";
            keyShort = keyPrefix + ".short";
        }

        String get(FluidTooltipContext ctx) {
            return ctx.shouldUseShortDescription() ? keyShort : keyLong;
        }
    }

    /** Singular, Plural, Symbol Locale Key. */
    static final class SpsLocaleKey {
        final String keySingular;
        final String keyPlural;
        final String keySymbol;

        public SpsLocaleKey(String keyPrefix) {
            keySingular = keyPrefix + LOCALE_KEY_SUFFIX_SINGLE;
            keyPlural = keyPrefix + LOCALE_KEY_SUFFIX_MULTIPLE;
            keySymbol = keyPrefix + LOCALE_KEY_SUFFIX_SYMBOL;
        }

        String get(boolean singular, FluidTooltipContext ctx) {
            return ctx.shouldUseSymbols() ? keySymbol : singular ? keySingular : keyPlural;
        }
    }

    /* package-private */ static String textToString(Text text) {
        StringBuilder sb = new StringBuilder();
        text.visit(s -> {
            sb.append(s);
            return Optional.empty();
        });
        return sb.toString();
    }
}
