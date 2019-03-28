package alexiil.mc.lib.attributes.fluid.filter;

import java.util.function.Predicate;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

/** A general {@link Predicate} interface for {@link FluidKey}s. */
@FunctionalInterface
public interface FluidFilter {

    /** Checks to see if the given filter matches the given fluid key.
     * 
     * @throws IllegalArgumentException if the given {@link FluidKey} is {@link FluidKey#isEmpty() empty}. */
    boolean matches(FluidKey fluidKey);

    default FluidFilter negate() {
        return stack -> !this.matches(stack);
    }

    default FluidFilter and(FluidFilter other) {
        return AggregateFluidFilter.and(this, other);
    }

    default FluidFilter or(FluidFilter other) {
        return AggregateFluidFilter.or(this, other);
    }

    default Predicate<FluidKey> asPredicate() {
        final FluidFilter filter = this;
        return new Predicate<FluidKey>() {
            @Override
            public boolean test(FluidKey stack) {
                if (stack == null || stack.isEmpty()) {
                    // Predicate.test doesn't have this restriction
                    return false;
                }
                return filter.matches(stack);
            }

            @Override
            public Predicate<FluidKey> negate() {
                // Because the real filter might have optimisations in negate()
                return filter.negate().asPredicate();
            }

            @Override
            public Predicate<FluidKey> and(Predicate<? super FluidKey> other) {
                if (other instanceof FluidFilter) {
                    // Because the real filter might have optimisations in and()
                    return filter.and((FluidFilter) other).asPredicate();
                } else {
                    return Predicate.super.and(other);
                }
            }

            @Override
            public Predicate<FluidKey> or(Predicate<? super FluidKey> other) {
                if (other instanceof FluidFilter) {
                    // Because the real filter might have optimisations in or()
                    return filter.or((FluidFilter) other).asPredicate();
                } else {
                    return Predicate.super.and(other);
                }
            }
        };
    }
}
