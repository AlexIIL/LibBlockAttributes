package alexiil.mc.lib.attributes.fluid.amount;

import java.math.BigInteger;

import javax.annotation.Nullable;

/** Base class for {@link FluidAmount} and {@link BigFluidAmount}.
 * <p>
 * All names are nonstandard to ensure that we don't expose them unnecessarily to consumers. */
/* package-private */ abstract class FluidAmountBase<T extends FluidAmountBase<T>> implements Comparable<T> {

    // ################
    // Properties
    // ################

    // Exposed

    public abstract boolean isZero();

    public abstract boolean isNegative();

    public abstract boolean isPositive();

    public abstract int sign();

    public abstract FluidAmount asLongIntExact();

    public abstract BigFluidAmount asBigInt();

    /** Null is treated as zero */
    @Override
    public abstract int compareTo(@Nullable T other);

    /** Null is treated as zero */
    public boolean isGreaterThan(@Nullable T other) {
        return compareTo(other) > 0;
    }

    /** Null is treated as zero */
    public boolean isLessThan(@Nullable T other) {
        return compareTo(other) < 0;
    }

    // Internal

    abstract BigInteger _bigWhole();

    abstract BigInteger _bigNumerator();

    abstract BigInteger _bigDenominator();

    /** @return this, casted to T. */
    abstract T _this();

    // ################
    // Operators
    // ################

    // Exposed

    public abstract T negate();

    /** @return The smaller of this value and then given value. */
    public T min(T other) {
        if (other.isLessThan(_this())) {
            return other;
        }
        return _this();
    }

    /** @return The greater of this value and then given value. */
    public T max(T other) {
        if (other.isGreaterThan(_this())) {
            return other;
        }
        return _this();
    }

    // Internal

    /** @throws ArithmeticException if the given values don't fit in a {@link FluidAmount} */
    static FluidAmount _roundToSmall(BigInteger whole, BigInteger numerator, BigInteger denominator) {

        // re-balance top-heavy over to whole
        BigInteger[] divRem = numerator.divideAndRemainder(denominator);
        BigInteger div = divRem[0];
        BigInteger rem = divRem[1];
        if (div.signum() != 0) {
            whole = whole.add(div);
            numerator = rem;
        }

        // Balance fraction
        BigInteger GCD = numerator.gcd(denominator);
        numerator = numerator.divide(GCD);
        denominator = denominator.divide(GCD);

        return _toSmall(whole, numerator, denominator);
    }

    /** @throws ArithmeticException if the given values don't fit in a {@link FluidAmount} */
    static FluidAmount _toSmall(BigInteger whole, BigInteger numerator, BigInteger denominator) {
        return new FluidAmount(whole.longValueExact(), numerator.longValueExact(), denominator.longValueExact());
    }

    BigFluidAmount _bigAdd(FluidAmountBase<?> by) {
        BigInteger w1 = _bigWhole();
        BigInteger w2 = by._bigWhole();
        BigInteger n1 = _bigNumerator();
        BigInteger n2 = by._bigNumerator();
        BigInteger d1 = _bigDenominator();
        BigInteger d2 = by._bigDenominator();

        BigInteger d = d1.multiply(d2);
        BigInteger n = n1.multiply(d2).add(n2.multiply(d1));
        return BigFluidAmount.of(w1.add(w2), n, d);
    }

    BigFluidAmount _bigSub(FluidAmountBase<?> other) {
        return _bigAdd(other.negate());
    }

    BigFluidAmount _bigMul(FluidAmountBase<?> other) {

        BigInteger w1 = this._bigWhole();
        BigInteger w2 = other._bigWhole();
        BigInteger w3 = w1.multiply(w2);

        BigInteger n1 = this._bigNumerator();
        BigInteger n2 = other._bigNumerator();
        BigInteger d1 = this._bigDenominator();
        BigInteger d2 = other._bigDenominator();
        BigInteger d3 = d1.multiply(d2);

        // (w1*n2*d1 + w2*n1*d2 + n1*n2)
        BigInteger n3 = w1.multiply(n2).multiply(d1)//
            .add(w2.multiply(n2).multiply(d2))//
            .add(n1.multiply(n2));

        return BigFluidAmount.of(w3, n3, d3);
    }

    BigFluidAmount _bigDiv(FluidAmountBase<?> other) {

        // (W1+(N1/D1)) / (W2+(N2/D2))
        // ( (W1*D1 + N1)/D1 ) / ( (W2*D2 + N2)/D2 )
        // ( (W1*D1 + N1) * D2 ) / ( (W2*D2 + N2) * D1 )
        // (W1*D1*D2 + N1*D2) / ( W2*D2*D1 + N2*D1 )

        BigInteger w1 = this._bigWhole();
        BigInteger w2 = other._bigWhole();

        BigInteger n1 = this._bigNumerator();
        BigInteger n2 = other._bigNumerator();

        BigInteger d1 = this._bigDenominator();
        BigInteger d2 = other._bigDenominator();

        BigInteger numerator = n1.multiply(d2)//
            .add(w1.multiply(d1).multiply(d2));
        BigInteger denominator = n2.multiply(d1)//
            .add(w2.multiply(d1).multiply(d2));

        return BigFluidAmount.of(numerator, denominator);
    }

    BigFluidAmount _bigReciprocal() {
        return FluidAmount.ONE._bigDiv(this);
    }
}
