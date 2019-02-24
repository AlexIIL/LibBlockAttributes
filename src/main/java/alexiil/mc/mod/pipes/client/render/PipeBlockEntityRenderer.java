package alexiil.mc.mod.pipes.client.render;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import alexiil.mc.mod.pipes.blocks.TilePipe;
import alexiil.mc.mod.pipes.blocks.TravellingItem;

public class PipeBlockEntityRenderer extends BlockEntityRenderer<TilePipe> {

    private static boolean inBatch = false;

    @Override
    public void render(TilePipe pipe, double x, double y, double z, float partialTicks, int int_1) {
        World world = pipe.getWorld();
        long now = world.getTime();
        int lightc = world.getLightLevel(pipe.getPos(), 0);

        List<TravellingItem> toRender = pipe.getAllItemsForRender();

        for (TravellingItem item : toRender) {
            Vec3d pos = item.getRenderPosition(BlockPos.ORIGIN, now, partialTicks, pipe);

            ItemStack stack = item.stack;
            if (stack != null && !stack.isEmpty()) {
                renderItemStack(x + pos.x, y + pos.y, z + pos.z, //
                    stack, lightc, item.getRenderDirection(now, partialTicks));
            }
            // if (item.colour != null) {
            // bb.setTranslation(x + pos.x, y + pos.y, z + pos.z);
            // int col = ColourUtil.getLightHex(item.colour);
            // int r = (col >> 16) & 0xFF;
            // int g = (col >> 8) & 0xFF;
            // int b = col & 0xFF;
            // for (MutableQuad q : COLOURED_QUADS) {
            // MutableQuad q2 = new MutableQuad(q);
            // q2.lighti(lightc);
            // q2.multColouri(r, g, b, 255);
            // q2.render(bb);
            // }
            // bb.setTranslation(0, 0, 0);
            // }
        }

        endItemBatch();
    }

    private static void renderItemStack(double x, double y, double z, ItemStack stack, int lightc,
        Direction renderDirection) {

        // if (!inBatch) {
        // inBatch = true;
        MinecraftClient.getInstance().getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y - 0.2, z);
        // GL11.glScaled(0.3, 0.3, 0.3);
        GuiLighting.disable();
        // }
        // OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightc % (float) 0x1_00_00,
        // lightc / (float) 0x1_00_00);
        BakedModel model = MinecraftClient.getInstance().getItemRenderer().getModel(stack, null, null);
        model.getTransformation().applyGl(ModelTransformation.Type.GROUND);
        MinecraftClient.getInstance().getItemRenderer().renderItemAndGlow(stack, model);

        GL11.glPopMatrix();

    }

    private static void endItemBatch() {
        if (inBatch) {
            inBatch = false;
            GL11.glPopMatrix();
        }
    }

}
