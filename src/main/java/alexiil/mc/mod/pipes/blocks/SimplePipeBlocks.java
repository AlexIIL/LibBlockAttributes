package alexiil.mc.mod.pipes.blocks;

import com.mojang.datafixers.types.Type;

import net.fabricmc.fabric.api.block.FabricBlockSettings;

import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

import alexiil.mc.mod.pipes.SimplePipes;

public class SimplePipeBlocks {

    public static final BlockPipeWooden WOODEN_PIPE;
    public static final BlockPipeStone STONE_PIPE;
    public static final BlockPipeIron IRON_PIPE;

    public static final BlockEntityType<TilePipeWood> WOODEN_PIPE_TILE;
    public static final BlockEntityType<TilePipeStone> STONE_PIPE_TILE;
    public static final BlockEntityType<TilePipeIron> IRON_PIPE_TILE;

    static {
        Block.Settings settings = FabricBlockSettings.of(Material.PART)//
            .build();

        WOODEN_PIPE = new BlockPipeWooden(settings);
        STONE_PIPE = new BlockPipeStone(settings);
        IRON_PIPE = new BlockPipeIron(settings);

        WOODEN_PIPE_TILE = create("simple_pipes:pipe_wooden", BlockEntityType.Builder.create(TilePipeWood::new));
        STONE_PIPE_TILE = create("simple_pipes:pipe_stone", BlockEntityType.Builder.create(TilePipeStone::new));
        IRON_PIPE_TILE = create("simple_pipes:pipe_iron", BlockEntityType.Builder.create(TilePipeIron::new));
    }

    private static <T extends BlockEntity> BlockEntityType<T> create(String name, BlockEntityType.Builder<T> builder) {
        Type<?> choiceType = null;
        // Schemas.getFixer().getSchema(DataFixUtils.makeKey(SharedConstants.getGameVersion().getWorldVersion()))
        // .getChoiceType(TypeReferences.BLOCK_ENTITY, name);
        return builder.build(choiceType);
    }

    public static void load() {
        registerBlock(WOODEN_PIPE, "pipe_wooden");
        registerBlock(STONE_PIPE, "pipe_stone");
        registerBlock(IRON_PIPE, "pipe_iron");

        registerTile(WOODEN_PIPE_TILE, "pipe_wooden");
        registerTile(STONE_PIPE_TILE, "pipe_stone");
        registerTile(IRON_PIPE_TILE, "pipe_iron");
    }

    private static void registerBlock(Block block, String name) {
        Registry.register(Registry.BLOCK, SimplePipes.MODID + ":" + name, block);
    }

    private static void registerTile(BlockEntityType<?> type, String name) {
        Registry.register(Registry.BLOCK_ENTITY, SimplePipes.MODID + ":" + name, type);
    }
}
