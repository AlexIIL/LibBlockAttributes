/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.collection.DefaultedList;

/** The base class for {@link AttributeList} and {@link ItemAttributeList}. */
public abstract class AbstractAttributeList<T> {

    public final Attribute<T> attribute;
    protected final DefaultedList<T> list = DefaultedList.of();
    private boolean hasFinishedAdding = false;

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
        hasFinishedAdding = false;
    }

    void finishAdding() {
        assertAdding();
        hasFinishedAdding = true;
    }

    protected void assertAdding() {
        assert !hasFinishedAdding;
    }

    protected void assertUsing() {
        assert hasFinishedAdding;
    }

}
