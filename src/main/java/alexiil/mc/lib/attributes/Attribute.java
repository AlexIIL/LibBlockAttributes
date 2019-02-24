package alexiil.mc.lib.attributes;

public class Attribute<T> {
    public final Class<T> clazz;

    public Attribute(Class<T> clazz) {
        this.clazz = clazz;
    }

    public final boolean isInstance(Object obj) {
        return clazz.isInstance(obj);
    }

    public final T cast(Object obj) {
        return clazz.cast(obj);
    }
}
