/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.amount;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

import javax.annotation.Nullable;

import com.google.common.math.LongMath;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

/** A simple mixed fraction. The value represented by this can be calculated with this: "{@link #whole} +
 * ({@link #numerator} / {@link #denominator})". Negative values are indicated with both {@link #whole} and
 * {@link #numerator} being negative - it is never permissible for only one of them to be less than 0 and the other to
 * be greater than 0.
 * <p>
 * Note: This class is intended to become a value-based class, so it will (eventually) obey the rules set out in
 * https://openjdk.java.net/jeps/390. */
/* openjdk.jep.390.@ValueBased */
public final class FluidAmount extends FluidAmountBase<FluidAmount> {

    public static final FluidAmount ZERO = ofWhole(0);
    public static final FluidAmount ONE = ofWhole(1);
    public static final FluidAmount NEGATIVE_ONE = ofWhole(-1);

    /** A very large amount of fluid - one million buckets. Used primarily in cases where we need to test if any fluid
     * is insertable, so we go above normal values (without going so far out of range to make common calculations
     * overflow into {@link BigFluidAmount}). */
    public static final FluidAmount A_MILLION = ofWhole(1_000_000);

    /** One bucket of fluid - which is always {@link #ONE}. */
    public static final FluidAmount BUCKET = ONE;

    /** One bottle is equal to a third of a bucket. */
    public static final FluidAmount BOTTLE = of(1, 3);

    /** {@link Long#MAX_VALUE} of buckets. */
    public static final FluidAmount MAX_BUCKETS = createDirect(Long.MAX_VALUE, 0, 1);

    /** {@link Long#MIN_VALUE} of buckets. */
    public static final FluidAmount MIN_BUCKETS = createDirect(Long.MIN_VALUE, 0, 1);

    /** The maximum possible value that a valid {@link FluidAmount} can hold. It's not recommended to use this as it can
     * cause headaches when adding or subtracting values from this. */
    public static final FluidAmount ABSOLUTE_MAXIMUM = createDirect(Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE);

    /** The minimum possible value that a valid {@link FluidAmount} can hold. It's not recommended to use this as it can
     * cause headaches when adding or subtracting values from this. */
    public static final FluidAmount ABSOLUTE_MINIMUM
        = createDirect(Long.MIN_VALUE, -Long.MAX_VALUE + 1, Long.MAX_VALUE);

    /** The maximum possible value that a valid {@link FluidAmount} can hold. It's not recommended to use this as it can
     * cause headaches when adding or subtracting values from this.
     * 
     * @deprecated As {@link #MAX_BUCKETS} should generally be used instead, however if you really need the absolute
     *             value then you can use {@link #ABSOLUTE_MAXIMUM}. */
    @Deprecated(since = "0.8.0", forRemoval = true)
    public static final FluidAmount MAX_VALUE = ABSOLUTE_MAXIMUM;

    /** The minimum possible value that a valid {@link FluidAmount} can hold. It's not recommended to use this as it can
     * cause headaches when adding or subtracting values from this.
     * 
     * @deprecated As {@link #MIN_BUCKETS} should generally be used instead, however if you really need the absolute
     *             minimum value then you can use {@link #ABSOLUTE_MINIMUM}. */
    @Deprecated(since = "0.8.0", forRemoval = true)
    public static final FluidAmount MIN_VALUE = ABSOLUTE_MINIMUM;

    public static final JsonDeserializer<FluidAmount> DESERIALIZER = (json, type, ctx) -> fromJson(json);

    public final long whole;
    public final long numerator;

    /** Always greater than 0. */
    public final long denominator;

    // Construction

    /** Constructs a new {@link FluidAmount} with the given whole value. The numerator is set to 0, and the denominator
     * is set to 1.
     * 
     * @deprecated As {@link #ofWhole(long)} should be used instead. */
    @Deprecated(since = "0.8.2", forRemoval = true)
    public FluidAmount(long whole) {
        this(whole, 0, 1);
    }

    /** Creates a new {@link FluidAmount} with the given values. This will reduce the fraction into it's simplest
     * form. */
    public static FluidAmount ofWhole(long whole) {
        return of(whole, 0, 1);
    }

    /** Creates a new {@link FluidAmount} with the given values. This will reduce the fraction into it's simplest
     * form. */
    public static FluidAmount of(long numerator, long denominator) {
        return of(0, numerator, denominator);
    }

    /** Legacy conversion method for creating a fraction with the given amount as it's numerator, and 1620 as it's
     * denominator */
    public static FluidAmount of1620(int amount) {
        return of(amount, 1620);
    }

    /** Creates a new {@link FluidAmount} with the given values. This will reduce the fraction into it's simplest form.
     * <p>
     * 
     * @throws IllegalArgumentException if either whole or numerator are negative, or if denominator is less than or
     *             equal to 0. */
    public static FluidAmount of(long whole, long numerator, long denominator) {
        if (denominator <= 0) {
            throw new IllegalArgumentException("The denominator (" + denominator + ") must be positive!");
        }

        if (whole < 0 && numerator > 0) {
            whole++;
            numerator = numerator - denominator;
        } else if (whole > 0 && numerator < 0) {
            whole--;
            numerator = denominator + numerator;
        }

        if (Math.abs(numerator) >= denominator) {
            long val = numerator / denominator;
            whole += val;
            numerator %= denominator;
        }

        if (numerator < 0) {
            long gcd = LongMath.gcd(-numerator, denominator);
            numerator /= gcd;
            denominator /= gcd;
        } else if (numerator > 0) {
            long gcd = LongMath.gcd(numerator, denominator);
            numerator /= gcd;
            denominator /= gcd;
        } else /* numerator == 0 */ {
            denominator = 1;
        }

        return createDirect(whole, numerator, denominator);
    }

