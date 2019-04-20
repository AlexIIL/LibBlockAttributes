package alexiil.mc.lib.attributes.fluid.impl;

import java.util.Arrays;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.fluid.FixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.FluidInvTankChangeListener;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class MappedFixedFluidInvView extends AbstractPartialFixedFluidInvView {

    private final int[] tanks;
    private final Int2IntMap inverseTankMap;

    public MappedFixedFluidInvView(FixedFluidInvView inv, int[] tanks) {
        super(inv);
        this.tanks = Arrays.copyOf(tanks, tanks.length);
        inverseTankMap = new Int2IntOpenHashMap(tanks.length);
        inverseTankMap.defaultReturnValue(-1);
        for (int i = 0; i < tanks.length; i++) {
            int s = tanks[i];
            if (s < 0 || s >= inv.getTankCount()) {
                throw new IllegalArgumentException("Invalid tank index: " + s
                    + ", as it must be between 0 and the tank count of " + inv.getTankCount());
            }
            int prev = inverseTankMap.put(s, i);
            if (prev != -1) {
                throw new IllegalStateException("Duplicated tank index! (" + s + " appears at both index " + prev
                    + " and " + i + " in " + Arrays.toString(tanks) + ")");
            }
        }
    }

    @Override
    protected int getInternalTank(int tank) {
        return tanks[tank];
    }

    @Override
    public int getTankCount() {
        return tanks.length;
    }

    @Override
    public ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
        FixedFluidInvView wrapper = this;
        return inv.addListener((realInv, slot, previous, current) -> {
            assert realInv == inv;
            int exposedTank = inverseTankMap.get(slot);
            if (exposedTank >= 0) {
                listener.onChange(wrapper, exposedTank, previous, current);
            }
        }, removalToken);
    }
}
