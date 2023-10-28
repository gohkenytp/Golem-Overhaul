package tech.alexnijjar.golemoverhaul.common.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import tech.alexnijjar.golemoverhaul.common.constants.ConstantAnimations;
import tech.alexnijjar.golemoverhaul.common.entities.base.BaseGolem;
import tech.alexnijjar.golemoverhaul.common.registry.ModSoundEvents;

public class CoalGolem extends BaseGolem {
    private static final EntityDataAccessor<Boolean> LIT = SynchedEntityData.defineId(CoalGolem.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SUMMONED = SynchedEntityData.defineId(CoalGolem.class, EntityDataSerializers.BOOLEAN);

    public CoalGolem(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        xpReward = 1;
        setMaxUpStep(0);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 8.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 8.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        super.registerControllers(controllerRegistrar);
        controllerRegistrar.add(new AnimationController<>(this, "death_controller", 20, state -> {
            if (deathTime == 0) return PlayState.STOP;
            state.getController().setAnimation(ConstantAnimations.DIE);
            return PlayState.CONTINUE;
        }));
    }

    public static @NotNull AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.ATTACK_DAMAGE, 2.0);
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(LIT, false);
        entityData.define(SUMMONED, false);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Lit", isLit());
        compound.putBoolean("Summoned", isSummoned());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setLit(compound.getBoolean("Lit"));
        setSummoned(compound.getBoolean("Summoned"));
    }

    @Override
    protected void dropAllDeathLoot(DamageSource damageSource) {
        if (isSummoned()) return;
        super.dropAllDeathLoot(damageSource);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new LeapAtTargetGoal(this, 0.4f));
    }

    @Override
    public int getDeathAnimationTicks() {
        return 13;
    }

    @Override
    public boolean hasCustomDeathAnimation() {
        return true;
    }

    @Override
    public boolean villageBound() {
        return false;
    }

    @Override
    public boolean doesSwingAttack() {
        return false;
    }

    @Override
    public Item getRepairItem() {
        return Items.COAL;
    }

    @Override
    public boolean shouldAttack(LivingEntity entity) {
        if (entity instanceof Creeper) return isLit();
        return super.shouldAttack(entity);
    }

    public boolean isLit() {
        return entityData.get(LIT);
    }

    public void setLit(boolean lit) {
        entityData.set(LIT, lit);
    }

    public boolean isSummoned() {
        return entityData.get(SUMMONED);
    }

    public void setSummoned(boolean summoned) {
        entityData.set(SUMMONED, summoned);
    }

    @Override
    public boolean hasAttackAnimation() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.COAL_GOLEM_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.COAL_GOLEM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.COAL_GOLEM_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        SoundType soundType = state.getSoundType();
        playSound(soundType.getStepSound(), soundType.getVolume() * 0.15f, soundType.getPitch());
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
    }

    @Override
    public void extinguishFire() {
        super.extinguishFire();
        if (isLit()) {
            setLit(false);
            playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE);
            for (int i = 0; i < 20; i++) {
                level().addParticle(ParticleTypes.LARGE_SMOKE,
                    getX() + random.nextGaussian() * 0.3,
                    getY() + 0.5 + random.nextGaussian() * 0.3,
                    getZ() + random.nextGaussian() * 0.3,
                    0, 0, 0);
            }
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (super.doHurtTarget(target)) {
            if (isLit()) {
                target.setSecondsOnFire(5);
                kill();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean fireImmune() {
        return super.fireImmune();
    }

    @Override
    public void lavaHurt() {
        super.lavaHurt();
        setLit(true);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) setLit(true);
        return super.hurt(source, amount);
    }

    @Override
    public double getAttributeValue(Attribute attribute) {
        if (attribute == Attributes.ATTACK_DAMAGE && isLit()) {
            return 12;
        }
        return super.getAttributeValue(attribute);
    }

    @Override
    public IronGolem.Crackiness getCrackiness() {
        return Crackiness.NONE;
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && tickCount > 2400 && isSummoned()) {
            kill();
        }
        super.tick();
    }

    @Override
    protected @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (stack.is(Items.FLINT_AND_STEEL) && !isLit()) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            playSound(SoundEvents.FLINTANDSTEEL_USE);
            setLit(true);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
}