/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package alexiil.mc.lib.attributes;

import net.minecraft.nbt.CompoundTag;

/** A savable attribute. */
public interface Savable {
    default CompoundTag toTag() {
        return this.toTag(new CompoundTag());
    }

    /** Saves this object to the {@link CompoundTag} provided.*/
    CompoundTag toTag(CompoundTag tag);

    /** Loads this object from the {@link CompoundTag} provided.*/
    void fromTag(CompoundTag tag);
}
