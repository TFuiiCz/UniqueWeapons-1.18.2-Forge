package com.twistedfantasy.uniqueweapons.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.InteractionResultHolder;
import com.twistedfantasy.uniqueweapons.events.TickHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Explosion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.UUID;

public class VoidAccelerator extends Item {
    
    private static final String TAG_MODE = "VoidMode";
    private static final String TAG_LAST_EXPLOSION_TIME = "LastExplosionTime";
    private static final String TAG_LAST_FLIGHT_TIME = "LastFlightTime";
    private static final String TAG_LAST_TIMESTOP_TIME = "LastTimestopTime";
    private static final UUID FLIGHT_SPEED_MODIFIER_ID = UUID.fromString("1e9d8a7b-3c4d-4e5f-6a7b-8c9d0e1f2a3b");

    private static final int EXPLOSION_COOLDOWN_SECONDS = 60;
    private static final int FLIGHT_COOLDOWN_SECONDS = 300;
    private static final int TIMESTOP_COOLDOWN_SECONDS = 60;
    
    private static final int FLIGHT_DURATION_TICKS = 1200;    // 60 секунд
    private static final int TIMESTOP_DURATION_TICKS = 200;   // 10 секунд
    
    private static final double EXPLOSION_RANGE = 100.0D;
    private static final double EXPLOSION_RADIUS = 4.0D;
    
