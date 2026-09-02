package com.twistedfantasy.uniqueweapons.entity;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.twistedfantasy.uniqueweapons.UniqueWeapons;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UniqueWeapons.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {
    
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(UniqueWeapons.WRATH_ZOMBIE.get(),
            AttributeSupplier.builder()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0D)
                .add(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get(), 1.0D)
                .add(net.minecraftforge.common.ForgeMod.NAMETAG_DISTANCE.get(), 64.0D)
                .add(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get(), 0.08D)
                .add(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get(), 0.6D)
                .build());
        event.put(UniqueWeapons.WRATH_SKELETON.get(),
            AttributeSupplier.builder()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get(), 1.0D)
                .add(net.minecraftforge.common.ForgeMod.NAMETAG_DISTANCE.get(), 64.0D)
                .add(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get(), 0.08D)
                .add(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get(), 0.6D)
                .build());
    }
}