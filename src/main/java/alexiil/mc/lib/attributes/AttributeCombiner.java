package alexiil.mc.lib.attributes;

import java.util.List;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface AttributeCombiner<T> {
    @Nonnull
    T combine(List<? extends T> attributes);
}
