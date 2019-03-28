package alexiil.mc.lib.attributes;

/** A token for any registered listener in LibBlockAttributes. */
@FunctionalInterface
public interface IListenerRemovalToken {
    /** Callback after any listener was removed. */
    void onListenerRemoved();
}
