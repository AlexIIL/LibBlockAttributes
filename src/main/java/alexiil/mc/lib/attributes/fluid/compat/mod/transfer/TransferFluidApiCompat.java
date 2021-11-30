/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.compat.mod.transfer;

import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.CustomAttributeAdder;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

final class TransferFluidApiCompat {

    static final long BUCKET_VALUE = 81000;

    static void load() {
        FluidAttributes.GROUPED_INV_VIEW.appendBlockAdder(createAdder(s -> true));
        FluidAttributes.GROUPED_INV.appendBlockAdder(createAdder(s -> s.supportsExtraction() || s.supportsInsertion()));
        FluidAttributes.EXTRACTABLE.appendBlockAdder(createAdder(Storage::supportsExtraction));
        FluidAttributes.INSERTABLE.appendBlockAdder(createAdder(Storage::supportsInsertion));
    }

    private static <T> CustomAttributeAdder<T> createAdder(Predicate<Storage<FluidVariant>> tester) {
        return (w, p, s, t) -> addBlock(tester, w, p, s, t);
    }

    private static <T> void addBlock(
        Predicate<Storage<FluidVariant>> tester, World world, BlockPos pos, BlockState state, AttributeList<T> to
    ) {
        Storage<FluidVariant> sto = getStorage(world, pos, state, to);
        if (sto != null && tester.test(sto)) {
            to.offer(new Transfer2Lba(sto));
        }
    }

    private static <T> Storage<FluidVariant> getStorage(
        World world, BlockPos pos, BlockState state, AttributeList<T> to
    ) {
        if (world.isClient()) {
            return null;
        }
        Storage<FluidVariant> sto = null;
        if (to.getTargetSide() != null) {
            sto = FluidStorage.SIDED.find(world, pos, state, null, to.getTargetSide());
        }
        return sto;
    }

    static final class Transfer2Lba implements GroupedFluidInv {

        final Storage<FluidVariant> from;

        Transfer2Lba(Storage<FluidVariant> from) {
            this.from = from;
        }

        @Override
        public Set<FluidKey> getStoredFluids() {
            Set<FluidKey> set = new HashSet<>();
            try (Transaction t = Transaction.openOuter()) {
                for (StorageView<FluidVariant> entry : from.iterable(t)) {
                    FluidVariant res = entry.getResource();
                    if (!res.isBlank()) {
                        set.add(FluidKeys.get(res.getFluid()));
                    }
                }
            }
            return set;
        }

        @Override
        public FluidAmount getTotalCapacity_F() {
            FluidAmount capacity = FluidAmount.ZERO;
            try (Transaction t = Transaction.openOuter()) {
                for (StorageView<FluidVariant> entry : from.iterable(t)) {
                    capacity = capacity.saturatedAdd(FluidAmount.of(entry.getAmount(), BUCKET_VALUE));
                }
            }
            return capacity;
        }

        @Override
        public FluidInvStatistic getStatistics(FluidFilter filter) {
            FluidAmount amount = FluidAmount.ZERO;
            FluidAmount spaceAddable = FluidAmount.ZERO;
            try (Transaction t = Transaction.openOuter()) {
                for (StorageView<FluidVariant> entry : from.iterable(t)) {
                    FluidVariant res = entry.getResource();

                    if (res.isBlank()) {
                        continue;
                    } else if (filter.matches(FluidKeys.get(res.getFluid()))) {
                        amount = amount.saturatedAdd(FluidAmount.of(entry.getAmount(), BUCKET_VALUE));
                        spaceAddable = spaceAddable
                            .saturatedAdd(FluidAmount.of(entry.getCapacity() - entry.getAmount(), BUCKET_VALUE));
                    }
                }
            }
            return new FluidInvStatistic(filter, amount, spaceAddable, FluidAmount.NEGATIVE_ONE);
        }

        @Override
        public FluidVolume attemptInsertion(FluidVolume volume, Simulation simulation) {
            Fluid mcFluid;
            if (volume.isEmpty() || (mcFluid = volume.getRawFluid()) == null) {
                return volume;
            }

            FluidVolume result;

            try (Transaction t = Transaction.openOuter()) {

                long inserted
                    = from.insert(FluidVariant.of(mcFluid), volume.amount().asLong(BUCKET_VALUE, RoundingMode.DOWN), t);

                if (inserted > 0) {
                    volume = volume.copy();
                    volume.split(FluidAmount.of(inserted, BUCKET_VALUE));
                    if (volume.isEmpty()) {
                        result = FluidVolumeUtil.EMPTY;
                    } else {
                        result = volume;
                    }
                } else {
                    result = volume;
                }
                endTransaction(simulation, t);
            }

            return result;
        }

        @Override
        public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {

            final FluidVolume result;

            if (filter instanceof ExactFluidFilter exact) {

                Fluid mcFluid;

                if ((mcFluid = exact.fluid.getRawFluid()) == null) {
                    return FluidVolumeUtil.EMPTY;
                }

                try (Transaction t = Transaction.openOuter()) {

                    long extracted
                        = from.extract(FluidVariant.of(mcFluid), maxAmount.asLong(BUCKET_VALUE, RoundingMode.DOWN), t);

                    if (extracted > 0) {
                        result = exact.fluid.withAmount(FluidAmount.of(extracted, BUCKET_VALUE));
                    } else {
                        result = FluidVolumeUtil.EMPTY;
                    }
                    endTransaction(simulation, t);
                }
            } else {

                try (Transaction t = Transaction.openOuter()) {

                    try_extract: {
                        for (StorageView<FluidVariant> entry : from.iterable(t)) {
                            FluidVariant res = entry.getResource();
                            if (res.isBlank()) {
                                continue;
                            }
                            FluidKey fluidKey = FluidKeys.get(res.getFluid());
                            if (filter.matches(fluidKey)) {

                                long extracted
                                    = from.extract(res, maxAmount.asLong(BUCKET_VALUE, RoundingMode.DOWN), t);

                                if (extracted > 0) {
                                    result = fluidKey.withAmount(FluidAmount.of(extracted, BUCKET_VALUE));
                                    break try_extract;
                                }
                            }
                        }
                        result = FluidVolumeUtil.EMPTY;
                    }
                    endTransaction(simulation, t);
                }
            }

            return result;
        }

        private static void endTransaction(Simulation simulation, Transaction t) {
            if (simulation.isSimulate()) {
                t.abort();
            } else {
                t.commit();
            }
        }
    }
}
