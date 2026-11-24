package com.evolvingmobs.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Блоки, поставленные мобами, исчезают через 30 секунд. */
public final class TempBlocks {
    private record Entry(ServerLevel level, BlockPos pos, long expire) {}

    private static final List<Entry> LIST = new ArrayList<>();
    private static int counter = 0;

    public static void add(Level lv, BlockPos p) {
        if (lv instanceof ServerLevel sl) LIST.add(new Entry(sl, p.immutable(), sl.getGameTime() + 600));
    }

    public static void tick() {
        if (++counter % 20 != 0) return;
        Iterator<Entry> it = LIST.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.level.getGameTime() < e.expire) continue;
            it.remove();
            if (e.level.isLoaded(e.pos) && e.level.getBlockState(e.pos).is(Blocks.COBBLESTONE)) {
                e.level.destroyBlock(e.pos, false);
            }
        }
    }

    public static void clear() { LIST.clear(); }
}
