package com.twistedfantasy.uniqueweapons.items;

import com.twistedfantasy.uniqueweapons.UniqueWeapons;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = UniqueWeapons.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EntityRendererRegistry {
    
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(UniqueWeapons.CUSTOM_TRIDENT.get(), ThunderSpearRender::new);
        event.registerEntityRenderer(UniqueWeapons.WRATH_ZOMBIE.get(), 
            context -> new ZombieRenderer(context));
        event.registerEntityRenderer(UniqueWeapons.WRATH_SKELETON.get(), 
            context -> new SkeletonRenderer(context));
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(UniqueWeapons.THUNDER_SPEAR.get(), 
                new ResourceLocation("throwing"), 
                (stack, level, entity, seed) -> {
                    return entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F;
                });
        });
    }
}