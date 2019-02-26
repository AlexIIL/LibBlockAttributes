package alexiil.mc.lib.attributes.item.filter;

import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

/** A general {@link Predicate} interface for {@link ItemStack}s. */
@FunctionalInterface
public interface IItemFilter extends Predicate<ItemStack> {

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

    /** {@link Predicate} delegate method to {@link #matches(ItemStack)}.
     * 
     * @deprecated because all subclasses should override matches instead of this. */
    @Override
    @Deprecated
    default boolean test(ItemStack t) {
        return matches(t);
    }
}
