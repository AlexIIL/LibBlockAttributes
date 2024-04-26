/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.jetbrains.annotations.Nullable;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public record PotionContents(Optional<RegistryEntry<Potion>> potion, Optional<Integer> customColor,
                             List<StatusEffectInstance> customEffects) {

    public List<StatusEffectInstance> customEffects() {
        return Lists.transform(this.customEffects, StatusEffectInstance::new);
    }

    public static PotionContents fromComponent(PotionContentsComponent component) {
        return new PotionContents(component.potion(), component.customColor(), component.customEffects());
    }
    
    public static PotionContents ofPotion(RegistryEntry<Potion> entry) {
        return new PotionContents(Optional.of(entry), Optional.empty(), List.of());
    }
    
    public static PotionContents ofPotion(Potion potion) {
        return new PotionContents(Optional.of(RegistryEntry.of(potion)), Optional.empty(), List.of());
    }

    public PotionContentsComponent toComponent() {
        return new PotionContentsComponent(potion, customColor, customEffects);
    }

    public static @Nullable PotionContents fromStack(ItemStack stack) {
        PotionContentsComponent component = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (component == null) return null;
        return fromComponent(component);
    }

    public ItemStack createStack(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponentTypes.POTION_CONTENTS, toComponent());
        return stack;
    }

    public Iterable<StatusEffectInstance> getEffects() {
        if (this.potion.isEmpty()) {
            return this.customEffects;
        } else {
            return this.customEffects.isEmpty()
                    ? this.potion.get().value().getEffects()
                    : Iterables.concat(this.potion.get().value().getEffects(), this.customEffects);
        }
    }

    public void forEachEffect(Consumer<StatusEffectInstance> effectConsumer) {
        if (this.potion.isPresent()) {
            for (StatusEffectInstance statusEffectInstance : this.potion.get().value().getEffects()) {
                effectConsumer.accept(new StatusEffectInstance(statusEffectInstance));
            }
        }

        for (StatusEffectInstance statusEffectInstance : this.customEffects) {
            effectConsumer.accept(new StatusEffectInstance(statusEffectInstance));
        }
    }

    public PotionContents with(RegistryEntry<Potion> potion) {
        return new PotionContents(Optional.of(potion), this.customColor, this.customEffects);
    }

    public PotionContents with(StatusEffectInstance customEffect) {
        return new PotionContents(this.potion, this.customColor, Util.withAppended(this.customEffects, customEffect));
    }

    public int getColor() {
        return this.customColor.orElseGet(() -> PotionContentsComponent.getColor(this.getEffects()));
    }

    public boolean hasEffects() {
        if (!this.customEffects.isEmpty()) {
            return true;
        } else {
            return this.potion.isPresent() && !this.potion.get().value().getEffects().isEmpty();
        }
    }

    public void buildTooltip(Consumer<Text> textConsumer, float durationMultiplier, float tickRate) {
        PotionContentsComponent.buildTooltip(this.getEffects(), textConsumer, durationMultiplier, tickRate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PotionContents that = (PotionContents) o;
        return customColor.equals(that.customColor) && potion.map(RegistryEntry::getKeyOrValue).equals(that.potion.map(RegistryEntry::getKeyOrValue)) && customEffects.equals(that.customEffects);
    }

    @Override
    public int hashCode() {
        int result = potion.map(RegistryEntry::getKeyOrValue).hashCode();
        result = 31 * result + customColor.hashCode();
        result = 31 * result + customEffects.hashCode();
        return result;
    }
}
