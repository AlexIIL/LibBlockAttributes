package alexiil.mc.lib.attributes;

import java.util.List;

public class CombinableAttribute<T> extends Attribute<T> {

    /** The instance to use when combining an empty list in {@link #combine(List)}. */
    // Is this a good idea? Or should this always just be "null"?
    private final T nullInstance;
    private final IAttributeCombiner<T> combiner;

    /** @param clazz The {@link Class} of this attribute.
     * @param nullInstance The value to return from {@link #combine(List)} if the incoming list is empty. Can be null if
     *            that makes sense for your attribute.
     * @param combiner The combiner that should turn a list of 2 or more instances into a single one that encapsulates
     *            all of them. */
    public CombinableAttribute(Class<T> clazz, T nullInstance, IAttributeCombiner<T> combiner) {
        super(clazz);
        this.nullInstance = nullInstance;
        this.combiner = combiner;
    }

    /** Combines all of the given instances into a single object. */
    public final T combine(List<T> list) {
        switch (list.size()) {
            case 0: {
                return nullInstance;
            }
            case 1: {
                return list.get(0);
            }
            default: {
                return combiner.combine(list);
            }
        }
    }

    @FunctionalInterface
    public interface IAttributeCombiner<T> {
        T combine(List<T> attributes);
    }
}
