package alexiil.mc.lib.attributes.fluid.amount;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

import javax.annotation.Nullable;

import com.google.common.math.LongMath;

import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.PacketByteBuf;

/** A simple mixed fraction. The value represented by this can be calculated with this: "{@link #whole} +
 * ({@link #numerator} / {@link #denominator})". Negative values are indicated with both {@link #whole} and
 * {@link #numerator} being negative - it is never permissible for only one of them to be less than 0 and the other to
 * be greater than 0. */
public final class FluidAmount extends FluidAmountBase<FluidAmount> {

    public static final FluidAmount ZERO = new FluidAmount(0);
    public static final FluidAmount ONE = new FluidAmount(1);
    public static final FluidAmount NEGATIVE_ONE = new FluidAmount(-1);

    /** A very large amount of fluid - one million buckets. Used primarily in cases where we need to test if any fluid
     * is insertable, so we go above normal values (without going so far out of range to make common calculations
     * overflow into {@link BigFluidAmount}). */
    public static final FluidAmount A_MILLION = new FluidAmount(1_000_000);

    public static final FluidAmount BUCKET = ONE;
    public static final FluidAmount BOTTLE = of(1, 3);

    /** The maximum possible value that a {@link FluidAmount} can hold. */
    public static final FluidAmount MAX_VALUE = new FluidAmount(Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE);
    public static final FluidAmount MIN_VALUE = new FluidAmount(Long.MIN_VALUE, -1 - Long.MAX_VALUE, Long.MAX_VALUE);

    public final long whole;
    public final long numerator;

    /** Always greater than 0. */
    public final long denominator;

    // Construction

    /** Constructs a new {@link FluidAmount} with the given whole value. The numerator is set to 0, and the denominator
     * is set to 1. */
    public FluidAmount(long whole) {
        this(whole, 0, 1);
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

        return new FluidAmount(whole, numerator, denominator);
    }

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
     * <li>If the text is a valid {@link Long} then that is parsed and returned as if from
     * {@link #FluidAmount(long)}.</li>
     * <li>If the text is a valid {@link Double} then it is parsed and returned as if from
     * {@link FluidAmount#fromDouble(double)}</li>
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
        // Allow leading or trailing whitespace
        final String original = text;
        text = text.trim();
        try {
            return new FluidAmount(Long.parseLong(text));
        } catch (NumberFormatException ignored) {
            // This is always permissible
        }

        try {
            return FluidAmount.fromDouble(Double.parseDouble(text));
        } catch (NumberFormatException ignored) {
            // This is always permissible
        } catch (IllegalArgumentException badDouble) {
            return simpleError(shouldThrow, badDouble.getMessage());
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
        long whole = 0;
        long numerator = 0;
        long denominator = 0;

        int stage = 0;
        int numberStart = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case ' ': {
                    if (numberStart != -1) {
                        return simpleError(shouldThrow, "");
                    }
                    continue;
                }
            }
        }

        return simpleError(shouldThrow, "todo");
    }

    private static String simpleError(boolean shouldThrow, String error) throws NumberFormatException {
        if (!shouldThrow) {
            return error;
        } else {
            throw new NumberFormatException(error);
        }
    }

    /* package-private */ FluidAmount(long whole, long numerator, long denominator) {
        this.whole = whole;
        this.numerator = numerator;
        this.denominator = denominator;
    }

    // Serialisation

