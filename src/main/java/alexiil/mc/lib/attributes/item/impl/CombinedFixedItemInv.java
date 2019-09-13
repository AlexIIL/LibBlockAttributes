/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;

/** An {@link FixedItemInv} that delegates to a list of them instead of storing items directly. */
public class CombinedFixedItemInv<InvType extends FixedItemInv> extends CombinedFixedItemInvView<InvType>
    implements FixedItemInv {

    public CombinedFixedItemInv(List<? extends InvType> views) {
        super(views);
    }

    public static FixedItemInv create(List<? extends FixedItemInv> list) {
        if (list.isEmpty()) {
            return EmptyFixedItemInv.INSTANCE;
        }

        List<CopyingFixedItemInv> copying = new ArrayList<>();
        List<ModifiableFixedItemInv> modifiable = new ArrayList<>();

        for (FixedItemInv inv : list) {
            if (inv instanceof CopyingFixedItemInv) {
                assert !(inv instanceof ModifiableFixedItemInv) : inv.getClass()
                    + " seems to be copying AND modifiable at the same time! That doesn't make sense!";

                if (!modifiable.isEmpty()) {
                    return new CombinedFixedItemInv<>(list);
                }
                copying.add((CopyingFixedItemInv) inv);
            } else if (inv instanceof ModifiableFixedItemInv) {
                assert !(inv instanceof CopyingFixedItemInv) : inv.getClass()
                    + " seems to be copying AND modifiable at the same time! That doesn't make sense!";

                if (!copying.isEmpty()) {
                    return new CombinedFixedItemInv<>(list);
                }
                modifiable.add((ModifiableFixedItemInv) inv);
            } else {
                return new CombinedFixedItemInv<>(list);
            }
        }

        assert copying.isEmpty() || modifiable.isEmpty();

        if (copying.isEmpty()) {
            return new OfModifiable<>(modifiable);
        } else {
            return new OfCopying<>(copying);
        }
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        return getInv(slot).setInvStack(getSubSlot(slot), to, simulation);
    }

    @Override
    public FixedItemInv getSubInv(int fromIndex, int toIndex) {
        return (FixedItemInv) super.getSubInv(fromIndex, toIndex);
    }

    @Override
    public FixedItemInv getMappedInv(int... slots) {
        return (FixedItemInv) super.getMappedInv(slots);
    }

    public static class OfModifiable<InvType extends ModifiableFixedItemInv> extends CombinedFixedItemInv<InvType>
        implements ModifiableFixedItemInv {

        public OfModifiable(List<? extends InvType> views) {
            super(views);
        }

        @Override
        public void markDirty() {
            for (InvType inv : views) {
                inv.markDirty();
            }
        }
    }

    public static class OfCopying<InvType extends CopyingFixedItemInv> extends CombinedFixedItemInv<InvType>
        implements CopyingFixedItemInv {

        public OfCopying(List<? extends InvType> views) {
            super(views);
        }

        @Override
        public ItemStack getUnmodifiableInvStack(int slot) {
            return getInv(slot).getUnmodifiableInvStack(getSubSlot(slot));
        }

        @Override
        public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
            final ListenerToken[] tokens = new ListenerToken[views.size()];
            final ListenerRemovalToken ourRemToken = new ListenerRemovalToken() {

                boolean hasAlreadyRemoved = false;

                @Override
                public void onListenerRemoved() {
                    for (ListenerToken token : tokens) {
                        if (token == null) {
                            // This means we have only half-initialised
                            // (and all of the next tokens must also be null)
                            return;
                        }
                        token.removeListener();
                    }
                    if (!hasAlreadyRemoved) {
                        hasAlreadyRemoved = true;
                        removalToken.onListenerRemoved();
                    }
                }
            };
            for (int i = 0; i < tokens.length; i++) {
                final int index = i;
                tokens[i] = views.get(i).addListener((inv, subSlot, previous, current) -> {
                    listener.onChange(this, subSlotStartIndex[index] + subSlot, previous, current);
                }, ourRemToken);
                if (tokens[i] == null) {
                    for (int j = 0; j < i; j++) {
                        tokens[j].removeListener();
                    }
                    return null;
                }
            }
            return () -> {
                for (ListenerToken token : tokens) {
                    token.removeListener();
                }
            };
        }
    }
}
