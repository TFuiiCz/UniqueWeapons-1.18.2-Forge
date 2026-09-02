package com.twistedfantasy.uniqueweapons.events;

import com.twistedfantasy.uniqueweapons.items.AxeOfWrath;
import com.twistedfantasy.uniqueweapons.items.ScytheSinner;
import com.twistedfantasy.uniqueweapons.items.MagmaBlade;
import com.twistedfantasy.uniqueweapons.UniqueWeapons;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = UniqueWeapons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TooltipHandler {
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        
        if (stack.getItem() instanceof ScytheSinner) {
            handleScytheSinner(event, stack);
        } else if (stack.getItem() instanceof MagmaBlade) {
            handleMagmaBlade(event, stack);
        } else if (stack.getItem() instanceof AxeOfWrath) {
            handleAxeOfWrath(event, stack);
        }
    }
    private static void handleScytheSinner(ItemTooltipEvent event, ItemStack stack) {
        event.getToolTip().clear();
        ScytheSinner scythe = (ScytheSinner) stack.getItem();
        net.minecraft.world.level.Level level = event.getEntity() != null 
            ? event.getEntity().level 
            : null;
        net.minecraft.world.item.TooltipFlag flag = event.getFlags();
        java.util.List<net.minecraft.network.chat.Component> customTooltip = 
            new java.util.ArrayList<>();
        scythe.appendHoverText(stack, level, customTooltip, flag);
        event.getToolTip().addAll(customTooltip);
    }
    private static void handleMagmaBlade(ItemTooltipEvent event, ItemStack stack) {
        event.getToolTip().clear();
        MagmaBlade blade = (MagmaBlade) stack.getItem();
        net.minecraft.world.level.Level level = event.getEntity() != null 
            ? event.getEntity().level 
            : null;
        net.minecraft.world.item.TooltipFlag flag = event.getFlags();
        java.util.List<net.minecraft.network.chat.Component> customTooltip = 
            new java.util.ArrayList<>();
        blade.appendHoverText(stack, level, customTooltip, flag);
        event.getToolTip().addAll(customTooltip);
    }
    private static void handleAxeOfWrath(ItemTooltipEvent event, ItemStack stack) {
        event.getToolTip().clear();
        AxeOfWrath axe = (AxeOfWrath) stack.getItem();
        net.minecraft.world.level.Level level = event.getEntity() != null 
            ? event.getEntity().level 
            : null;
        net.minecraft.world.item.TooltipFlag flag = event.getFlags();
        java.util.List<net.minecraft.network.chat.Component> customTooltip = 
            new java.util.ArrayList<>();
        axe.appendHoverText(stack, level, customTooltip, flag);
        event.getToolTip().addAll(customTooltip);
    }
}