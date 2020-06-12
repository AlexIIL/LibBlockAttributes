/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

/** Defines the priority used for custom attribute adders. */
public enum AttributeSourceType {

    /** Highest priority of all the source types - when the object itself implements/extends the attribute
     * class/interface, or stores it in a field or (etc).
     * <p>
     * The main point is that instance must be designed directly for the given attribute, or the implementation must be
     * the best possible version for the actual instance. */
    INSTANCE,

    // Should any more be defined? I'm not sure what other kinds of implementations will actually happen.

    /** Lowest priority of all the source types - when the implementation is just a wrapper over a different API, and as
     * such it may suffer from poorer performance or be a poor fit for the target interface/class. */
    COMPAT_WRAPPER;
}
