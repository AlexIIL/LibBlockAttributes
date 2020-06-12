/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

/** Marker interface for {@link FluidFilter} that indicates that object obtaining instances of this might be able to
 * read the real contents.
 * <p>
 * NOTE: This can only be implemented by classes <strong>included in LibBlockAttributes!</strong>. (As many
 * implementations must implement direct support for subclasses of this).
 * <p>
 * As such you should pretend that this interface is "sealed", and the only valid subtypes are:
 * <ol>
 * <li>{@link ConstantFluidFilter}</li>
 * <li>{@link ExactFluidFilter}</li>
 * <li>{@link AggregateFluidFilter}</li>
 * <li>{@link FluidSetFilter}</li>
 * <li>{@link InvertedFluidFilter}</li>
 * <li>{@link RawFluidTagFilter}</li>
 * <li>{@link FluidTagFilter}</li>
 * </ol>
 */
public interface ReadableFluidFilter extends FluidFilter {

    public static void checkValidity(ReadableFluidFilter filter) {
        String clsName = ReadableFluidFilter.class.getName();
        String expectedPackage = clsName.substring(0, clsName.lastIndexOf('.'));
        if (!filter.getClass().getName().startsWith(expectedPackage)) {
            throw new IllegalStateException(
                "The owner of " + filter.getClass() + " has incorrectly implemented ReadableFluidFilter!\n"
                    + "Note that only LibBlockAttributes should define readable fluid filters, "
                    + "as otherwise there's no way to guarentee compatibility!"
            );
        }
    }
}
