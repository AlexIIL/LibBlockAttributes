package alexiil.mc.lib.attributes;

import net.fabricmc.api.ModInitializer;

import alexiil.mc.lib.attributes.fluid.volume.WaterVolumeTester;

public class Tester implements ModInitializer {
    @Override
    public void onInitialize() {
        WaterVolumeTester.runTests();
    }
}
