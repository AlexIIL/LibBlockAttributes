/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.mixin.api;

import net.minecraft.block.entity.BlockEntity;

/** Optional interface for {@link BlockEntity}s to implement if they need to do something when the chunk they are in is
 * unloaded.
 * <p>
 * In 1.17 minecraft also calls {@link BlockEntity#markRemoved()} after this is called, so you don't need to put
 * duplicate logic in both. */
public interface UnloadableBlockEntity {
    void onChunkUnload();
}
