package alexiil.mc.mod.pipes.util;

import java.util.BitSet;
import java.util.EnumSet;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public enum TagUtil {
    ;

    private static final String NULL_ENUM_STRING = "_NULL";

    public static <E extends Enum<E>> Tag writeEnum(E value) {
        if (value == null) {
            return new StringTag(NULL_ENUM_STRING);
        }
        return new StringTag(value.name());
    }

    public static <E extends Enum<E>> E readEnum(Tag tag, Class<E> clazz) {
        if (tag instanceof StringTag) {
            String value = ((StringTag) tag).asString();
            if (NULL_ENUM_STRING.equals(value)) {
                return null;
            }
            try {
                return Enum.valueOf(clazz, value);
            } catch (Throwable t) {
                // In case we didn't find the constant
                System.out.println("Tried and failed to read the value(" + value + ") from " + clazz.getSimpleName());
                t.printStackTrace();
                return null;
            }
        } else if (tag == null) {
            return null;
        } else {
            new IllegalArgumentException(
                "Tried to read an enum value when it was not a string! This is probably not good!").printStackTrace();
            ;
            return null;
        }
    }

    /** Writes an {@link EnumSet} to an {@link Tag}. The returned type will either be {@link ByteTag} or
     * {@link ByteArrayTag}.
     * 
     * @param clazz The class that the {@link EnumSet} is of. This is required as we have no way of getting the class
     *            from the set. */
    public static <E extends Enum<E>> Tag writeEnumSet(EnumSet<E> set, Class<E> clazz) {
        E[] constants = clazz.getEnumConstants();
        if (constants == null) throw new IllegalArgumentException("Not an enum type " + clazz);
        BitSet bitset = new BitSet();
        for (E e : constants) {
            if (set.contains(e)) {
                bitset.set(e.ordinal());
            }
        }
        byte[] bytes = bitset.toByteArray();
        if (bytes.length == 1) {
            return new ByteTag(bytes[0]);
        } else {
            return new ByteArrayTag(bytes);
        }
    }

    public static <E extends Enum<E>> EnumSet<E> readEnumSet(Tag tag, Class<E> clazz) {
        E[] constants = clazz.getEnumConstants();
        if (constants == null) throw new IllegalArgumentException("Not an enum type " + clazz);
        byte[] bytes;
        if (tag instanceof ByteTag) {
            bytes = new byte[] { ((ByteTag) tag).getByte() };
        } else if (tag instanceof ByteArrayTag) {
            bytes = ((ByteArrayTag) tag).getByteArray();
        } else {
            bytes = new byte[] {};
            System.out.println("[lib.nbt] Tried to read an enum set from " + tag);
        }
        BitSet bitset = BitSet.valueOf(bytes);
        EnumSet<E> set = EnumSet.noneOf(clazz);
        for (E e : constants) {
            if (bitset.get(e.ordinal())) {
                set.add(e);
            }
        }
        return set;
    }
}
