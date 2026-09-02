package com.twistedfantasy.uniqueweapons.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import com.twistedfantasy.uniqueweapons.UniqueWeapons;

import java.util.EnumSet;

public class WrathSummonedZombie extends Zombie {
    private Player summoner;
    private int despawnTimer = 1200;
    private int lastAttackTime = 0;
    
    public WrathSummonedZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }
    
    public void init(Player summoner) {
        this.summoner = summoner;
        setupSummonedMob();
    }
    
    private void setupSummonedMob() {
        ItemStack axe = new ItemStack(com.twistedfantasy.uniqueweapons.UniqueWeapons.AXE_OF_WRATH.get());
        this.setItemSlot(EquipmentSlot.MAINHAND, axe);
        
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        
        this.setHealth(this.getMaxHealth());
        
        if (!this.level.isClientSide) {
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 1200, 1, false, false));
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 1200, 1, false, false));
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 1200, 0, false, false));
        }
        
        this.setTarget(null);
        
        this.targetSelector.removeAllGoals();
        this.goalSelector.removeAllGoals();
        
        this.goalSelector.addGoal(1, new FollowSummonerGoal(this, 1.0D, 10.0F, 2.0F));
        
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (summoner != null && summoner.getLastHurtByMob() != null && 
                    summoner.getLastHurtByMob() != summoner &&
                    !(summoner.getLastHurtByMob() instanceof WrathSummonedZombie) &&
                    !(summoner.getLastHurtByMob() instanceof WrathSummonedSkeleton)) {
                    return true;
                }
                if (this.mob.getLastHurtByMob() != null && 
                    this.mob.getLastHurtByMob() != summoner &&
                    !(this.mob.getLastHurtByMob() instanceof WrathSummonedZombie) &&
                    !(this.mob.getLastHurtByMob() instanceof WrathSummonedSkeleton)) {
                    return true;
                }
                return false;
            }
            
            @Override
            public void start() {
                if (summoner != null && summoner.getLastHurtByMob() != null) {
                    this.mob.setTarget(summoner.getLastHurtByMob());
                }
                super.start();
            }
        });
        
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.LivingEntity.class, 10, true, false, 
            (entity) -> {
                if (summoner != null && summoner.getLastHurtMob() == entity && entity != summoner) {
                    return true;
                }
                if (entity instanceof net.minecraft.world.entity.monster.Monster &&
                    !(entity instanceof WrathSummonedZombie) &&
                    !(entity instanceof WrathSummonedSkeleton) &&
                    entity != summoner) {
                    return true;
                }
                if (entity instanceof Player && entity != summoner) {
                    if (summoner != null && summoner.getLastHurtMob() == entity) {
                        return true;
                    }
                }
                return false;
            }));
    }
    
    public Player getSummoner() {
        return summoner;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level.isClientSide) {
            despawnTimer--;
            
            if (despawnTimer <= 100 && this.tickCount % 10 == 0) {
                this.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D),
                    0.0D, 0.05D, 0.0D);
            }
            
            if (despawnTimer <= 0) {
                if (this.isAlive()) {
                    for(int i = 0; i < 5; ++i) {
                        this.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D),
                            0.0D, 0.1D, 0.0D);
                    }
                    this.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
                return;
            }
            
            if (summoner != null && summoner.isAlive()) {
                if (this.distanceToSqr(summoner) > 900.0D) {
                    this.getNavigation().moveTo(summoner, 1.2D);
                }
                
                if (summoner.getLastHurtMob() != null && 
                    summoner.getLastHurtMob().isAlive() &&
                    summoner.getLastHurtMob() != summoner &&
                    this.tickCount - lastAttackTime > 20) {
                    
                    net.minecraft.world.entity.LivingEntity playerTarget = summoner.getLastHurtMob();
                    if (!(playerTarget instanceof WrathSummonedZombie) && 
                        !(playerTarget instanceof WrathSummonedSkeleton)) {
                        
                        this.setTarget(playerTarget);
                        lastAttackTime = this.tickCount;
                    }
                }
                
                if (summoner.getLastHurtByMob() != null && 
                    summoner.getLastHurtByMob().isAlive() &&
                    summoner.getLastHurtByMob() != summoner) {
                    
                    net.minecraft.world.entity.LivingEntity attacker = summoner.getLastHurtByMob();
                    if (!(attacker instanceof WrathSummonedZombie) && 
                        !(attacker instanceof WrathSummonedSkeleton)) {
                        
                        this.setTarget(attacker);
                    }
                }
                
                if (this.distanceToSqr(summoner) > 2500.0D && this.tickCount % 100 == 0) {
                    this.teleportTo(summoner.getX(), summoner.getY(), summoner.getZ());
                }
            }
            
            if (this.getTarget() != null && 
                (!this.getTarget().isAlive() || this.getTarget() == summoner ||
                 this.getTarget() instanceof WrathSummonedZombie ||
                 this.getTarget() instanceof WrathSummonedSkeleton)) {
                this.setTarget(null);
            }
        }
    }
    
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getEntity() == summoner) {
            return false;
        }
        
        if (source.getEntity() instanceof WrathSummonedZombie || 
            source.getEntity() instanceof WrathSummonedSkeleton) {
            return false;
        }
        
        if (source.getEntity() != null) {
            amount *= 0.4f;
        }
        
        return super.hurt(source, amount);
    }
    
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        if (entity == summoner) {
            return false;
        }
        
        if (entity instanceof WrathSummonedZombie || entity instanceof WrathSummonedSkeleton) {
            return false;
        }
        
        lastAttackTime = this.tickCount;
        
        return super.doHurtTarget(entity);
    }
    
    @Override
    public boolean canAttackType(net.minecraft.world.entity.EntityType<?> type) {
        if (type == UniqueWeapons.WRATH_ZOMBIE.get() || type == UniqueWeapons.WRATH_SKELETON.get()) {
            return false;
        }
        return super.canAttackType(type);
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DespawnTimer", despawnTimer);
        tag.putInt("LastAttackTime", lastAttackTime);
        if (summoner != null) {
            tag.putUUID("SummonerUUID", summoner.getUUID());
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("DespawnTimer")) {
            despawnTimer = tag.getInt("DespawnTimer");
        }
        if (tag.contains("LastAttackTime")) {
            lastAttackTime = tag.getInt("LastAttackTime");
        }
        if (tag.hasUUID("SummonerUUID") && this.level != null) {
            if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.minecraft.world.entity.Entity entity = serverLevel.getEntity(tag.getUUID("SummonerUUID"));
                if (entity instanceof Player) {
                    summoner = (Player) entity;
                }
            }
        }
    }
    
    @Override
    public boolean isSunSensitive() {
        return false;
    }
    
    @Override
    public boolean isCustomNameVisible() {
        return false;
    }
    
    private class FollowSummonerGoal extends Goal {
        private final WrathSummonedZombie mob;
        private final double speedModifier;
        private final float startDistance;
        private final float stopDistance;
        private int timeToRecalcPath;
        
        public FollowSummonerGoal(WrathSummonedZombie mob, double speedModifier, float startDistance, float stopDistance) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            Player summoner = mob.getSummoner();
            if (summoner == null || !summoner.isAlive()) {
                return false;
            }
            return mob.distanceToSqr(summoner) >= (double)(startDistance * startDistance);
        }
        
        @Override
        public boolean canContinueToUse() {
            Player summoner = mob.getSummoner();
            if (summoner == null || !summoner.isAlive()) {
                return false;
            }
            return !mob.getNavigation().isDone() && mob.distanceToSqr(summoner) > (double)(stopDistance * stopDistance);
        }
        
        @Override
        public void start() {
            this.timeToRecalcPath = 0;
        }
        
        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
        
        @Override
        public void tick() {
            Player summoner = mob.getSummoner();
            if (summoner != null && summoner.isAlive()) {
                mob.getLookControl().setLookAt(summoner, 10.0F, mob.getMaxHeadXRot());
                if (--timeToRecalcPath <= 0) {
                    timeToRecalcPath = 10;
                    mob.getNavigation().moveTo(summoner, speedModifier);
                }
            }
        }
    }
}