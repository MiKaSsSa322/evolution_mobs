package com.evolvingmobs;

import com.evolvingmobs.ai.EvoBreakerGoal;
import com.evolvingmobs.ai.EvoBuilderGoal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Slime;
import net.minecraftforge.common.Tags;

import java.util.UUID;

public final class Evo {
    public static final String STAGE = "EvoStage", BONUS = "EvoBonus", INIT = "EvoInit", NAMED = "EvoNamed";

    /** -1 = авто (по дням), 0..3 = принудительная стадия (команда /mobevo force). */
    public static volatile int forced = -1;

    private static final UUID HP_ID = UUID.fromString("5c1b7a10-0001-4e11-8a11-aaaaaaaa0001");
    private static final UUID DMG_ID = UUID.fromString("5c1b7a10-0002-4e11-8a11-aaaaaaaa0002");
    private static final UUID SPD_ID = UUID.fromString("5c1b7a10-0003-4e11-8a11-aaaaaaaa0003");
    private static final UUID FOL_ID = UUID.fromString("5c1b7a10-0004-4e11-8a11-aaaaaaaa0004");
    private static final UUID KB_ID = UUID.fromString("5c1b7a10-0005-4e11-8a11-aaaaaaaa0005");

    //                                   0    1     2     3
    private static final double[] HP     = {0, 0.5, 1.5, 4.0};   // +50% / +150% / +400% здоровья
    private static final double[] DMG    = {0, 0.25, 0.75, 2.0}; // урон x1.25 / x1.75 / x3
    private static final double[] SPD    = {0, 0.0, 0.10, 0.20};
    private static final double[] FOLLOW = {0, 0.25, 0.5, 1.0};  // дальность обнаружения
    private static final double[] KB     = {0, 0.0, 0.3, 0.7};   // сопротивление отбрасыванию

    /** Размер хитбокса/модели: ширина и высота по стадиям. */
    public static final float[] W = {1f, 1.05f, 1.2f, 1.5f};
    public static final float[] H = {1f, 1.05f, 1.3f, 1.8f};

    public static boolean eligible(Entity e) {
        return e instanceof Mob && e instanceof Enemy && !(e instanceof Slime)
                && !e.getType().is(Tags.EntityTypes.BOSSES);
    }

    public static int getStage(Entity e) {
        return e.level().isClientSide ? ClientStages.get(e.getId()) : e.getPersistentData().getInt(STAGE);
    }

    public static float damageMult(int s) { return 1f + (float) DMG[s]; }

    public static int dayStage(ServerLevel l) {
        long day = l.getServer().overworld().getDayTime() / 24000L;
        if (day >= Config.DAY3.get()) return 3;
        if (day >= Config.DAY2.get()) return 2;
        if (day >= Config.DAY1.get()) return 1;
        return 0;
    }

    /** Пересчитать стадию моба (по дням мира + бонус). */
    public static void update(Mob mob) {
        if (!(mob.level() instanceof ServerLevel sl)) return;
        int bonus = mob.getPersistentData().getInt(BONUS);
        int s = forced >= 0 ? forced : Math.min(3, dayStage(sl) + bonus);
        if (s != mob.getPersistentData().getInt(STAGE)) setStage(mob, s);
    }

    public static void setStage(Mob mob, int s) {
        var data = mob.getPersistentData();
        int old = data.getInt(STAGE);
        data.putInt(STAGE, s);

        mod(mob, Attributes.MAX_HEALTH, HP_ID, HP[s], AttributeModifier.Operation.MULTIPLY_BASE);
        mod(mob, Attributes.ATTACK_DAMAGE, DMG_ID, DMG[s], AttributeModifier.Operation.MULTIPLY_BASE);
        mod(mob, Attributes.MOVEMENT_SPEED, SPD_ID, SPD[s], AttributeModifier.Operation.MULTIPLY_BASE);
        mod(mob, Attributes.FOLLOW_RANGE, FOL_ID, FOLLOW[s], AttributeModifier.Operation.MULTIPLY_BASE);
        mod(mob, Attributes.KNOCKBACK_RESISTANCE, KB_ID, KB[s], AttributeModifier.Operation.ADDITION);

        if (s > old) mob.setHealth(mob.getMaxHealth());
        if (s >= 1) mob.setCanPickUpLoot(true);

        if (mob.getNavigation() instanceof GroundPathNavigation nav) nav.setCanOpenDoors(s >= 1);

        // Стадия 2+: не горят на солнце. Стадия 3: регенерация.
        if (s >= 2) mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        else mob.removeEffect(MobEffects.FIRE_RESISTANCE);
        if (s >= 3) mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        else mob.removeEffect(MobEffects.REGENERATION);

        if (s >= 3 && mob.getCustomName() == null) {
            mob.setCustomName(Component.literal("Выродок: ").append(mob.getType().getDescription()).withStyle(ChatFormatting.DARK_RED));
            mob.setCustomNameVisible(true);
            data.putBoolean(NAMED, true);
        } else if (s < 3 && data.getBoolean(NAMED)) {
            mob.setCustomName(null);
            mob.setCustomNameVisible(false);
            data.putBoolean(NAMED, false);
        }

        mob.refreshDimensions();
        EvolvingMobs.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                new StagePacket(mob.getId(), s));
    }

    private static void mod(Mob mob, Attribute attr, UUID id, double amount, AttributeModifier.Operation op) {
        AttributeInstance in = mob.getAttribute(attr);
        if (in == null) return;
        in.removeModifier(id);
        if (amount != 0) in.addPermanentModifier(new AttributeModifier(id, "evolvingmobs", amount, op));
    }

    /** Добавляет «умные» цели. Каждая сама проверяет стадию, поэтому ставятся один раз при входе в мир. */
    public static void addGoals(Mob mob) {
        if (GoalUtils.hasGroundPathNavigation(mob)) {
            mob.goalSelector.addGoal(2, new OpenDoorGoal(mob, true) {
                @Override public boolean canUse() { return getStage(mob) >= 1 && super.canUse(); }
                @Override public boolean canContinueToUse() { return getStage(mob) >= 1 && super.canContinueToUse(); }
            });
            mob.goalSelector.addGoal(2, new BreakDoorGoal(mob, d -> true) {
                @Override public boolean canUse() { return getStage(mob) >= 2 && super.canUse(); }
            });
        }
        mob.goalSelector.addGoal(3, new EvoBuilderGoal(mob));
        mob.goalSelector.addGoal(3, new EvoBreakerGoal(mob));
    }
}
