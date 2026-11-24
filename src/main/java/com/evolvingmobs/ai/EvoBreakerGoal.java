package com.evolvingmobs.ai;

import com.evolvingmobs.Config;
import com.evolvingmobs.Evo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Стадия 3: моб ломает блоки (твёрдость до 8), если застрял на пути к цели. */
public class EvoBreakerGoal extends Goal {
    private final Mob mob;
    private BlockPos pos;
    private int progress, needed;

    public EvoBreakerGoal(Mob mob) { this.mob = mob; }

    @Override
    public boolean canUse() {
        if (Evo.getStage(mob) < 3 || !Config.BREAK_BLOCKS.get()) return false;
        LivingEntity t = mob.getTarget();
        if (t == null || !t.isAlive() || mob.distanceToSqr(t) > 24 * 24) return false;
        if (!mob.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
        if (!(mob.getNavigation().isDone() || mob.getNavigation().isStuck())) return false;
        return find(t) != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = mob.getTarget();
        return Evo.getStage(mob) >= 3 && t != null && t.isAlive() && find(t) != null;
    }

    private BlockPos find(LivingEntity t) {
        Level lv = mob.level();
        Direction d = Direction.getNearest(t.getX() - mob.getX(), 0, t.getZ() - mob.getZ());
        BlockPos base = mob.blockPosition();
        int h = (int) Math.ceil(mob.getBbHeight());
        for (int y = 0; y < h; y++) {
            BlockPos p = base.relative(d).above(y);
            if (breakable(lv, p)) return p;
        }
        if (t.getY() - mob.getY() > 1.5 && breakable(lv, base.above(h))) return base.above(h);
        if (t.getY() - mob.getY() < -1.5 && breakable(lv, base.below())) return base.below();
        return null;
    }

    private static boolean breakable(Level lv, BlockPos p) {
        BlockState s = lv.getBlockState(p);
        if (s.isAir() || s.getCollisionShape(lv, p).isEmpty()) return false;
        float hard = s.getDestroySpeed(lv, p);
        return hard >= 0 && hard <= 8f;
    }

    @Override
    public void tick() {
        LivingEntity t = mob.getTarget();
        if (t == null) return;
        Level lv = mob.level();
        BlockPos p = find(t);
        if (p == null) { reset(); return; }
        if (!p.equals(pos)) {
            reset();
            pos = p;
            needed = (int) (lv.getBlockState(p).getDestroySpeed(lv, p) * 10) + 15;
        }
        progress++;
        mob.getLookControl().setLookAt(Vec3.atCenterOf(p));
        if (progress % 5 == 0) {
            mob.swing(InteractionHand.MAIN_HAND);
            lv.playSound(null, p, lv.getBlockState(p).getSoundType().getHitSound(), SoundSource.HOSTILE, 1f, 0.8f);
        }
        lv.destroyBlockProgress(mob.getId(), pos, Math.min(9, (int) (progress * 10f / needed)));
        if (progress >= needed) {
            lv.destroyBlock(pos, true, mob);
            reset();
        }
    }

    @Override
    public void stop() { reset(); }

    private void reset() {
        if (pos != null) mob.level().destroyBlockProgress(mob.getId(), pos, -1);
        pos = null;
        progress = 0;
    }
}
