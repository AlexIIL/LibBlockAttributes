package alexiil.mc.lib.attributes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.DefaultedList;

public abstract class AbstractAttributeList<T> {

    enum AttributeListMode {
        ADDING,
        USING;
    }

    public final Attribute<T> attribute;
    protected final DefaultedList<T> list = DefaultedList.of();
    private AttributeListMode mode = AttributeListMode.ADDING;

    public AbstractAttributeList(Attribute<T> attribute) {
        this.attribute = attribute;
    }

    /** @return The number of attribute instances added to this list. */
    public int getCount() {
        assertUsing();
        return list.size();
    }

    @Nonnull
    public T get(int index) {
        assertUsing();
        return list.get(index);
    }

    @Nullable
    public T getFirstOrNull() {
        assertUsing();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Nonnull
    public T getFirst(DefaultedAttribute<T> defaulted) {
        assertUsing();
        if (list.isEmpty()) {
            return defaulted.defaultValue;
        }
        return list.get(0);
    }

    /** @return A combined version of this list, or the attribute's default value if this list is empty. */
    @Nonnull
    public T combine(CombinableAttribute<T> combinable) {
        assertUsing();
        return combinable.combine(list);
    }

    void reset() {
        list.clear();
        mode = AbstractAttributeList.AttributeListMode.ADDING;
    }

    void finishAdding() {
        assertAdding();
        mode = AbstractAttributeList.AttributeListMode.USING;
    }

    protected void assertAdding() {
        assert mode == AbstractAttributeList.AttributeListMode.ADDING;
    }

    protected void assertUsing() {
        assert mode == AbstractAttributeList.AttributeListMode.USING;
    }

}
