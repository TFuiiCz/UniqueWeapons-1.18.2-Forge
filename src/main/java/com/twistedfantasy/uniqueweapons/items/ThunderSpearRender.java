package com.twistedfantasy.uniqueweapons.items;

import com.twistedfantasy.uniqueweapons.UniqueWeapons;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;

public class ThunderSpearRender extends ThrownTridentRenderer {
    private static final ResourceLocation TEXTURE = 
        new ResourceLocation(UniqueWeapons.MODID, "textures/item/crimson_lance.png");
    private final TridentModel model;

    public ThunderSpearRender(EntityRendererProvider.Context context) {
        super(context);
        this.model = new TridentModel(context.bakeLayer(ModelLayers.TRIDENT));
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownTrident entity) {
        return TEXTURE;
    }
    @Override
    public void render(ThrownTrident entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()) + 90.0F));
        float scale = 1.5F;
        poseStack.scale(scale, scale, scale);
        float shake = (float)entity.shakeTime - partialTicks;
        if (shake > 0.0F) {
            float shakeOffset = -Mth.sin(shake * 3.0F) * shake;
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(shakeOffset));
        }
        VertexConsumer vertexconsumer = bufferSource.getBuffer(this.model.renderType(this.getTextureLocation(entity)));
        this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        
        poseStack.popPose();
    }
}