    public VoidAccelerator(Properties properties) {
        super(properties
            .tab(net.minecraft.world.item.CreativeModeTab.TAB_COMBAT)
            .fireResistant()
            .rarity(Rarity.EPIC)
            .durability(2000)
            .setNoRepair());
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!world.isClientSide) {
            useCurrentAbility(world, player, stack, hand);
        }
        if (world.isClientSide) {
            spawnUseParticles(world, player, stack);
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    public void cycleMode(ItemStack stack, Player player) {
        int currentMode = getMode(stack);
        int newMode = (currentMode + 1) % 3;
        setMode(stack, newMode);
        displayModeMessage(player, stack);
        if (player.level.isClientSide) {
            spawnModeSwitchParticles(player.level, player, stack);
        }
    }
    private boolean isAbilityOnCooldown(Level world, ItemStack stack, int abilityMode) {
        if (world == null) {
            return false;
        }
        if (world.isClientSide) {
            return false;
        }
        
        long currentTime = world.getGameTime();
        long lastUseTime = getLastUseTime(stack, abilityMode);
        
        if (lastUseTime == 0) return false;
        
        long elapsedTicks = currentTime - lastUseTime;
        long requiredTicks = getCooldownTicks(abilityMode);
        
        return elapsedTicks < requiredTicks;
    }

    private long getLastUseTime(ItemStack stack, int abilityMode) {
        CompoundTag tag = stack.getOrCreateTag();
        switch (abilityMode) {
            case 0:
                return tag.contains(TAG_LAST_EXPLOSION_TIME) ? tag.getLong(TAG_LAST_EXPLOSION_TIME) : 0;
            case 1:
                return tag.contains(TAG_LAST_FLIGHT_TIME) ? tag.getLong(TAG_LAST_FLIGHT_TIME) : 0;
            case 2:
                return tag.contains(TAG_LAST_TIMESTOP_TIME) ? tag.getLong(TAG_LAST_TIMESTOP_TIME) : 0;
            default:
                return 0;
        }
    }

    private void setAbilityLastUseTime(Level world, ItemStack stack, int abilityMode) {
        CompoundTag tag = stack.getOrCreateTag();
        long currentTime = world.getGameTime();
        
        switch (abilityMode) {
            case 0:
                tag.putLong(TAG_LAST_EXPLOSION_TIME, currentTime);
                break;
            case 1:
                tag.putLong(TAG_LAST_FLIGHT_TIME, currentTime);
                break;
            case 2:
                tag.putLong(TAG_LAST_TIMESTOP_TIME, currentTime);
                break;
        }
        stack.setTag(tag);
    }
    
    private long getCooldownTicks(int abilityMode) {
        switch (abilityMode) {
            case 0: return EXPLOSION_COOLDOWN_SECONDS * 20L;
            case 1: return FLIGHT_COOLDOWN_SECONDS * 20L;
            case 2: return TIMESTOP_COOLDOWN_SECONDS * 20L;
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
    
    private String getCooldownTimeString(int abilityMode) {
        switch (abilityMode) {
            case 0: return "1 минута";
            case 1: return "5 минут";
            case 2: return "1 минута";
            default: return "";
        }
    }
    
    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
        super.onCraftedBy(stack, world, player);
        if (!stack.hasTag() || !stack.getTag().contains(TAG_MODE)) {
            setMode(stack, 0);
        }
    }
    
    @Override
    public int getEnchantmentValue() {
        return 30;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Level world, List<net.minecraft.network.chat.Component> tooltip, 
                               net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        
        int mode = getMode(stack);
        String modeName = "";
        switch (mode) {
            case 0: modeName = "Взрыв Пустоты"; break;
            case 1: modeName = "Полет Тьмы"; break;
            case 2: modeName = "Остановка Времени"; break;
            default: modeName = "Неизвестно"; break;
        }
        
        tooltip.add(new TextComponent("Режим: ").withStyle(ChatFormatting.GRAY)
            .append(new TextComponent(modeName).withStyle(ChatFormatting.DARK_PURPLE)));
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("ПКМ: Использовать способность").withStyle(ChatFormatting.GOLD));
        tooltip.add(new TextComponent("Клавиша T: Сменить режим").withStyle(ChatFormatting.YELLOW));
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("Способности:").withStyle(ChatFormatting.LIGHT_PURPLE));
        boolean canCheckCooldown = world != null && !world.isClientSide;
        
        boolean explosionOnCooldown = canCheckCooldown && isAbilityOnCooldown(world, stack, 0);
        int explosionSeconds = canCheckCooldown ? getRemainingCooldownSeconds(world, stack, 0) : 0;
        ChatFormatting explosionColor = explosionOnCooldown ? ChatFormatting.RED : ChatFormatting.GRAY;
        
        boolean flightOnCooldown = canCheckCooldown && isAbilityOnCooldown(world, stack, 1);
        int flightSeconds = canCheckCooldown ? getRemainingCooldownSeconds(world, stack, 1) : 0;
        ChatFormatting flightColor = flightOnCooldown ? ChatFormatting.RED : ChatFormatting.GRAY;
        
        boolean timestopOnCooldown = canCheckCooldown && isAbilityOnCooldown(world, stack, 2);
        int timestopSeconds = canCheckCooldown ? getRemainingCooldownSeconds(world, stack, 2) : 0;
        ChatFormatting timestopColor = timestopOnCooldown ? ChatFormatting.RED : ChatFormatting.GRAY;
        
        tooltip.add(new TextComponent("1. Взрыв Пустоты").withStyle(explosionColor));
        tooltip.add(new TextComponent("   Перезарядка: " + (explosionOnCooldown ? explosionSeconds + " сек" : "1 минута")).withStyle(ChatFormatting.DARK_GRAY));
        
        tooltip.add(new TextComponent("2. Полет Тьмы").withStyle(flightColor));
        tooltip.add(new TextComponent("   Перезарядка: " + (flightOnCooldown ? flightSeconds + " сек" : "5 минут")).withStyle(ChatFormatting.DARK_GRAY));
        
        tooltip.add(new TextComponent("3. Остановка Времени").withStyle(timestopColor));
        tooltip.add(new TextComponent("   Перезарядка: " + (timestopOnCooldown ? timestopSeconds + " сек" : "1 минута")).withStyle(ChatFormatting.DARK_GRAY));
    }
    
    private void displayModeMessage(Player player, ItemStack stack) {
        int mode = getMode(stack);
        String message = "";
        switch (mode) {
            case 0: message = "§5Выбран режим: Взрыв Пустоты"; break;
            case 1: message = "§5Выбран режим: Полет Тьмы"; break;
            case 2: message = "§5Выбран режим: Остановка Времени"; break;
            default: message = "§5Неизвестный режим"; break;
        }
        player.displayClientMessage(new TextComponent(message), true);
    }
    
