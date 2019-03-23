package alexiil.mc.lib.attributes.item;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;

/** Utility methods for creating {@link Map}'s and {@link Set}'s based on {@link ItemStack}'s.
 * <p>
 * Every set or map factory method will produce a set of map using {@link #STRATEGY_IGNORE_AMOUNT} or
 * {@link #COMPARATOR_IGNORE_AMOUNT}, rather than their exact versions. */
public enum ItemStackCollections {
    ;

    /** A hash {@link Strategy} to use for {@link Map Map's} and {@link Set Set's} of {@link ItemStack}'s where the
     * {@link ItemStack#getAmount()} is important. */
    public static final Strategy<ItemStack> STRATEGY_EXACT = new Strategy<ItemStack>() {
        @Override
        public int hashCode(ItemStack o) {
            if (o.isEmpty()) {
                return 0;
            }
            return Arrays.hashCode(
                new int[] { o.getAmount(), System.identityHashCode(o.getItem()), Objects.hashCode(o.getTag()) });
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            return ItemStack.areEqual(a, b);
        }
    };

    /** A hash {@link Strategy} to use for {@link Map Map's} and {@link Set Set's} of {@link ItemStack}'s where the
     * {@link ItemStack#getAmount()} is important. */
    public static final Strategy<ItemStack> STRATEGY_IGNORE_AMOUNT = new Strategy<ItemStack>() {
        @Override
        public int hashCode(ItemStack o) {
            if (o.isEmpty()) {
                return 0;
            }
            return Arrays.hashCode(new int[] { System.identityHashCode(o.getItem()), Objects.hashCode(o.getTag()) });
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            return ItemStackUtil.areEqualIgnoreAmounts(a, b);
        }
    };

    /** A {@link Comparator} that compares {@link ItemStack}'s by their Registry {@link Identifier}, then
     * {@link ItemStack#getTag()}, then amounts. */
    public static final Comparator<ItemStack> COMPARATOR_ID_EXACT = ItemStackCollections::compareItemStacksExact;

    /** A {@link Comparator} that compares {@link ItemStack}'s by their Registry {@link Identifier}, then
     * {@link ItemStack#getTag()}. */
    public static final Comparator<ItemStack> COMPARATOR_IGNORE_AMOUNT =
        ItemStackCollections::compareItemStacksIgnoreAmounts;

    private static int compareItemStacksExact(ItemStack a, ItemStack b) {
        int comp = compareItemStacksIgnoreAmounts(a, b);
        if (comp != 0) {
            return comp;
        }
        return Integer.compare(a.getAmount(), b.getAmount());
    }

    private static int compareItemStacksIgnoreAmounts(ItemStack a, ItemStack b) {
        if (a.isEmpty()) {
            return b.isEmpty() ? 0 : 1;
        }
        if (b.isEmpty()) {
            return -1;
        }
        if (a.getItem() != b.getItem()) {
            Identifier idA = Registry.ITEM.getId(a.getItem());
            Identifier idB = Registry.ITEM.getId(b.getItem());
            int comp;
            if ((comp = idA.getNamespace().compareTo(idB.getNamespace())) != 0) {
                return comp;
            }
            if ((comp = idA.getPath().compareTo(idB.getPath())) != 0) {
                return comp;
            }
        }
        CompoundTag tagA = a.getTag();
        CompoundTag tagB = b.getTag();
        if (tagA == null) {
            if (tagB != null) {
                return 1;
            }
        } else if (tagB == null) {
            return -1;
        } else {
            // TODO: Actually compare the tags in a reasonable order!
        }
        return 0;
    }

    // ########
    //
    // Sets
    //
    // ########

    /** Creates a set that can store {@link ItemStack}'s.
     * <p>
     * The current implementation delegates to {@link #openHashSet()}. */
    public static Set<ItemStack> set() {
        return openHashSet();
    }

