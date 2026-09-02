package com.twistedfantasy.uniqueweapons.events;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.twistedfantasy.uniqueweapons.UniqueWeapons;
import com.twistedfantasy.uniqueweapons.items.MagicianDeceptionBook;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = UniqueWeapons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickHandler {
    
    private static final Map<String, DelayedTask> scheduledTasks = new ConcurrentHashMap<>();
    private static int taskCounter = 0;
    
    public static String scheduleTask(Runnable task, int delayTicks) {
        String taskId = "task_" + System.currentTimeMillis() + "_" + (taskCounter++);
        scheduledTasks.put(taskId, new DelayedTask(task, delayTicks));
        return taskId;
    }
    
    public static String schedulePlayerTask(UUID playerId, Runnable task, int delayTicks) {
        String taskId = "player_" + playerId.toString() + "_" + System.currentTimeMillis();
        scheduledTasks.put(taskId, new DelayedTask(task, delayTicks));
        return taskId;
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            List<String> toRemove = new ArrayList<>();
            
            for (Map.Entry<String, DelayedTask> entry : scheduledTasks.entrySet()) {
                DelayedTask task = entry.getValue();
                task.remainingTicks--;
                
                if (task.remainingTicks <= 0) {
                    try {
                        task.runnable.run();
                    } catch (Exception e) {
                        System.err.println("Error executing scheduled task: " + e.getMessage());
                    }
                    toRemove.add(entry.getKey());
                }
            }
            
            for (String taskId : toRemove) {
                scheduledTasks.remove(taskId);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level.isClientSide) {
            if (event.player.level.getGameTime() % 20 == 0) {
                MagicianDeceptionBook.applyKingAura(event.player, event.player.level);
            }
        }
    }
    
    public static void cancelTask(String taskId) {
        scheduledTasks.remove(taskId);
    }
    
    private static class DelayedTask {
        final Runnable runnable;
        int remainingTicks;
        
        DelayedTask(Runnable runnable, int delayTicks) {
            this.runnable = runnable;
            this.remainingTicks = delayTicks;
        }
    }
}