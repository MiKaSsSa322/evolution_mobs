package com.evolvingmobs;

import com.evolvingmobs.ai.TempBlocks;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = EvolvingMobs.MODID)
public class EvoEvents {

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob mob) || !Evo.eligible(mob)) return;
        CompoundTag d = mob.getPersistentData();
        if (!d.getBoolean(Evo.INIT)) {
            d.putBoolean(Evo.INIT, true);
            if (mob.getRandom().nextDouble() < Config.BONUS_CHANCE.get()) d.putInt(Evo.BONUS, 1);
        }
        Evo.addGoals(mob);
        Evo.update(mob);
        mob.refreshDimensions();
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide()) ClientStages.remove(e.getEntity().getId());
    }

    @SubscribeEvent
    public static void onTrack(PlayerEvent.StartTracking e) {
        if (e.getEntity() instanceof ServerPlayer sp && Evo.eligible(e.getTarget())) {
            EvolvingMobs.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new StagePacket(e.getTarget().getId(), Evo.getStage(e.getTarget())));
        }
    }

    /** Хитбокс растёт вместе со стадией (и на сервере, и на клиенте). */
    @SubscribeEvent
    public static void onSize(EntityEvent.Size e) {
        Entity en = e.getEntity();
        if (en.level() == null || !Evo.eligible(en)) return;
        int s = Evo.getStage(en);
        if (s <= 0) return;
        e.setNewSize(e.getNewSize().scale(Evo.W[s], Evo.H[s]), true);
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent e) {
        if (!(e.getEntity() instanceof Mob mob) || mob.level().isClientSide || !Evo.eligible(mob)) return;
        int t = mob.tickCount + mob.getId();
        if (t % 200 == 0) Evo.update(mob); // со временем мир взрослеет -> мобы эволюционируют
        if (Evo.getStage(mob) >= 3 && mob.level() instanceof ServerLevel sl) {
            if (t % 4 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                        2, mob.getBbWidth() * 0.3, mob.getBbHeight() * 0.3, mob.getBbWidth() * 0.3, 0.01);
            }
            if (mob.getTarget() != null && mob.getRandom().nextInt(400) == 0) {
                mob.playSound(SoundEvents.ENDERMAN_SCREAM, 1.5f, 0.4f);
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        Entity attacker = e.getSource().getEntity();
        // Стрелы, огненные шары и т.п. тоже усиливаются
        if (attacker instanceof Mob m && Evo.eligible(m) && e.getSource().getDirectEntity() != attacker) {
            int s = Evo.getStage(m);
            if (s > 0) e.setAmount(e.getAmount() * Evo.damageMult(s));
        }
        // Стая: раненый моб стадии 2+ зовёт всех вокруг
        if (e.getEntity() instanceof Mob victim && Evo.eligible(victim) && Evo.getStage(victim) >= 2
                && attacker instanceof Player p && !p.isCreative() && !p.isSpectator()) {
            victim.level().getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(20),
                    m -> m != victim && m instanceof Enemy && m.getTarget() == null).forEach(m -> m.setTarget(p));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (Config.KILL_EVOLVE.get() && e.getEntity() instanceof Player
                && e.getSource().getEntity() instanceof Mob m && Evo.eligible(m)) {
            CompoundTag d = m.getPersistentData();
            d.putInt(Evo.BONUS, Math.min(3, d.getInt(Evo.BONUS) + 1));
            Evo.update(m);
        }
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent e) {
        if (Evo.eligible(e.getEntity())) {
            e.setDroppedExperience(e.getDroppedExperience() * (1 + Evo.getStage(e.getEntity())));
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        LivingEntity le = e.getEntity();
        if (Evo.eligible(le) && Evo.getStage(le) >= 3 && le.getRandom().nextFloat() < 0.3f) {
            e.getDrops().add(new ItemEntity(le.level(), le.getX(), le.getY(), le.getZ(), new ItemStack(Items.DIAMOND)));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) TempBlocks.tick();
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { TempBlocks.clear(); }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("mobevo").requires(s -> s.hasPermission(2))
                .then(Commands.literal("force")
                        .then(Commands.argument("stage", IntegerArgumentType.integer(-1, 3)).executes(ctx -> {
                            int s = IntegerArgumentType.getInteger(ctx, "stage");
                            Evo.forced = s;
                            int n = 0;
                            for (ServerLevel l : ctx.getSource().getServer().getAllLevels()) {
                                for (Entity en : l.getAllEntities()) {
                                    if (Evo.eligible(en)) { Evo.update((Mob) en); n++; }
                                }
                            }
                            final int count = n;
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    (s < 0 ? "Стадия: авто (по дням мира)" : "Стадия принудительно: " + s)
                                            + ". Затронуто мобов: " + count), true);
                            return count;
                        }))));
    }
}
