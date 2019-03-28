package alexiil.mc.lib.attributes.item.impl;

import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.misc.BoolRef;

/** An {@link FixedItemInvView} that delegates to a list of them instead of storing items directly. */
public class CombinedFixedItemInvView<InvType extends FixedItemInvView> implements FixedItemInvView {

    public final List<? extends InvType> views;
    private final int[] subSlotStartIndex;
    private final int invSize;

    public CombinedFixedItemInvView(List<? extends InvType> views) {
        this.views = views;
        subSlotStartIndex = new int[views.size()];
        int size = 0;
        for (int i = 0; i < views.size(); i++) {
            subSlotStartIndex[i] = size;
            FixedItemInvView view = views.get(i);
            int s = view.getSlotCount();
            size += s;
        }
        invSize = size;
    }

    @Override
    public int getSlotCount() {
        return invSize;
    }

    protected InvType getInv(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Slot must be non-negative! (was " + slot + ")");
        }

        for (int i = 0; i < subSlotStartIndex.length; i++) {
            int startIndex = subSlotStartIndex[i];
            if (slot < startIndex) {
                return views.get(i);
            }
        }
        if (slot < invSize) {
            return views.get(views.size() - 1);
        }

        throw new IllegalArgumentException(
            "Slot must be less than getInvSize() (was " + slot + ", maximum slot is " + (invSize - 1) + ")");
    }

    protected int getSubSlot(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Slot must be non-negative! (was " + slot + ")");
        }

        for (int i = 0; i < subSlotStartIndex.length; i++) {
            int startIndex = subSlotStartIndex[i];
            if (slot < startIndex) {
                if (i == 0) {
                    return slot;
                }
                return slot - subSlotStartIndex[i - 1];
            }
        }
        if (slot < invSize) {
            return slot - subSlotStartIndex[subSlotStartIndex.length - 1];
        }

        throw new IllegalArgumentException(
            "Slot must be less than getInvSize() (was " + slot + ", maximum slot is " + (invSize - 1) + ")");
    }

    @Override
    public ItemStack getInvStack(int slot) {
        return getInv(slot).getInvStack(getSubSlot(slot));
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return getInv(slot).isItemValidForSlot(getSubSlot(slot), item);
    }

    @Override
    public ItemFilter getFilterForSlot(int slot) {
        return getInv(slot).getFilterForSlot(getSubSlot(slot));
    }

    @Override
    public int getMaxAmount(int slot, ItemStack stack) {
        return getInv(slot).getMaxAmount(getSubSlot(slot), stack);
    }

    @Override
    public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
        final ListenerToken[] tokens = new ListenerToken[views.size()];
        final BoolRef hasAlreadyRemoved = new BoolRef(false);
        final ListenerRemovalToken ourRemToken = () -> {
            for (ListenerToken token : tokens) {
                if (token == null) {
                    // This means we have only half-initialised
                    // (and all of the next tokens must also be null)
                    return;
                }
                token.removeListener();
            }
            if (!hasAlreadyRemoved.value) {
                hasAlreadyRemoved.value = true;
                removalToken.onListenerRemoved();
            }

        };
        for (int i = 0; i < tokens.length; i++) {
            final int index = i;
            tokens[i] = views.get(i).addListener((inv, subTank, previous, current) -> {
                listener.onChange(this, subSlotStartIndex[index] + subTank, previous, current);
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
