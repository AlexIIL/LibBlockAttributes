/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

/** Listener {@link FunctionalInterface} for
 * {@link FixedItemInvView#addListener(InvMarkDirtyListener, alexiil.mc.lib.attributes.ListenerRemovalToken)}. */
@FunctionalInterface
public interface InvMarkDirtyListener {

    /** @param inv The inventory that was modified - this is always the inventory object that you registered the
     *            listener with, and never any delegate inventories! */
    void onMarkDirty(AbstractItemInvView inv);
}