    private void useCurrentAbility(Level world, Player player, ItemStack stack, InteractionHand hand) {
    int mode = getMode(stack);
    if (isAbilityOnCooldown(world, stack, mode)) {
        int seconds = getRemainingCooldownSeconds(world, stack, mode);
        String modeName = "";
        switch (mode) {
            case 0: modeName = "Взрыв Пустоты"; break;
            case 1: modeName = "Полет Тьмы"; break;
            case 2: modeName = "Остановка Времени"; break;
        }
        player.displayClientMessage(new TextComponent("§c" + modeName + " на перезарядке! Осталось: " + seconds + " сек"), true);
        return;
    }
    boolean success = false;
        switch (mode) {
            case 0: 
                useVoidExplosion(world, player);
                setAbilityLastUseTime(world, stack, 0);
                success = true;
                break;
            case 1: 
                if (useDarkFlight(world, player)) {
                    setAbilityLastUseTime(world, stack, 1);
                    success = true;
                }
                break;
            case 2: 
                useTimeStop(world, player);
                setAbilityLastUseTime(world, stack, 2);
                success = true;
                break;
        }
        if (success) {
            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
            world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 0.5F);
        }
    }
    
    private void useVoidExplosion(Level world, Player player) {
        HitResult hitResult = player.pick(EXPLOSION_RANGE, 0.0F, false);
        Vec3 lookPos = hitResult.getLocation();
        
        try {
            Explosion.BlockInteraction interaction = null;
            try {
                interaction = Explosion.BlockInteraction.valueOf("NONE");
            } catch (IllegalArgumentException e) {
                try {
                    interaction = Explosion.BlockInteraction.valueOf("KEEP");
                } catch (IllegalArgumentException e2) {
                    interaction = Explosion.BlockInteraction.valueOf("DESTROY");
                }
            }
            
            if (interaction != null) {
                world.explode(null, lookPos.x, lookPos.y, lookPos.z, 8.0F, false, interaction);
            } else {
                createCustomExplosion(world, player, lookPos);
            }
        } catch (Exception e) {
            createCustomExplosion(world, player, lookPos);
        }
        for (int i = 0; i < 50; i++) {
            world.addParticle(ParticleTypes.DRAGON_BREATH,
                lookPos.x + (world.random.nextDouble() - 0.5) * EXPLOSION_RADIUS * 2,
                lookPos.y + (world.random.nextDouble() - 0.5) * EXPLOSION_RADIUS * 2,
                lookPos.z + (world.random.nextDouble() - 0.5) * EXPLOSION_RADIUS * 2,
                (world.random.nextDouble() - 0.5) * 0.3,
                0.15,
                (world.random.nextDouble() - 0.5) * 0.3);
        }
        
        player.displayClientMessage(new TextComponent("§5Выпущен Взрыв Пустоты"), true);
    }
    
    private void createCustomExplosion(Level world, Player player, Vec3 pos) {
        AABB explosionArea = new AABB(
            pos.x - EXPLOSION_RADIUS, pos.y - EXPLOSION_RADIUS, pos.z - EXPLOSION_RADIUS,
            pos.x + EXPLOSION_RADIUS, pos.y + EXPLOSION_RADIUS, pos.z + EXPLOSION_RADIUS
        );
        
        List<Entity> entities = world.getEntities(player, explosionArea);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity && entity != player) {
                double distance = entity.distanceToSqr(pos.x, pos.y, pos.z);
                double maxDistance = EXPLOSION_RADIUS * EXPLOSION_RADIUS;
                if (distance < maxDistance) {
                    float damage = 15.0F * (1.0F - (float)(distance / maxDistance));
                    entity.hurt(net.minecraft.world.damagesource.DamageSource.explosion(player), damage);
                }
            }
        }
        
        world.playSound(null, pos.x, pos.y, pos.z, 
            SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 6.0F,
            (1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F) * 0.7F);
            
        if (world.isClientSide) {
            for (int i = 0; i < 150; ++i) {
                double d0 = world.random.nextDouble() * 2.0D - 1.0D;
                double d1 = world.random.nextDouble() * 2.0D - 1.0D;
                double d2 = world.random.nextDouble() * 2.0D - 1.0D;
                double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                d0 = d0 / d3 * 0.05D;
                d1 = d1 / d3 * 0.05D;
                d2 = d2 / d3 * 0.05D;
                
                world.addParticle(ParticleTypes.EXPLOSION,
                    pos.x, pos.y, pos.z,
                    d0 * 15.0D, d1 * 15.0D, d2 * 15.0D);
            }
        }
        
        for (int i = 0; i < 100; ++i) {
            world.addParticle(ParticleTypes.SMOKE,
                pos.x + (world.random.nextDouble() - 0.5) * EXPLOSION_RADIUS * 2,
                pos.y + (world.random.nextDouble() - 0.5) * EXPLOSION_RADIUS * 2,
                pos.z + (world.random.nextDouble() - 0.5) * EXPLOSION_RADIUS * 2,
                (world.random.nextDouble() - 0.5) * 0.3,
                0.15,
                (world.random.nextDouble() - 0.5) * 0.3);
        }
    }
    
   private boolean useDarkFlight(Level world, Player player) {
        boolean hasFlight = player.getAbilities().mayfly;
        
        if (!hasFlight) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            
            AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                speedAttribute.addTransientModifier(new AttributeModifier(
                    FLIGHT_SPEED_MODIFIER_ID, "Void Flight Speed", 0.3,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            
            player.displayClientMessage(new TextComponent("§5Активирован Полет Тьмы"), true);
            String taskId = "flight_" + player.getUUID().toString();
            Runnable disableFlight = () -> {
                if (player.isAlive() && !player.getAbilities().instabuild) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                    
                    AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (attr != null) {
                        attr.removeModifier(FLIGHT_SPEED_MODIFIER_ID);
                    }
                    
                    player.displayClientMessage(new TextComponent("§7Полет Тьмы закончился"), true);
                }
            };
            TickHandler.scheduleTask(disableFlight, FLIGHT_DURATION_TICKS);
            
            return true;
        } else {
            player.displayClientMessage(new TextComponent("§cПолет уже активен!"), true);
            return false;
        }
    }

    private void useTimeStop(Level world, Player player) {
        AABB area = new AABB(
            player.getX() - 50, player.getY() - 50, player.getZ() - 50,
            player.getX() + 50, player.getY() + 50, player.getZ() + 50
        );
        
        List<Entity> entities = world.getEntities(player, area);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity && entity != player) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TIMESTOP_DURATION_TICKS, 6, false, false, true));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, TIMESTOP_DURATION_TICKS, 6, false, false, true));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, TIMESTOP_DURATION_TICKS, 6, false, false, true));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, TIMESTOP_DURATION_TICKS, 0, false, false, true));
                
                if (entity instanceof Mob mob) {
                    mob.setNoAi(true);
                    UUID mobId = mob.getUUID();
                    Runnable enableAi = () -> {
                        if (mob.isAlive()) {
                            mob.setNoAi(false);
                        }
                    };
                    TickHandler.scheduleTask(enableAi, 200);
                }
                
                for (int i = 0; i < 5; i++) {
                    world.addParticle(ParticleTypes.ENCHANT,
                        entity.getX() + (world.random.nextDouble() - 0.5) * entity.getBbWidth(),
                        entity.getY() + world.random.nextDouble() * entity.getBbHeight(),
                        entity.getZ() + (world.random.nextDouble() - 0.5) * entity.getBbWidth(),
                        0, 0.1, 0);
                }
            }
        }
        
        player.displayClientMessage(new TextComponent("§5Время остановлено"), true);
        
        for (int i = 0; i < 200; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = world.random.nextDouble() * 50;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + world.random.nextDouble() * 30;
            
            world.addParticle(ParticleTypes.PORTAL,
                x, y, z,
                (world.random.nextDouble() - 0.5) * 0.1,
                -0.05,
                (world.random.nextDouble() - 0.5) * 0.1);
        }
    }
    
    private void spawnUseParticles(Level world, Player player, ItemStack stack) {
        int mode = getMode(stack);
        
        for (int i = 0; i < 10; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = 0.5 + world.random.nextDouble() * 0.3;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 1.0 + world.random.nextDouble() * 0.5;
            switch (mode) {
                case 0: world.addParticle(ParticleTypes.DRAGON_BREATH, x, y, z, 0, 0.02, 0); break;
                case 1: world.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.02, 0); break;
                case 2: world.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.02, 0); break;
            }
        }
    }
    
    private void spawnModeSwitchParticles(Level world, Player player, ItemStack stack) {
        int mode = getMode(stack);
        
        for (int i = 0; i < 25; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = 1.0;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 1.5 + world.random.nextDouble() * 1.0;
            switch (mode) {
                case 0: 
                    world.addParticle(ParticleTypes.DRAGON_BREATH, x, y, z, 0, 0.05, 0); 
                    world.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.03, 0);
                    break;
                case 1: 
                    world.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.05, 0); 
                    world.addParticle(ParticleTypes.WITCH, x, y, z, 0, 0.03, 0);
                    break;
                case 2: 
                    world.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.05, 0); 
                    world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.03, 0);
                    break;
            }
        }
        world.playSound(player, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5F, 1.5F);
    }
    
    private int getMode(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt(TAG_MODE);
    }
    
    private void setMode(ItemStack stack, int mode) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_MODE, mode);
    }
}