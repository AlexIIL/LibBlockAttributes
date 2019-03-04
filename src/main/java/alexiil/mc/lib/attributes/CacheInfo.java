package alexiil.mc.lib.attributes;

public final class CacheInfo {

    public static final CacheInfo NOT_CACHABLE = new CacheInfo();

    private CacheInfo() {
        // Private because we're going to change this in the future.
    }
}
