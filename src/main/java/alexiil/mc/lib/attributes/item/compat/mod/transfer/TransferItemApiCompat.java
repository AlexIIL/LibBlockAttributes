/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat.mod.transfer;

import java.util.Set;
import java.util.function.Predicate;

import com.google.common.math.IntMath;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.CustomAttributeAdder;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.ItemAttributes;
import alexiil.mc.lib.attributes.item.ItemStackCollections;
import alexiil.mc.lib.attributes.item.filter.ExactItemStackFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

final class TransferItemApiCompat {
    static void load() {
        ItemAttributes.GROUPED_INV_VIEW.appendBlockAdder(createAdder(s -> true));
        ItemAttributes.GROUPED_INV.appendBlockAdder(createAdder(s -> s.supportsExtraction() || s.supportsInsertion()));
        ItemAttributes.EXTRACTABLE.appendBlockAdder(createAdder(Storage::supportsExtraction));
        ItemAttributes.INSERTABLE.appendBlockAdder(createAdder(Storage::supportsInsertion));
    }

    private static <T> CustomAttributeAdder<T> createAdder(Predicate<Storage<ItemVariant>> tester) {
        return (w, p, s, t) -> addBlock(tester, w, p, s, t);
    }

    private static <T> void addBlock(
        Predicate<Storage<ItemVariant>> tester, World world, BlockPos pos, BlockState state, AttributeList<T> to
    ) {
        Storage<ItemVariant> sto = getStorage(world, pos, state, to);
        if (sto != null && tester.test(sto)) {
            to.offer(new Transfer2Lba(sto));
        }
    }

    private static <T> Storage<ItemVariant> getStorage(
        World world, BlockPos pos, BlockState state, AttributeList<T> to
    ) {
        if (world.isClient()) {
            return null;
        }
        Storage<ItemVariant> sto = null;
        if (to.getTargetSide() != null) {
            sto = ItemStorage.SIDED.find(world, pos, state, null, to.getTargetSide());
        }
        return sto;
    }

    static final class Transfer2Lba implements GroupedItemInv {

        final Storage<ItemVariant> from;

        Transfer2Lba(Storage<ItemVariant> from) {
            this.from = from;
        }

        @Override
        public Set<ItemStack> getStoredStacks() {
            Set<ItemStack> set = ItemStackCollections.set();
            try (Transaction t = beginTransaction()) {
                for (StorageView<ItemVariant> entry : from.iterable(t)) {
                    ItemVariant res = entry.getResource();
                    if (!res.isBlank()) {
                        set.add(res.toStack());
                    }
                }
            }
            return set;
        }

        @Override
        public int getTotalCapacity() {
            int capacity = 0;
            try (Transaction t = beginTransaction()) {
                for (StorageView<ItemVariant> entry : from.iterable(t)) {
                    int c = (int) Math.max(0, Math.min(Integer.MAX_VALUE, entry.getCapacity()));
                    capacity = IntMath.saturatedAdd(capacity, c);
                }
            }
            return capacity;
        }

        @Override
        public ItemInvStatistic getStatistics(ItemFilter filter) {
            int amount = 0;
            int spaceAddable = 0;
            try (Transaction t = beginTransaction()) {
                for (StorageView<ItemVariant> entry : from.iterable(t)) {
                    ItemVariant res = entry.getResource();

                    if (res.isBlank()) {
                        continue;
                    } else if (filter.matches(res.toStack())) {
                        int a = (int) Math.max(0, Math.min(Integer.MAX_VALUE, entry.getAmount()));
                        amount = IntMath.saturatedAdd(amount, a);

                        int s = (int) Math.max(0, Math.min(Integer.MAX_VALUE, entry.getCapacity() - entry.getAmount()));
                        spaceAddable = IntMath.saturatedAdd(spaceAddable, s);
                    }
                }
            }
            return new ItemInvStatistic(filter, amount, spaceAddable, -1);
        }

        @Override
        public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
            if (stack.isEmpty()) {
                return stack;
            }

            ItemStack result;

            try (Transaction t = beginTransaction()) {

                int inserted = (int) from.insert(ItemVariant.of(stack), stack.getCount(), t);

                if (inserted == stack.getCount()) {
                    result = ItemStack.EMPTY;
                } else if (inserted > 0) {
                    stack = stack.copy();
                    stack.decrement(inserted);
                    result = stack;
                } else {
                    result = stack;
                }
                endTransaction(simulation, t);
            }

            return result;
        }

        @Override
        public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {

            final ItemStack result;

            if (filter instanceof ExactItemStackFilter exact) {

                try (Transaction t = beginTransaction()) {

                    long extracted = from.extract(ItemVariant.of(exact.stack), maxAmount, t);

                    if (extracted > 0) {
                        result = exact.stack.copy();
                        result.setCount((int) extracted);

                    } else {
                        result = ItemStack.EMPTY;
                    }
                    endTransaction(simulation, t);
                }
            } else {

                try (Transaction t = beginTransaction()) {

                    try_extract: {
                        for (StorageView<ItemVariant> entry : from.iterable(t)) {
                            ItemVariant res = entry.getResource();
                            if (!res.isBlank() && filter.matches(res.toStack())) {

                                long extracted = from.extract(res, maxAmount, t);

                                if (extracted > 0) {
                                    result = res.toStack();
                                    result.setCount((int) extracted);
                                    break try_extract;
                                }
                            }
                        }
                        result = ItemStack.EMPTY;
                    }
                    endTransaction(simulation, t);
                }
            }

            return result;
        }

        private static Transaction beginTransaction() {
            return Transaction.openNested(Transaction.getCurrentUnsafe());
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
