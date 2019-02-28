package alexiil.mc.lib.attributes.item.filter;

import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

/** A general {@link Predicate} interface for {@link ItemStack}s. */
@FunctionalInterface
public interface IItemFilter {

    /** An {@link IItemFilter} that matches any {@link ItemStack}. */
    public static final IItemFilter ANY_STACK = stack -> {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("You should never test an IItemFilter with an empty stack!");
        }
        return true;
    };

    /** An {@link IItemFilter} that matches absolutely nothing. There's no reason to use this(?) */
    public static final IItemFilter NOTHING = stack -> {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("You should never test an IItemFilter with an empty stack!");
        }
        return false;
    };

    /** Checks to see if the given filter matches the given stack. Note that this must not care about
     * {@link ItemStack#getAmount()}.
     * 
     * @throws IllegalArgumentException if the given {@link ItemStack} is {@link ItemStack#isEmpty() empty}. */
    boolean matches(ItemStack stack);

    default IItemFilter negate() {
        return stack -> !this.matches(stack);
    }

    default IItemFilter and(IItemFilter other) {
        return AggregateStackFilter.and(this, other);
    }

    default IItemFilter or(IItemFilter other) {
        return AggregateStackFilter.or(this, other);
    }

    default Predicate<ItemStack> asPredicate() {
        final IItemFilter filter = this;
        return new Predicate<ItemStack>() {
            @Override
            public boolean test(ItemStack stack) {
                if (stack == null || stack.isEmpty()) {
                    // Predicate.test doesn't have this restriction
                    return false;
                }
                return filter.matches(stack);
            }

            @Override
            public Predicate<ItemStack> negate() {
                // Because the real filter might have optimisations in negate()
                return filter.negate().asPredicate();
            }

            @Override
            public Predicate<ItemStack> and(Predicate<? super ItemStack> other) {
                if (other instanceof IItemFilter) {
                    // Because the real filter might have optimisations in and()
                    return filter.and((IItemFilter) other).asPredicate();
                } else {
                    return Predicate.super.and(other);
                }
            }

            @Override
            public Predicate<ItemStack> or(Predicate<? super ItemStack> other) {
                if (other instanceof IItemFilter) {
                    // Because the real filter might have optimisations in or()
                    return filter.or((IItemFilter) other).asPredicate();
                } else {
                    return Predicate.super.and(other);
                }
            }
        };
    }
}
