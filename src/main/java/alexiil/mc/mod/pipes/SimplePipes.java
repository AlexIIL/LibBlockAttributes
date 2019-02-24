package alexiil.mc.mod.pipes;

import net.fabricmc.api.ModInitializer;

import alexiil.mc.mod.pipes.blocks.SimplePipeBlocks;

public class SimplePipes implements ModInitializer {

    public static final String MODID = "simple_pipes";

    @Override
    public void onInitialize() {
        SimplePipeBlocks.load();
        // SimplePipeItems.load();
    }
}
