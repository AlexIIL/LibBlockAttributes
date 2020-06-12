/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

/** Placeholder class until caches are worked out. */
public final class CacheInfo {

    public static final CacheInfo NOT_CACHABLE = new CacheInfo();

    private CacheInfo() {
        // Private because we're going to change this in the future.
    }
}
