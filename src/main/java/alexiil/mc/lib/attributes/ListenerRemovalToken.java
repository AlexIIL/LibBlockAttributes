package alexiil.mc.lib.attributes;

/** A token for any registered listener in LibBlockAttributes. */
@FunctionalInterface
public interface ListenerRemovalToken {
    /** Callback after any listener was removed. */
    void onListenerRemoved();
}
