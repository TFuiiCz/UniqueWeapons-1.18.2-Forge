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
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class MagicianDeceptionBook extends Item {
    
    private static final String TAG_MODE = "MagicianMode";
    private static final String TAG_LAST_SWAP_TIME = "LastSwapTime";
    private static final String TAG_LAST_LEVITATE_TIME = "LastLeviateTime";
    private static final String TAG_LAST_SHORT_TP_TIME = "LastShortTPTime";
    private static final String TAG_LAST_SLOW_TIME = "LastSlowTime";
    private static final String TAG_LAST_RETRIBUTION_TIME = "LastRetributionTime";
    private static final String TAG_LAST_SHADOW_DANCE_TIME = "LastShadowDanceTime";
    
    private static final int SWAP_COOLDOWN_SECONDS = 20;
    private static final int LEVITATE_COOLDOWN_SECONDS = 20;
    private static final int SHORT_TP_COOLDOWN_SECONDS = 5;
    private static final int SLOW_COOLDOWN_SECONDS = 25;
    private static final int RETRIBUTION_COOLDOWN_SECONDS = 40;
    private static final int SHADOW_DANCE_COOLDOWN_SECONDS = 60;

    private static final int SWAP_RADIUS = 50;
    private static final int LEVITATE_RADIUS = 30;
    private static final int SHORT_TP_DISTANCE = 20;
    private static final int SLOW_RADIUS = 50;
    private static final int SLOW_DURATION = 200;
    private static final int AURA_RADIUS = 50;
    private static final double RETRIBUTION_RANGE = 30.0;
    private static final int SHADOW_DANCE_DURATION = 400;
    
    public MagicianDeceptionBook(Properties properties) {
        super(properties
            .tab(net.minecraft.world.item.CreativeModeTab.TAB_COMBAT)
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .durability(500)
            .setNoRepair());
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!world.isClientSide) {
            int mode = getMode(stack);
            
            if (isAbilityOnCooldown(world, stack, mode)) {
                int seconds = getRemainingCooldownSeconds(world, stack, mode);
                String abilityName = getAbilityName(mode);
                player.displayClientMessage(new TextComponent("§c" + abilityName + " на перезарядке! Осталось: " + seconds + " сек"), true);
                return InteractionResultHolder.success(stack);
            }
            
            boolean success = useCurrentAbility(world, player, stack, hand);
            
            if (success) {
                setAbilityLastUseTime(world, stack, mode);
                if (stack.hurt(1, world.random, null)) {
                    stack.shrink(1);
                    player.broadcastBreakEvent(hand);
                }
            }
            
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.success(stack);
    }
    
    public void cycleMode(ItemStack stack, Player player) {
        int currentMode = getMode(stack);
        int newMode = (currentMode + 1) % 6;
        setMode(stack, newMode);
        displayModeMessage(player, newMode);
        
        if (player.level.isClientSide) {
            spawnModeSwitchParticles(player.level, player, newMode);
            player.level.playSound(player, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.5F);
        }
    }
    
    private boolean isAbilityOnCooldown(Level world, ItemStack stack, int abilityMode) {
        if (world == null || world.isClientSide) {
            return false;
        }
        
        long currentTime = world.getGameTime();
        long lastUseTime = getLastUseTime(stack, abilityMode);
        
        if (lastUseTime == 0) return false;
        
        long elapsedTicks = currentTime - lastUseTime;
        long requiredTicks = getCooldownTicks(abilityMode);
        
        return elapsedTicks < requiredTicks;
    }
    
    private void setAbilityLastUseTime(Level world, ItemStack stack, int abilityMode) {
        CompoundTag tag = stack.getOrCreateTag();
        long currentTime = world.getGameTime();
        
        switch (abilityMode) {
            case 0:
                tag.putLong(TAG_LAST_SWAP_TIME, currentTime);
                break;
            case 1:
                tag.putLong(TAG_LAST_LEVITATE_TIME, currentTime);
                break;
            case 2:
                tag.putLong(TAG_LAST_SHORT_TP_TIME, currentTime);
                break;
            case 3:
                tag.putLong(TAG_LAST_SLOW_TIME, currentTime);
                break;
            case 4:
                tag.putLong(TAG_LAST_RETRIBUTION_TIME, currentTime);
                break;
            case 5:
                tag.putLong(TAG_LAST_SHADOW_DANCE_TIME, currentTime);
                break;
        }
    }
    
    private long getLastUseTime(ItemStack stack, int abilityMode) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        
        switch (abilityMode) {
            case 0:
                return tag.getLong(TAG_LAST_SWAP_TIME);
            case 1:
                return tag.getLong(TAG_LAST_LEVITATE_TIME);
            case 2:
                return tag.getLong(TAG_LAST_SHORT_TP_TIME);
            case 3:
                return tag.getLong(TAG_LAST_SLOW_TIME);
            case 4:
                return tag.getLong(TAG_LAST_RETRIBUTION_TIME);
            case 5:
                return tag.getLong(TAG_LAST_SHADOW_DANCE_TIME);
            default:
                return 0;
        }
    }
    
    private long getCooldownTicks(int abilityMode) {
        switch (abilityMode) {
            case 0: return SWAP_COOLDOWN_SECONDS * 20L;
            case 1: return LEVITATE_COOLDOWN_SECONDS * 20L;
            case 2: return SHORT_TP_COOLDOWN_SECONDS * 20L;
            case 3: return SLOW_COOLDOWN_SECONDS * 20L;
            case 4: return RETRIBUTION_COOLDOWN_SECONDS * 20L;
            case 5: return SHADOW_DANCE_COOLDOWN_SECONDS * 20L;
            default: return 0;
        }
    }
    
    private int getRemainingCooldownSeconds(Level world, ItemStack stack, int abilityMode) {
        if (world == null || world.isClientSide) {
            return 0;
        }
        
        long currentTime = world.getGameTime();
        long lastUseTime = getLastUseTime(stack, abilityMode);
        
        if (lastUseTime == 0) return 0;
        
        long elapsedTicks = currentTime - lastUseTime;
        long requiredTicks = getCooldownTicks(abilityMode);
        
        if (elapsedTicks >= requiredTicks) return 0;
        
        long remainingTicks = requiredTicks - elapsedTicks;
        return (int) ((remainingTicks + 19) / 20);
    }
    
    private String getAbilityName(int mode) {
        switch (mode) {
            case 0: return "Верховный обман";
            case 1: return "Антигравитация";
            case 2: return "Ближняя телепортация";
            case 3: return "Волна Замедления";
            case 4: return "Воздаяние";
            case 5: return "Теневой Танец";
            default: return "Неизвестная способность";
        }
    }
    
    private String getCooldownTime(int mode) {
        switch (mode) {
            case 0: return "20 сек";
            case 1: return "20 сек";
            case 2: return "5 сек";
            case 3: return "25 сек";
            case 4: return "40 сек";
            case 5: return "60 сек";
            default: return "";
        }
    }
    
    private boolean useCurrentAbility(Level world, Player player, ItemStack stack, InteractionHand hand) {
        int mode = getMode(stack);
        boolean success = false;
        
        switch (mode) {
            case 0: 
                success = useEntitySwap(world, player);
                break;
            case 1: 
                success = useMassLeviate(world, player);
                break;
            case 2: 
                success = useShortTeleport(world, player);
                break;
            case 3: 
                success = useMassSlow(world, player);
                break;
            case 4:
                success = useRetribution(world, player);
                break;
            case 5:
                success = useShadowDance(world, player);
                break;
        }
        
        if (success) {
            switch (mode) {
                case 0:
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.5F);
                    break;
                case 1:
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8F, 1.2F);
                    break;
                case 2:
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.5F);
                    break;
                case 3:
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0F, 0.8F);
                    break;
                case 4:
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
                    break;
                case 5:
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.7F);
                    break;
            }
            
            if (!world.isClientSide) {
                stack.hurtAndBreak(1, player, (p) -> {
                    if (stack.getDamageValue() >= stack.getMaxDamage()) {
                        p.broadcastBreakEvent(hand);
                    }
                });
            }
            
            if (world.isClientSide) {
                spawnUseParticles(world, player, stack);
            }
        }
        
        return success;
    }
    private boolean useEntitySwap(Level world, Player player) {
        AABB area = new AABB(
            player.getX() - SWAP_RADIUS, player.getY() - SWAP_RADIUS, player.getZ() - SWAP_RADIUS,
            player.getX() + SWAP_RADIUS, player.getY() + SWAP_RADIUS, player.getZ() + SWAP_RADIUS
        );
        
        List<Entity> entities = world.getEntities(player, area, 
            entity -> entity instanceof LivingEntity && entity != player);
        
        if (entities.size() < 2) {
            player.displayClientMessage(new TextComponent("§cНедостаточно существ для перестановки!"), true);
            return false;
        }
        
        List<Vec3> positions = new ArrayList<>();
        for (Entity entity : entities) {
            positions.add(entity.position());
        }
        
        Collections.shuffle(positions);
        
        int swappedCount = 0;
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            Vec3 newPos = positions.get(i);
            
            for (int j = 0; j < 10; j++) {
                world.addParticle(ParticleTypes.PORTAL,
                    entity.getX(), entity.getY() + 1, entity.getZ(),
                    (world.random.nextDouble() - 0.5) * 0.2,
                    0.1,
                    (world.random.nextDouble() - 0.5) * 0.2);
            }
            
            entity.teleportTo(newPos.x, newPos.y, newPos.z);
            swappedCount++;
            
            for (int j = 0; j < 10; j++) {
                world.addParticle(ParticleTypes.REVERSE_PORTAL,
                    newPos.x, newPos.y + 1, newPos.z,
                    (world.random.nextDouble() - 0.5) * 0.2,
                    0.1,
                    (world.random.nextDouble() - 0.5) * 0.2);
            }
            
            world.playSound(null, newPos.x, newPos.y, newPos.z, 
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.5F, 1.5F);
        }
        
        player.displayClientMessage(new TextComponent("§5Верховный обман переставил " + swappedCount + " существ!"), true);
        return true;
    }
    private boolean useMassLeviate(Level world, Player player) {
        AABB area = new AABB(
            player.getX() - LEVITATE_RADIUS, player.getY() - LEVITATE_RADIUS, player.getZ() - LEVITATE_RADIUS,
            player.getX() + LEVITATE_RADIUS, player.getY() + LEVITATE_RADIUS, player.getZ() + LEVITATE_RADIUS
        );
        
        List<Entity> entities = world.getEntities(player, area, 
            entity -> entity instanceof LivingEntity && entity != player);
        
        int levitatedCount = 0;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.addEffect(new MobEffectInstance(
                    MobEffects.LEVITATION, 
                    200,
                    0, 
                    false, 
                    true, 
                    true
                ));
                
                levitatedCount++;
                
                for (int i = 0; i < 15; i++) {
                    world.addParticle(ParticleTypes.CLOUD,
                        livingEntity.getX() + (world.random.nextDouble() - 0.5) * 1.5,
                        livingEntity.getY() + world.random.nextDouble() * 2,
                        livingEntity.getZ() + (world.random.nextDouble() - 0.5) * 1.5,
                        0, 0.2, 0);
                }
                
                world.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), 
                    SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 0.7F, 1.3F);
            }
        }
        
        for (int i = 0; i < 100; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = world.random.nextDouble() * LEVITATE_RADIUS;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + world.random.nextDouble() * 2;
            
            world.addParticle(ParticleTypes.CLOUD,
                x, y, z,
                0, 0.1, 0);
        }
        
        player.displayClientMessage(new TextComponent("§5Антигравитация подняла " + levitatedCount + " существ!"), true);
        return true;
    }
    private boolean useShortTeleport(Level world, Player player) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.getEyePosition(1.0F);
        Vec3 endPos = startPos.add(lookVec.scale(SHORT_TP_DISTANCE));
        
        ClipContext context = new ClipContext(startPos, endPos, 
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hitResult = world.clip(context);
        
        Vec3 teleportPos;
        
        if (hitResult.getType() != BlockHitResult.Type.MISS) {
            teleportPos = hitResult.getLocation().subtract(lookVec.scale(1.0));
        } else {
            teleportPos = endPos.subtract(lookVec.scale(1.0));
        }
        
        BlockPos checkPos = new BlockPos(teleportPos);
        BlockState blockState = world.getBlockState(checkPos);
        
        if (!blockState.isAir() && !blockState.getMaterial().isReplaceable()) {
            for (int i = 1; i <= 3; i++) {
                BlockPos upPos = checkPos.above(i);
                if (world.getBlockState(upPos).isAir() && 
                    world.getBlockState(upPos.above()).isAir()) {
                    teleportPos = new Vec3(upPos.getX() + 0.5, upPos.getY(), upPos.getZ() + 0.5);
                    break;
                }
            }
        }
        
        for (int i = 0; i < 20; i++) {
            world.addParticle(ParticleTypes.POOF,
                player.getX() + (world.random.nextDouble() - 0.5) * 1.5,
                player.getY() + world.random.nextDouble() * 2,
                player.getZ() + (world.random.nextDouble() - 0.5) * 1.5,
                (world.random.nextDouble() - 0.5) * 0.1,
                0.1,
                (world.random.nextDouble() - 0.5) * 0.1);
        }
        
        CompoundTag persistentData = player.getPersistentData();
        persistentData.putBoolean("NoFallDamage", true);
        persistentData.putDouble("TeleportStartY", teleportPos.y);
        persistentData.putInt("NoFallTicks", 3);
        
        player.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
        player.fallDistance = 0;
        
        for (int i = 0; i < 20; i++) {
            world.addParticle(ParticleTypes.POOF,
                teleportPos.x + (world.random.nextDouble() - 0.5) * 1.5,
                teleportPos.y + world.random.nextDouble() * 2,
                teleportPos.z + (world.random.nextDouble() - 0.5) * 1.5,
                (world.random.nextDouble() - 0.5) * 0.1,
                0.1,
                (world.random.nextDouble() - 0.5) * 0.1);
        }
        
        player.displayClientMessage(new TextComponent("§5Короткая телепортация выполнена!"), true);
        return true;
    }
    private boolean useMassSlow(Level world, Player player) {
        AABB area = new AABB(
            player.getX() - SLOW_RADIUS, player.getY() - SLOW_RADIUS, player.getZ() - SLOW_RADIUS,
            player.getX() + SLOW_RADIUS, player.getY() + SLOW_RADIUS, player.getZ() + SLOW_RADIUS
        );
        
        List<Entity> entities = world.getEntities(player, area, 
            entity -> entity instanceof LivingEntity && entity != player && 
                     !(entity instanceof Player && ((Player)entity).isCreative()));
        
        int slowedCount = 0;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 
                    SLOW_DURATION,
                    3,
                    false, 
                    true, 
                    true
                ));
                
                livingEntity.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 
                    SLOW_DURATION,
                    1, 
                    false, 
                    true, 
                    true
                ));
                
                slowedCount++;
                
                for (int i = 0; i < 10; i++) {
                    world.addParticle(ParticleTypes.SNOWFLAKE,
                        livingEntity.getX() + (world.random.nextDouble() - 0.5) * 1.5,
                        livingEntity.getY() + world.random.nextDouble() * 2,
                        livingEntity.getZ() + (world.random.nextDouble() - 0.5) * 1.5,
                        0, 0.05, 0);
                }
                
                world.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), 
                    SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.5F, 0.8F);
            }
        }
        
        for (int wave = 0; wave < 3; wave++) {
            for (int i = 0; i < 360; i += 10) {
                double angle = Math.toRadians(i);
                double radius = (wave + 1) * 5.0;
                
                for (int j = 0; j < 3; j++) {
                    double x = player.getX() + Math.cos(angle) * (radius + world.random.nextDouble() * 2);
                    double z = player.getZ() + Math.sin(angle) * (radius + world.random.nextDouble() * 2);
                    double y = player.getY() + 0.5 + world.random.nextDouble() * 1.5;
                    
                    world.addParticle(ParticleTypes.SNOWFLAKE,
                        x, y, z,
                        0, 0.05, 0);
                    
                    world.addParticle(ParticleTypes.CLOUD,
                        x, y, z,
                        (world.random.nextDouble() - 0.5) * 0.1,
                        0.05,
                        (world.random.nextDouble() - 0.5) * 0.1);
                }
            }
        }
        
        player.displayClientMessage(new TextComponent(
            "§5Волна замедления повлияла на " + slowedCount + " существ!"), true);
        return true;
    }
    private boolean useRetribution(Level world, Player player) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.getEyePosition(1.0F);
        Vec3 endPos = startPos.add(lookVec.scale(RETRIBUTION_RANGE));
        
        ClipContext context = new ClipContext(startPos, endPos, 
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hitResult = world.clip(context);
        
        Vec3 hitPos = hitResult.getLocation();
        AABB searchArea = new AABB(
            hitPos.x - 2, hitPos.y - 2, hitPos.z - 2,
            hitPos.x + 2, hitPos.y + 2, hitPos.z + 2
        );
        
        List<Entity> entities = world.getEntities(player, searchArea, 
            entity -> entity instanceof LivingEntity && entity != player);
        
        if (entities.isEmpty()) {
            player.displayClientMessage(new TextComponent("§cНет цели для Воздаяния!"), true);
            return false;
        }
        
        LivingEntity target = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                double distance = entity.distanceTo(player);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    target = livingEntity;
                }
            }
        }
        
        if (target == null) {
            return false;
        }
        
        float playerHealth = player.getHealth();
        float targetHealth = target.getHealth();
        
        float playerMaxHealth = player.getMaxHealth();
        float targetMaxHealth = target.getMaxHealth();
        
        player.setHealth(Math.min(targetHealth, playerMaxHealth));
        target.setHealth(Math.min(playerHealth, targetMaxHealth));
        
        for (int i = 0; i < 30; i++) {
            double progress = world.random.nextDouble();
            double x = player.getX() + (target.getX() - player.getX()) * progress;
            double y = player.getY() + 1.0 + (target.getY() - (player.getY() + 1.0)) * progress;
            double z = player.getZ() + (target.getZ() - player.getZ()) * progress;
            
            world.addParticle(ParticleTypes.SOUL,
                x, y, z,
                0, 0.05, 0);
            
            double x2 = target.getX() + (player.getX() - target.getX()) * progress;
            double y2 = target.getY() + (player.getY() - target.getY()) * progress;
            double z2 = target.getZ() + (player.getZ() - target.getZ()) * progress;
            
            world.addParticle(ParticleTypes.HEART,
                x2, y2, z2,
                0, 0.05, 0);
        }
        
        for (int i = 0; i < 20; i++) {
            world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                player.getX() + (world.random.nextDouble() - 0.5) * 1.5,
                player.getY() + world.random.nextDouble() * 2,
                player.getZ() + (world.random.nextDouble() - 0.5) * 1.5,
                (world.random.nextDouble() - 0.5) * 0.1,
                0.1,
                (world.random.nextDouble() - 0.5) * 0.1);
        }
        
        for (int i = 0; i < 20; i++) {
            world.addParticle(ParticleTypes.DAMAGE_INDICATOR,
                target.getX() + (world.random.nextDouble() - 0.5) * 1.5,
                target.getY() + world.random.nextDouble() * 2,
                target.getZ() + (world.random.nextDouble() - 0.5) * 1.5,
                (world.random.nextDouble() - 0.5) * 0.1,
                0.1,
                (world.random.nextDouble() - 0.5) * 0.1);
        }
        
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), 
            SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 0.8F, 1.0F);
        
        player.displayClientMessage(new TextComponent(
            "§5Воздаяние! Вы обменялись здоровьем с " + target.getName().getString()), true);
        
        return true;
    }
    private boolean useShadowDance(Level world, Player player) {
        player.getPersistentData().putBoolean("ShadowDance", true);
        player.getPersistentData().putLong("ShadowDanceStart", world.getGameTime());
        player.getPersistentData().putInt("ShadowDanceDuration", SHADOW_DANCE_DURATION);
        
        player.addEffect(new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            SHADOW_DANCE_DURATION,
            4,
            false,
            false
        ));
        
        player.addEffect(new MobEffectInstance(
            MobEffects.INVISIBILITY,
            SHADOW_DANCE_DURATION,
            0,
            false,
            false
        ));
        
        player.addEffect(new MobEffectInstance(
            MobEffects.MOVEMENT_SPEED,
            SHADOW_DANCE_DURATION,
            2,
            false,
            false
        ));
        
        player.addEffect(new MobEffectInstance(
            MobEffects.JUMP,
            SHADOW_DANCE_DURATION,
            1,
            false,
            false
        ));
        
        for (int i = 0; i < 100; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = 2.0;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + world.random.nextDouble() * 2;
            
            world.addParticle(ParticleTypes.SMOKE,
                x, y, z,
                (world.random.nextDouble() - 0.5) * 0.1,
                0.1,
                (world.random.nextDouble() - 0.5) * 0.1);
            
            world.addParticle(ParticleTypes.ASH,
                x, y, z,
                0, 0.05, 0);
            
            if (world.random.nextDouble() < 0.3) {
                world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    x, y, z,
                    0, 0.03, 0);
            }
        }
        
        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            double radius = 3.0;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 0.5;
            
            for (int j = 0; j < 3; j++) {
                world.addParticle(ParticleTypes.SMOKE,
                    x, y + j * 0.5, z,
                    0, 0.05, 0);
            }
        }
        
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.7F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.AMBIENT_CAVE, SoundSource.PLAYERS, 0.8F, 0.5F);
        
        player.displayClientMessage(new TextComponent(
            "§5Теневой Танец активирован! Вы неуязвимы на 20 секунд!"), true);
        
        com.twistedfantasy.uniqueweapons.events.TickHandler.scheduleTask(() -> {
            if (!player.level.isClientSide) {
                deactivateShadowDance(player);
            }
        }, SHADOW_DANCE_DURATION);
        
        return true;
    }
    private void deactivateShadowDance(Player player) {
        CompoundTag playerData = player.getPersistentData();
        
        if (playerData.getBoolean("ShadowDance")) {
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            player.removeEffect(MobEffects.INVISIBILITY);
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
            player.removeEffect(MobEffects.JUMP);
            
            playerData.remove("ShadowDance");
            playerData.remove("ShadowDanceStart");
            playerData.remove("ShadowDanceDuration");
            
            for (int i = 0; i < 50; i++) {
                player.level.addParticle(ParticleTypes.SMOKE,
                    player.getX() + (player.level.random.nextDouble() - 0.5) * 2,
                    player.getY() + player.level.random.nextDouble() * 2,
                    player.getZ() + (player.level.random.nextDouble() - 0.5) * 2,
                    (player.level.random.nextDouble() - 0.5) * 0.2,
                    0.1,
                    (player.level.random.nextDouble() - 0.5) * 0.2);
            }
            
            player.displayClientMessage(new TextComponent("§5Теневой Танец завершен!"), true);
        }
    }
    public static void applyKingAura(Player player, Level world) {
        if (world.isClientSide || player == null) return;
        boolean hasBook = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof MagicianDeceptionBook) {
                hasBook = true;
                break;
            }
        }
        if (!hasBook) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            hasBook = mainHand.getItem() instanceof MagicianDeceptionBook || 
                     offHand.getItem() instanceof MagicianDeceptionBook;
        }
        
        if (!hasBook) return;
        AABB auraArea = new AABB(
            player.getX() - AURA_RADIUS, player.getY() - AURA_RADIUS, player.getZ() - AURA_RADIUS,
            player.getX() + AURA_RADIUS, player.getY() + AURA_RADIUS, player.getZ() + AURA_RADIUS
        );
        
        List<Entity> enemies = world.getEntities(player, auraArea, 
            entity -> entity instanceof LivingEntity && entity != player && 
                     !(entity instanceof Player && ((Player)entity).isCreative()));
        for (Entity enemy : enemies) {
            if (enemy instanceof LivingEntity livingEnemy) {
                livingEnemy.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS,
                    100,
                    0,
                    false,
                    true,
                    true
                ));
                if (world.getGameTime() % 20 == 0) {
                    world.addParticle(ParticleTypes.ASH,
                        livingEnemy.getX(),
                        livingEnemy.getY() + livingEnemy.getBbHeight() / 2,
                        livingEnemy.getZ(),
                        0, 0.05, 0);
                }
            }
        }
        if (world.getGameTime() % 20 == 0) {
            for (int i = 0; i < 360; i += 30) {
                double angle = Math.toRadians(i);
                double radius = AURA_RADIUS * 0.8;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                double y = player.getY() + 0.5;
                
                world.addParticle(ParticleTypes.ASH,
                    x, y, z,
                    0, 0.02, 0);
            }
        }
    }
    
    private void displayModeMessage(Player player, int mode) {
        String message = "";
        switch (mode) {
            case 0: message = "§5Выбран режим: Верховный обман (20 сек перезарядки)"; break;
            case 1: message = "§5Выбран режим: Антигравитация (20 сек перезарядки)"; break;
            case 2: message = "§5Выбран режим: Ближняя телепортация (5 сек перезарядки)"; break;
            case 3: message = "§5Выбран режим: Волна Замедления (25 сек перезарядки)"; break;
            case 4: message = "§5Выбран режим: Воздаяние (40 сек перезарядки)"; break;
            case 5: message = "§5Выбран режим: Теневой Танец (60 сек перезарядки)";
        }
        player.displayClientMessage(new TextComponent(message), true);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Level world, List<net.minecraft.network.chat.Component> tooltip, 
                               net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        
        int currentMode = getMode(stack);
        String modeName = getAbilityName(currentMode);
        
        tooltip.add(new TextComponent("Текущий режим: ").withStyle(ChatFormatting.GRAY)
            .append(new TextComponent(modeName).withStyle(ChatFormatting.DARK_AQUA)));
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("ПКМ: Использовать способность").withStyle(ChatFormatting.GOLD));
        tooltip.add(new TextComponent("Клавиша T: Сменить режим").withStyle(ChatFormatting.YELLOW));
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("Активные способности:").withStyle(ChatFormatting.AQUA));
        
        for (int i = 0; i < 6; i++) {
            String abilityName = getAbilityName(i);
            String cooldownTime = getCooldownTime(i);
            boolean onCooldown = isAbilityOnCooldown(world, stack, i);
            int remainingSeconds = getRemainingCooldownSeconds(world, stack, i);
            
            ChatFormatting abilityColor = onCooldown ? ChatFormatting.RED : ChatFormatting.DARK_GRAY;
            
            if (i == currentMode) {
                tooltip.add(new TextComponent("→ " + abilityName + " ←").withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(new TextComponent((i + 1) + ". " + abilityName).withStyle(abilityColor));
            }
            
            if (onCooldown) {
                tooltip.add(new TextComponent("   Перезарядка: " + remainingSeconds + " сек").withStyle(ChatFormatting.RED));
            } else {
                tooltip.add(new TextComponent("   Перезарядка: " + cooldownTime).withStyle(ChatFormatting.DARK_GRAY));
            }
            
            switch (i) {
                case 0:
                    tooltip.add(new TextComponent("   Случайно меняет местами всех существ в радиусе 50 блоков").withStyle(ChatFormatting.GRAY));
                    break;
                case 1:
                    tooltip.add(new TextComponent("   Накладывает левитацию на всех существ вокруг (кроме себя)").withStyle(ChatFormatting.GRAY));
                    break;
                case 2:
                    tooltip.add(new TextComponent("   Быстрая телепортация на 20 блоков по направлению взгляда").withStyle(ChatFormatting.GRAY));
                    break;
                case 3:
                    tooltip.add(new TextComponent("   Сильное замедление и ослабление врагов в радиусе 50 блоков").withStyle(ChatFormatting.GRAY));
                    break;
                case 4:
                    tooltip.add(new TextComponent("   Обменяйся здоровьем с существом, на которое смотришь").withStyle(ChatFormatting.GRAY));
                    break;
                case 5:
                    tooltip.add(new TextComponent("   Полная неуязвимость, невидимость и скорость на 20 секунд").withStyle(ChatFormatting.GRAY));
                    break;
            }
            
            if (i < 5) {
                tooltip.add(new TextComponent(""));
            }
        }
        
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("Пассивные способности:").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(new TextComponent("Аура Короля").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(new TextComponent("   Враги в радиусе 50 блоков получают Слабость I").withStyle(ChatFormatting.GRAY));
        tooltip.add(new TextComponent("   Действует пока книга в инвентаре или в руках").withStyle(ChatFormatting.GRAY));
    }
    
    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
        super.onCraftedBy(stack, world, player);
        if (!stack.hasTag() || !stack.getTag().contains(TAG_MODE)) {
            setMode(stack, 0);
        }
    }
    
    private void spawnUseParticles(Level world, Player player, ItemStack stack) {
        int mode = getMode(stack);
        
        for (int i = 0; i < 30; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = 1.0;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 1.0 + world.random.nextDouble() * 1.5;
            
            switch (mode) {
                case 0:
                    world.addParticle(ParticleTypes.PORTAL, x, y, z, 0, 0.1, 0);
                    break;
                case 1:
                    world.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0.1, 0);
                    break;
                case 2:
                    world.addParticle(ParticleTypes.POOF, x, y, z, 
                        (world.random.nextDouble() - 0.5) * 0.1, 0.05, (world.random.nextDouble() - 0.5) * 0.1);
                    break;
                case 3:
                    world.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, 0.1, 0);
                    break;
                case 4:
                    world.addParticle(ParticleTypes.SOUL, x, y, z, 0, 0.1, 0);
                    break;
                case 5:
                    world.addParticle(ParticleTypes.SMOKE, x, y, z, 
                        (world.random.nextDouble() - 0.5) * 0.05, 0.05, (world.random.nextDouble() - 0.5) * 0.05);
                    break;
            }
        }
    }
    
    private void spawnModeSwitchParticles(Level world, Player player, int mode) {
        for (int i = 0; i < 50; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = 1.5;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 1.5 + world.random.nextDouble() * 1.0;
            
            switch (mode) {
                case 0:
                    world.addParticle(ParticleTypes.PORTAL, x, y, z, 0, 0.05, 0);
                    world.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.03, 0);
                    break;
                case 1:
                    world.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0.05, 0);
                    world.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0, 0.03, 0);
                    break;
                case 2:
                    world.addParticle(ParticleTypes.POOF, x, y, z, 0, 0.05, 0);
                    world.addParticle(ParticleTypes.CLOUD, x, y, z, 
                        (world.random.nextDouble() - 0.5) * 0.1, 0.03, (world.random.nextDouble() - 0.5) * 0.1);
                    break;
                case 3:
                    world.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, 0.05, 0);
                    world.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0.03, 0);
                    break;
                case 4:
                    world.addParticle(ParticleTypes.SOUL, x, y, z, 0, 0.05, 0);
                    world.addParticle(ParticleTypes.HEART, x, y, z, 0, 0.03, 0);
                    break;
                case 5:
                    world.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.05, 0);
                    world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.03, 0);
                    break;
            }
        }
    }
    
    private int getMode(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt(TAG_MODE);
    }
    
    private void setMode(ItemStack stack, int mode) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_MODE, mode);
    }
    
    @Override
    public int getEnchantmentValue() {
        return 20;
    }
    
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return net.minecraft.world.InteractionResult.PASS;
    }
}