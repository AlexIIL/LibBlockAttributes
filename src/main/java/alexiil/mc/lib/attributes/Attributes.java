package alexiil.mc.lib.attributes;

import javax.annotation.Nonnull;

public class Attributes {
    public static <T> Attribute<T> create(Class<T> clazz) {
        return new Attribute<>(clazz);
    }

    public static <T> Attribute<T> create(Class<T> clazz, IAttributeCustomAdder<T> customAdder) {
        return new Attribute<>(clazz, customAdder);
    }

    public static <T> AttributeDefaulted<T> createDefaulted(Class<T> clazz, @Nonnull T defaultValue) {
        return new AttributeDefaulted<>(clazz, defaultValue);
    }

    public static <T> AttributeDefaulted<T> createDefaulted(Class<T> clazz, @Nonnull T defaultValue,
        IAttributeCustomAdder<T> customAdder) {
        return new AttributeDefaulted<>(clazz, defaultValue, customAdder);
    }

    public static <T> AttributeCombinable<T> createCombinable(Class<T> clazz, @Nonnull T defaultValue,
        IAttributeCombiner<T> combiner) {
        return new AttributeCombinable<>(clazz, defaultValue, combiner);
    }

    public static <T> AttributeCombinable<T> createCombinable(Class<T> clazz, @Nonnull T defaultValue,
        IAttributeCombiner<T> combiner, IAttributeCustomAdder<T> customAdder) {
        return new AttributeCombinable<>(clazz, defaultValue, combiner, customAdder);
    }
}
