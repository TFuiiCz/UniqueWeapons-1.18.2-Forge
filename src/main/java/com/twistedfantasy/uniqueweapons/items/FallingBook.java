package com.twistedfantasy.uniqueweapons.items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.UUID;

public class FallingBook extends Item {
    
     
    private static final String TAG_SELECTED_ABILITY = "SelectedAbility";
    private static final String TAG_BARRIER_ACTIVE = "BarrierActive";
    private static final String TAG_BARRIER_END_TIME = "BarrierEndTime";
    private static final String TAG_AVATAR_ACTIVE = "AvatarActive";
    private static final String TAG_AVATAR_END_TIME = "AvatarEndTime";
    
    private static final String TAG_LAST_BARRIER_TIME = "LastBarrierTime";
    private static final String TAG_LAST_RETURN_TIME = "LastReturnTime";
    private static final String TAG_LAST_AVATAR_TIME = "LastAvatarTime";
    private static final String TAG_LAST_DISPEL_TIME = "LastDispelTime";
    private static final String TAG_LAST_CURSE_TIME = "LastCurseTime";
    
     
    private static final int BARRIER_COOLDOWN_SECONDS = 60;
    private static final int BARRIER_DURATION = 600;  
    private static final int RETURN_COOLDOWN_SECONDS = 40;
    private static final int AVATAR_COOLDOWN_SECONDS = 180;
    private static final int AVATAR_DURATION = 300;  
    private static final int DISPEL_COOLDOWN_SECONDS = 45;
    private static final int CURSE_COOLDOWN_SECONDS = 90;
    private static final int CURSE_DURATION = 200;  
    
     
    private static final int BARRIER_RADIUS = 5;  
    private static final int RETURN_RADIUS = 50;
    private static final int DISPEL_RADIUS = 30;
    
     
    private static final UUID AVATAR_HEALTH_MODIFIER_ID = UUID.fromString("1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d");
    private static final UUID AVATAR_STRENGTH_MODIFIER_ID = UUID.fromString("2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e");
    
     
    public FallingBook(Properties properties) {
        super(properties
            .tab(net.minecraft.world.item.CreativeModeTab.TAB_COMBAT)
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .durability(1000)
            .setNoRepair());
    }
    
     
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!world.isClientSide) {
            int selectedAbility = getSelectedAbility(stack);
            
             
            if (selectedAbility == 0) {
                openAbilityMenu(player, stack);
                return InteractionResultHolder.success(stack);
            }
            
             
            if (isAbilityOnCooldown(world, stack, selectedAbility)) {
                int seconds = getRemainingCooldownSeconds(world, stack, selectedAbility);
                String abilityName = getAbilityName(selectedAbility);
                player.displayClientMessage(new TextComponent("§c" + abilityName + " на перезарядке! Осталось: " + seconds + " сек"), true);
                return InteractionResultHolder.success(stack);
            }
            
             
            boolean success = useSelectedAbility(world, player, stack, hand);
            
            if (success) {
                setAbilityLastUseTime(world, stack, selectedAbility);
                
                 
                if (selectedAbility != 6) {
                    if (stack.hurt(1, world.random, null)) {
                        stack.shrink(1);
                        player.broadcastBreakEvent(hand);
                    }
                }
            }
            
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.success(stack);
    }
    
     
    public void openAbilityMenu(Player player, ItemStack stack) {
        player.displayClientMessage(new TextComponent("§6Откройте меню способностей (клавиша M)"), true);
        player.displayClientMessage(new TextComponent("§7Используйте ПКМ по воздуху для выбора способности"), true);
        
        player.level.playSound(player, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
    
     
    public void selectAbility(ItemStack stack, Player player, int abilityId) {
        int currentAbility = getSelectedAbility(stack);
        
        if (abilityId >= 1 && abilityId <= 6) {
            setSelectedAbility(stack, abilityId);
            displayAbilitySelectedMessage(player, abilityId);
            
            if (player.level.isClientSide) {
                spawnAbilitySelectParticles(player.level, player, abilityId);
                player.level.playSound(player, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.5F);
            }
        } else if (abilityId == 0) {
            openAbilityMenu(player, stack);
        }
    }
    
     
    private boolean useSelectedAbility(Level world, Player player, ItemStack stack, InteractionHand hand) {
        int ability = getSelectedAbility(stack);
        boolean success = false;
        
        switch (ability) {
            case 1:
                success = useBarrier(world, player);
                break;
            case 2:
                success = useDamageReturn(world, player);
                break;
            case 3:
                success = useAvatarOfLords(world, player);
                break;
            case 4:
                success = useDispelTechnique(world, player);
                break;
            case 5:
                success = useHeavenCurse(world, player);
                break;
            case 6:
                setSelectedAbility(stack, 0);
                player.displayClientMessage(new TextComponent("§5Выбор способности сброшен"), true);
                success = true;
                break;
        }
        
        if (success && ability != 6) {
            playAbilitySound(world, player, ability);
            spawnAbilityParticles(world, player, ability);
        }
        
        return success;
    }
    
     
    private boolean useBarrier(Level world, Player player) {
        if (isBarrierActive(player)) {
            player.displayClientMessage(new TextComponent("§cБарьер уже активен!"), true);
            return false;
        }
        
         
        setBarrierActive(player, true);
        setBarrierEndTime(player, world.getGameTime() + BARRIER_DURATION);
        
         
        player.displayClientMessage(new TextComponent("§5§lЗАЩИТНЫЙ БАРЬЕР АКТИВИРОВАН НА 30 СЕКУНД"), true);
        player.displayClientMessage(new TextComponent("§6● Создаёт видимое энергетическое поле вокруг вас"), true);
        player.displayClientMessage(new TextComponent("§6● Отталкивает врагов и отражает снаряды"), true);
        
         
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 0.9F);
        
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        
         
        if (!world.isClientSide) {
            spawnBarrierActivationParticles(world, player);
        }
        
        return true;
    }
    
     
    private void spawnBarrierActivationParticles(Level world, Player player) {
        for (int i = 0; i < 120; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = BARRIER_RADIUS - 0.3;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 0.3 + world.random.nextDouble() * 2.4;
            world.addParticle(ParticleTypes.ENCHANT,
                x, y, z,
                (world.random.nextDouble() - 0.5) * 0.08,
                0.04,
                (world.random.nextDouble() - 0.5) * 0.08);
        }
        
        for (int i = 0; i < 80; i++) {
            world.addParticle(ParticleTypes.END_ROD,
                player.getX() + (world.random.nextDouble() - 0.5) * (BARRIER_RADIUS - 1),
                player.getY() + 0.5 + world.random.nextDouble() * 2,
                player.getZ() + (world.random.nextDouble() - 0.5) * (BARRIER_RADIUS - 1),
                (world.random.nextDouble() - 0.5) * 0.12,
                0.06,
                (world.random.nextDouble() - 0.5) * 0.12);
        }
        
        for (int i = 0; i < 60; i++) {
            world.addParticle(ParticleTypes.CRIT,
                player.getX() + (world.random.nextDouble() - 0.5) * 1.5,
                player.getY() + world.random.nextDouble() * 2.5,
                player.getZ() + (world.random.nextDouble() - 0.5) * 1.5,
                (world.random.nextDouble() - 0.5) * 0.15,
                0.08,
                (world.random.nextDouble() - 0.5) * 0.15);
        }
        
        for (int i = 0; i < 40; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = BARRIER_RADIUS * 0.7;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 1.5;
            
            world.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                x, y, z,
                0, 0.1, 0);
        }
    }
    
     
    public void updateBarrierVisuals(Level world, Player player) {
        if (!world.isClientSide) return;
        
        if (!isBarrierActive(player)) return;
        
        long gameTime = world.getGameTime();
        
         
        for (int i = 0; i < 8; i++) {
            double angle = (gameTime * 0.05) + (i * Math.PI / 4);
            double radius = BARRIER_RADIUS - 0.2;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 1.0;
            
            world.addParticle(ParticleTypes.ENCHANT,
                x, y, z,
                0, 0, 0);
            
             
            double innerAngle = angle + Math.PI/8;
            double innerRadius = BARRIER_RADIUS - 0.8;
            double innerX = player.getX() + Math.cos(innerAngle) * innerRadius;
            double innerZ = player.getZ() + Math.sin(innerAngle) * innerRadius;
            
            world.addParticle(ParticleTypes.END_ROD,
                innerX, y + 0.5, innerZ,
                0, 0.02, 0);
        }
        
         
        if (world.random.nextFloat() < 0.4) {
            for (int i = 0; i < 2; i++) {
                world.addParticle(ParticleTypes.CRIT,
                    player.getX() + (world.random.nextDouble() - 0.5) * (BARRIER_RADIUS - 1.5),
                    player.getY() + 0.5 + world.random.nextDouble() * 2,
                    player.getZ() + (world.random.nextDouble() - 0.5) * (BARRIER_RADIUS - 1.5),
                    (world.random.nextDouble() - 0.5) * 0.05,
                    0.03,
                    (world.random.nextDouble() - 0.5) * 0.05);
            }
        }
        
         
        if (world.random.nextFloat() < 0.3) {
            for (int i = 0; i < 3; i++) {
                double groundAngle = world.random.nextDouble() * Math.PI * 2;
                double groundX = player.getX() + Math.cos(groundAngle) * BARRIER_RADIUS;
                double groundZ = player.getZ() + Math.sin(groundAngle) * BARRIER_RADIUS;
                
                world.addParticle(ParticleTypes.ENCHANT,
                    groundX, player.getY() + 0.1, groundZ,
                    0, 0.08 + world.random.nextDouble() * 0.04,
                    0);
            }
        }
        
         
        if (gameTime % 10 == 0) {
            for (int i = 0; i < 3; i++) {
                double verticalAngle = world.random.nextDouble() * Math.PI * 2;
                double verticalRadius = world.random.nextDouble() * (BARRIER_RADIUS - 1);
                double verticalX = player.getX() + Math.cos(verticalAngle) * verticalRadius;
                double verticalZ = player.getZ() + Math.sin(verticalAngle) * verticalRadius;
                double startY = player.getY() + 0.1;
                
                 
                for (int j = 0; j < 3; j++) {
                    double yPos = startY + (j * 0.8);
                    world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        verticalX, yPos, verticalZ,
                        0, 0.02, 0);
                }
            }
        }
        
         
        if (world.random.nextFloat() < 0.2) {
            double sphereAngle1 = world.random.nextDouble() * Math.PI * 2;
            double sphereAngle2 = world.random.nextDouble() * Math.PI;
            double sphereRadius = BARRIER_RADIUS * 0.5;
            
            double sphereX = player.getX() + Math.sin(sphereAngle2) * Math.cos(sphereAngle1) * sphereRadius;
            double sphereY = player.getY() + 1.5 + Math.cos(sphereAngle2) * sphereRadius;
            double sphereZ = player.getZ() + Math.sin(sphereAngle2) * Math.sin(sphereAngle1) * sphereRadius;
            
            world.addParticle(ParticleTypes.GLOW,
                sphereX, sphereY, sphereZ,
                0, 0, 0);
        }
    }
    
     
    private boolean useDamageReturn(Level world, Player player) {
        player.displayClientMessage(new TextComponent("§5Извращённая техника активирована"), true);
        player.displayClientMessage(new TextComponent("§7Следующий полученный урон будет возвращён врагам"), true);
        
         
        CompoundTag tag = player.getPersistentData();
        tag.putBoolean("DamageReturnActive", true);
        tag.putLong("DamageReturnTime", world.getGameTime());
        
        return true;
    }
    
     
    private boolean useAvatarOfLords(Level world, Player player) {
        if (isAvatarActive(player)) {
            player.displayClientMessage(new TextComponent("§cВоплощение повелителей уже активно!"), true);
            return false;
        }
        
         
        setAvatarActive(player, true);
        setAvatarEndTime(player, world.getGameTime() + AVATAR_DURATION);
        
         
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double baseHealth = maxHealthAttr.getBaseValue();
            AttributeModifier healthModifier = new AttributeModifier(
                AVATAR_HEALTH_MODIFIER_ID,
                "Avatar Health Boost",
                baseHealth * 2,  
                AttributeModifier.Operation.ADDITION
            );
            
            maxHealthAttr.removeModifier(AVATAR_HEALTH_MODIFIER_ID);
            maxHealthAttr.addTransientModifier(healthModifier);
            
            player.setHealth(player.getMaxHealth());
        }
        
         
        AttributeInstance attackDamageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttr != null) {
            AttributeModifier strengthModifier = new AttributeModifier(
                AVATAR_STRENGTH_MODIFIER_ID,
                "Avatar Strength Boost",
                8.0,  
                AttributeModifier.Operation.ADDITION
            );
            
            attackDamageAttr.removeModifier(AVATAR_STRENGTH_MODIFIER_ID);
            attackDamageAttr.addTransientModifier(strengthModifier);
        }
        
         
        player.addEffect(new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            AVATAR_DURATION,
            1,  
            false,
            true,
            true
        ));
        
        player.addEffect(new MobEffectInstance(
            MobEffects.REGENERATION,
            AVATAR_DURATION,
            2,  
            false,
            true,
            true
        ));
        
        player.addEffect(new MobEffectInstance(
            MobEffects.DAMAGE_BOOST,
            AVATAR_DURATION,
            1,  
            false,
            true,
            true
        ));
        
        player.addEffect(new MobEffectInstance(
            MobEffects.ABSORPTION,
            AVATAR_DURATION,
            4,  
            false,
            true,
            true
        ));
        
        player.addEffect(new MobEffectInstance(
            MobEffects.FIRE_RESISTANCE,
            AVATAR_DURATION,
            0,
            false,
            true,
            true
        ));
        
        player.addEffect(new MobEffectInstance(
            MobEffects.MOVEMENT_SPEED,
            AVATAR_DURATION,
            1,  
            false,
            true,
            true
        ));
        
        player.addEffect(new MobEffectInstance(
            MobEffects.JUMP,
            AVATAR_DURATION,
            1,  
            false,
            true,
            true
        ));
        
         
        if (!world.isClientSide) {
            for (int i = 0; i < 150; i++) {
                world.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX() + (world.random.nextDouble() - 0.5) * 4,
                    player.getY() + world.random.nextDouble() * 4,
                    player.getZ() + (world.random.nextDouble() - 0.5) * 4,
                    (world.random.nextDouble() - 0.5) * 0.2,
                    0.2,
                    (world.random.nextDouble() - 0.5) * 0.2);
            }
            
            for (int i = 0; i < 30; i++) {
                world.addParticle(ParticleTypes.FLAME,
                    player.getX() + (world.random.nextDouble() - 0.5) * 3,
                    player.getY() + world.random.nextDouble() * 3,
                    player.getZ() + (world.random.nextDouble() - 0.5) * 3,
                    0, 0.1, 0);
            }
        }
        
         
        player.displayClientMessage(new TextComponent("§5§lВОПЛОЩЕНИЕ ПОВЕЛИТЕЛЕЙ АКТИВИРОВАНО"), true);
        player.displayClientMessage(new TextComponent("§6● Здоровье ×3"), true);
        player.displayClientMessage(new TextComponent("§6● +20 сердец поглощения"), true);
        player.displayClientMessage(new TextComponent("§6● Сопротивление II + Регенерация III"), true);
        player.displayClientMessage(new TextComponent("§6● Сила II + Огнестойкость"), true);
        player.displayClientMessage(new TextComponent("§6● Скорость II + Прыжок II"), true);
        player.displayClientMessage(new TextComponent("§6Длительность: 15 секунд"), true);
        
        return true;
    }
    
     
    private boolean useDispelTechnique(Level world, Player player) {
        AABB area = new AABB(
            player.getX() - DISPEL_RADIUS, player.getY() - DISPEL_RADIUS, player.getZ() - DISPEL_RADIUS,
            player.getX() + DISPEL_RADIUS, player.getY() + DISPEL_RADIUS, player.getZ() + DISPEL_RADIUS
        );
        
        List<Entity> entities = world.getEntities(player, area);
        int affectedCount = 0;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity && entity != player) {
                 
                livingEntity.removeAllEffects();
                
                 
                float currentHealth = livingEntity.getHealth();
                float damage = currentHealth / 2.0f;
                
                if (damage > 0) {
                    livingEntity.hurt(DamageSource.MAGIC, damage);
                    affectedCount++;
                }
                
                 
                if (!world.isClientSide) {
                    for (int i = 0; i < 10; i++) {
                        world.addParticle(ParticleTypes.ENCHANT,
                            livingEntity.getX() + (world.random.nextDouble() - 0.5) * 1.5,
                            livingEntity.getY() + world.random.nextDouble() * 2,
                            livingEntity.getZ() + (world.random.nextDouble() - 0.5) * 1.5,
                            (world.random.nextDouble() - 0.5) * 0.2,
                            0.1,
                            (world.random.nextDouble() - 0.5) * 0.2);
                    }
                }
                
                world.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), 
                    SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 0.5F, 1.5F);
            }
        }
        
        player.displayClientMessage(new TextComponent(
            "§5Развеивание сняло эффекты с " + affectedCount + " существ"), true);
        
         
        if (!world.isClientSide) {
            for (int i = 0; i < 50; i++) {
                double angle = world.random.nextDouble() * Math.PI * 2;
                double radius = world.random.nextDouble() * 5;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                double y = player.getY() + world.random.nextDouble() * 3;
                
                world.addParticle(ParticleTypes.ENCHANT,
                    x, y, z,
                    (world.random.nextDouble() - 0.5) * 0.1,
                    0.05,
                    (world.random.nextDouble() - 0.5) * 0.1);
            }
        }
        
        return true;
    }
    
     
    private boolean useHeavenCurse(Level world, Player player) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.getEyePosition(1.0F);
        Vec3 endPos = startPos.add(lookVec.scale(50));
        
        ClipContext context = new ClipContext(startPos, endPos, 
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult blockHit = world.clip(context);
        
        Entity target = null;
        double closestDistance = Double.MAX_VALUE;
        
         
        List<Entity> entities = world.getEntities(player, 
            new AABB(startPos.x - 5, startPos.y - 5, startPos.z - 5,
                    startPos.x + 5, startPos.y + 5, startPos.z + 5));
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity && entity != player) {
                 
                Vec3 toEntity = livingEntity.position().subtract(startPos).normalize();
                double dot = lookVec.dot(toEntity);
                
                if (dot > 0.9) {  
                    double distance = startPos.distanceTo(livingEntity.position());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        target = livingEntity;
                    }
                }
            }
        }
        
        if (target instanceof LivingEntity livingTarget) {
             
            CompoundTag targetData = livingTarget.getPersistentData();
            targetData.putBoolean("HeavenCurseActive", true);
            targetData.putLong("HeavenCurseEndTime", world.getGameTime() + CURSE_DURATION * 20);
            targetData.putUUID("CursedBy", player.getUUID());
            
             
            livingTarget.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                CURSE_DURATION,
                127,  
                false,
                true,
                true
            ));
            
            livingTarget.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                CURSE_DURATION,
                127,  
                false,
                true,
                true
            ));
            
            livingTarget.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                CURSE_DURATION,
                0,
                false,
                true,
                true
            ));
            
            livingTarget.addEffect(new MobEffectInstance(
                MobEffects.DIG_SLOWDOWN,
                CURSE_DURATION,
                127,  
                false,
                true,
                true
            ));
            
             
            if (!world.isClientSide) {
                for (int i = 0; i < 100; i++) {
                    world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        livingTarget.getX() + (world.random.nextDouble() - 0.5) * 2,
                        livingTarget.getY() + world.random.nextDouble() * 3,
                        livingTarget.getZ() + (world.random.nextDouble() - 0.5) * 2,
                        (world.random.nextDouble() - 0.5) * 0.1,
                        0.1,
                        (world.random.nextDouble() - 0.5) * 0.1);
                }
            }
            
            world.playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(), 
                SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.0F, 0.3F);
            
            player.displayClientMessage(new TextComponent(
                "§5Проклятие Небес наложено на " + livingTarget.getName().getString()), true);
            
            return true;
        } else {
            player.displayClientMessage(new TextComponent("§cЦель для проклятия не найдена"), true);
            return false;
        }
    }
    
     
    public void handleDamageReturn(Level world, Player player, float damageAmount) {
        CompoundTag tag = player.getPersistentData();
        
        if (tag.getBoolean("DamageReturnActive")) {
            tag.putBoolean("DamageReturnActive", false);
            
            AABB area = new AABB(
                player.getX() - RETURN_RADIUS, player.getY() - RETURN_RADIUS, player.getZ() - RETURN_RADIUS,
                player.getX() + RETURN_RADIUS, player.getY() + RETURN_RADIUS, player.getZ() + RETURN_RADIUS
            );
            
            List<Entity> entities = world.getEntities(player, area);
            int affectedCount = 0;
            
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity livingEntity && entity != player) {
                     
                    livingEntity.hurt(DamageSource.MAGIC, damageAmount * 1.5f);
                    affectedCount++;
                    
                    if (!world.isClientSide) {
                        for (int i = 0; i < 5; i++) {
                            world.addParticle(ParticleTypes.DAMAGE_INDICATOR,
                                livingEntity.getX() + (world.random.nextDouble() - 0.5) * 1,
                                livingEntity.getY() + world.random.nextDouble() * 2,
                                livingEntity.getZ() + (world.random.nextDouble() - 0.5) * 1,
                                0, 0.1, 0);
                        }
                    }
                }
            }
            
            player.displayClientMessage(new TextComponent(
                "§5Извращённая техника вернула " + damageAmount + " урона " + affectedCount + " врагам"), true);
            
            world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5F, 1.5F);
        }
    }
    
     
    public void handleBarrier(Level world, Player player) {
        CompoundTag tag = player.getPersistentData();
        
        if (tag.getBoolean("BarrierActive")) {
            long currentTime = world.getGameTime();
            long barrierEndTime = tag.getLong("BarrierEndTime");
            
             
            if (currentTime > barrierEndTime) {
                endBarrier(world, player);
                return;
            }
            
            AABB area = new AABB(
                player.getX() - BARRIER_RADIUS, player.getY() - BARRIER_RADIUS, player.getZ() - BARRIER_RADIUS,
                player.getX() + BARRIER_RADIUS, player.getY() + BARRIER_RADIUS, player.getZ() + BARRIER_RADIUS
            );
            
            List<Entity> entities = world.getEntities(player, area);
            
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity livingEntity && entity != player) {
                     
                    Vec3 pushDir = entity.position().subtract(player.position());
                    if (pushDir.length() > 0) {
                        pushDir = pushDir.normalize().scale(0.5);  
                        entity.setDeltaMovement(pushDir.x, pushDir.y + 0.2, pushDir.z);
                        entity.hurtMarked = true;
                        
                         
                        if (!world.isClientSide) {
                            for (int i = 0; i < 5; i++) {
                                world.addParticle(ParticleTypes.CLOUD,
                                    livingEntity.getX() + (world.random.nextDouble() - 0.5),
                                    livingEntity.getY() + world.random.nextDouble(),
                                    livingEntity.getZ() + (world.random.nextDouble() - 0.5),
                                    (world.random.nextDouble() - 0.5) * 0.15,
                                    0.08,
                                    (world.random.nextDouble() - 0.5) * 0.15);
                            }
                        }
                    }
                }
                
                 
                if (entity instanceof Projectile projectile) {
                    Vec3 motion = projectile.getDeltaMovement();
                     
                    projectile.setDeltaMovement(motion.reverse().scale(2.0));  
                    
                     
                    if (!world.isClientSide) {
                        for (int i = 0; i < 12; i++) {
                            world.addParticle(ParticleTypes.CRIT,
                                projectile.getX(), projectile.getY(), projectile.getZ(),
                                (world.random.nextDouble() - 0.5) * 0.4,
                                world.random.nextDouble() * 0.3,
                                (world.random.nextDouble() - 0.5) * 0.4);
                        }
                    }
                }
            }
        }
    }
    
     
    public void onPlayerTick(Level world, Player player) {
        if (world.isClientSide) {
            updateBarrierVisuals(world, player);
        }
    }
    
     
    private void endBarrier(Level world, Player player) {
        CompoundTag tag = player.getPersistentData();
        tag.remove("BarrierActive");
        tag.remove("BarrierEndTime");
        
        player.displayClientMessage(new TextComponent("§5Действие Защитного Барьера закончилось"), true);
        
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6F, 0.5F);
        
         
        if (!world.isClientSide) {
            for (int i = 0; i < 60; i++) {
                world.addParticle(ParticleTypes.CLOUD,
                    player.getX() + (world.random.nextDouble() - 0.5) * 3,
                    player.getY() + world.random.nextDouble() * 3,
                    player.getZ() + (world.random.nextDouble() - 0.5) * 3,
                    (world.random.nextDouble() - 0.5) * 0.15,
                    0.1,
                    (world.random.nextDouble() - 0.5) * 0.15);
            }
            
            for (int i = 0; i < 40; i++) {
                world.addParticle(ParticleTypes.SMOKE,
                    player.getX() + (world.random.nextDouble() - 0.5) * 2.5,
                    player.getY() + world.random.nextDouble() * 2.5,
                    player.getZ() + (world.random.nextDouble() - 0.5) * 2.5,
                    (world.random.nextDouble() - 0.5) * 0.12,
                    0.06,
                    (world.random.nextDouble() - 0.5) * 0.12);
            }
            
            for (int i = 0; i < 30; i++) {
                world.addParticle(ParticleTypes.ENCHANT,
                    player.getX() + (world.random.nextDouble() - 0.5) * 2,
                    player.getY() + world.random.nextDouble() * 2,
                    player.getZ() + (world.random.nextDouble() - 0.5) * 2,
                    (world.random.nextDouble() - 0.5) * 0.1,
                    0.05,
                    (world.random.nextDouble() - 0.5) * 0.1);
            }
        }
    }
    
     
    public void handleCurse(Level world, LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        
        if (tag.getBoolean("HeavenCurseActive")) {
            long currentTime = world.getGameTime();
            long curseEndTime = tag.getLong("HeavenCurseEndTime");
            
            if (currentTime > curseEndTime) {
                 
                tag.remove("HeavenCurseActive");
                tag.remove("HeavenCurseEndTime");
                tag.remove("CursedBy");
            } else {
                 
                entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
                
                 
                if (world.isClientSide && world.random.nextFloat() < 0.3) {
                    world.addParticle(ParticleTypes.SOUL,
                        entity.getX() + (world.random.nextDouble() - 0.5),
                        entity.getY() + world.random.nextDouble() * 2,
                        entity.getZ() + (world.random.nextDouble() - 0.5),
                        0, 0.05, 0);
                }
            }
        }
    }
    
     
    public void handleAvatar(Level world, Player player) {
        CompoundTag tag = player.getPersistentData();
        
        if (tag.getBoolean("AvatarActive")) {
            long currentTime = world.getGameTime();
            long avatarEndTime = tag.getLong("AvatarEndTime");
            
            if (currentTime > avatarEndTime) {
                endAvatar(world, player);
            }
        }
    }
    
     
    private void endAvatar(Level world, Player player) {
        CompoundTag tag = player.getPersistentData();
        tag.remove("AvatarActive");
        tag.remove("AvatarEndTime");
        
         
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.removeModifier(AVATAR_HEALTH_MODIFIER_ID);
        }
        
        AttributeInstance attackDamageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttr != null) {
            attackDamageAttr.removeModifier(AVATAR_STRENGTH_MODIFIER_ID);
        }
        
         
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        
        player.displayClientMessage(new TextComponent("§5Действие Воплощения повелителей закончилось"), true);
        
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.5F);
        
        if (!world.isClientSide) {
            for (int i = 0; i < 30; i++) {
                world.addParticle(ParticleTypes.SMOKE,
                    player.getX() + (world.random.nextDouble() - 0.5) * 2,
                    player.getY() + world.random.nextDouble() * 3,
                    player.getZ() + (world.random.nextDouble() - 0.5) * 2,
                    (world.random.nextDouble() - 0.5) * 0.1,
                    0.05,
                    (world.random.nextDouble() - 0.5) * 0.1);
            }
        }
    }
    
     
    
    private void playAbilitySound(Level world, Player player, int ability) {
        switch (ability) {
            case 1:
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 0.9F);
                break;
            case 2:
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 0.7F);
                break;
            case 3:
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 0.5F);
                break;
            case 4:
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.2F);
                break;
            case 5:
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.3F);
                break;
        }
    }
    
    private void spawnAbilityParticles(Level world, Player player, int ability) {
        if (!world.isClientSide) return;
        
        switch (ability) {
            case 1:
                for (int i = 0; i < 50; i++) {
                    world.addParticle(ParticleTypes.ENCHANT,
                        player.getX() + (world.random.nextDouble() - 0.5) * 3,
                        player.getY() + world.random.nextDouble() * 3,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 3,
                        0, 0.15, 0);
                }
                break;
            case 2:
                for (int i = 0; i < 30; i++) {
                    world.addParticle(ParticleTypes.WITCH,
                        player.getX() + (world.random.nextDouble() - 0.5) * 2,
                        player.getY() + world.random.nextDouble() * 2,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 2,
                        (world.random.nextDouble() - 0.5) * 0.1,
                        0.1,
                        (world.random.nextDouble() - 0.5) * 0.1);
                }
                break;
            case 3:
                for (int i = 0; i < 50; i++) {
                    world.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                        player.getX() + (world.random.nextDouble() - 0.5) * 3,
                        player.getY() + world.random.nextDouble() * 3,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 3,
                        0, 0.1, 0);
                }
                break;
            case 4:
                for (int i = 0; i < 40; i++) {
                    world.addParticle(ParticleTypes.ENCHANT,
                        player.getX() + (world.random.nextDouble() - 0.5) * 3,
                        player.getY() + world.random.nextDouble() * 3,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 3,
                        (world.random.nextDouble() - 0.5) * 0.1,
                        0.1,
                        (world.random.nextDouble() - 0.5) * 0.1);
                }
                break;
            case 5:
                for (int i = 0; i < 40; i++) {
                    world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        player.getX() + (world.random.nextDouble() - 0.5) * 2,
                        player.getY() + world.random.nextDouble() * 2,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 2,
                        0, 0.1, 0);
                }
                break;
        }
    }
    
    private void spawnAbilitySelectParticles(Level world, Player player, int ability) {
        if (!world.isClientSide) return;
        
        for (int i = 0; i < 30; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = 1.5;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 1.5 + world.random.nextDouble();
            
            switch (ability) {
                case 1:
                    world.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.05, 0);
                    break;
                case 2:
                    world.addParticle(ParticleTypes.WITCH, x, y, z, 0, 0.05, 0);
                    break;
                case 3:
                    world.addParticle(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 0, 0.05, 0);
                    break;
                case 4:
                    world.addParticle(ParticleTypes.ENCHANTED_HIT, x, y, z, 0, 0.05, 0);
                    break;
                case 5:
                    world.addParticle(ParticleTypes.SOUL, x, y, z, 0, 0.05, 0);
                    break;
            }
        }
    }
    
    private void displayAbilitySelectedMessage(Player player, int abilityId) {
        String message = "";
        String cooldown = "";
        
        switch (abilityId) {
            case 1:
                message = "§5Выбрана способность: §dБарьер";
                cooldown = "60 сек перезарядки";
                break;
            case 2:
                message = "§5Выбрана способность: §dИзвращённая техника";
                cooldown = "40 сек перезарядки";
                break;
            case 3:
                message = "§5Выбрана способность: §dВоплощение повелителей";
                cooldown = "180 сек перезарядки";
                break;
            case 4:
                message = "§5Выбрана способность: §dНевозможная техника: Развеивание";
                cooldown = "45 сек перезарядки";
                break;
            case 5:
                message = "§5Выбрана способность: §dНевозможная техника: Проклятие Небес";
                cooldown = "90 сек перезарядки";
                break;
        }
        
        player.displayClientMessage(new TextComponent(message + " (" + cooldown + ")"), true);
    }
    
     
    @Override
    public void appendHoverText(ItemStack stack, Level world, List<net.minecraft.network.chat.Component> tooltip, 
                               net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        
        int selectedAbility = getSelectedAbility(stack);
        String abilityName = selectedAbility == 0 ? "Меню" : getAbilityName(selectedAbility);
        
        tooltip.add(new TextComponent("Выбранная способность: ").withStyle(ChatFormatting.GRAY)
            .append(new TextComponent(abilityName).withStyle(ChatFormatting.DARK_PURPLE)));
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("ПКМ: Использовать способность").withStyle(ChatFormatting.GOLD));
        tooltip.add(new TextComponent("ПКМ (без выбора): Открыть меню").withStyle(ChatFormatting.YELLOW));
        tooltip.add(new TextComponent("Клавиша M: Открыть меню способностей").withStyle(ChatFormatting.AQUA));
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("Способности:").withStyle(ChatFormatting.LIGHT_PURPLE));
        
        String[] abilities = {
            "1. Барьер",
            "2. Извращённая техника",
            "3. Воплощение повелителей",
            "4. Невозможная техника: Развеивание",
            "5. Невозможная техника: Проклятие Небес"
        };
        
        String[] descriptions = {
            "Создаёт видимое энергетическое поле. Отталкивает врагов, отражает снаряды",
            "Возвращает полученный урон всем врагам в радиусе 50 блоков",
            "Мощное усиление на 15 секунд. ×3 здоровья, +20 сердец поглощения",
            "Снимает все эффекты с врагов и наносит урон (50% здоровья)",
            "Полностью блокирует цель на 10 секунд. Неподвижность, нельзя атаковать"
        };
        
        String[] cooldowns = {
            "60 сек",
            "40 сек",
            "180 сек",
            "45 сек",
            "90 сек"
        };
        
        for (int i = 0; i < abilities.length; i++) {
            boolean isSelected = (i + 1) == selectedAbility;
            ChatFormatting color = isSelected ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY;
            
            tooltip.add(new TextComponent(abilities[i]).withStyle(color));
            tooltip.add(new TextComponent("  " + descriptions[i]).withStyle(ChatFormatting.GRAY));
            tooltip.add(new TextComponent("  Перезарядка: " + cooldowns[i]).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(new TextComponent(""));
        }
    }
    
     
    
    private int getSelectedAbility(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt(TAG_SELECTED_ABILITY);
    }
    
    private void setSelectedAbility(ItemStack stack, int ability) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_SELECTED_ABILITY, ability);
    }
    
    private boolean isBarrierActive(Player player) {
        CompoundTag tag = player.getPersistentData();
        return tag.getBoolean("BarrierActive");
    }
    
    private void setBarrierActive(Player player, boolean active) {
        CompoundTag tag = player.getPersistentData();
        tag.putBoolean("BarrierActive", active);
    }
    
    private void setBarrierEndTime(Player player, long endTime) {
        CompoundTag tag = player.getPersistentData();
        tag.putLong("BarrierEndTime", endTime);
    }
    
    private boolean isAvatarActive(Player player) {
        CompoundTag tag = player.getPersistentData();
        return tag.getBoolean("AvatarActive");
    }
    
    private void setAvatarActive(Player player, boolean active) {
        CompoundTag tag = player.getPersistentData();
        tag.putBoolean("AvatarActive", active);
    }
    
    private void setAvatarEndTime(Player player, long endTime) {
        CompoundTag tag = player.getPersistentData();
        tag.putLong("AvatarEndTime", endTime);
    }
    
     
    
    private boolean isAbilityOnCooldown(Level world, ItemStack stack, int ability) {
        if (world == null || world.isClientSide) {
            return false;
        }
        
        long currentTime = world.getGameTime();
        long lastUseTime = getLastUseTime(stack, ability);
        
        if (lastUseTime == 0) return false;
        
        long elapsedTicks = currentTime - lastUseTime;
        long requiredTicks = getCooldownTicks(ability);
        
        return elapsedTicks < requiredTicks;
    }
    
    private long getLastUseTime(ItemStack stack, int ability) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        
        switch (ability) {
            case 1: return tag.getLong(TAG_LAST_BARRIER_TIME);
            case 2: return tag.getLong(TAG_LAST_RETURN_TIME);
            case 3: return tag.getLong(TAG_LAST_AVATAR_TIME);
            case 4: return tag.getLong(TAG_LAST_DISPEL_TIME);
            case 5: return tag.getLong(TAG_LAST_CURSE_TIME);
            default: return 0;
        }
    }
    
    private void setAbilityLastUseTime(Level world, ItemStack stack, int ability) {
        CompoundTag tag = stack.getOrCreateTag();
        long currentTime = world.getGameTime();
        
        switch (ability) {
            case 1: tag.putLong(TAG_LAST_BARRIER_TIME, currentTime); break;
            case 2: tag.putLong(TAG_LAST_RETURN_TIME, currentTime); break;
            case 3: tag.putLong(TAG_LAST_AVATAR_TIME, currentTime); break;
            case 4: tag.putLong(TAG_LAST_DISPEL_TIME, currentTime); break;
            case 5: tag.putLong(TAG_LAST_CURSE_TIME, currentTime); break;
            default: break;
        }
    }
    
    private long getCooldownTicks(int ability) {
        switch (ability) {
            case 1: return BARRIER_COOLDOWN_SECONDS * 20L;
            case 2: return RETURN_COOLDOWN_SECONDS * 20L;
            case 3: return AVATAR_COOLDOWN_SECONDS * 20L;
            case 4: return DISPEL_COOLDOWN_SECONDS * 20L;
            case 5: return CURSE_COOLDOWN_SECONDS * 20L;
            default: return 0;
        }
    }
    
    private int getRemainingCooldownSeconds(Level world, ItemStack stack, int ability) {
        if (world == null || world.isClientSide) {
            return 0;
        }
        
        long currentTime = world.getGameTime();
        long lastUseTime = getLastUseTime(stack, ability);
        
        if (lastUseTime == 0) return 0;
        
        long elapsedTicks = currentTime - lastUseTime;
        long requiredTicks = getCooldownTicks(ability);
        
        if (elapsedTicks >= requiredTicks) return 0;
        
        long remainingTicks = requiredTicks - elapsedTicks;
        return (int) ((remainingTicks + 19) / 20);
    }
    
    private String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "Меню";
            case 1: return "Барьер";
            case 2: return "Извращённая техника";
            case 3: return "Воплощение повелителей";
            case 4: return "Невозможная техника: Развеивание";
            case 5: return "Невозможная техника: Проклятие Небес";
            case 6: return "Сброс выбора";
            default: return "Неизвестная способность";
        }
    }
    
    @Override
    public int getEnchantmentValue() {
        return 30;
    }
    
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return net.minecraft.world.InteractionResult.PASS;
    }
}