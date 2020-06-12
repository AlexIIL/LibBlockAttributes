/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

/** Variant of {@link AttributeList} but for {@link Item}'s rather than {@link Block}'s. */
public class ItemAttributeList<T> extends AbstractAttributeList<T> {
    private final Predicate<T> searchMatcher;

    /** The number of calls to {@link #add(Object)}. */
    private int offeredCount;

    public ItemAttributeList(Attribute<T> attribute) {
        super(attribute);
        this.searchMatcher = null;
    }

    /** @param searchMatcher A filter for all {@link #add(Object) added} objects. */
    public ItemAttributeList(Attribute<T> attribute, Predicate<T> searchMatcher) {
        super(attribute);
        this.searchMatcher = searchMatcher;
    }

    // Adders (used by attribute providers)

    /** Directly adds the given object to this list.
     * 
     * @param object The object to add. */
    public void add(T object) {
        assertAdding();
        offeredCount++;
        if (searchMatcher != null && !searchMatcher.test(object)) {
            return;
        }
        list.add(object);
    }

    /** Offers the given object to this list. If the given object is not an instance of the current {@link #attribute}
     * (and it cannot be {@link Convertible#convertTo(Class) converted} into it) then this will not affect this list.
     * 
     * @param object The object to offer, which may implement {@link Convertible} if it can be converted into many
     *            different forms. */
    public void offer(Object object) {
        // Always check before to throw the error as early as possible
        assertAdding();
        T converted = Convertible.getAs(object, attribute.clazz);
        if (converted != null) {
            add(converted);
        }
    }

    /** @return True if any calls to {@link #add(Object)} have been made. (Including calls to {@link #offer(Object)} and
     *         it's variants). */
    public boolean hasOfferedAny() {
        return offeredCount > 0;
    }

    /** @return The number of calls to {@link #add(Object)} (including calls to {@link #offer(Object)} and it's
     *         variants). */
    public int getOfferedCount() {
        return offeredCount;
    }

    /** @return A combined version of this list and then the second given list, or the attribute's default value if both
     *         lists are empty. */
    @Nonnull
    public T combine(AttributeList<T> after, CombinableAttribute<T> combinable) {
        assertUsing();
        return combinable.combine(list, after.list);
    }
}
