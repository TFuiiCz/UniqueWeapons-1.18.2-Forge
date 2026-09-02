package com.twistedfantasy.uniqueweapons.client.renderer;

import com.twistedfantasy.uniqueweapons.UniqueWeapons;
import com.twistedfantasy.uniqueweapons.entity.WrathSummonedZombie;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WrathZombieRenderer extends HumanoidMobRenderer<WrathSummonedZombie, ZombieModel<WrathSummonedZombie>> {
    private static final ResourceLocation TEXTURE = 
        new ResourceLocation(UniqueWeapons.MODID, "textures/entity/wrath_zombie.png");
    
    public WrathZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }
    
    @Override
    public ResourceLocation getTextureLocation(WrathSummonedZombie entity) {
        return TEXTURE;
    }
}