package alexiil.mc.lib.attributes;

/** Used in various places for testing [...] */
// TODO: grammar!
public enum Simulation {
    // TODO: Better names!
    // for all 3 here...
    SIMULATE,
    ACTION;

    public boolean isSimulate() {
        return this == SIMULATE;
    }

    public boolean isAction() {
        return this == ACTION;
    }
}