    /** Creates a sorted set that can store {@link ItemStack}'s.
     * <p>
     * The current implementation delegates to {@link #sortedAvlTreeSet()}. */
    public static ObjectSortedSet<ItemStack> sortedSet() {
        return sortedAvlTreeSet();
    }

    public static ObjectOpenCustomHashSet<ItemStack> openHashSet() {
        return new ObjectOpenCustomHashSet<>(STRATEGY_IGNORE_AMOUNT);
    }

    public static ObjectLinkedOpenCustomHashSet<ItemStack> openLinkedHashSet() {
        return new ObjectLinkedOpenCustomHashSet<>(STRATEGY_IGNORE_AMOUNT);
    }

    public static ObjectRBTreeSet<ItemStack> sortedRbTreeSet() {
        return new ObjectRBTreeSet<>(COMPARATOR_IGNORE_AMOUNT);
    }

    public static ObjectAVLTreeSet<ItemStack> sortedAvlTreeSet() {
        return new ObjectAVLTreeSet<>(COMPARATOR_IGNORE_AMOUNT);
    }

    // ########
    //
    // Object Maps
    //
    // ########

    /** Creates a {@link Map} that can map {@link ItemStack}'s to objects.
     * <p>
     * The current implementation delegates to {@link #hashMap()}. */
    public static <V> Map<ItemStack, V> map() {
        return hashMap();
    }

    /** Creates a {@link SortedMap} that can map {@link ItemStack}'s to objects.
     * <p>
     * The current implementation delegates to {@link #avlTreeMap()}. */
    public static <V> SortedMap<ItemStack, V> sortedMap() {
        return avlTreeMap();
    }

    public static <V> Object2ObjectOpenCustomHashMap<ItemStack, V> hashMap() {
        return new Object2ObjectOpenCustomHashMap<>(STRATEGY_IGNORE_AMOUNT);
    }

    public static <V> Object2ObjectLinkedOpenCustomHashMap<ItemStack, V> linkedHashMap() {
        return new Object2ObjectLinkedOpenCustomHashMap<>(STRATEGY_IGNORE_AMOUNT);
    }

    public static <V> Object2ObjectRBTreeMap<ItemStack, V> rbTreeMap() {
        return new Object2ObjectRBTreeMap<>(COMPARATOR_IGNORE_AMOUNT);
    }

    public static <V> Object2ObjectAVLTreeMap<ItemStack, V> avlTreeMap() {
        return new Object2ObjectAVLTreeMap<>(COMPARATOR_IGNORE_AMOUNT);
    }

    // ########
    //
    // Int Maps
    //
    // ########

    /** Creates a {@link Map} that can map {@link ItemStack}'s to ints.
     * <p>
     * The current implementation delegates to {@link #intHashMap()}. */
    public static Object2IntMap<ItemStack> intMap() {
        return intHashMap();
    }

    /** Creates a {@link SortedMap} that can map {@link ItemStack}'s to ints.
     * <p>
     * The current implementation delegates to {@link #intAvlTreeMap()}. */
    public static Object2IntSortedMap<ItemStack> intSortedMap() {
        return intAvlTreeMap();
    }

    public static Object2IntOpenCustomHashMap<ItemStack> intHashMap() {
        return new Object2IntOpenCustomHashMap<>(STRATEGY_IGNORE_AMOUNT);
    }

    public static Object2IntLinkedOpenCustomHashMap<ItemStack> intLinkedHashMap() {
        return new Object2IntLinkedOpenCustomHashMap<>(STRATEGY_IGNORE_AMOUNT);
    }

    public static Object2IntRBTreeMap<ItemStack> intRbTreeMap() {
        return new Object2IntRBTreeMap<>(COMPARATOR_IGNORE_AMOUNT);
    }

    public static Object2IntAVLTreeMap<ItemStack> intAvlTreeMap() {
        return new Object2IntAVLTreeMap<>(COMPARATOR_IGNORE_AMOUNT);
    }
}
