package com.twistedfantasy.uniqueweapons.events;

import com.twistedfantasy.uniqueweapons.UniqueWeapons;
import com.twistedfantasy.uniqueweapons.items.AxeOfWrath;
import com.twistedfantasy.uniqueweapons.items.ScytheSinner;
import com.twistedfantasy.uniqueweapons.items.FallingBook;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UniqueWeapons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {
    
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        Entity targetEntity = event.getEntity();
        if (sourceEntity instanceof Player player && targetEntity != null) {
            ItemStack mainHandItem = player.getMainHandItem();
            if (mainHandItem.getItem() instanceof ScytheSinner scytheSinner) {
                if (isValidTarget(targetEntity)) {
                    scytheSinner.incrementKillCount(mainHandItem);
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            CompoundTag persistentData = player.getPersistentData();
            if (persistentData.getBoolean("NoFallDamage")) {
                event.setCanceled(true);
                event.setDamageMultiplier(0.0F);
                int noFallTicks = persistentData.getInt("NoFallTicks");
                if (noFallTicks > 0) {
                    persistentData.putInt("NoFallTicks", noFallTicks - 1);
                } else {
                    persistentData.remove("NoFallDamage");
                    persistentData.remove("NoFallTicks");
                    persistentData.remove("TeleportStartY");
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Player player = event.player;
            
            ItemStack mainHandItem = player.getMainHandItem();
            
            if (mainHandItem.getItem() instanceof AxeOfWrath) {
                player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    40,
                    2,
                    true,
                    true,
                    false
                ));
            }
            
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof FallingBook book) {
                    // Вызов обработки барьера для визуальных эффектов
                    book.onPlayerTick(player.level, player);
                    
                    // Вызов обработки других эффектов
                    book.handleBarrier(player.level, player);
                    book.handleAvatar(player.level, player);
                    book.handleCurse(player.level, player);
                    break;
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof FallingBook book) {
                    book.handleDamageReturn(player.level, player, event.getAmount());
                    break;
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            Player player = livingEntity.level.getNearestPlayer(livingEntity, 100);
            if (player != null) {
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.getItem() instanceof FallingBook book) {
                        book.handleCurse(player.level, livingEntity);
                        break;
                    }
                }
            }
        }
    }
    
    private static boolean isValidTarget(Entity entity) {
        if (entity instanceof Villager) {
            return true;
        }
        if (entity instanceof Witch) {
            return true;
        }
        return false;
    }
}