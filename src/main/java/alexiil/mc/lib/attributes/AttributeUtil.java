/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

/** Misleadingly named class: this only stores the boolean checker {@link #EXPENSIVE_DEBUG_CHECKS}, and doesn't actually
 * have any other utility methods. */
public final class AttributeUtil {
    private AttributeUtil() {}

    // Ensure that it defaults to true, unless some-one explicitly goes looking for this
    public static final boolean EXPENSIVE_DEBUG_CHECKS
        = !Boolean.getBoolean("libblockattributes.disable_expensive_debug_checks");
}
