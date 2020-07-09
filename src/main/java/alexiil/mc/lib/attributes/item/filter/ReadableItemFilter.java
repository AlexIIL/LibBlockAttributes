/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

/** Marker interface for {@link ItemFilter} that indicates that object obtaining instances of this might be able to read
 * the real contents.
 * <p>
 * NOTE: This can only be implemented by classes <strong>included in LibBlockAttributes!</strong>. (As many
 * implementations must implement direct support for subclasses of this). *
 * <p>
 * As such you should pretend that this interface is "sealed", and the only valid subtypes are:
 * <ol>
 * <li>{@link ConstantItemFilter}</li>
 * <li>{@link ExactItemFilter}</li>
 * <li>{@link ExactItemStackFilter}</li>
 * <li>{@link ItemClassFilter}</li>
 * <li>{@link ExactItemSetFilter}</li>
 * <li>{@link AggregateItemFilter}</li>
 * </ol>
 */
public interface ReadableItemFilter extends ItemFilter {

    public static void checkValidity(ReadableItemFilter filter) {
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
