/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import alexiil.mc.lib.attributes.item.impl.SimpleLimitedGroupedItemInv;

/** A modifiable version of {@link GroupedItemInvView}, except that all modification methods are provided by
 * {@link ItemExtractable} and {@link ItemInsertable}. */
public interface GroupedItemInv extends GroupedItemInvView, ItemTransferable {

    /** @return A new {@link LimitedGroupedItemInv} that provides a more controllable version of this
     *         {@link GroupedItemInv}. */
    default LimitedGroupedItemInv createLimitedGroupedInv() {
        return new SimpleLimitedGroupedItemInv(this);
    }
}
