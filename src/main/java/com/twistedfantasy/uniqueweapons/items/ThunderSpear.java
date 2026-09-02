package com.twistedfantasy.uniqueweapons.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import java.util.Map;

public class ThunderSpear extends TridentItem {
    public ThunderSpear(Properties properties) {
        super(properties);
    }
    public static float getThrowingPredicate(ItemStack stack, LivingEntity entity, int seed) {
        if (entity instanceof Player player) {
            return player.getUseItem() == stack ? 1.0F : 0.0F;
        }
        return 0.0F;
    }
    
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int useDuration = this.getUseDuration(stack) - timeLeft;
            
            if (useDuration >= 10) {
                if (!level.isClientSide) {
                    stack.hurtAndBreak(1, player, (p) -> {
                        p.broadcastBreakEvent(entity.getUsedItemHand());
                    });
                    
                    ThunderSpearThrown thrownTrident = new ThunderSpearThrown(level, player, stack);
                    thrownTrident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
                    int loyaltyLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY, stack);
                    if (player.getAbilities().instabuild) {
                        thrownTrident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    } else {
                        thrownTrident.pickup = AbstractArrow.Pickup.ALLOWED;
                    }
                    
                    level.addFreshEntity(thrownTrident);
                    level.playSound(null, thrownTrident, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
                    if (!player.getAbilities().instabuild) {
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack slotStack = player.getInventory().getItem(i);
                            if (slotStack == stack) {
                                player.getInventory().setItem(i, ItemStack.EMPTY);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (isForbiddenEnchantment(enchantment)) {
            return false;
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }
    
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(book);
        
        for (Enchantment enchantment : enchantments.keySet()) {
            if (isForbiddenEnchantment(enchantment)) {
                return false;
            }
        }
        
        return super.isBookEnchantable(stack, book);
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }
    
    private boolean isForbiddenEnchantment(Enchantment enchantment) {
        return enchantment == Enchantments.CHANNELING || enchantment == Enchantments.RIPTIDE;
    }
}