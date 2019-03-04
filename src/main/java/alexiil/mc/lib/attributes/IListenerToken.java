package alexiil.mc.lib.attributes;

/** A token for any registered listener in LibBlockAttributes. */
@FunctionalInterface
public interface IListenerToken {
    /** Removes the listener from wherever it's registered. */
    void removeListener();
}
