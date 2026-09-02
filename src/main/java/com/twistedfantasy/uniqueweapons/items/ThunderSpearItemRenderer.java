package com.twistedfantasy.uniqueweapons.items;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.twistedfantasy.uniqueweapons.UniqueWeapons;

public class ThunderSpearItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(UniqueWeapons.MODID, "textures/item/crimson_lance.png");
    private final TridentModel model;
    public ThunderSpearItemRenderer() {
        super(null, null);
        this.model = new TridentModel(net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.TRIDENT));
    }
    @Override
    public void renderByItem(ItemStack stack, ItemTransforms.TransformType transformType, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.1F, 0.5F);
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
        float scale = 1.5F;
        poseStack.scale(scale, scale, scale);
        switch (transformType) {
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND:
                poseStack.mulPose(Vector3f.XP.rotationDegrees(15.0F));
                break;
                
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
                poseStack.mulPose(Vector3f.XP.rotationDegrees(10.0F));
                poseStack.translate(0.0F, -0.1F, 0.0F);
                break;
                
            case GROUND:
                poseStack.mulPose(Vector3f.XP.rotationDegrees(5.0F));
                break;
                
            case GUI:
                poseStack.mulPose(Vector3f.XP.rotationDegrees(25.0F));
                break;
                
            default:
                poseStack.mulPose(Vector3f.XP.rotationDegrees(10.0F));
                break;
        }
        
        VertexConsumer vertexconsumer = ItemRenderer.getFoilBufferDirect(bufferSource, this.model.renderType(TEXTURE), false, stack.hasFoil());
        this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        
        poseStack.popPose();
    }
}
