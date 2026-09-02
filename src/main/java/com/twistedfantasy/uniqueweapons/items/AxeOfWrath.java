package com.twistedfantasy.uniqueweapons.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import com.twistedfantasy.uniqueweapons.entity.WrathSummonedSkeleton;
import com.twistedfantasy.uniqueweapons.entity.WrathSummonedZombie;
import com.twistedfantasy.uniqueweapons.UniqueWeapons;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;

public class AxeOfWrath extends SwordItem {
    private static final float CUSTOM_BASE_DAMAGE = 13.0f;
    private static final float CUSTOM_ATTACK_SPEED = 0.5f;
    
    public AxeOfWrath(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }
    
    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return true;
    }
    
    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }
    
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.category == EnchantmentCategory.WEAPON 
                && enchantment != Enchantments.KNOCKBACK;
    }

    @Override
    public boolean isFireResistant() {
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
    }
    
private void summonWrathMinions(Player player, Level level) {
    if (!level.isClientSide) {     
        try {
            net.minecraft.world.entity.EntityType<WrathSummonedZombie> zombieType = 
                UniqueWeapons.WRATH_ZOMBIE.get();
            net.minecraft.world.entity.EntityType<WrathSummonedSkeleton> skeletonType = 
                UniqueWeapons.WRATH_SKELETON.get();
            for (int i = 0; i < 2; i++) {
                WrathSummonedZombie zombie = zombieType.create(level);
                if (zombie != null) {
                    zombie.init(player);
                    double offsetX = player.getX() + (level.random.nextDouble() - 0.5) * 3.0;
                    double offsetZ = player.getZ() + (level.random.nextDouble() - 0.5) * 3.0;
                    zombie.moveTo(offsetX, player.getY(), offsetZ, 
                        player.getYRot(), player.getXRot());
                    for(int j = 0; j < 8; j++) {
                        level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                            offsetX, player.getY() + 0.5, offsetZ,
                            (level.random.nextDouble() - 0.5) * 0.1,
                            0.15,
                            (level.random.nextDouble() - 0.5) * 0.1);
                    }
                    
                    level.addFreshEntity(zombie);
                }
            }
            for (int i = 0; i < 2; i++) {
                WrathSummonedSkeleton skeleton = skeletonType.create(level);
                if (skeleton != null) {
                    skeleton.init(player);
                    double offsetX = player.getX() + (level.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = player.getZ() + (level.random.nextDouble() - 0.5) * 2.0;
                    skeleton.moveTo(offsetX, player.getY(), offsetZ, 
                        player.getYRot(), player.getXRot());
                    for(int j = 0; j < 6; j++) {
                        level.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                            offsetX, player.getY() + 0.5, offsetZ,
                            (level.random.nextDouble() - 0.5) * 0.15,
                            0.2,
                            (level.random.nextDouble() - 0.5) * 0.15);
                    }
                    level.addFreshEntity(skeleton);
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.EVOKER_PREPARE_SUMMON,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.8F);
            
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.9F);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 1, false, false));
            
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 100, 0, false, false));
            
        } catch (Exception e) {
            player.sendMessage(new TextComponent("§cОшибка при призыве воинов!"), player.getUUID());
        }
    } else {
        for (int i = 0; i < 25; i++) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                player.getX(), player.getY() + 1.0, player.getZ(),
                (level.random.nextDouble() - 0.5) * 0.6,
                0.25,
                (level.random.nextDouble() - 0.5) * 0.6);
        }
    }
}
    public void activateSummonAbility(Player player, Level level, ItemStack stack) {
        if (!level.isClientSide) {
            if (!player.getCooldowns().isOnCooldown(this)) {
                summonWrathMinions(player, level);
                player.getCooldowns().addCooldown(this, 2400);
                stack.hurtAndBreak(100, player, (p) -> {
                    p.broadcastBreakEvent(player.getUsedItemHand());
                });
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 300, 2, false, false));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.HUNGER, 200, 2, false, false));
            } else {
                float cooldownPercent = player.getCooldowns().getCooldownPercent(this, 0.0F);
                int secondsLeft = (int)(cooldownPercent * 120);
                if (secondsLeft < 1) secondsLeft = 1;
                player.displayClientMessage(new TextComponent(
                    String.format("§cПризыв доступен через §e%d§c секунд", secondsLeft)), true);
            }
        }
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int durability = stack.getMaxDamage() - stack.getDamageValue();
        int maxDurability = stack.getMaxDamage();
        float totalDamage = CUSTOM_BASE_DAMAGE + getTier().getAttackDamageBonus();
        
        tooltip.add(new TextComponent("═══════[ ")
            .append(new TextComponent("Топор Повелителя Ярости").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
            .append(new TextComponent(" ]═══════").withStyle(ChatFormatting.DARK_GRAY))
        );
        
        tooltip.add(new TextComponent(""));
        
        tooltip.add(createStatLine("✦ Урон:", String.format("%.1f", totalDamage), ChatFormatting.RED));
        tooltip.add(createStatLine("✦ Скорость:", String.format("%.1f", CUSTOM_ATTACK_SPEED), ChatFormatting.GREEN));
        tooltip.add(createStatLine("✦ Прочность:", durability + "/" + maxDurability, ChatFormatting.AQUA));
        
        tooltip.add(new TextComponent(""));
        
        tooltip.add(new TextComponent("⚡ ОСОБЕННОСТИ:").withStyle(ChatFormatting.YELLOW));
        tooltip.add(new TextComponent("  §8• §7Ломает щиты противника"));
        tooltip.add(new TextComponent("  §8• §7Нельзя зачаровать на §cОтталкивание"));
        tooltip.add(new TextComponent("  §8• §7Огнестойкий"));
        
        tooltip.add(new TextComponent(""));
        
        tooltip.add(new TextComponent("👻 ПРИЗЫВ:").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(new TextComponent("  §8• §7Нажмите §eR§7 для призыва армии"));
        tooltip.add(new TextComponent("  §8• §72 зомби и 2 скелета с копиями топора"));
        tooltip.add(new TextComponent("  §8• §7Длительность: §c60 секунд"));
        tooltip.add(new TextComponent("  §8• §7Кулдаун: §c2 минуты"));
        tooltip.add(new TextComponent("  §8• §7Тратит §c100§7 прочности"));
        
        tooltip.add(new TextComponent(""));
        
        tooltip.add(new TextComponent("⚠ ПРЕДУПРЕЖДЕНИЕ:").withStyle(ChatFormatting.RED));
        tooltip.add(new TextComponent("  §8• §7Замедляет владельца при держании в руке"));
        tooltip.add(new TextComponent("  §8• §7Эффекты действуют только при держании"));
        tooltip.add(new TextComponent("  §8• §7После призыва: §cЗамедление I§7 и §cГолод II"));
    }
    
    private Component createStatLine(String label, String value, ChatFormatting valueColor) {
        return new TextComponent(label).withStyle(ChatFormatting.GRAY)
            .append(new TextComponent(" " + value).withStyle(valueColor));
    }
}