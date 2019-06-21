package alexiil.mc.lib.attributes;

import net.fabricmc.api.ModInitializer;

import alexiil.mc.lib.attributes.fluid.volume.WaterVolumeTester;
import alexiil.mc.lib.attributes.item.impl.DirectFixedItemInvTester;

public class Tester implements ModInitializer {
    @Override
    public void onInitialize() {
        WaterVolumeTester.runTests();
        DirectFixedItemInvTester.runTests();

        // TODO: Write tests for SimpleLimitedFixedItemInv and SimpleGroupedItemInv!
    }
}
