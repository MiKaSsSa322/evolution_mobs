package com.evolvingmobs.ai;

import com.evolvingmobs.Config;
import com.evolvingmobs.Evo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Стадия 2+: моб строит мосты и столбы, чтобы добраться до цели. */
public class EvoBuilderGoal extends Goal {
    private final Mob mob;
    private int cooldown;

    public EvoBuilderGoal(Mob mob) { this.mob = mob; }

    @Override
    public boolean canUse() {
        if (Evo.getStage(mob) < 2 || !Config.PLACE_BLOCKS.get()) return false;
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive()
                && mob.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void tick() {
        if (--cooldown > 0) return;
        cooldown = 4;
        LivingEntity t = mob.getTarget();
        if (t == null) return;
        Level lv = mob.level();

        double dx = t.getX() - mob.getX(), dz = t.getZ() - mob.getZ(), dy = t.getY() - mob.getY();
        double dist = Math.sqrt(dx * dx + dz * dz);

        // Цель выше — строим столб под собой: прыжок, и на вершине ставим блок.
        if (dy >= 1.8 && dist < 6) {
            if (mob.onGround()) mob.getJumpControl().jump();
            else if (mob.getDeltaMovement().y <= 0.05) place(lv, mob.blockPosition().below());
            return;
        }

        // Пути нет (обрыв/яма) — достраиваем мост в сторону цели.
        if (dist > 2 && dist < 24 && mob.onGround() && mob.getNavigation().isDone()) {
            Direction d = Direction.getNearest(dx, 0, dz);
            BlockPos front = mob.blockPosition().relative(d);
            if (lv.getBlockState(front).isAir() && canReplace(lv.getBlockState(front.below()))) {
                place(lv, front.below());
                mob.getMoveControl().setWantedPosition(front.getX() + 0.5, mob.getY(), front.getZ() + 0.5, 1.1);
            }
        }
    }

    private static boolean canReplace(BlockState s) { return s.isAir() || s.canBeReplaced(); }

    private void place(Level lv, BlockPos pos) {
        if (!canReplace(lv.getBlockState(pos))) return;
        lv.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
        lv.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1f, 0.9f);
        mob.swing(InteractionHand.MAIN_HAND);
        TempBlocks.add(lv, pos);
    }
}
