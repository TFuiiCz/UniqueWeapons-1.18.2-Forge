package com.twistedfantasy.uniqueweapons.items;

import net.minecraft.world.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;

public class ScytheSinner extends SwordItem {
    private static final String KILL_COUNT_TAG = "KillCount";
    private static final String BASE_DAMAGE_TAG = "BaseDamage";
    private static final int MAX_KILLS = 46;
    private static final float DAMAGE_BONUS_PER_KILL = 0.5f;
    private static final float INITIAL_BASE_DAMAGE = 7.0f;
    
    public ScytheSinner(Tier tier, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }
    public int getKillCount(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt(KILL_COUNT_TAG);
    }
    public void incrementKillCount(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        int currentKills = tag.getInt(KILL_COUNT_TAG);
        
        if (currentKills < MAX_KILLS) {
            int newKills = currentKills + 1;
            tag.putInt(KILL_COUNT_TAG, newKills);
            
            float currentBaseDamage = tag.getFloat(BASE_DAMAGE_TAG);
            if (currentBaseDamage == 0) {
                currentBaseDamage = INITIAL_BASE_DAMAGE;
            }
            
            float newBaseDamage = currentBaseDamage + DAMAGE_BONUS_PER_KILL;
            tag.putFloat(BASE_DAMAGE_TAG, newBaseDamage);
            
            stack.setTag(tag);
        }
    }
    
    public float getBaseDamage(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        float baseDamage = tag.getFloat(BASE_DAMAGE_TAG);
        if (baseDamage == 0) {
            return INITIAL_BASE_DAMAGE;
        }
        return baseDamage;
    }
    
    public float getTotalDamage(ItemStack stack) {
        return getBaseDamage(stack);
    }
    
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        float totalDamage = getTotalDamage(stack);
        target.hurt(DamageSource.mobAttack(attacker), totalDamage);
        stack.hurtAndBreak(1, attacker, (entity) -> {
            entity.broadcastBreakEvent(attacker.getUsedItemHand());
        });
        
        return true;
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        float totalDamage = getTotalDamage(stack);
        int durability = stack.getMaxDamage() - stack.getDamageValue();
        int maxDurability = stack.getMaxDamage();
        float bonusDamage = totalDamage - INITIAL_BASE_DAMAGE;
        tooltip.add(new TextComponent("═══════[ ")
            .append(new TextComponent("КОСА ГРЕШНИКА").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
            .append(new TextComponent(" ]═══════").withStyle(ChatFormatting.DARK_GRAY))
        );
        
        tooltip.add(new TextComponent(""));
        tooltip.add(createStatLine("✦ Урон:", String.format("%.1f", totalDamage), ChatFormatting.RED));
        tooltip.add(createStatLine("✦ Скорость:", String.format("%.1f", 1.6f), ChatFormatting.GREEN));
        tooltip.add(createStatLine("✦ Прочность:", durability + "/" + maxDurability, ChatFormatting.AQUA));
        
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("⚡ ОСОБЕННОСТИ:").withStyle(ChatFormatting.GOLD));
        tooltip.add(new TextComponent("  §8• §7Урон растет с каждым убийством"));
        tooltip.add(new TextComponent("  §8• §7+" + String.format("%.1f", DAMAGE_BONUS_PER_KILL) + " урона за цель"));
        
        tooltip.add(new TextComponent(""));
        if (bonusDamage > 0) {
            tooltip.add(new TextComponent("📈 ТЕКУЩИЙ БОНУС:")
                .withStyle(ChatFormatting.YELLOW));
            tooltip.add(new TextComponent("  §7Накопленный урон: §a+" + String.format("%.1f", bonusDamage)));
        }
    }
    private Component createStatLine(String label, String value, ChatFormatting valueColor) {
        return new TextComponent(label).withStyle(ChatFormatting.GRAY)
            .append(new TextComponent(" " + value).withStyle(valueColor));
    }
    
    @Override
    public void onCraftedBy(ItemStack stack, Level level, net.minecraft.world.entity.player.Player player) {
        super.onCraftedBy(stack, level, player);
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(BASE_DAMAGE_TAG)) {
            tag.putFloat(BASE_DAMAGE_TAG, INITIAL_BASE_DAMAGE);
            tag.putInt(KILL_COUNT_TAG, 0);
        }
    }
}