package alexiil.mc.lib.attributes;

/** A token for any registered listener in LibBlockAttributes. */
@FunctionalInterface
public interface ListenerToken {
    /** Removes the listener from wherever it's registered. */
    void removeListener();
}
