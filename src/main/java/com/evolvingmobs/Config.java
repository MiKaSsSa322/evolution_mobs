package com.evolvingmobs;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue DAY1, DAY2, DAY3;
    public static final ForgeConfigSpec.DoubleValue BONUS_CHANCE;
    public static final ForgeConfigSpec.BooleanValue PLACE_BLOCKS, BREAK_BLOCKS, KILL_EVOLVE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("evolution");
        DAY1 = b.comment("День мира, с которого мобы становятся Закалёнными (стадия 1)")
                .defineInRange("stage1Day", 5, 0, 1000000);
        DAY2 = b.comment("День мира, с которого мобы становятся Мутантами (стадия 2)")
                .defineInRange("stage2Day", 15, 0, 1000000);
        DAY3 = b.comment("День мира, с которого мобы становятся Выродками (стадия 3)")
                .defineInRange("stage3Day", 30, 0, 1000000);
        BONUS_CHANCE = b.comment("Шанс, что моб при спавне сразу будет на стадию сильнее")
                .defineInRange("bonusChance", 0.10, 0.0, 1.0);
        KILL_EVOLVE = b.comment("Моб, убивший игрока, эволюционирует на стадию выше")
                .define("evolveOnPlayerKill", true);
        b.pop();
        b.push("griefing");
        PLACE_BLOCKS = b.comment("Мобы стадии 2+ ставят блоки (мосты, столбы). Блоки исчезают через 30 сек")
                .define("allowBlockPlacing", true);
        BREAK_BLOCKS = b.comment("Мобы стадии 3 ломают блоки на пути к цели")
                .define("allowBlockBreaking", true);
        b.pop();
        SPEC = b.build();
    }
}
