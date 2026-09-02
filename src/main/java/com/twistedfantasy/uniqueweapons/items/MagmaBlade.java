package com.twistedfantasy.uniqueweapons.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;

public class MagmaBlade extends SwordItem {
    private static final float CUSTOM_BASE_DAMAGE = 6.0f;
    private static final float CUSTOM_ATTACK_SPEED = 2.3f;
    
    public MagmaBlade(Tier tier, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }
    
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        float totalDamage = CUSTOM_BASE_DAMAGE + getTier().getAttackDamageBonus();
        
        target.hurt(net.minecraft.world.damagesource.DamageSource.mobAttack(attacker), totalDamage);
        
        int fireTime = 15;
        target.setSecondsOnFire(fireTime);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        
        if (attacker.level.isClientSide) {
            spawnFireParticles(target);
        }
        stack.hurtAndBreak(1, attacker, (entity) -> {
            entity.broadcastBreakEvent(attacker.getUsedItemHand());
        });
        
        return true;
    }
    
    private void spawnFireParticles(LivingEntity target) {
    }
    
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        if (enchantment == net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT) {
            return false;
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }
    
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(book)
                .containsKey(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT)) {
            return false;
        }
        return super.isBookEnchantable(stack, book);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int durability = stack.getMaxDamage() - stack.getDamageValue();
        int maxDurability = stack.getMaxDamage();
        float totalDamage = CUSTOM_BASE_DAMAGE + getTier().getAttackDamageBonus();
        tooltip.add(new TextComponent("═══════[ ")
            .append(new TextComponent("МАГМОВЫЙ КЛИНОК").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(new TextComponent(" ]═══════").withStyle(ChatFormatting.DARK_GRAY))
        );
        tooltip.add(new TextComponent(""));
        tooltip.add(createStatLine("✦ Урон:", String.format("%.1f", totalDamage), ChatFormatting.RED));
        tooltip.add(createStatLine("✦ Скорость:", String.format("%.1f", CUSTOM_ATTACK_SPEED), ChatFormatting.GREEN));
        tooltip.add(createStatLine("✦ Прочность:", durability + "/" + maxDurability, ChatFormatting.AQUA));
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("🔥 ОСОБЕННОСТИ:").withStyle(ChatFormatting.YELLOW));
        tooltip.add(new TextComponent("  §8• §7Поджигает цель на §c15 секунд"));
        tooltip.add(new TextComponent("  §8• §7Замедления IV на §65 сек"));
        tooltip.add(new TextComponent("  §8• §7Слабости II на §65 сек"));
        
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("⚠ ОГРАНИЧЕНИЯ:").withStyle(ChatFormatting.RED));
        tooltip.add(new TextComponent("  §8• §7Несовместимо с §cЗачарованием Огня               "));
    }
    
    private Component createStatLine(String label, String value, ChatFormatting valueColor) {
        return new TextComponent(label).withStyle(ChatFormatting.GRAY)
            .append(new TextComponent(" " + value).withStyle(valueColor));
    }
}