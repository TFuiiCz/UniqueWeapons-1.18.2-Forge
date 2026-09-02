package com.twistedfantasy.uniqueweapons.items;

import com.twistedfantasy.uniqueweapons.UniqueWeapons;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

public class ThunderSpearThrown extends ThrownTrident {
    private boolean lightningSpawned = false;
    private ItemStack tridentItem;
    private boolean dealtDamage;
    public int clientSideReturnTridentTickCount;

    public ThunderSpearThrown(EntityType<? extends ThunderSpearThrown> entityType, Level level) {
        super(entityType, level);
        this.tridentItem = new ItemStack(net.minecraft.world.item.Items.TRIDENT);
    }

    public ThunderSpearThrown(Level level, LivingEntity shooter, ItemStack stack) {
        super(UniqueWeapons.CUSTOM_TRIDENT.get(), level);
        this.setOwner(shooter);
        this.tridentItem = stack.copy();
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }
        Entity entity = this.getOwner();
        int loyaltyLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY, this.tridentItem);
        if (loyaltyLevel > 0 && (this.dealtDamage || this.isNoPhysics()) && entity != null) {
            if (!this.isAcceptibleReturnOwner()) {
                if (!this.level.isClientSide && this.pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED) {
                    this.spawnAtLocation(this.getPickupItem(), 0.1F);
                }
                this.discard();
            } else {
                this.setNoPhysics(true);
                Vec3 vec3 = entity.getEyePosition().subtract(this.position());
                this.setPosRaw(this.getX(), this.getY() + vec3.y * 0.015 * (double)loyaltyLevel, this.getZ());
                if (this.level.isClientSide) {
                    this.yOld = this.getY();
                }

                double d0 = 0.05 * (double)loyaltyLevel;
                this.setDeltaMovement(this.getDeltaMovement().scale(0.95).add(vec3.normalize().scale(d0)));
                if (this.clientSideReturnTridentTickCount == 0) {
                    this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
                }

                ++this.clientSideReturnTridentTickCount;
            }
        }

        super.tick();
    }

    private boolean isAcceptibleReturnOwner() {
        Entity entity = this.getOwner();
        if (entity != null && entity.isAlive()) {
            return !(entity instanceof ServerPlayer) || !entity.isSpectator();
        } else {
            return false;
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        this.dealtDamage = true;
        spawnLightning();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        spawnLightning();
    }
    private void spawnLightning() {
        if (!this.level.isClientSide && !lightningSpawned) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(this.level);
            if (lightning != null) {
                lightning.moveTo(this.getX(), this.getY(), this.getZ());
                lightning.setVisualOnly(false);
                this.level.addFreshEntity(lightning);
                lightningSpawned = true;
                
                this.level.playSound(null, this.getX(), this.getY(), this.getZ(), 
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
            }
        }
    }
    @Override
    protected ItemStack getPickupItem() {
        return this.tridentItem.copy();
    }
    @Override
    public void playerTouch(Player player) {
        if ((this.getOwner() != null && this.getOwner().getUUID().equals(player.getUUID())) || this.getOwner() == null) {
            super.playerTouch(player);
        }
    }
}