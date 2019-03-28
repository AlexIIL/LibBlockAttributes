package alexiil.mc.lib.attributes;

import java.util.function.Predicate;

public class SearchOption<T> {

    private final Predicate<T> searchMatcher;

    SearchOption() {
        this.searchMatcher = null;
    }

    public SearchOption(Predicate<T> searchMatcher) {
        this.searchMatcher = searchMatcher;
    }

    public final boolean matches(T obj) {
        return searchMatcher != null ? searchMatcher.test(obj) : true;
    }
}
