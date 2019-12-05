package alexiil.mc.lib.attributes.fluid.amount;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

import javax.annotation.Nullable;

/** Exact version of {@link FluidAmount}. This is generally intended for calculations if the end result should fit into
 * a {@link FluidAmount}, but the intermediate steps might not. */
// Currently this isn't exposed through the API - one alternative might be to create
// "interface IFluidAmount", and then make this extend that.
// (And that interface would have the standard methods for add/sub/mul/div/etc)
//
// However that might have bad implications for perf... and require more work to save+load instances
public final class BigFluidAmount extends FluidAmountBase<BigFluidAmount> {

    public static final BigFluidAmount ZERO = FluidAmount.ZERO.asBigInt();
    public static final BigFluidAmount ONE = FluidAmount.ONE.asBigInt();
    public static final BigFluidAmount NEGATIVE_ONE = FluidAmount.NEGATIVE_ONE.asBigInt();

    public static final BigFluidAmount BUCKET = ONE;
    public static final BigFluidAmount BOTTLE = FluidAmount.BOTTLE.asBigInt();

    public final BigInteger whole;
    public final BigInteger numerator;

    /** Always greater than 0. */
    public final BigInteger denominator;

    /* package-private */ BigFluidAmount(BigInteger whole, BigInteger numerator, BigInteger denominator) {
        this.whole = whole;
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public BigFluidAmount(FluidAmount from) {
        this.whole = from._bigWhole();
        this.numerator = from._bigNumerator();
        this.denominator = from._bigDenominator();
    }

    // Construction

    /** Creates a new {@link FluidAmount} with the given values. This will reduce the fraction into it's simplest
     * form. */
    public static BigFluidAmount of(BigInteger numerator, BigInteger denominator) {
        return of(BigInteger.ZERO, numerator, denominator);
    }

    /** Creates a new {@link FluidAmount} with the given values. This will reduce the fraction into it's simplest form.
     * <p>
     * 
     * @throws IllegalArgumentException if either whole or numerator are negative, or if denominator is less than or
     *             equal to 0. */
    public static BigFluidAmount of(BigInteger whole, BigInteger numerator, BigInteger denominator) {
        if (denominator.signum() <= 0) {
            throw new IllegalArgumentException("The denominator (" + denominator + ") must be positive!");
        }

        if (whole.signum() < 0 && numerator.signum() > 0) {
            whole = whole.add(BigInteger.valueOf(1));
            numerator = numerator.subtract(denominator);
        } else if (whole.signum() > 0 && numerator.signum() < 0) {
            whole = whole.subtract(BigInteger.valueOf(1));
            numerator = denominator.add(numerator);
        }

        if (numerator.abs().compareTo(denominator) >= 0) {
            BigInteger[] divRem = numerator.divideAndRemainder(denominator);
            whole = whole.add(divRem[0]);
            numerator = divRem[1];
        }

        if (numerator.signum() < 0) {
            BigInteger gcd = numerator.negate().gcd(denominator);
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
        } else if (numerator.signum() > 0) {
            BigInteger gcd = numerator.gcd(denominator);
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
        } else /* numerator == 0 */ {
            denominator = BigInteger.ONE;
        }

        return new BigFluidAmount(whole, numerator, denominator);
    }

    // Properties

    @Override
    public boolean isZero() {
        return whole.equals(BigInteger.ZERO) && numerator.equals(BigInteger.ZERO);
    }

    @Override
    public boolean isNegative() {
        return whole.signum() < 0 || numerator.signum() < 0;
    }

    @Override
    public boolean isPositive() {
        return whole.signum() > 0 || numerator.signum() > 0;
    }

    /** @return The sign: Either -1 if this is negative, +1 if this is positive, or 0 if this is zero. */
    @Override
    public int sign() {
        if (whole.signum() != 0) {
            return whole.signum();
        }
        return numerator.signum();
    }

    // Operators

    @Override
    public BigFluidAmount negate() {
        return new BigFluidAmount(whole.negate(), numerator.negate(), denominator);
    }

    public BigFluidAmount reciprocal() {
        return _bigReciprocal();
    }

    /** @throws ArithmeticException if the given values don't fit in a {@link FluidAmount} */
    @Override
    public FluidAmount asLongIntExact() {
        return _toSmall(whole, numerator, denominator);
    }

    /** @return True if {@link #asLongIntExact()} will not throw an arithmetic exception. */
    public boolean fitsInLongInt() {
        // I *think* this is ok?
        // as BigInteger checks mag.length as well, but I think that's just an optimisation?
        return whole.bitLength() < 64 && numerator.bitLength() < 64 && denominator.bitLength() < 64;
    }

    /** Converts this into a normal long-based {@link FluidAmount}. If this is too big to fit then this returns either
     * {@link FluidAmount#MIN_VALUE} or {@link FluidAmount#MAX_VALUE} according this the {@link #sign()}. */
    public FluidAmount asLongIntSaturated() {
        if (fitsInLongInt()) {
            return asLongIntExact();
        } else {
            return isNegative() ? FluidAmount.MIN_VALUE : FluidAmount.MAX_VALUE;
        }
    }

    /** Converts this into a normal long-based {@link FluidAmount}.
     * <p>
     * If {@link #whole} is too large to fit in a long then either {@link FluidAmount#MIN_VALUE} or
     * {@link FluidAmount#MAX_VALUE} is returned (depending on this sign).
     * <p>
     * Otherwise this is approximately rounded to a valid value. */
    public FluidAmount asLongIntRounded(RoundingMode rounding) {
        if (fitsInLongInt()) {
            return asLongIntExact();
        }

        if (rounding == RoundingMode.UNNECESSARY) {
            throw new ArithmeticException(
                this + " doesn't fit into a normal FluidAmount, and the rounding mode is specified as UNNECESSARY!"
            );
        }

        if (whole.bitLength() >= 64) {
            return isNegative() ? FluidAmount.MIN_VALUE : FluidAmount.MAX_VALUE;
        }

        long w = whole.longValue();

        // Okay, so:
        // We need to round N1/D1 (this) to N2/D2, where D2 < Long.MAX_VALUE
        // and the magnitude of (N1/D1) - (N2/D2) is as small as possible.
        // So... how do we accomplish this?
        // Should we halve the denominator until it fits?
        // or should we try to use any divisor instead in the hope that it might be closer?
        // ...for now we'll just divide the divisor by 2 to the power of whatever is necessary

        int bits = denominator.bitLength();
        assert bits > 63;
        BigInteger shiftedD = denominator.shiftRight(bits - 63);
        BigInteger shiftedN = numerator.shiftRight(bits - 63);
        assert shiftedD.bitLength() <= 63;
        assert shiftedN.bitLength() <= 63;
        return FluidAmount.of(w, shiftedN.longValue(), shiftedD.longValue());
    }

    @Override
    public BigFluidAmount asBigInt() {
        return this;
    }

    public BigFluidAmount add(long by) {
        if (by == 0) {
            return this;
        }
        return of(whole.add(BigInteger.valueOf(by)), numerator, denominator);
    }

    public BigFluidAmount add(@Nullable BigFluidAmount by) {
        if (by == null || by.isZero()) {
            return this;
        }
        if (isZero()) {
            return by;
        }
        return _bigAdd(by);
    }

    public BigFluidAmount add(@Nullable FluidAmount by) {
        if (by == null || by.isZero()) {
            return this;
        }
        if (isZero()) {
            return by.asBigInt();
        }
        return _bigAdd(by);
    }

    public BigFluidAmount sub(long by) {
        return add(-by);
    }

    public BigFluidAmount sub(@Nullable BigFluidAmount by) {
        return _bigSub(by);
    }

    public BigFluidAmount sub(@Nullable FluidAmount by) {
        return _bigSub(by);
    }

    public BigFluidAmount mul(long by) {
        if (by == 1) {
            return this;
        }
        if (by == -1) {
            return negate();
        }
        if (by == 0 || isZero()) {
            return ZERO;
        }
        return _bigMul(FluidAmount.of(by, 0, 1));
    }

    public BigFluidAmount mul(BigFluidAmount by) {
        if (isZero() || by.isZero()) {
            return ZERO;
        }
        if (by.equals(ONE)) {
            return this;
        }
        if (by.equals(NEGATIVE_ONE)) {
            return negate();
        }
        if (equals(ONE)) {
            return by;
        }
        if (equals(NEGATIVE_ONE)) {
            return by.negate();
        }
        return _bigMul(by);
    }

    public BigFluidAmount mul(FluidAmount by) {
        if (isZero() || by.isZero()) {
            return ZERO;
        }
        if (by.equals(FluidAmount.ONE)) {
            return this;
        }
        if (by.equals(FluidAmount.NEGATIVE_ONE)) {
            return negate();
        }
        if (equals(ONE)) {
            return by.asBigInt();
        }
        if (equals(NEGATIVE_ONE)) {
            return by.negate().asBigInt();
        }
        return _bigMul(by);
    }

    public BigFluidAmount div(long by) {
        // TODO: Make this more optimal!
        if (by == 1) {
            return this;
        } else if (by == -1) {
            return negate();
        } else if (by == 0) {
            throw new ArithmeticException("divide by 0");
        }
        return _bigDiv(FluidAmount.of(by, 0, 1));
    }

    public BigFluidAmount div(BigFluidAmount by) {
        if (by.equals(ONE)) {
            return this;
        } else if (by.equals(NEGATIVE_ONE)) {
            return negate();
        } else if (by.isZero()) {
            throw new ArithmeticException("divide by 0");
        }
        return _bigDiv(by);
    }

    public BigFluidAmount div(FluidAmount by) {
        if (by.equals(FluidAmount.ONE)) {
            return this;
        } else if (by.equals(FluidAmount.NEGATIVE_ONE)) {
            return negate();
        } else if (by.isZero()) {
            throw new ArithmeticException("divide by 0");
        }
        return _bigDiv(by);
    }

    // Object Methods

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof BigFluidAmount)) {
            return false;
        }
        return equals((BigFluidAmount) obj);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new BigInteger[] { whole, numerator, denominator });
    }

    @Override
    public String toString() {
        return "{BigFluidAmount " + whole + " + " + numerator + "/" + denominator + "}";
    }

    // Comparison

    /** @return True if the number that this {@link FluidAmount} represents is equal to the number that the given
     *         {@link FluidAmount} represents. */
    public boolean equals(BigFluidAmount other) {
        return whole.equals(other.whole) && numerator.equals(other.numerator) && denominator.equals(other.denominator);
    }

    @Override
    public int compareTo(BigFluidAmount o) {
        if (o == null) {
            return sign();
        }
        if (!whole.equals(o.whole)) {
            return whole.compareTo(o.whole);
        }
        if (denominator.equals(o.denominator)) {
            return numerator.compareTo(o.numerator);
        }
        BigInteger a = numerator.multiply(o.denominator);
        BigInteger b = o.numerator.multiply(denominator);
        return a.compareTo(b);
    }

    // Internal

    @Override
    BigInteger _bigWhole() {
        return whole;
    }

    @Override
    BigInteger _bigNumerator() {
        return numerator;
    }

    @Override
    BigInteger _bigDenominator() {
        return denominator;
    }

    @Override
    BigFluidAmount _this() {
        return this;
    }
}
