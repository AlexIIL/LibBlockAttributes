package alexiil.mc.lib.attributes.misc;

import java.util.function.Supplier;

import alexiil.mc.lib.attributes.Simulation;

/** A variant of {@link CallableRef} that uses {@link Supplier} for {@link #get()}, and {@link LimitedConsumer} for
 * {@link #set(Object)} and {@link #isValid(Object)}. */
public final class SimulatableRef<T> implements Reference<T> {

    private final Supplier<T> getter;
    private final LimitedConsumer<T> setter;

    public SimulatableRef(Supplier<T> getter, LimitedConsumer<T> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public T get() {
        return getter.get();
    }

    @Override
    public boolean set(T value) {
        return setter.offer(value);
    }

    @Override
    public boolean isValid(T value) {
        return setter.wouldAccept(value);
    }

    @Override
    public boolean set(T value, Simulation simulation) {
        return setter.offer(value, simulation);
    }
}
