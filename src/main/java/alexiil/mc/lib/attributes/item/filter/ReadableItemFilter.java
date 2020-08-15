/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import alexiil.mc.lib.attributes.AttributeUtil;

/** Marker interface for {@link ItemFilter} that indicates that object obtaining instances of this might be able to read
 * the real contents.
 * <p>
 * NOTE: This can only be implemented by classes <strong>included in LibBlockAttributes!</strong>. (As many
 * implementations must implement direct support for subclasses of this). Note, however, that LBA may add new
 * implementing classes in the future - so it is a bad idea to crash if this list isn't complete.
 * <p>
 * As such you should pretend that this interface is "sealed", and the only valid subtypes are:
 * <ol>
 * <li>{@link ConstantItemFilter}</li>
 * <li>{@link ExactItemFilter}</li>
 * <li>{@link ExactItemSetFilter}</li>
 * <li>{@link ExactItemStackFilter}</li>
 * <li>{@link ItemClassFilter}</li>
 * <li>{@link ExactItemSetFilter}</li>
 * <li>{@link AggregateItemFilter}</li>
 * <li>{@link ResolvableItemFilter}</li>
 * </ol>
 * Note that {@link ResolvableItemFilter} is <em>not sealed</em>, but it must only return {@link ReadableItemFilter}s
 * that are {@link ReadableItemFilter}s, other than {@link ResolvableItemFilter}. */
public interface ReadableItemFilter extends ItemFilter {

    public static void checkValidity(ReadableItemFilter filter) {
        if (AttributeUtil.EXPENSIVE_DEBUG_CHECKS) {
            String clsName = ReadableItemFilter.class.getName();
            String expectedPackage = clsName.substring(0, clsName.lastIndexOf('.'));
            if (!filter.getClass().getName().startsWith(expectedPackage)) {
                throw new IllegalStateException(
                    "The owner of " + filter.getClass() + " has incorrectly implemented IReadableItemFilter!\n"
                        + "Note that only LibBlockAttributes should define readable item filters, "
                        + "as otherwise there's no way to guarentee compatibility!"
                );
            }
        }
    }
}
