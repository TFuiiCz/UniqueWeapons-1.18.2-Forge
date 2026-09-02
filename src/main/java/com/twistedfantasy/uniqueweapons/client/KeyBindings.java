package com.twistedfantasy.uniqueweapons.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "uniqueweapons", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyBindings {
    
    public static KeyMapping SUMMON_KEY;
    public static KeyMapping SWAP_KEY;
    public static KeyMapping ABILITY_MENU_KEY;
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        SUMMON_KEY = new KeyMapping(
            "key.uniqueweapons.summon",
            GLFW.GLFW_KEY_R,
            "key.categories.uniqueweapons"
        );
        SWAP_KEY = new KeyMapping(
            "key.uniqueweapons.swap",
            GLFW.GLFW_KEY_T,
            "key.categories.uniqueweapons"
        );
        ABILITY_MENU_KEY = new KeyMapping(
            "key.uniqueweapons.ability_menu",
            GLFW.GLFW_KEY_T,
            "key.categories.uniqueweapons"
        );
        
        ClientRegistry.registerKeyBinding(SUMMON_KEY);
        ClientRegistry.registerKeyBinding(SWAP_KEY);
        ClientRegistry.registerKeyBinding(ABILITY_MENU_KEY);
    }
}