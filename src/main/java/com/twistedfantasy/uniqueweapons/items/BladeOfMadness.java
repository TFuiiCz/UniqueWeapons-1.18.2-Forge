package com.twistedfantasy.uniqueweapons.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;

import java.util.Map;

public class BladeOfMadness extends SwordItem {
    
    public BladeOfMadness(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        
        if (result && !attacker.level.isClientSide) {
            if (!target.isAlive()) {
                applyStrengthEffect(attacker);
                attacker.level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), 
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.8F + attacker.getRandom().nextFloat() * 0.4F);
            }
        }
        
        return result;
    }

    private void applyStrengthEffect(LivingEntity attacker) {
        MobEffectInstance strengthEffect = new MobEffectInstance(
            MobEffects.DAMAGE_BOOST,
            300,
            4,
            false,
            true,
            true
        );
        
        attacker.addEffect(strengthEffect);
        MobEffectInstance regenerationEffect = new MobEffectInstance(
            MobEffects.REGENERATION,
            100,
            1,
            false,
            false,
            true
        );
        attacker.addEffect(regenerationEffect);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.MOB_LOOTING) {
            return false;
        }
        if (enchantment == Enchantments.FIRE_ASPECT) {
            return false;
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(book);
        
        for (Enchantment enchantment : enchantments.keySet()) {
            if (enchantment == Enchantments.MOB_LOOTING || enchantment == Enchantments.FIRE_ASPECT) {
                return false;
            }
        }
        
        return super.isBookEnchantable(stack, book);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}