package com.twistedfantasy.uniqueweapons.client.renderer;

import com.twistedfantasy.uniqueweapons.UniqueWeapons;
import com.twistedfantasy.uniqueweapons.entity.WrathSummonedSkeleton;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class WrathSkeletonRenderer extends HumanoidMobRenderer<WrathSummonedSkeleton, SkeletonModel<WrathSummonedSkeleton>> {
    private static final ResourceLocation TEXTURE = 
        new ResourceLocation(UniqueWeapons.MODID, "textures/entity/wrath_skeleton.png");
    
    public WrathSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this, 
            new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),
            new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR))));
    }
    
    @Override
    public ResourceLocation getTextureLocation(WrathSummonedSkeleton entity) {
        return TEXTURE;
    }
}