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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ArchmageBook extends Item {
    
    private static final String TAG_MODE = "ArchmageMode";
    private static final String TAG_LAST_INVIS_TIME = "LastInvisTime";
    private static final String TAG_LAST_PUSH_TIME = "LastPushTime";
    private static final String TAG_LAST_BLIND_TIME = "LastBlindTime";
    private static final String TAG_LAST_DEBUFF_TIME = "LastDebuffTime";
    private static final String TAG_LAST_TELEPORT_TIME = "LastTeleportTime";
    
    private static final int INVIS_COOLDOWN_SECONDS = 30;
    private static final int PUSH_COOLDOWN_SECONDS = 10;
    private static final int BLIND_COOLDOWN_SECONDS = 20;
    private static final int DEBUFF_COOLDOWN_SECONDS = 20;
    private static final int TELEPORT_COOLDOWN_SECONDS = 45;

    private static final int ABILITY_DURATION = 300;
    private static final int INVISIBILITY_DURATION = 300;
    private static final int BLIND_RADIUS = 100;
    private static final int PUSH_RADIUS = 30;
    private static final int DEBUFF_RADIUS = 30;
    private static final int TELEPORT_MAX_DISTANCE = 200;
    
    public ArchmageBook(Properties properties) {
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
        int newMode = (currentMode + 1) % 5;
        setMode(stack, newMode);
        displayModeMessage(player, newMode);
        
        if (player.level.isClientSide) {
            spawnModeSwitchParticles(player.level, player, newMode);
            player.level.playSound(player, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.2F);
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
                tag.putLong(TAG_LAST_INVIS_TIME, currentTime);
                break;
            case 1:
                tag.putLong(TAG_LAST_PUSH_TIME, currentTime);
                break;
            case 2:
                tag.putLong(TAG_LAST_BLIND_TIME, currentTime);
                break;
            case 3:
                tag.putLong(TAG_LAST_DEBUFF_TIME, currentTime);
                break;
            case 4:
                tag.putLong(TAG_LAST_TELEPORT_TIME, currentTime);
                break;
        }
    }
    
    private long getLastUseTime(ItemStack stack, int abilityMode) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        
        switch (abilityMode) {
            case 0:
                return tag.getLong(TAG_LAST_INVIS_TIME);
            case 1:
                return tag.getLong(TAG_LAST_PUSH_TIME);
            case 2:
                return tag.getLong(TAG_LAST_BLIND_TIME);
            case 3:
                return tag.getLong(TAG_LAST_DEBUFF_TIME);
            case 4:
                return tag.getLong(TAG_LAST_TELEPORT_TIME);
            default:
                return 0;
        }
    }
    
    private long getCooldownTicks(int abilityMode) {
        switch (abilityMode) {
            case 0: return INVIS_COOLDOWN_SECONDS * 20L;
            case 1: return PUSH_COOLDOWN_SECONDS * 20L;
            case 2: return BLIND_COOLDOWN_SECONDS * 20L;
            case 3: return DEBUFF_COOLDOWN_SECONDS * 20L;
            case 4: return TELEPORT_COOLDOWN_SECONDS * 20L;
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
            case 0: return "Теневой Покров";
            case 1: return "Отталкивающая Волна";
            case 2: return "Волна Тьмы";
            case 3: return "Чума Проклятий";
            case 4: return "Теневой Телепорт";
            default: return "Неизвестная способность";
        }
    }
    
    private String getCooldownTime(int mode) {
        switch (mode) {
            case 0: return "30 сек";
            case 1: return "10 сек";
            case 2: return "20 сек";
            case 3: return "20 сек";
            case 4: return "45 сек";
            default: return "";
        }
    }
    
    private boolean useCurrentAbility(Level world, Player player, ItemStack stack, InteractionHand hand) {
        int mode = getMode(stack);
        boolean success = false;
        
        switch (mode) {
            case 0: 
                success = useCompleteInvisibility(world, player);
                break;
            case 1: 
                success = useEntityRepulsion(world, player);
                break;
            case 2: 
                success = useMassBlindness(world, player);
                break;
            case 3: 
                success = useMassDebuff(world, player);
                break;
            case 4:
                success = useShadowTeleport(world, player);
                break;
        }
        
        if (success) {
            switch (mode) {
                case 0:
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.5F);
                    break;
                case 1:
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.5F);
                    break;
                case 2:
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.7F);
                    break;
                case 3:
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0F, 0.5F);
                    break;
                case 4:
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
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
    
    private boolean useCompleteInvisibility(Level world, Player player) {
        player.addEffect(new MobEffectInstance(
            MobEffects.INVISIBILITY, 
            INVISIBILITY_DURATION, 
            0, 
            false, 
            false, 
            true
        ));
        
        player.addEffect(new MobEffectInstance(
            MobEffects.MOVEMENT_SPEED, 
            INVISIBILITY_DURATION, 
            4, 
            false, 
            false, 
            true
        ));
        
        for (int i = 0; i < 50; i++) {
            world.addParticle(ParticleTypes.SMOKE,
                player.getX() + (world.random.nextDouble() - 0.5) * 2,
                player.getY() + world.random.nextDouble() * 2,
                player.getZ() + (world.random.nextDouble() - 0.5) * 2,
                (world.random.nextDouble() - 0.5) * 0.1,
                0.1,
                (world.random.nextDouble() - 0.5) * 0.1);
        }
        
        player.displayClientMessage(new TextComponent("§5Активирована Полная Невидимость на 15 секунд"), true);
        return true;
    }
    
    private boolean useEntityRepulsion(Level world, Player player) {
        AABB area = new AABB(
            player.getX() - PUSH_RADIUS, player.getY() - PUSH_RADIUS, player.getZ() - PUSH_RADIUS,
            player.getX() + PUSH_RADIUS, player.getY() + PUSH_RADIUS, player.getZ() + PUSH_RADIUS
        );
        
        List<Entity> entities = world.getEntities(player, area);
        int pushedCount = 0;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity && entity != player) {
                Vec3 pushDirection = entity.position().subtract(player.position());
                
                if (pushDirection.length() > 0) {
                    pushDirection = pushDirection.normalize().scale(3.0);
                    
                    entity.setDeltaMovement(
                        pushDirection.x,
                        Math.min(1.5, pushDirection.y + 0.5),
                        pushDirection.z
                    );
                    entity.hurtMarked = true;
                    pushedCount++;
                    
                    for (int i = 0; i < 5; i++) {
                        world.addParticle(ParticleTypes.POOF,
                            entity.getX() + (world.random.nextDouble() - 0.5) * 0.5,
                            entity.getY() + world.random.nextDouble() * 1.5,
                            entity.getZ() + (world.random.nextDouble() - 0.5) * 0.5,
                            (world.random.nextDouble() - 0.5) * 0.2,
                            0.1,
                            (world.random.nextDouble() - 0.5) * 0.2);
                    }
                }
            }
        }
        
        player.displayClientMessage(new TextComponent(
            "§5Отталкивающая волна отбросила " + pushedCount + " существ"), true);
        return true;
    }
    
    private boolean useMassBlindness(Level world, Player player) {
        AABB area = new AABB(
            player.getX() - BLIND_RADIUS, player.getY() - BLIND_RADIUS, player.getZ() - BLIND_RADIUS,
            player.getX() + BLIND_RADIUS, player.getY() + BLIND_RADIUS, player.getZ() + BLIND_RADIUS
        );
        
        List<Entity> entities = world.getEntities(player, area);
        int blindedCount = 0;
        
        for (Entity entity : entities) {
            if (entity instanceof Player targetPlayer && targetPlayer != player) {
                targetPlayer.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS, 
                    ABILITY_DURATION, 
                    0, 
                    false, 
                    true, 
                    true
                ));
                
                targetPlayer.addEffect(new MobEffectInstance(
                    MobEffects.CONFUSION, 
                    ABILITY_DURATION, 
                    0, 
                    false, 
                    true, 
                    true
                ));
                
                blindedCount++;
                
                for (int i = 0; i < 10; i++) {
                    world.addParticle(ParticleTypes.SQUID_INK,
                        targetPlayer.getX() + (world.random.nextDouble() - 0.5) * 2,
                        targetPlayer.getY() + world.random.nextDouble() * 2,
                        targetPlayer.getZ() + (world.random.nextDouble() - 0.5) * 2,
                        0, 0.1, 0);
                }
                
                world.playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(), 
                    SoundEvents.AMBIENT_CAVE, SoundSource.HOSTILE, 1.0F, 0.3F);
            }
        }
        
        player.displayClientMessage(new TextComponent(
            "§5Волна тьмы ослепила " + blindedCount + " игроков на 15 секунд"), true);
        
        for (int i = 0; i < 200; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = world.random.nextDouble() * BLIND_RADIUS;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + world.random.nextDouble() * 10;
            
            world.addParticle(ParticleTypes.SQUID_INK,
                x, y, z,
                (world.random.nextDouble() - 0.5) * 0.2,
                -0.1,
                (world.random.nextDouble() - 0.5) * 0.2);
        }
        
        return true;
    }
    
    private boolean useMassDebuff(Level world, Player player) {
        AABB area = new AABB(
            player.getX() - DEBUFF_RADIUS, player.getY() - DEBUFF_RADIUS, player.getZ() - DEBUFF_RADIUS,
            player.getX() + DEBUFF_RADIUS, player.getY() + DEBUFF_RADIUS, player.getZ() + DEBUFF_RADIUS
        );
        
        List<Entity> entities = world.getEntities(player, area);
        int affectedCount = 0;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity && entity != player) {
                livingEntity.addEffect(new MobEffectInstance(
                    MobEffects.HARM, 
                    1, 
                    1, 
                    false, 
                    true, 
                    true
                ));
                
                livingEntity.addEffect(new MobEffectInstance(
                    MobEffects.POISON, 
                    ABILITY_DURATION, 
                    1, 
                    false, 
                    true, 
                    true
                ));
                
                livingEntity.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 
                    ABILITY_DURATION, 
                    2, 
                    false, 
                    true, 
                    true
                ));
                
                livingEntity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 
                    ABILITY_DURATION, 
                    1, 
                    false, 
                    true, 
                    true
                ));
                
                affectedCount++;
                
                for (int i = 0; i < 8; i++) {
                    world.addParticle(ParticleTypes.DAMAGE_INDICATOR,
                        livingEntity.getX() + (world.random.nextDouble() - 0.5) * entity.getBbWidth(),
                        livingEntity.getY() + world.random.nextDouble() * entity.getBbHeight(),
                        livingEntity.getZ() + (world.random.nextDouble() - 0.5) * entity.getBbWidth(),
                        0, 0.05, 0);
                }
                
                world.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), 
                    SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 0.3F, 1.2F);
            }
        }
        
        player.displayClientMessage(new TextComponent(
            "§5Чума проклятий поразила " + affectedCount + " существ"), true);
        
        for (int i = 0; i < 100; i++) {
            world.addParticle(ParticleTypes.WITCH,
                player.getX() + (world.random.nextDouble() - 0.5) * 5,
                player.getY() + world.random.nextDouble() * 3,
                player.getZ() + (world.random.nextDouble() - 0.5) * 5,
                (world.random.nextDouble() - 0.5) * 0.1,
                0.2,
                (world.random.nextDouble() - 0.5) * 0.1);
        }
        
        return true;
    }
    
    private boolean useShadowTeleport(Level world, Player player) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.getEyePosition(1.0F);
        Vec3 endPos = startPos.add(lookVec.scale(TELEPORT_MAX_DISTANCE));
        ClipContext context = new ClipContext(startPos, endPos, 
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hitResult = world.clip(context);
        
        Vec3 teleportPos;
        
        if (hitResult.getType() != BlockHitResult.Type.MISS) {
            teleportPos = hitResult.getLocation().subtract(lookVec.scale(0.5));
        } else {
            teleportPos = endPos.subtract(lookVec.scale(2.0));
        }
        
        BlockPos checkPos = new BlockPos(teleportPos);
        BlockState blockState = world.getBlockState(checkPos);
        
        if (!blockState.isAir() && !blockState.getMaterial().isReplaceable()) {
            for (int i = 1; i <= 5; i++) {
                BlockPos upPos = checkPos.above(i);
                if (world.getBlockState(upPos).isAir() && 
                    world.getBlockState(upPos.above()).isAir()) {
                    teleportPos = new Vec3(upPos.getX() + 0.5, upPos.getY(), upPos.getZ() + 0.5);
                    break;
                }
            }
        }
        for (int i = 0; i < 30; i++) {
            world.addParticle(ParticleTypes.PORTAL,
                player.getX() + (world.random.nextDouble() - 0.5) * 1.5,
                player.getY() + world.random.nextDouble() * 2,
                player.getZ() + (world.random.nextDouble() - 0.5) * 1.5,
                (world.random.nextDouble() - 0.5) * 0.5,
                world.random.nextDouble() * 0.5,
                (world.random.nextDouble() - 0.5) * 0.5);
        }
        CompoundTag persistentData = player.getPersistentData();
        persistentData.putBoolean("NoFallDamage", true);
        persistentData.putDouble("TeleportStartY", teleportPos.y);
        persistentData.putInt("NoFallTicks", 5);
        player.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
        player.fallDistance = 0;
        for (int i = 0; i < 30; i++) {
            world.addParticle(ParticleTypes.PORTAL,
                teleportPos.x + (world.random.nextDouble() - 0.5) * 1.5,
                teleportPos.y + world.random.nextDouble() * 2,
                teleportPos.z + (world.random.nextDouble() - 0.5) * 1.5,
                (world.random.nextDouble() - 0.5) * 0.5,
                world.random.nextDouble() * 0.5,
                (world.random.nextDouble() - 0.5) * 0.5);
        }
        for (int i = 0; i < 20; i++) {
            world.addParticle(ParticleTypes.CLOUD,
                teleportPos.x + (world.random.nextDouble() - 0.5) * 2.0,
                teleportPos.y - 0.5,
                teleportPos.z + (world.random.nextDouble() - 0.5) * 2.0,
                (world.random.nextDouble() - 0.5) * 0.1,
                0.05,
                (world.random.nextDouble() - 0.5) * 0.1);
        }
        world.playSound(null, teleportPos.x, teleportPos.y, teleportPos.z, 
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
        
        player.displayClientMessage(new TextComponent("§5Телепортирован в точку взгляда"), true);
        return true;
    }
    
    private void displayModeMessage(Player player, int mode) {
        String message = "";
        switch (mode) {
            case 0: message = "§5Выбран режим: Теневой Покров (30 сек перезарядки)"; break;
            case 1: message = "§5Выбран режим: Отталкивающая Волна (10 сек перезарядки)"; break;
            case 2: message = "§5Выбран режим: Волна Тьмы (20 сек перезарядки)"; break;
            case 3: message = "§5Выбран режим: Чума Проклятий (20 сек перезарядки)"; break;
            case 4: message = "§5Выбран режим: Теневой Телепорт (45 сек перезарядки)";
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
            .append(new TextComponent(modeName).withStyle(ChatFormatting.DARK_PURPLE)));
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("ПКМ: Использовать способность").withStyle(ChatFormatting.GOLD));
        tooltip.add(new TextComponent("Клавиша T: Сменить режим").withStyle(ChatFormatting.YELLOW));
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("Способности:").withStyle(ChatFormatting.LIGHT_PURPLE));
        for (int i = 0; i < 5; i++) {
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
                    tooltip.add(new TextComponent("   Полная невидимость + Скорость V на 15с").withStyle(ChatFormatting.GRAY));
                    break;
                case 1:
                    tooltip.add(new TextComponent("   Отталкивает всех существ в радиусе 30 блоков").withStyle(ChatFormatting.GRAY));
                    break;
                case 2:
                    tooltip.add(new TextComponent("   Ослепляет всех игроков в радиусе 100 блоков").withStyle(ChatFormatting.GRAY));
                    break;
                case 3:
                    tooltip.add(new TextComponent("   Накладывает негативные эффекты на всех существ").withStyle(ChatFormatting.GRAY));
                    break;
                case 4:
                    tooltip.add(new TextComponent("   Телепортирует в точку взгляда (до 200 блоков)").withStyle(ChatFormatting.GRAY));
                    break;
            }
            
            tooltip.add(new TextComponent(""));
        }
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
                    world.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.1, 0);
                    break;
                case 1:
                    world.addParticle(ParticleTypes.POOF, x, y, z, 
                        (world.random.nextDouble() - 0.5) * 0.1, 0.05, (world.random.nextDouble() - 0.5) * 0.1);
                    break;
                case 2:
                    world.addParticle(ParticleTypes.SQUID_INK, x, y, z, 0, 0.1, 0);
                    break;
                case 3:
                    world.addParticle(ParticleTypes.WITCH, x, y, z, 
                        (world.random.nextDouble() - 0.5) * 0.05, 0.05, (world.random.nextDouble() - 0.5) * 0.05);
                    break;
                case 4:
                    world.addParticle(ParticleTypes.PORTAL, x, y, z, 
                        (world.random.nextDouble() - 0.5) * 0.1, 0.05, (world.random.nextDouble() - 0.5) * 0.1);
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
                    world.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.05, 0);
                    world.addParticle(ParticleTypes.ASH, x, y, z, 0, 0.03, 0);
                    break;
                case 1:
                    world.addParticle(ParticleTypes.POOF, x, y, z, 0, 0.05, 0);
                    world.addParticle(ParticleTypes.CLOUD, x, y, z, 
                        (world.random.nextDouble() - 0.5) * 0.1, 0.03, (world.random.nextDouble() - 0.5) * 0.1);
                    break;
                case 2:
                    world.addParticle(ParticleTypes.SQUID_INK, x, y, z, 0, 0.05, 0);
                    world.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.03, 0);
                    break;
                case 3:
                    world.addParticle(ParticleTypes.WITCH, x, y, z, 0, 0.05, 0);
                    world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.03, 0);
                    break;
                case 4:
                    world.addParticle(ParticleTypes.PORTAL, x, y, z, 0, 0.05, 0);
                    world.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.03, 0);
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
        return 25;
    }
    
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return net.minecraft.world.InteractionResult.PASS;
    }
}