    /** @deprecated Use {@link #parse(String)} instead. */
    @Deprecated(since = "0.6.4", forRemoval = true)
    public static FluidAmount fromDouble(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Cannot turn infinity or NaN into a FluidAmount!");
        }
        throw new AbstractMethodError("// TODO: Implement this!");
    }

    /** Attempts to parse the given text as a {@link FluidAmount}.
     * <p>
     * The text is parsed according to the following rules:
     * <ol>
     * <li>If the text is a valid {@link Long} then that is parsed and returned as if from {@link #ofWhole(long)}.</li>
     * </ol>
     * Otherwise it must contain the following:
     * <ol>
     * <li>(Optional) "-": The minus sign, which indicates that the whole part is negative.
     * <li>(Optional) "(": An opening bracket.
     * <li>A valid {@link Long}, that only contains [0-9] (From {@link Long#parseLong(String)}). This is taken as the
     * {@link #whole}</li>
     * <li>Either a single "+" or "-", which indicates that the
     * <li>(Optional) "(": An opening bracket.
     * <li>A valid {@link Long}, that only contains [0-9] (From {@link Long#parseLong(String)}). This is taken as the
     * {@link #numerator}. This may be greater than the {@link #denominator}.</li>
     * <li>A "/" symbol.</li>
     * <li>A valid {@link Long}, that only contains [0-9], and doesn't equal 0. (From {@link Long#parseLong(String)}).
     * This is taken as the {@link #denominator}</li>
     * <li>Either one or two ")" symbols, matching the two optional opening "(" symbols.</li>
     * </ol>
     * There can be any number of spaces between each item in the above list.
     * <p>
     * Alternatively the first 4 items can be left off.
     * <p>
     * For example these strings are all valid:
     * <ul>
     * <li>"1"</li>
     * <li>"-1"</li>
     * <li>"-1 - 3 / 4"</li>
     * <li>"-(1 + 3 / 4)"</li>
     * <li>"1 + 3 / 4"</li>
     * <li>"12 / 4"</li>
     * <li>"6 + 12 / 4"</li>
     * <li>"6+12/4"</li>
     * <li>"-(6+12/4)"</li>
     * <li>"-(6+(12/4))"</li>
     * <li>"-6-(12/4)"</li>
     * </ul>
     * 
     * @throws NumberFormatException if the input text was not a valid fraction. */
    public static FluidAmount parse(String text) throws NumberFormatException {
        return (FluidAmount) parse0(text, true);
    }

    /** Attempts to parse the given text as a {@link FluidAmount}. This uses the same parsing rules as
     * {@link #parse(String)}, except that this returns either a {@link FluidAmount} (if it parsed correctly) or a
     * {@link String} (if it didn't parse correctly). This will never throw an exception.
     * 
     * @return {@link String} (if there's an error and _throw is false) or the parsed {@link FluidAmount} if the text
     *         could be parsed. */
    public static Object tryParse(String text) {
        return parse0(text, false);
    }

    private static Object parse0(String text, boolean shouldThrow) throws NumberFormatException {
        if (text == null) {
            return simpleError(shouldThrow, "The text was null!");
        }
        try {
            return ofWhole(Long.parseLong(text.trim()));
        } catch (NumberFormatException ignored) {
            // This is always permissible
        }

        // Spec copied here

        // ("-" | "+")
        // 1("(")
        // long -> whole
        // "-"|"+"
        // 2("(")
        // long -> numerator
        // "/"
        // +long > denominator
        // 2(")")
        // 1(")")

        char sign0 = 0;
        char sign1 = 0;
        boolean bracket0 = false;
        boolean bracket1 = false;
        long whole = 0;
        long numerator = 0;
        long denominator = 0;
        boolean isDecimal = false;
        int decimalLength = -1;

        // 0: looking for -/+
        // 1: looking for "("
        // 2: looking for whole
        // 3: looking for -/+ (->4) or "." (->10) or "/" (->7 and swap{whole, numerator}) or end
        // 4: looking for "(" or ->5
        // 5: looking for numerator
        // 6: looking for "/"
        // 7: looking for denominator
        // 8: if bracket1 looking for ")" -> 9
        // 9: if bracket0 looking for ")" -> end
        // 10: looking for number (for "number.number" double parsing) -> end
        // 11: <end>
        int stage = 0;
        int numberStart = -1;
        int numberEnd = -1;

        text_loop: for (int i = 0; true; i++) {
            // This allows us to handle the end alongside everything else
            final boolean end = i >= text.length();
            not_a_number: {
                String badCharDesc = null;
                good_char: {
                    if (end) {
                        if (stage == 3 || ((stage == 7 || stage == 10) && numberStart != -1)) {
                            numberEnd = i;
                            break not_a_number;
                        }
                        if (stage == 11) {
                            break text_loop;
                        } else {
                            badCharDesc = "the end";
                            break good_char;
                        }
                    }
                    char c = text.charAt(i);
                    if (c < '0' || '9' < c) {
                        if (numberStart != -1) {
                            numberEnd = i;
                            i--;
                            break not_a_number;
                        }
                    }
                    switch (c) {
                        case ' ': {
                            numberEnd = i;
                            break not_a_number;
                        }
                        case '/': {
                            if (stage == 6) {
                                stage = 7;
                                continue text_loop;
                            }
                            if (stage == 3) {
                                stage = 7;
                                numerator = whole;
                                whole = 0;
                                sign1 = '+';
                                if (bracket0) {
                                    bracket1 = true;
                                    bracket0 = false;
                                }
                                continue text_loop;
                            }
                            badCharDesc = "the division symbol '/'";
                            break good_char;
                        }
                        case '-':
                        case '+': {
                            if (stage == 0) {
                                sign0 = c;
                                stage = 1;
                                continue text_loop;
                            }
                            if (stage == 3) {
                                sign1 = c;
                                stage = 4;
                                continue text_loop;
                            }
                            badCharDesc = "the sign '" + c + "'";
                            break good_char;
                        }
                        case '(': {
                            if (stage == 0) {
                                sign0 = '+';
                                stage = 1;
                            } else if (stage == 3) {
                                sign1 = '+';
                                stage = 4;
                            }
                            if (stage == 1) {
                                bracket0 = true;
                                stage = 2;
                                continue text_loop;
                            }
                            if (stage == 4) {
                                bracket1 = true;
                                stage = 5;
                                continue text_loop;
                            }
                            badCharDesc = "the open bracket '('";
                            break good_char;
                        }
                        case ')': {
                            if (bracket1 && stage == 8) {
                                if (bracket0) {
                                    stage = 9;
                                } else {
                                    stage = 11;
                                }
                                continue text_loop;
                            }
                            if (bracket1 && stage == 9) {
                                stage = 11;
                                continue text_loop;
                            }
                            badCharDesc = "the closing bracket ')'";
                            break good_char;
                        }
                        case '.': {
                            if (stage == 3) {
                                stage = 10;
                                isDecimal = true;
                                continue text_loop;
                            }
                            badCharDesc = "the decimal point '.'";
                            break good_char;
                        }
                        default: {
                            if ('0' <= c && c <= '9') {
                                if (stage == 0) {
                                    sign0 = '+';
                                    bracket0 = false;
                                    stage = 2;
                                } else if (stage == 1) {
                                    bracket0 = false;
                                    stage = 2;
                                } else if (stage == 4) {
                                    bracket1 = false;
                                    stage = 5;
                                }

                                if (stage == 2 || stage == 5 || stage == 7 || stage == 10) {
                                    if (numberStart == -1) {
                                        numberStart = i;
                                    }
                                    continue text_loop;
                                } else {
                                    badCharDesc = "a number '" + c + "'";
                                    break good_char;
                                }
                            } else {
                                badCharDesc = "an unknown character '" + c + "' (" + Character.getName(c) + ")";
                                break good_char;
                            }
                        }
                    }
                } // good_char
                String expected = null;
                switch (stage) {
                    case 0:
                        expected = "either '+', '-', '(', or a number";
                        break;
                    case 1:
                        expected = "either '(' or a number for the whole";
                        break;
                    case 2:
                        expected = "a number for the whole";
                        break;
                    case 3:
                        expected = "either '+', '-', '.', '/', or a number";
                        break;
                    case 4:
                        expected = "either '(' or a number for the numerator";
                        break;
                    case 5:
                        expected = "a number for the numerator";
                        break;
                    case 6:
                        expected = "the division symbol '/'";
                        break;
                    case 7:
                        expected = "a number for the denominator";
                        break;
                    case 8:
                        if (bracket1) {
                            expected = "the closing bracket ')'";
                        } else {
                            throw new IllegalStateException(
                                "Bad state: bracket1 is false, but we've been moved to stage 8!"
                            );
                        }
                        break;
                    case 9:
                        if (bracket0) {
                            expected = "the closing bracket ')'";
                        } else {
                            throw new IllegalStateException(
                                "Bad state: bracket0 is false, but we've been moved to stage 9!"
                            );
                        }
                        break;
                    case 10:
                        expected = "a number for the decimal";
                        break;
                    case 11:
                        expected = "the end";
                        break;
                    default: {
                        throw new IllegalStateException(
                            "Bad state: we've been moved to an unknown stage (" + stage + ")"
                        );
                    }
                }
                return simpleError(
                    shouldThrow,
                    "Expected " + expected + ", but got " + badCharDesc + " at index " + i + " for input '" + text + "'"
                );
            } // not_a_number

            if (numberStart == -1) {
                continue text_loop;
            }
            long lval = 1;
            boolean parseFailed = false;
            String error = null;
            String sub = text.substring(numberStart, numberEnd);
            try {
                lval = Long.parseLong(sub);
            } catch (NumberFormatException nfe) {
                parseFailed = true;
            }
            numberStart = -1;
            numberEnd = -1;
            String name = null;
            good_number: {
                switch (stage) {
                    case 2: {
                        if (parseFailed) {
                            name = "whole";
                            break good_number;
                        }
                        whole = lval;
                        stage = 3;
                        continue text_loop;
                    }
                    case 5: {
                        if (parseFailed) {
                            name = "numerator";
                            break good_number;
                        }
                        numerator = lval;
                        stage = 6;
                        continue text_loop;
                    }
                    case 7: {
                        if (!parseFailed && lval <= 0) {
                            error = "non-positive values are not allowed";
                        }
                        if (parseFailed) {
                            name = "denominator";
                            break good_number;
                        }
                        denominator = lval;
                        if (bracket1) {
                            stage = 8;
                        } else if (bracket0) {
                            stage = 9;
                        } else {
                            stage = 11;
                        }
                        continue text_loop;
                    }
                    case 10: {
                        if (parseFailed) {
                            name = "decimal";
                            break good_number;
                        }
                        numerator = lval;
                        decimalLength = sub.length();
                        stage = 11;
                        continue text_loop;
                    }
                    default: {
                        throw new IllegalStateException("One of the stages didn't handle this correctly... " + stage);
                    }
                }
            } // good_number
            return simpleError(shouldThrow, "Bad " + name + ": '" + sub + "'" + (error == null ? "" : ": " + error));
        } // text_loop

        assert stage == 11 : "Bad stage " + stage;
        assert sign0 == '-' || sign0 == '+' : "Missing sign0";
        if (isDecimal) {
            assert sign1 == 0 : "Was both decimal and sign1!";
            assert decimalLength > 0 : "Missing decimal length";
            sign1 = sign0;
            bracket0 = false;
            denominator = LongMath.pow(10, decimalLength);
        }
        assert sign1 == '-' || sign1 == '+' : "Missing sign1";

        if (sign0 == '-') {
            whole = -whole;
            if (bracket0) {
                numerator = -numerator;
            }
        }
        if (sign1 == '-') {
            numerator = -numerator;
        }

        return of(whole, numerator, denominator);
    }

    private static String simpleError(boolean shouldThrow, String error) throws NumberFormatException {
        if (!shouldThrow) {
            return error;
        } else {
            throw new NumberFormatException(error);
        }
    }

    /* package-private */ static FluidAmount createDirect(long whole, long numerator, long denominator) {
        return new FluidAmount(whole, numerator, denominator);
    }

    private FluidAmount(long whole, long numerator, long denominator) {
        this.whole = whole;
        this.numerator = numerator;
        this.denominator = denominator;
    }

    // Serialisation

    public static FluidAmount fromNbt(NbtCompound tag) {
        long w = tag.getLong("w");
        long n = tag.getLong("n");
        long d = Math.max(1, tag.getLong("d"));
        return of(w, n, d);
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putLong("w", whole);
        tag.putLong("n", numerator);
        tag.putLong("d", denominator);
        return tag;
    }

    /** Reads a {@link FluidAmount} from a standard netty {@link ByteBuf}, using 3 {@link ByteBuf#readLong() longs}. */
    public static FluidAmount fromStdBuffer(ByteBuf buffer) {
        long w = buffer.readLong();
        long n = buffer.readLong();
        long d = Math.max(1, buffer.readLong());
        return of(w, n, d);
    }

    /** Writes a {@link FluidAmount} to a standard netty {@link ByteBuf}, using 3 {@link ByteBuf#writeLong(long)
     * longs}. */
    public void toStdBuffer(ByteBuf buffer) {
        buffer.writeLong(whole);
        buffer.writeLong(numerator);
        buffer.writeLong(denominator);
    }

    /** Reads a {@link FluidAmount} from a minecraft {@link PacketByteBuf}, using 3 {@link PacketByteBuf#readVarLong()
     * variable-length longs}. */
    public static FluidAmount fromMcBuffer(PacketByteBuf buffer) {
        long w = buffer.readVarLong();
        long n = buffer.readVarLong();
        long d = Math.max(1, buffer.readVarLong());
        return of(w, n, d);
    }

    /** Writes a {@link FluidAmount} to a minecraft {@link PacketByteBuf}, using 3
     * {@link PacketByteBuf#writeVarLong(long) variable-length longs}. */
    public void toMcBuffer(PacketByteBuf buffer) {
        buffer.writeVarLong(whole);
        buffer.writeVarLong(numerator);
        buffer.writeVarLong(denominator);
    }

    public static FluidAmount fromJson(JsonElement json) throws JsonSyntaxException {
        if (json.isJsonPrimitive()) {
            JsonPrimitive primitive = json.getAsJsonPrimitive();

            if (primitive.isString()) {
                Object result = tryParse(primitive.getAsString());
                if (result instanceof FluidAmount) {
                    return (FluidAmount) result;
                } else {
                    throw new JsonSyntaxException((String) result);
                }
            }

            if (primitive.isNumber()) {
                return FluidAmount.ofWhole(primitive.getAsLong());
            }
            throw new JsonSyntaxException("Cannot convert " + primitive + " to a FluidAmount!");
        } else {
            throw new JsonSyntaxException("Expected either a string or an integer, but got " + json + "!");
        }
    }

    public JsonElement toJson() {
        if (numerator == 0) {
            return new JsonPrimitive(whole);
        } else {
            return new JsonPrimitive(toParseableString());
        }
    }

    // Properties

    @Override
    public boolean isZero() {
        return whole == 0 && numerator == 0;
    }

    @Override
    public boolean isNegative() {
        return whole < 0 || numerator < 0;
    }

    @Override
    public boolean isPositive() {
        return whole > 0 || numerator > 0;
    }

    /** @return The sign: Either -1 if this is negative, +1 if this is positive, or 0 if this is zero. */
    @Override
    public int sign() {
        if (whole != 0) {
            return whole < 0 ? -1 : +1;
        }
        if (numerator != 0) {
            return numerator < 0 ? -1 : +1;
        }
        return 0;
    }

    /** @return True if this {@link FluidAmount} has potentially overflowed out of a long. This will return true if
     *         either {@link #whole} or {@link #numerator} is {@link Long#MIN_VALUE} or {@link Long#MAX_VALUE}, or if
     *         {@link #denominator} is {@link Long#MAX_VALUE}. */
    public boolean isOverflow() {
        return whole == Long.MIN_VALUE || whole == Long.MAX_VALUE || numerator == Long.MIN_VALUE
            || numerator == Long.MAX_VALUE || denominator == Long.MAX_VALUE;
    }

    @Override
    public FluidAmount getDivisor() {
        if (denominator == 1) {
            return ONE;
        } else {
            return createDirect(0, 1, denominator);
        }
    }

    /** @return Rounded-up value of this {@link FluidAmount} using a base of 1620. */
    public int as1620() {
        return asInt(1620);
    }

    /** @return Rounded-up value of this {@link FluidAmount} using a base of 1620. */
    public int as1620(RoundingMode rounding) {
        return asInt(1620, rounding);
    }

    /** @return Rounded-up integer value of this {@link FluidAmount} using the given base. Returns
     *         {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE} if the true value is out-of-range. */
    public int asInt(int base) {
        return asInt(base, RoundingMode.UP);
    }

    public int asInt(int base, RoundingMode rounding) {
        long lvalue = asLong(base, rounding);
        if (lvalue < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        } else if (lvalue > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) lvalue;
        }
    }

    /** @return Rounded-up long value of this {@link FluidAmount} using the given base. Returns {@link Long#MAX_VALUE}
     *         or {@link Long#MIN_VALUE} if the true value is out-of-range. */
    public long asLong(long base) {
        return asLong(base, RoundingMode.UP);
    }

    /** @return Rounded-up long value of this {@link FluidAmount} using the given base. Returns {@link Long#MAX_VALUE}
     *         or {@link Long#MIN_VALUE} if the true value is out-of-range. */
    public long asLong(long base, RoundingMode rounding) {
        if (base < 1) {
            throw new IllegalArgumentException("Base (" + base + ") must be greater than 0!");
        }
        FluidAmount mult = saturatedMul(base);
        if (mult.isOverflow()) {
            return mult.isNegative() ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        if (mult.numerator == 0) {
            return mult.whole;
        }
        switch (rounding) {
            case DOWN:
                return mult.whole;
            case UP:
                return LongMath.saturatedAdd(mult.whole, mult.sign());
            case CEILING:
                return mult.whole > 0 ? LongMath.saturatedAdd(mult.whole, +1) : mult.whole;
            case FLOOR:
                return mult.whole < 0 ? LongMath.saturatedAdd(mult.whole, -1) : mult.whole;
            case HALF_DOWN: {
                if (Math.abs(mult.numerator) <= mult.denominator / 2) {
                    return mult.whole;
                }
                return LongMath.saturatedAdd(mult.whole, mult.sign());
            }
            case HALF_UP: {
                // den = 3 (/2=1)
                // num = 1 -> 1 <= den/2 = true -> correct
                // num = 2 -> 2 <= den/2 = false -> correct
                long pnum = Math.abs(mult.numerator);
                if (pnum <= mult.denominator / 2) {
                    if (pnum * 2 < mult.denominator) {
                        return mult.whole;
                    }
                }
                return LongMath.saturatedAdd(mult.whole, mult.sign());
            }
            case HALF_EVEN: {
                long pnum = Math.abs(mult.numerator);
                if (pnum <= mult.denominator / 2) {
                    if (pnum * 2 < mult.denominator) {
                        return mult.whole;
                    } else if ((mult.whole & 1) == 0) {
                        return mult.whole;
                    }
                }
                return LongMath.saturatedAdd(mult.whole, mult.sign());
            }
            case UNNECESSARY:
                throw new ArithmeticException(
                    "Rounding Mode is 'UNNECESSARY', but the fraction has a non-zero numerator! " + this
                );
            default:
                throw new IllegalArgumentException("Unknown rounding mode " + rounding);
        }
    }

    // Object Methods

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof FluidAmount)) {
            return false;
        }
        return equals((FluidAmount) obj);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new long[] { whole, numerator, denominator });
    }

    /** @return A string version of this {@link FluidAmount} that should be shown to players. */
    public String toDisplayString() {
        // TODO: Look into repeating decimals!
        // (And potentially improvements to how minecraft actually renders combining chars...)
        if (numerator == 0 || denominator == 1) {
            return "" + whole;
        }
        String str = "" + whole;

        if (denominator < (1L << 52)) {
            // It should fit into the range of a double
            double fraction = numerator / (double) denominator;
            int decimalPlaces = 1 + Long.toString(denominator - 1).length();
            int roundingValue = (int) Math.pow(10, decimalPlaces);
            String fractionStr = Long.toString((long) (fraction * roundingValue));
            while (fractionStr.length() < decimalPlaces) {
                fractionStr = "0" + fractionStr;
            }
            while (fractionStr.endsWith("0")) {
                fractionStr = fractionStr.substring(0, fractionStr.length() - 1);
            }
            return str + "." + fractionStr;
        }

        // We can't use double because it doesn't have enough bits to support the long's
        // so instead we have to do it the long way.
        return "{TODO: Display this huge fraction properly (" + whole + " + " + numerator + "/" + denominator + ")}";
        // ...oh. That wasn't very long?
    }

    @Override
    public String toString() {
        return "{FluidAmount " + whole + " + " + numerator + "/" + denominator + "}";
    }

    /** @return This {@link FluidAmount} represented as a string that could be parsed by {@link #parse(String)} to
     *         return another {@link FluidAmount} equal to itself. */
    public String toParseableString() {
        String str = toParseableString0();
        assert this.equals(parse(str));
        return str;
    }

    private String toParseableString0() {
        if (whole == 0) {
            if (numerator == 0) {
                return "0";
            }
            return numerator + "/" + denominator;
        } else {
            if (numerator == 0) {
                return Long.toString(whole);
            }
            if (numerator < 0) {
                return whole + " " + numerator + "/" + denominator;
            }
            return whole + "+" + numerator + "/" + denominator;
        }
    }

    // Comparison

    /** @return True if the number that this {@link FluidAmount} represents is equal to the number that the given
     *         {@link FluidAmount} represents. */
    public boolean equals(FluidAmount other) {
        return whole == other.whole && numerator == other.numerator && denominator == other.denominator;
    }

    @Override
    public int compareTo(@Nullable FluidAmount o) {
        if (o == null) {
            return sign();
        }
        if (whole != o.whole) {
            return Long.compare(whole, o.whole);
        }
        if (denominator == o.denominator) {
            return Long.compare(numerator, o.numerator);
        }
        normal: {
            long a = LongMath.saturatedMultiply(numerator, o.denominator);
            long b = LongMath.saturatedMultiply(o.numerator, denominator);
            if (didOverflow(a) || didOverflow(b)) {
                break normal;
            }
            return Long.compare(a, b);
        }
        BigInteger a = _bigNumerator().multiply(o._bigDenominator());
        BigInteger b = o._bigNumerator().multiply(_bigDenominator());
        return a.compareTo(b);
    }

    // TODO: Are these public?

    // public boolean isLessThan(FluidAmount other) {
    // return compareTo(other) < 0;
    // }
    //
    // public boolean isGreaterThan(FluidAmount other) {
    // return compareTo(other) > 0;
    // }

    @Override
    public double asInexactDouble() {
        return whole + numerator / (double) denominator;
    }

    // ###########
    //
    // Operators
    //
    // ###########

    @Override
    public FluidAmount negate() {
        return createDirect(-whole, -numerator, denominator);
    }

    @Override
    public FluidAmount lcm(FluidAmount other) {
        return _bigLcm(other).asLongIntExact();
    }

    // --------
    // Addition
    // --------

    public static final class SafeAddResult {
        public final FluidAmount roundedResult;

        /** The true value of the two {@link FluidAmount}'s added together, equal to
         * {@link FluidAmount#bigAdd(FluidAmount)}. */
        public final BigFluidAmount exactValue;

        private BigFluidAmount error;

        public SafeAddResult(FluidAmount roundedResult, BigFluidAmount exactValue) {
            this.roundedResult = roundedResult;
            this.exactValue = exactValue;
        }

        public SafeAddResult(FluidAmount exactValue) {
            this.roundedResult = exactValue;
            this.exactValue = roundedResult.asBigInt();
        }

        public SafeAddResult(BigFluidAmount exactValue, RoundingMode rounding) {
            this.roundedResult = exactValue.asLongIntRounded(rounding);
            this.exactValue = exactValue;
        }

        @Override
        public String toString() {
            return "{SafeAddResult rounded=" + roundedResult + " exact=" + exactValue + " error=" + getError() + " }";
        }

        /** @return The difference between the {@link #exactValue} and the {@link #roundedResult}. This will always be
         *         equal to the <code>exactValue.sub(roundedResult)</code> */
        public BigFluidAmount getError() {
            if (error == null) {
                error = exactValue.sub(roundedResult);
            }
            return error;
        }
    }

    /** The recommended method for adding two {@link FluidAmount}'s together if you don't want to think about inexact
     * answers.
     * <p>
     * (Internally this calls {@link FluidAmount#safeAdd(FluidAmount)} if you want to know the details).
     * 
     * @param other The other {@link FluidAmount}. Null values will return "this". */
    public FluidAmount add(@Nullable FluidAmount other) {
        return roundedAdd(other);
    }

    /** Safely adds the given {@link FluidAmount} to this one, returning the merged result. Unlike
     * {@link #checkedAdd(FluidAmount)} this will only throw an {@link ArithmeticException} if the result is
     * out-of-range and the rounding mode is {@link RoundingMode#UNNECESSARY}, instead it will round the real answer to
     * the nearest valid {@link FluidAmount} (using {@link BigFluidAmount#asLongIntRounded(RoundingMode)}) */
    public SafeAddResult safeAdd(@Nullable FluidAmount other, RoundingMode rounding) {
        if (other == null) {
            return new SafeAddResult(this);
        }
        return new SafeAddResult(bigAdd(other), rounding);
    }

    /** Safely adds the given {@link FluidAmount} to this one, returning the merged result. Unlike
     * {@link #checkedAdd(FluidAmount)} this will never throw an {@link ArithmeticException} if the result is
     * out-of-range, instead it will round the real answer to the nearest valid {@link FluidAmount} (using
     * {@link BigFluidAmount#asLongIntRounded(RoundingMode)} with a rounding mode of {@link RoundingMode#HALF_EVEN
     * HALF_EVEN}). */
    public SafeAddResult safeAdd(@Nullable FluidAmount other) {
        return safeAdd(other, RoundingMode.HALF_EVEN);
    }

    /** @return the result of {@link #safeAdd(FluidAmount, RoundingMode)}.{@link SafeAddResult#roundedResult
     *         roundedResult}. */
    public FluidAmount roundedAdd(@Nullable FluidAmount other, RoundingMode rounding) {
        return add0(other, rounding);
    }

    /** @return the result of {@link #safeAdd(FluidAmount)}.{@link SafeAddResult#roundedResult roundedResult}. */
    public FluidAmount roundedAdd(@Nullable FluidAmount other) {
        return roundedAdd(other, RoundingMode.HALF_EVEN);
    }

    /** Adds the given long value to this {@link FluidAmount}, without performing any checking or saturation. */
    public FluidAmount add(long by) {
        if (by == 0) {
            return this;
        } else if (isZero()) {
            return ofWhole(by);
        }
        return of(by + whole, numerator, denominator);
    }

    /** Directly adds the given {@link FluidAmount} to this one.
     * 
     * @param by The amount to add. If it's null or zero then "this" will be returned.
     * @throws ArithmeticException if the result doesn't fit into a {@link FluidAmount}. */
    public FluidAmount checkedAdd(@Nullable FluidAmount by) {
        return add0(by, RoundingMode.UNNECESSARY);
    }

    /** Similar to {@link #checkedAdd(FluidAmount)}, but returns either {@link #MAX_BUCKETS} or {@link #MIN_BUCKETS}
     * instead of throwing an exception. */
    public FluidAmount saturatedAdd(@Nullable FluidAmount by) {
        return add0(by, RoundingMode.HALF_EVEN);
    }

    private FluidAmount add0(FluidAmount by, RoundingMode rounding) {
        if (by == null || by.isZero()) {
            return this;
        } else if (isZero()) {
            return by;
        }

        if (rounding == null) {
            rounding = RoundingMode.HALF_EVEN;
        }

        // W3 + N3/D3 = W1 + N1/D1 + W2 + N2/D2
        // .. = W1+W2 + N1/D1 + N2/D2
        // .. = W1+W2 + (N1*D2)/(D1*D2) + (N2*D1)/(D1*D2)
        // .. = W1+W2 + (N1*D2 + N2*D1) / (D1*D2)
        // W3 = W1+W2
        // N3 = N1*D2+N2*D1
        // D3 = D1*D2
        long w = LongMath.saturatedAdd(whole, by.whole);
        if (didOverflow(w)) {
            if (rounding == RoundingMode.UNNECESSARY) {
                throw new ArithmeticException(
                    "Cannot add the values " + whole + " and " + by.whole
                        + " as they overflow, and the RoundingMode is UNNECESSARY!"
                );
            }
            return w > 0 ? MAX_BUCKETS : MIN_BUCKETS;
        }
        normal: {
            long d = LongMath.saturatedMultiply(denominator, by.denominator);
            if (didOverflow(d)) break normal;
            long n1d2 = LongMath.saturatedMultiply(numerator, by.denominator);
            if (didOverflow(n1d2)) break normal;
            long n2d1 = LongMath.saturatedMultiply(by.numerator, denominator);
            if (didOverflow(n2d1)) break normal;
            long n = LongMath.saturatedAdd(n1d2, n2d1);
            if (didOverflow(n2d1)) break normal;
            return of(w, n, d);
        }
        BigFluidAmount bigResult = _bigAdd(by);
        return bigResult.asLongIntRounded(rounding);
    }

    /** Directly adds the given {@link FluidAmount} to this one, returning the result as a {@link BigFluidAmount}.
     * 
     * @param by The amount to add. If it's null or zero then this will be returned, converted to a
     *            {@link BigFluidAmount}. */
    public BigFluidAmount bigAdd(@Nullable FluidAmount by) {
        if (by == null || by.isZero()) {
            return asBigInt();
        } else if (this.isZero()) {
            return by.asBigInt();
        }
        return _bigAdd(by);
    }

    /** Directly adds the given {@link BigFluidAmount} to this one.
     * 
     * @param by The amount to add. If it's null or zero then this will be returned, converted into a
     *            {@link BigFluidAmount}. */
    public BigFluidAmount add(@Nullable BigFluidAmount by) {
        if (by == null || by.isZero()) {
            return asBigInt();
        } else if (this.isZero()) {
            return by;
        }
        return _bigAdd(by);
    }

    // -----------
    // Subtraction
    // -----------

    public FluidAmount sub(long by) {
        return add(-by);
    }

    /** The recommended method for subtracting another {@link FluidAmount} from this if you don't want to think about
     * inexact answers.
     * <p>
     * (Internally this calls {@link FluidAmount#roundedSub(FluidAmount)} if you want to know the details).
     * 
     * @param other The other {@link FluidAmount}. Null values will return "this". */
    public FluidAmount sub(@Nullable FluidAmount other) {
        return roundedSub(other);
    }

    /** @param by Either Null or a value that will be {@link #negate() negated} and then passed to
     *            {@link #checkedAdd(FluidAmount)}.
     * @throws ArithmeticException if the result doesn't fit into a {@link FluidAmount}. */
    public FluidAmount checkedSub(@Nullable FluidAmount by) {
        return by == null ? this : checkedAdd(by.negate());
    }

    /** @param by Either Null or a value that will be {@link #negate() negated} and then passed to
     *            {@link #saturatedAdd(FluidAmount)}. */
    public FluidAmount saturatedSub(@Nullable FluidAmount by) {
        return by == null ? this : saturatedAdd(by.negate());
    }

    /** @param by Either Null or a value that will be {@link #negate() negated} and then passed to
     *            {@link #safeAdd(FluidAmount, RoundingMode)}. */
    public SafeAddResult safeSub(@Nullable FluidAmount by, RoundingMode rounding) {
        return by == null ? new SafeAddResult(this) : safeAdd(by.negate(), rounding);
    }

    /** @param by Either Null or a value that will be {@link #negate() negated} and then passed to
     *            {@link #safeAdd(FluidAmount)}. */
    public SafeAddResult safeSub(@Nullable FluidAmount by) {
        return by == null ? new SafeAddResult(this) : safeAdd(by.negate());
    }

    /** @param by Either Null or a value that will be {@link #negate() negated} and then passed to
     *            {@link #safeAdd(FluidAmount, RoundingMode)}.
     * @return The {@link SafeAddResult#roundedResult}. */
    public FluidAmount roundedSub(@Nullable FluidAmount by, RoundingMode rounding) {
        return by == null ? this : roundedAdd(by.negate(), rounding);
    }

    /** @param by Either Null or a value that will be {@link #negate() negated} and then passed to
     *            {@link #safeAdd(FluidAmount)}.
     * @return The {@link SafeAddResult#roundedResult}. */
    public FluidAmount roundedSub(@Nullable FluidAmount by) {
        return by == null ? this : roundedAdd(by.negate());
    }

    /** @param by Either Null or a value that will be {@link #negate() negated} and then passed to
     *            {@link #add(BigFluidAmount)}. */
    public BigFluidAmount sub(@Nullable BigFluidAmount by) {
        return by == null ? asBigInt() : add(by.negate());
    }

    // -----------
    // Merging
    // -----------

    /** Specifies how merged {@link FluidAmount}'s should handle rounding. */
    public enum FluidMergeRounding {
        /** Specifies that any fluid amounts that don't completely fit together should fail. */
        FAIL(null),

        /** Move only an amount which can be validly moved, leaving the rest in the source. (As such fluid will never be
         * lost, but you might get some leftovers). */
        MAXIMUM_POSSIBLE(null),

        /** Always empty the source, but leave the target with (potentially) slightly more fluid than the exact sum of
         * the two volumes.
         * <p>
         * Use {@link FluidAmount#safeAdd(FluidAmount, RoundingMode)} with a {@link RoundingMode} of
         * {@link RoundingMode#UP UP}. */
        ROUND_UP(RoundingMode.UP),

        /** Always empty the source, but leave the target with (potentially) slightly less fluid than the exact sum of
         * the two volumes.
         * <p>
         * Use {@link FluidAmount#safeAdd(FluidAmount, RoundingMode)} with a {@link RoundingMode} of
         * {@link RoundingMode#DOWN DOWN}. */
        ROUND_DOWN(RoundingMode.DOWN),

        /** Always empty the source, but leave the target with (potentially) slightly different amount fluid than the
         * exact sum of the two volumes.
         * <p>
         * Use {@link FluidAmount#safeAdd(FluidAmount, RoundingMode)} with a {@link RoundingMode} of
         * {@link RoundingMode#UP UP}. */
        ROUND_CEILING(RoundingMode.CEILING),

        /** Always empty the source, but leave the target with (potentially) slightly different amount fluid than the
         * exact sum of the two volumes.
         * <p>
         * Use {@link FluidAmount#safeAdd(FluidAmount, RoundingMode)} with a {@link RoundingMode} of
         * {@link RoundingMode#FLOOR FLOOR}. */
        ROUND_FLOOR(RoundingMode.FLOOR),

        /** Always empty the source, but leave the target with (potentially) slightly different amount of fluid than the
         * exact sum of the two volumes.
         * <p>
         * Use {@link FluidAmount#safeAdd(FluidAmount, RoundingMode)} with a {@link RoundingMode} of
         * {@link RoundingMode#HALF_UP HALF_UP}. */
        ROUND_HALF_UP(RoundingMode.HALF_UP),

        /** Always empty the source, but leave the target with (potentially) slightly different amount of fluid than the
         * exact sum of the two volumes.
         * <p>
         * Use {@link FluidAmount#safeAdd(FluidAmount, RoundingMode)} with a {@link RoundingMode} of
         * {@link RoundingMode#HALF_DOWN HALF_DOWN}. */
        ROUND_HALF_DOWN(RoundingMode.HALF_DOWN),

        /** Always empty the source, but leave the target with (potentially) slightly different amount of fluid than the
         * exact sum of the two volumes.
         * <p>
         * Use {@link FluidAmount#safeAdd(FluidAmount, RoundingMode)} with a {@link RoundingMode} of
         * {@link RoundingMode#HALF_EVEN HALF_EVEN}. */
        ROUND_HALF_EVEN(RoundingMode.HALF_EVEN),

        /** Throw an {@link ArithmeticException} if the end result needs to be rounded.
         * <p>
         * Use {@link FluidAmount#checkedAdd(FluidAmount)}. */
        ROUND_UNNECESSARY(RoundingMode.UNNECESSARY);

        public static final FluidMergeRounding DEFAULT = ROUND_HALF_EVEN;

        /** The rounding mode to use, or null if this requires special handling. */
        @Nullable
        public final RoundingMode rounding;

        private FluidMergeRounding(@Nullable RoundingMode rounding) {
            this.rounding = rounding;
        }

        public static FluidMergeRounding fromRounding(@Nullable RoundingMode rounding) {
            if (rounding == null) {
                return FAIL;
            }
            FluidMergeRounding r = values()[rounding.ordinal() + 2];
            assert r.rounding == rounding;
            return r;
        }
    }

    public static final class FluidMergeResult {

        /** The result of the merging. */
        public final FluidAmount merged;

        /** The leftover from the merging. If the {@link FluidMergeRounding} wasn't
         * {@link FluidMergeRounding#MAXIMUM_POSSIBLE} or {@link FluidMergeRounding#FAIL} then this will be zero. */
        public final FluidAmount excess;

        /** The amount that was lost (or gained, if this is positive) due to rounding. */
        public final BigFluidAmount roundingError;

        /** Constructs a new {@link FluidMergeResult} with the given {@link #merged} value, and with both
         * {@link #excess} and {@link #roundingError} set to zero. */
        public FluidMergeResult(FluidAmount merged) {
            this.merged = merged;
            this.excess = ZERO;
            this.roundingError = BigFluidAmount.ZERO;
        }

        public FluidMergeResult(FluidAmount merged, FluidAmount excess, BigFluidAmount error) {
            this.merged = merged;
            this.excess = excess;
            this.roundingError = error;
        }
    }

    /** Calls {@link FluidAmount#merge(FluidAmount, FluidAmount, FluidMergeRounding)} with a {@link FluidMergeRounding}
     * of {@link FluidMergeRounding#ROUND_HALF_EVEN ROUND_HALF_EVEN}. */
    public static FluidMergeResult merge(FluidAmount target, FluidAmount toAdd) {
        return merge(target, toAdd, FluidMergeRounding.ROUND_HALF_EVEN);
    }

    /** @param target
     * @param toAdd
     * @param rounding The {@link FluidMergeRounding} to use if the addition doesn't result in an exact
     *            {@link FluidAmount}.
     * @return */
    public static FluidMergeResult merge(FluidAmount target, FluidAmount toAdd, FluidMergeRounding rounding) {
        switch (rounding) {
            case ROUND_UNNECESSARY: {
                return new FluidMergeResult(target.checkedAdd(toAdd), ZERO, BigFluidAmount.ZERO);
            }
            case MAXIMUM_POSSIBLE: {
                SafeAddResult result = target.safeAdd(toAdd);
                if (result.getError().isZero()) {
                    return new FluidMergeResult(result.roundedResult);
                }

                FluidAmount d1 = target.getDivisor();
                FluidAmount d2 = toAdd.getDivisor();
                FluidAmount movable = d1.lcm(d2);
                long multiple = toAdd.getCountOf(movable);
                if (multiple == 0) {
                    return new FluidMergeResult(target, toAdd, BigFluidAmount.ZERO);
                }
                FluidAmount toMove = movable.checkedMul(multiple);
                SafeAddResult merged = target.safeAdd(toMove);
                SafeAddResult excess = toAdd.safeSub(toMove);
                if (merged.roundedResult.isOverflow() || excess.roundedResult.isOverflow()) {
                    // Technically we could try to move a little bit less, if the multiple is more than 1.
                    return new FluidMergeResult(target, toAdd, BigFluidAmount.ZERO);
                }
                assert merged.getError().isZero() : merged;
                assert excess.getError().isZero() : excess;
                return new FluidMergeResult(merged.roundedResult, excess.roundedResult, BigFluidAmount.ZERO);
            }
            case FAIL: {
                SafeAddResult result = target.safeAdd(toAdd);
                if (result.getError().isZero()) {
                    return new FluidMergeResult(result.roundedResult);
                } else {
                    return new FluidMergeResult(target, toAdd, BigFluidAmount.ZERO);
                }
            }
            default: {
                assert rounding.rounding != null : "Unknown mode " + rounding;
                SafeAddResult result = target.safeAdd(toAdd, rounding.rounding);
                return new FluidMergeResult(result.roundedResult, FluidAmount.ZERO, result.getError());
            }
        }
    }

    /** @param target
     * @param toAdd
     * @param denominatorTarget The denominator that the target must be under (a multiple of it's reciprocal), or 0 if
     *            the target doesn't need to stay under a particular denominator.
     * @param denominatorAdd The denominator that the excess must be under (a multiple of it's reciprocal), or 0 if the
     *            excess doesn't need to stay under a particular denominator. */
    public static FluidMergeResult merge(
        FluidAmount target, FluidAmount toAdd, long denominatorTarget, long denominatorAdd
    ) {
        // Validate that we have been passed the right values
        if (denominatorTarget < 0) {
            throw new IllegalArgumentException("denominatorTarget must be >= 0!");
        } else if (denominatorTarget > 0) {
            if (denominatorTarget < target.denominator) {
                throw new IllegalArgumentException(
                    "The target " + target + " has a denominator greater than the required " + denominatorTarget + "!"
                );
            }
            long rem = denominatorTarget % target.denominator;
            if (rem != 0) {
                throw new IllegalArgumentException(
                    "The target " + target + " has a denominator that's not a multiple of the required "
                        + denominatorTarget + "!"
                );
            }
        }

        if (denominatorAdd < 0) {
            throw new IllegalArgumentException("denominatorAdd must be >= 0!");
        } else if (denominatorAdd > 0) {
            if (denominatorAdd < toAdd.denominator) {
                throw new IllegalArgumentException(
                    "The 'toAdd' " + toAdd + " has a denominator greater than the required " + denominatorAdd + "!"
                );
            }
            long rem = denominatorAdd % toAdd.denominator;
            if (rem != 0) {
                throw new IllegalArgumentException(
                    "The 'toAdd' " + toAdd + " has a denominator that's not a multiple of the required "
                        + denominatorAdd + "!"
                );
            }
        }
        long gcd = LongMath.gcd(denominatorAdd, denominatorTarget);
        if (gcd == 0) {
            // No requirements - just use normal addition
            return new FluidMergeResult(target.checkedAdd(toAdd), ZERO, BigFluidAmount.ZERO);
        }

        throw new AbstractMethodError("// TODO: Implement this!");
    }

    // --------------
    // Multiplication
    // --------------

    /** The recommended method for multiplying two {@link FluidAmount}'s together if you don't want to think about
     * inexact answers.
     * <p>
     * (Internally this calls {@link FluidAmount#roundedMul(FluidAmount)} if you want to know the details). */
    public FluidAmount mul(FluidAmount by) {
        return roundedMul(by);
    }

    /** The recommended method for multiplying this by a long if you don't want to think about inexact answers.
     * <p>
     * (Internally this calls {@link FluidAmount#roundedMul(long)} if you want to know the details). */
    public FluidAmount mul(long by) {
        return roundedMul(by);
    }

    public FluidAmount checkedMul(long by) {
        return roundedMul(by, RoundingMode.UNNECESSARY);
    }

    public FluidAmount saturatedMul(long by) {
        if (by == 0 || isZero()) {
            return ZERO;
        } else if (by == 1) {
            return this;
        } else if (by == -1) {
            return negate();
        }
        long nb = LongMath.saturatedMultiply(numerator, by);
        long w = LongMath.saturatedMultiply(whole, by);
        if (didOverflow(nb) || didOverflow(w)) {
            return isPositive() == (by > 0) ? MAX_BUCKETS : MIN_BUCKETS;
        }
        long div = nb / denominator;
        long rem = nb % denominator;
        w = LongMath.saturatedAdd(w, div);
        if (didOverflow(w)) {
            return isPositive() == (by > 0) ? MAX_BUCKETS : MIN_BUCKETS;
        }
        return of(w, rem, denominator);
    }

    public FluidAmount roundedMul(long by) {
        return roundedMul(by, RoundingMode.HALF_EVEN);
    }

    public FluidAmount roundedMul(long by, RoundingMode rounding) {
        if (by == 0) {
            return ZERO;
        } else if (by == 1) {
            return this;
        } else if (by == -1) {
            return negate();
        }
        long nb = LongMath.saturatedMultiply(by, numerator);
        long w = LongMath.saturatedMultiply(whole, by);
        if (!didOverflow(nb) && !didOverflow(w)) {
            return of(w, nb, denominator);
        }
        return _bigMul(ofWhole(by)).asLongIntRounded(rounding);
    }

    public FluidAmount checkedMul(FluidAmount by) {
        return mul0(by, RoundingMode.UNNECESSARY);
    }

    public FluidAmount saturatedMul(FluidAmount by) {
        return mul0(by, null);
    }

    public FluidAmount roundedMul(FluidAmount by) {
        return mul0(by, RoundingMode.HALF_EVEN);
    }

    public FluidAmount roundedMul(FluidAmount by, RoundingMode rounding) {
        return mul0(by, rounding);
    }

    private FluidAmount mul0(FluidAmount by, RoundingMode rounding) {
        if (by.isZero() || isZero()) {
            return ZERO;
        } else if (by.equals(ONE)) {
            return this;
        } else if (equals(ONE)) {
            return by;
        } else if (by.equals(NEGATIVE_ONE)) {
            return negate();
        } else if (equals(NEGATIVE_ONE)) {
            return by.negate();
        }

        // w3 + n3/d3 = (w1 + n1/d1) * (w2 + n2/d2)
        // w3 + n3/d3 = w1*w2 + (w1 * n2)/d2 + (w2 * n1)/d1 + (n1*n2)/(d1*d2)
        // w3 = w1*w2
        // n3/d3 = (w1*n2)/d2 + (w2*n1/d1) + (n1*n2)/(d1*d2)
        // n3/d3 = (w1*n2*d1)/(d1*d2) + (w2*n1*d2)/(d1*d2) + (n1*n2)/(d1*d2)
        // n3/d3 = (w1*n2*d1 + w2*n1*d2 + n1*n2)/(d1*d2)

        long w1 = whole;
        long w2 = by.whole;

        long n1 = numerator;
        long n2 = by.numerator;

        long d1 = denominator;
        long d2 = by.denominator;

        final long w3;
        if (rounding == RoundingMode.UNNECESSARY) {
            w3 = LongMath.checkedMultiply(w1, w2);
        } else {
            w3 = LongMath.saturatedMultiply(w1, w2);
            if (didOverflow(w3)) {
                return w3 < 0 ? MIN_BUCKETS : MAX_BUCKETS;
            }
        }

        normal: {
            // Just check to see if we will fit in long's all the way...
            long w1n2 = LongMath.saturatedMultiply(w1, n2);
            if (didOverflow(w1n2)) break normal;
            long w1n2d1 = LongMath.saturatedMultiply(w1n2, d1);
            if (didOverflow(w1n2d1)) break normal;

            long w2n1 = LongMath.saturatedMultiply(w2, n1);
            if (didOverflow(w2n1)) break normal;
            long w2n1d2 = LongMath.saturatedMultiply(w2n1, d2);
            if (didOverflow(w2n1d2)) break normal;

            long d1d2 = LongMath.saturatedMultiply(d1, d2);
            if (didOverflow(d1d2)) break normal;
            long n1n2 = LongMath.saturatedMultiply(n1, n2);
            if (didOverflow(n1n2)) break normal;

            long n3_a = LongMath.saturatedAdd(w1n2d1, w2n1d2);
            if (didOverflow(n3_a)) break normal;
            long n3 = LongMath.saturatedAdd(n3_a, n1n2);
            if (didOverflow(n3)) break normal;

            // No overflow: we can compute the result using only long's
            // Rely on the static factory to re-balance.
            return of(w3, n3, d1d2);
        }

        BigFluidAmount result = _bigMul(by);
        return rounding == null ? result.asLongIntSaturated() : result.asLongIntRounded(rounding);
    }

    public BigFluidAmount mul(BigFluidAmount by) {
        return _bigMul(by);
    }

    public BigFluidAmount bigMul(FluidAmount by) {
        return _bigMul(by);
    }

    private static boolean didOverflow(long value) {
        return value == Long.MIN_VALUE || value == Long.MAX_VALUE;
    }

    @Override
    public FluidAmount reciprocal() {
        return ONE.div(this);
    }

    public BigFluidAmount bigReciprocal() {
        return _bigReciprocal();
    }

    // --------------
    // Division
    // --------------

    /** @return The {@link FluidAmount#whole} value from {@link #saturatedDiv(FluidAmount)}. */
    public long getCountOf(FluidAmount by) {
        return saturatedDiv(by).whole;
    }

    /** The recommended method for dividing this by a {@link Long} if you don't want to think about inexact answers.
     * <p>
     * (Internally this calls {@link FluidAmount#roundedDiv(long)} if you want to know the details). */
    public FluidAmount div(long other) {
        return roundedDiv(other);
    }

    /** The recommended method for dividing this by another {@link FluidAmount} if you don't want to think about inexact
     * answers.
     * <p>
     * (Internally this calls {@link FluidAmount#roundedDiv(FluidAmount)} if you want to know the details). */
    public FluidAmount div(FluidAmount other) {
        return roundedDiv(other);
    }

    public FluidAmount checkedDiv(long by) {
        return divInner(by, RoundingMode.UNNECESSARY);
    }

    public FluidAmount saturatedDiv(long by) {
        return divInner(by, null);
    }

    public FluidAmount roundedDiv(long by) {
        return divInner(by, RoundingMode.HALF_EVEN);
    }

    public FluidAmount roundedDiv(long by, RoundingMode rounding) {
        return divInner(by, rounding);
    }

    private FluidAmount divInner(long by, RoundingMode rounding) {
        if (by == 1) {
            return this;
        } else if (by == -1) {
            return negate();
        } else if (by == 0) {
            throw new ArithmeticException("divide by 0");
        }

        normal: {
            long w1 = this.whole;
            long w2 = by;
            long n1 = this.numerator;
            long d1 = this.denominator;

            long w1d1 = LongMath.saturatedMultiply(w1, d1);
            if (didOverflow(w1d1)) break normal;
            long n1_w1d1 = LongMath.saturatedAdd(n1, w1d1);
            if (didOverflow(n1_w1d1)) break normal;
            long w2d1 = LongMath.saturatedMultiply(w2, d1);
            if (didOverflow(w2d1)) break normal;
            return of(n1_w1d1, w2d1);
        }

        BigFluidAmount result = _bigDiv(FluidAmount.of(by, 0, 1));
        return rounding == null ? result.asLongIntSaturated() : result.asLongIntRounded(rounding);
    }

    public FluidAmount checkedDiv(FluidAmount by) {
        return divInner(by, RoundingMode.UNNECESSARY);
    }

    public FluidAmount saturatedDiv(FluidAmount by) {
        return divInner(by, null);
    }

    public FluidAmount roundedDiv(FluidAmount by) {
        return divInner(by, RoundingMode.HALF_EVEN);
    }

    public FluidAmount roundedDiv(FluidAmount by, RoundingMode rounding) {
        return divInner(by, rounding);
    }

    private FluidAmount divInner(FluidAmount by, @Nullable RoundingMode rounding) {
        if (by.equals(ONE)) {
            return this;
        } else if (by.equals(NEGATIVE_ONE)) {
            return negate();
        } else if (by.isZero()) {
            throw new ArithmeticException("divide by 0");
        }

        normal: {
            long w1 = this.whole;
            long w2 = by.whole;

            long n1 = this.numerator;
            long n2 = by.numerator;

            long d1 = this.denominator;
            long d2 = by.denominator;

            long n1d2 = LongMath.saturatedMultiply(n1, d2);
            if (didOverflow(n1d2)) break normal;

            long w1d1 = LongMath.saturatedMultiply(w1, d1);
            if (didOverflow(w1d1)) break normal;

            long w1d1d2 = LongMath.saturatedMultiply(w1d1, d2);
            if (didOverflow(w1d1d2)) break normal;

            long n1d2_w1d1d2 = LongMath.saturatedAdd(n1d2, w1d1d2);
            if (didOverflow(n1d2_w1d1d2)) break normal;

            long n2d1 = LongMath.saturatedMultiply(n2, d1);
            if (didOverflow(n2d1)) break normal;

            long w2d1 = LongMath.saturatedMultiply(w2, d1);
            if (didOverflow(w2d1)) break normal;

            long w2d1d2 = LongMath.saturatedMultiply(w2d1, d2);
            if (didOverflow(w2d1d2)) break normal;

            long n2d1_w2d1d2 = LongMath.saturatedAdd(n2d1, w2d1d2);
            if (didOverflow(n2d1_w2d1d2)) break normal;

            return of(n1d2_w1d1d2, n2d1_w2d1d2);
        }

        BigFluidAmount result = bigDiv(by);
        return rounding == null ? result.asLongIntSaturated() : result.asLongIntRounded(rounding);
    }

    public BigFluidAmount div(BigFluidAmount by) {
        return _bigDiv(by);
    }

    public BigFluidAmount bigDiv(FluidAmount by) {
        return _bigDiv(by);
    }

    // --------------
    // Splitting
    // --------------

    /** Splits this {@link FluidAmount} evenly into the given count, but not letting the denominator exceed the default
     * value (2000). If the denominator is bigger than the default then some of the entries might be
     * {@link FluidAmount#ZERO}.
     * 
     * @return An array with length "count" containing the split fluids. May contain duplicates. */
    public FluidAmount[] splitBalanced(int count) {
        return splitBalanced(count, 2000);
    }

    /** Splits this {@link FluidAmount} evenly into the given count, but not letting the denominator exceed the given
     * value. If the denominator is bigger than the default then some of the entries might be {@link FluidAmount#ZERO}.
     * 
     * @return An array with length "count" containing the split fluids. May contain duplicates. */
    public FluidAmount[] splitBalanced(int count, long maxDenominator) {
        return splitBalanced(new FluidAmount[count], maxDenominator);
    }

    /** Splits this {@link FluidAmount} evenly into the given count, but not letting the denominator exceed the given
     * value. If the denominator is bigger than the default then some of the entries might be {@link FluidAmount#ZERO}.
     * 
     * @param dest The array to place the {@link FluidAmount}s into, which will also be returned.
     * @return The "dest" array, which will contain the split fluids. May contain duplicates.
     * @throws IllegalArgumentException if "dest" is an array of length 0 and this is not {@link #isZero()}. */
    public FluidAmount[] splitBalanced(FluidAmount[] dest, long maxDenominator) {
        FluidAmount[] ret = splitBalanced0(dest, maxDenominator);

        boolean validate = false;
        assert validate = true;
        if (validate) {
            assert ret == dest : "ret != dest";
            FluidAmount total = ZERO;
            for (FluidAmount in : dest) {
                total = total.checkedAdd(in);
            }
            assert equals(total) : "this " + this + " != total " + total + " for " + Arrays.toString(ret);
        }

        return ret;
    }

    private FluidAmount[] splitBalanced0(FluidAmount[] dest, long maxDenominator) {
        int count = dest.length;
        if (count == 1) {
            dest[0] = this;
            return dest;
        } else if (count == 0) {
            if (isZero()) {
                return dest;
            }
            throw new IllegalArgumentException("Cannot balance a FluidAmount into nothing unless we're zero!");
        }

        long realDivisor = LongMath.saturatedMultiply(denominator, count);
        if (!didOverflow(realDivisor) && realDivisor <= maxDenominator) {
            // We're below (or equal to) the maximum denominator
            // So we don't need to do anything special
            FluidAmount amount = div(count);
            Arrays.fill(dest, amount);
            return dest;
        } else {
            // Divide up the numerator and whole parts by count

            long numeratorPer = numerator / count;
            long overflowNumerator = numerator % count;

            long wholePer = whole / count;
            long overflowWhole = whole % count;

            if (overflowWhole != 0) {
                long addToNumerator = denominator * overflowWhole / count;
                long remainder = denominator * overflowWhole % count;
                if (remainder >= count) {
                    addToNumerator += remainder / count;
                    remainder %= count;
                }
                overflowNumerator += remainder;

                if (overflowNumerator > count) {
                    overflowNumerator -= count;
                    numeratorPer++;
                } else if (overflowNumerator < -count) {
                    overflowNumerator += count;
                    numeratorPer--;
                }

                numeratorPer += addToNumerator;
                if (numeratorPer > denominator) {
                    numeratorPer -= denominator;
                    wholePer++;
                } else if (numeratorPer < -denominator) {
                    numeratorPer += denominator;
                    wholePer--;
                }
            }

            if (overflowNumerator == 0) {
                // Exact split
                FluidAmount amount = of(wholePer, numeratorPer, denominator);
                Arrays.fill(dest, amount);
                return dest;
            } else {
                if (overflowNumerator < 0) {
                    numeratorPer--;
                    overflowNumerator = -overflowNumerator;
                }
                // Some of overflowNumerator needs to go to the bigger amount, the rest to the smaller
                FluidAmount bigger = of(wholePer, numeratorPer + 1, denominator);
                FluidAmount smaller = of(wholePer, numeratorPer, denominator);
                Arrays.fill(dest, 0, (int) overflowNumerator, bigger);
                Arrays.fill(dest, (int) overflowNumerator, dest.length, smaller);
                return dest;
            }

        }
    }

    // /** Splits this {@link FluidAmount} up according to the ratios given, but not letting the denominator exceed the
    // * default value (2000). If the denominator is bigger than the default then some of the entries might be
    // * {@link FluidAmount#ZERO}.
    // *
    // * @param ratios An array with the same length as the ratios array.
    // * @return An array that's the same size as the ratio array containing the split fluids. May contain duplicates.
    // */
    // public FluidAmount[] splitRatio(FluidAmount[] ratios) {
    // return splitRatio(ratios, 2000);
    // }
    //
    // public FluidAmount[] splitRatio(FluidAmount[] ratios, long maxDenominator) {
    //
    // }

    // Internal

    @Override
    BigInteger _bigWhole() {
        return BigInteger.valueOf(whole);
    }

    @Override
    BigInteger _bigNumerator() {
        return BigInteger.valueOf(numerator);
    }

    @Override
    BigInteger _bigDenominator() {
        return BigInteger.valueOf(denominator);
    }

    @Override
    FluidAmount _this() {
        return this;
    }

    @Override
    public FluidAmount asLongIntExact() {
        return this;
    }

    @Override
    public BigFluidAmount asBigInt() {
        return new BigFluidAmount(this);
    }
}