    public static FluidAmount fromNbt(CompoundTag tag) {
        long w = tag.getLong("w");
        long n = tag.getLong("n");
        long d = Math.max(1, tag.getLong("d"));
        return of(w, n, d);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
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
        FluidAmount mult = mul(base);
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
                return LongMath.saturatedAdd(mult.whole, +1);
            case FLOOR:
                return LongMath.saturatedAdd(mult.whole, -1);
            case HALF_DOWN: {
                long pnum = mult.numerator < 0 ? -mult.numerator : mult.numerator;
                if (pnum <= mult.denominator / 2) {
                    return mult.whole;
                }
                return LongMath.saturatedAdd(mult.whole, mult.sign());
            }
            case HALF_UP: {
                long pnum = mult.numerator < 0 ? -mult.numerator : mult.numerator;
                if (pnum < mult.denominator / 2) {
                    return mult.whole;
                }
                return LongMath.saturatedAdd(mult.whole, mult.sign());
            }
            case HALF_EVEN: {
                long pnum = mult.numerator < 0 ? -mult.numerator : mult.numerator;
                if (pnum < mult.denominator / 2) {
                    return mult.whole;
                } else if (pnum > mult.denominator / 2) {
                    return LongMath.saturatedAdd(mult.whole, mult.sign());
                } else if ((mult.whole & 1) == 0) {
                    return mult.whole;
                } else {
                    return LongMath.saturatedAdd(mult.whole, mult.sign());
                }
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
        if (numerator == 0 || denominator == 1) {
            return "" + whole;
        }
        String str = "" + whole;

        if (denominator < (1L << 52)) {
            // It should fit into the range of a double
            double fraction = numerator / (double) denominator;
            int decimalPlaces = Long.toString(denominator - 1).length();
            int roundingValue = (int) Math.pow(10, decimalPlaces);
            String fractionStr = Long.toString((long) (fraction * roundingValue));
            while (fractionStr.length() < decimalPlaces) {
                fractionStr = "0" + fractionStr;
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

    // ###########
    //
    // Operators
    //
    // ###########

    @Override
    public FluidAmount negate() {
        return new FluidAmount(-whole, -numerator, denominator);
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
            return "{SafeAddResult rounded=" + roundedResult + " exact=" + exactValue + " }";
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

    /** Safely adds the given {@link FluidAmount} to this one, returning the merged result. Unlike
     * {@link #checkedAdd(FluidAmount)} this will never throw an {@link ArithmeticException} if the result is
     * out-of-range, instead it will round the real answer to the nearest valid {@link FluidAmount} (using
     * {@link BigFluidAmount#asLongIntRounded(RoundingMode)}) */
    public SafeAddResult safeAdd(FluidAmount other, RoundingMode rounding) {
        return new SafeAddResult(bigAdd(other), rounding);
    }

    /** Safely adds the given {@link FluidAmount} to this one, returning the merged result. Unlike
     * {@link #checkedAdd(FluidAmount)} this will never throw an {@link ArithmeticException} if the result is
     * out-of-range, instead it will round the real answer to the nearest valid {@link FluidAmount} (using
     * {@link BigFluidAmount#asLongIntRounded(RoundingMode)} with a rounding mode of {@link RoundingMode#HALF_EVEN
     * HALF_EVEN}). */
    public SafeAddResult safeAdd(FluidAmount other) {
        return safeAdd(other, RoundingMode.HALF_EVEN);
    }

    /** @return the result of {@link #safeAdd(FluidAmount, RoundingMode)}.{@link SafeAddResult#roundedResult
     *         roundedResult}. */
    public FluidAmount roundedAdd(FluidAmount other, RoundingMode rounding) {
        return safeAdd(other, rounding).roundedResult;
    }

    /** @return the result of {@link #safeAdd(FluidAmount)}.{@link SafeAddResult#roundedResult roundedResult}. */
    public FluidAmount roundedAdd(FluidAmount other) {
        return safeAdd(other).roundedResult;
    }

    /** Adds the given long value to this {@link FluidAmount}, without performing any checking or saturation. */
    public FluidAmount add(long by) {
        if (by == 0) {
            return this;
        } else if (isZero()) {
            return new FluidAmount(by);
        }
        return of(by + whole, numerator, denominator);
    }

    /** Directly adds the given {@link FluidAmount} to this one.
     * 
     * @param by The amount to add. If it's null or zero then "this" will be returned.
     * @throws ArithmeticException if the result doesn't fit into a {@link FluidAmount}. */
    public FluidAmount checkedAdd(@Nullable FluidAmount by) {
        if (by == null || by.isZero()) {
            return this;
        } else if (isZero()) {
            return by;
        }

        // W3 + N3/D3 = W1 + N1/D1 + W2 + N2/D2
        // .. = W1+W2 + N1/D1 + N2/D2
        // .. = W1+W2 + (N1*D2)/(D1*D2) + (N2*D1)/(D1*D2)
        // .. = W1+W2 + (N1*D2 + N2*D1) / (D1*D2)
        // W3 = W1+W2
        // N3 = N1*D2+N2*D1
        // D3 = D1*D2

        long w = LongMath.checkedAdd(whole, by.whole);
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

        return _bigAdd(by).asLongIntExact();
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

    /** @param by Either Null or a value that will be {@link #negate() negated} and then passed to
     *            {@link #checkedAdd(FluidAmount)}.
     * @throws ArithmeticException if the result doesn't fit into a {@link FluidAmount}. */
    public FluidAmount checkedSub(@Nullable FluidAmount by) {
        return by == null ? this : checkedAdd(by.negate());
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
        return safeSub(by, rounding).roundedResult;
    }

    /** @param by Either Null or a value that will be {@link #negate() negated} and then passed to
     *            {@link #safeAdd(FluidAmount)}.
     * @return The {@link SafeAddResult#roundedResult}. */
    public FluidAmount roundedSub(@Nullable FluidAmount by) {
        return safeSub(by).roundedResult;
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
        ROUND_HALF_EVEN(RoundingMode.HALF_EVEN);

        /** The rounding mode to use, or null if this requires special handling. */
        @Nullable
        public final RoundingMode rounding;

        private FluidMergeRounding(@Nullable RoundingMode rounding) {
            this.rounding = rounding;
        }
    }

    public static final class FluidMergeResult {
        public final FluidAmount merged, excess;

        public FluidMergeResult(FluidAmount merged, FluidAmount excess) {
            this.merged = merged;
            this.excess = excess;
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
            return new FluidMergeResult(target.checkedAdd(toAdd), ZERO);
        }

        throw new AbstractMethodError("// TODO: Implement this!");
    }

    // --------------
    // Multiplication
    // --------------

    public FluidAmount mul(long by) {
        if (by == 0) {
            return ZERO;
        } else if (by == 1) {
            return this;
        } else if (by == -1) {
            return negate();
        }
        long nb = LongMath.saturatedMultiply(by, numerator);
        long w = LongMath.checkedMultiply(whole, by);
        if (!didOverflow(nb)) {
            return of(w, nb, denominator);
        }
        BigInteger NB = BigInteger.valueOf(by).multiply(BigInteger.valueOf(numerator));
        BigInteger[] divRem = NB.divideAndRemainder(BigInteger.valueOf(denominator));
        BigInteger div = divRem[0];
        BigInteger rem = divRem[1];
        return of(LongMath.checkedAdd(w, div.longValueExact()), rem.longValueExact(), denominator);
    }

    public FluidAmount mul(FluidAmount by) {
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

        // If w is too big then there's nothing we can do about it.
        long w3 = LongMath.checkedMultiply(w1, w2);

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

        return _bigMul(by).asLongIntExact();
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

    public FluidAmount reciprocal() {
        return _bigReciprocal().asLongIntExact();
    }

    public BigFluidAmount bigReciprocal() {
        return _bigReciprocal();
    }

    public FluidAmount div(long by) {
        if (by == 1) {
            return this;
        } else if (by == -1) {
            return negate();
        } else if (by == 0) {
            throw new ArithmeticException("divide by 0");
        }

        // boolean isNegative = by < 0;
        // if (isNegative) {
        // by = -by;
        // }
        //
        // // normal: {
        // // (W+N/D)/B
        // // W/B + N/(D*B)
        // // (W*D)/(D*B) + N/(D*B)
        // // (W*D+N)/(D*B)
        // // }
        // if (isNegative) {
        // by = -by;
        // }
        return _bigDiv(FluidAmount.of(by, 0, 1)).asLongIntExact();
    }

    public FluidAmount div(FluidAmount by) {
        if (by.equals(ONE)) {
            return this;
        } else if (by.equals(NEGATIVE_ONE)) {
            return negate();
        } else if (by.isZero()) {
            throw new ArithmeticException("divide by 0");
        }

        normal: {
            // TODO: Implement the non-big division
            break normal;
        }

        return bigDiv(by).asLongIntExact();
    }

    public BigFluidAmount div(BigFluidAmount by) {
        return _bigDiv(by);
    }

    public BigFluidAmount bigDiv(FluidAmount by) {
        return _bigDiv(by);
    }

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
