package alexiil.mc.lib.attributes;

public class Attribute<T> {
    public final Class<T> clazz;

    // Is there a good reason why this constructor is protected?

    protected Attribute(Class<T> clazz) {
        this.clazz = clazz;
    }

    public static <T> Attribute<T> create(Class<T> clazz) {
        return new Attribute<>(clazz);
    }

    public final boolean isInstance(Object obj) {
        return clazz.isInstance(obj);
    }

    public final T cast(Object obj) {
        return clazz.cast(obj);
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    // I'm not sure if this is a good idea?
    // Either way we would need "directional attributes" vs "don't care" attributes...

    // @Nullable
    // public final List<T> get(World world, BlockPos pos) {
    // // Perhaps if this wasn't final then all of the hooks could be in subclasses rather than AttributeObtainingImpl?
    // return AttributeUtil.getAttribute(world, pos, this);
    // }
}
