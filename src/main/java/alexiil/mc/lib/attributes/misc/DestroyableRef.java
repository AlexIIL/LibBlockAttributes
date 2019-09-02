package alexiil.mc.lib.attributes.misc;

/** A {@link Reference} that can be modified until {@link #destroy()} is called, after which all calls to
 * {@link #set(Object)} and {@link #isValid(Object)} will return false. */
public final class DestroyableRef<T> implements Reference<T> {

    private final Reference<T> ref;
    private boolean isAlive = true;

    public DestroyableRef(Reference<T> ref) {
        this.ref = ref;
    }

    public void destroy() {
        isAlive = false;
    }

    @Override
    public T get() {
        return ref.get();
    }

    @Override
    public boolean set(T value) {
        return isAlive && ref.set(value);
    }

    @Override
    public boolean isValid(T value) {
        return isAlive && ref.isValid(value);
    }

}
