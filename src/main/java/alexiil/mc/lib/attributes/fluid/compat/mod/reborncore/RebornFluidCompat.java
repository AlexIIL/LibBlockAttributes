/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.compat.mod.reborncore;

import java.math.RoundingMode;
import java.util.Collections;
import java.util.Set;

import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.AttributeSourceType;
import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import alexiil.mc.lib.attributes.fluid.FluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

import reborncore.common.blockentity.FluidConfiguration;
import reborncore.common.blockentity.FluidConfiguration.ExtractConfig;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.common.fluid.FluidValue;
import reborncore.common.fluid.container.FluidInstance;
import reborncore.common.util.Tank;

/* package-private */ final class RebornFluidCompat {
    private RebornFluidCompat() {}

    static void load() {
        FluidAttributes.forEachInv(RebornFluidCompat::appendAdder);
    }

    private static <T> void appendAdder(CombinableAttribute<T> attribute) {
        attribute.putBlockEntityClassAdder(
            AttributeSourceType.COMPAT_WRAPPER, MachineBaseBlockEntity.class, true, RebornFluidCompat::addWrapper
        );
    }

    private static <T> void addWrapper(MachineBaseBlockEntity machine, AttributeList<T> list) {
        Direction dir = list.getSearchDirection();
        if (dir == null) {
            return;
        }
        Direction side = dir.getOpposite();

        Tank tank = machine.getTank();
        if (tank == null) {
            return;
        }

        if (tank.getCapacity().getRawValue() <= 0) {
            return;
        }
        list.offer(new RebornFluidTankWrapper(side, machine, tank));
    }

    static final class RebornFluidTankWrapper implements GroupedFluidInv, FixedFluidInv {
        private static final int REBORN_UNIT = 1000;
        private final Direction side;
        private final MachineBaseBlockEntity base;
        private final Tank tank;

        RebornFluidTankWrapper(Direction side, MachineBaseBlockEntity base, Tank tank) {
            this.side = side;
            this.base = base;
            this.tank = tank;
        }

        // FluidInsertable

        @Override
        public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
            FluidValue max = base.fluidTransferAmount();
            if (max.getRawValue() <= 0) {
                return fluid;
            }
            // TODO: Filters!
            // (Although that would need RebornCore to add a filtering method...
            Fluid rawFluid = fluid.getFluidKey().getRawFluid();
            if (rawFluid == null) {
                return fluid;
            }
            FluidInstance fi = tank.getFluidInstance();
            int tankAmount = fi.getAmount().getRawValue();
            int space = tank.getCapacity().getRawValue() - tankAmount;
            if (space <= 0) {
                return fluid;
            }
            if (tankAmount > 0 && fi.getFluid() != rawFluid) {
                return fluid;
            }
            FluidConfiguration cfg = base.fluidConfiguration;
            ExtractConfig ioConfig = cfg != null ? cfg.getSideDetail(side).getIoConfig() : ExtractConfig.ALL;
            if (ioConfig.isInsert()) {
                int offeredAmount = fluid.getAmount_F().asInt(REBORN_UNIT, RoundingMode.DOWN);
                if (offeredAmount > space) {
                    offeredAmount = space;
                }
                if (offeredAmount > max.getRawValue()) {
                    offeredAmount = max.getRawValue();
                }
                FluidVolume ret = fluid.copy();
                FluidVolume offered = ret.split(FluidAmount.of(offeredAmount, REBORN_UNIT));

                FluidAmount mul = offered.getAmount_F().checkedMul(REBORN_UNIT);
                assert mul.whole == offeredAmount : "Bad split! (whole)";
                assert mul.numerator == 0 : "Bad split! (numerator)";

                if (simulation.isAction()) {
                    FluidValue val = FluidValue.fromRaw(offeredAmount);
                    if (tankAmount <= 0) {
                        fi = new FluidInstance(rawFluid, val);
                    } else {
                        fi.addAmount(val);
                    }
                    tank.setFluid(side, fi);
                }
                return ret;
            }
            return fluid;
        }

        // FluidExtractable

        @Override
        public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
            FluidValue max = base.fluidTransferAmount();
            if (max.getRawValue() <= 0) {
                return FluidVolumeUtil.EMPTY;
            }
            FluidInstance fi = tank.getFluidInstance();
            int tankAmount = fi.getAmount().getRawValue();
            if (tankAmount <= 0) {
                return FluidVolumeUtil.EMPTY;
            }
            int available
                = Math.min(tankAmount, Math.min(max.getRawValue(), maxAmount.asInt(REBORN_UNIT, RoundingMode.DOWN)));
            if (available <= 0) {
                return FluidVolumeUtil.EMPTY;
            }
            FluidKey key = FluidKeys.get(tank.getFluid());
            if (!filter.matches(key)) {
                return FluidVolumeUtil.EMPTY;
            }
            FluidConfiguration cfg = base.fluidConfiguration;
            ExtractConfig ioConfig = cfg != null ? cfg.getSideDetail(side).getIoConfig() : ExtractConfig.ALL;
            if (ioConfig.isExtact()) {
                FluidVolume volume = key.withAmount(FluidAmount.of(available, REBORN_UNIT));
                if (simulation.isAction()) {
                    fi.subtractAmount(FluidValue.fromRaw(available));
                    tank.setFluid(side, fi);
                }
                return volume;
            }
            return FluidVolumeUtil.EMPTY;
        }

        // GroupedFluidInvView

        @Override
        public Set<FluidKey> getStoredFluids() {
            FluidInstance fi = tank.getFluidInstance();
            int amount = fi.getAmount().getRawValue();
            if (amount <= 0) {
                return Collections.emptySet();
            } else {
                return Collections.singleton(FluidKeys.get(fi.getFluid()));
            }
        }

        @Override
        public FluidInvStatistic getStatistics(FluidFilter filter) {
            FluidInstance fi = tank.getFluidInstance();
            int amount = fi.getAmount().getRawValue();
            if (amount <= 0) {
                FluidAmount capacity = FluidAmount.of(tank.getCapacity().getRawValue(), REBORN_UNIT);
                return new FluidInvStatistic(filter, FluidAmount.ZERO, capacity, capacity);
            } else {
                FluidKey key = FluidKeys.get(fi.getFluid());
                if (filter.matches(key)) {
                    FluidAmount fa = FluidAmount.of(amount, REBORN_UNIT);
                    FluidAmount capacity = FluidAmount.of(tank.getCapacity().getRawValue(), REBORN_UNIT);
                    return new FluidInvStatistic(filter, fa, capacity, capacity);
                } else {
                    FluidAmount fa = FluidAmount.of(amount, REBORN_UNIT);
                    FluidAmount capacity = FluidAmount.of(tank.getCapacity().getRawValue(), REBORN_UNIT);
                    return new FluidInvStatistic(filter, FluidAmount.ZERO, FluidAmount.ZERO, capacity);
                }
            }
        }

        // FixedFluidInvView

        @Override
        public int getTankCount() {
            return 1;
        }

        @Override
        public FluidVolume getInvFluid(int t) {
            FluidInstance fi = tank.getFluidInstance();
            int amount = fi.getAmount().getRawValue();
            if (amount <= 0) {
                return FluidVolumeUtil.EMPTY;
            } else {
                return FluidKeys.get(fi.getFluid()).withAmount(FluidAmount.of(amount, REBORN_UNIT));
            }
        }

        @Override
        public ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
            return null;
        }

        @Override
        public boolean isFluidValidForTank(int tank, FluidKey fluid) {
            return true; // Unfortunately we don't have filters :(
        }

        // FixedFluidInv

        @Override
        public boolean setInvFluid(int t, FluidVolume to, Simulation simulation) {
            if (to.isEmpty()) {
                if (simulation.isAction()) {
                    tank.setFluid(side, new FluidInstance());
                }
                return true;
            }
            FluidAmount mul = to.getAmount_F().saturatedMul(REBORN_UNIT);
            if (mul.numerator != 0) {
                return false;
            }
            if (mul.whole > Integer.MAX_VALUE) {
                return false;
            }
            Fluid raw = to.getRawFluid();
            if (raw == null) {
                return false;
            }
            if (simulation.isAction()) {
                FluidInstance fi = tank.getFluidInstance();
                FluidValue value = FluidValue.fromRaw((int) mul.whole);
                if (fi.getFluid().equals(raw)) {
                    // So we keep the tag
                    fi.setAmount(value);
                } else {
                    tank.setFluid(side, new FluidInstance(raw, value));
                }
            }
            return true;
        }

        @Override
        public GroupedFluidInv getGroupedInv() {
            return this;
        }
    }
}
