package alexiil.mc.lib.attributes;

import net.minecraft.util.math.Direction;

public class SearchParamDirectional extends SearchParameter {

    private static final SearchParamDirectional[] SIDES;

    /** The direction that this search is going in. */
    public final Direction direction;

    SearchParamDirectional(Direction direction) {
        this.direction = direction;
    }

    public static SearchParamDirectional of(Direction dir) {
        return SIDES[dir.ordinal()];
    }

    static {
        SIDES = new SearchParamDirectional[6];
        for (Direction dir : Direction.values()) {
            SIDES[dir.ordinal()] = new SearchParamDirectional(dir);
        }
    }
}
