package alexiil.mc.lib.attributes;

import java.util.EnumMap;
import java.util.function.Predicate;

import net.minecraft.util.math.Direction;

public class SearchOptionDirectional<T> extends SearchOption<T> {

    private static final EnumMap<Direction, SearchOptionDirectional<Object>> SIDES;

    /** The direction that this search is going in. */
    public final Direction direction;

    SearchOptionDirectional(Direction direction) {
        this.direction = direction;
    }

    public SearchOptionDirectional(Direction direction, Predicate<T> searchMatcher) {
        super(searchMatcher);
        this.direction = direction;
    }

    public static SearchOptionDirectional<Object> of(Direction dir) {
        return SIDES.get(dir);
    }

    static {
        SIDES = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            SIDES.put(dir, new SearchOptionDirectional<>(dir));
        }
    }
}
