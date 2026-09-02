package com.twistedfantasy.uniqueweapons.client;

import com.twistedfantasy.uniqueweapons.network.PacketHandler;
import com.twistedfantasy.uniqueweapons.items.ArchmageBook;
import com.twistedfantasy.uniqueweapons.items.VoidAccelerator;
import com.twistedfantasy.uniqueweapons.items.MagicianDeceptionBook;
import com.twistedfantasy.uniqueweapons.items.FallingBook;
import com.twistedfantasy.uniqueweapons.client.screen.AbilitySelectionScreen;
import com.twistedfantasy.uniqueweapons.network.PacketHandler.SummonPacket;
import com.twistedfantasy.uniqueweapons.network.PacketHandler.SwapModePacket;
import com.twistedfantasy.uniqueweapons.network.PacketHandler.SwapArchmageBookModePacket;
import com.twistedfantasy.uniqueweapons.network.PacketHandler.SwapMagicianBookModePacket;
import com.twistedfantasy.uniqueweapons.network.PacketHandler.OpenAbilityMenuPacket;
import com.twistedfantasy.uniqueweapons.network.PacketHandler.SelectAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "uniqueweapons", value = Dist.CLIENT)
public class ClientEventHandler {
    private static boolean wasPressed = false;
    private static boolean wasSwapPressed = false;
    private static boolean wasMenuPressed = false;
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {
                if (KeyBindings.SUMMON_KEY != null) {
                    boolean isPressed = KeyBindings.SUMMON_KEY.isDown();
                    
                    if (isPressed && !wasPressed) {
                        ItemStack mainHand = mc.player.getMainHandItem();
                        ItemStack offHand = mc.player.getOffhandItem();
                        
                        boolean hasAxe = mainHand.getItem() instanceof com.twistedfantasy.uniqueweapons.items.AxeOfWrath ||
                                        offHand.getItem() instanceof com.twistedfantasy.uniqueweapons.items.AxeOfWrath;
                        
                        if (hasAxe) {
                            PacketHandler.INSTANCE.sendToServer(new SummonPacket());
                        }
                    }
                    wasPressed = isPressed;
                }
                
                if (KeyBindings.SWAP_KEY != null) {
                    boolean isSwapPressed = KeyBindings.SWAP_KEY.isDown();
                    
                    if (isSwapPressed && !wasSwapPressed) {
                        ItemStack mainHand = mc.player.getMainHandItem();
                        if (mainHand.getItem() instanceof VoidAccelerator) {
                            PacketHandler.INSTANCE.sendToServer(new SwapModePacket());
                        } else if (mainHand.getItem() instanceof ArchmageBook) {
                            PacketHandler.INSTANCE.sendToServer(new SwapArchmageBookModePacket());
                        } else if (mainHand.getItem() instanceof MagicianDeceptionBook) {
                            PacketHandler.INSTANCE.sendToServer(new SwapMagicianBookModePacket());
                        }
                    }
                    wasSwapPressed = isSwapPressed;
                }
                
                if (KeyBindings.ABILITY_MENU_KEY != null) {
                    boolean isMenuPressed = KeyBindings.ABILITY_MENU_KEY.isDown();
                    
                    if (isMenuPressed && !wasMenuPressed) {
                        ItemStack mainHand = mc.player.getMainHandItem();
                        ItemStack offHand = mc.player.getOffhandItem();
                        
                        boolean hasBook = mainHand.getItem() instanceof FallingBook ||
                                        offHand.getItem() instanceof FallingBook;
                        
                        if (hasBook) {
                            if (mainHand.getItem() instanceof FallingBook book) {
                                openAbilityMenu(book, mainHand);
                            } else if (offHand.getItem() instanceof FallingBook book) {
                                openAbilityMenu(book, offHand);
                            }
                        }
                    }
                    wasMenuPressed = isMenuPressed;
                }
            }
        }
    }
    
    private static void openAbilityMenu(FallingBook book, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new AbilitySelectionScreen(book, stack));
        }
    }
}