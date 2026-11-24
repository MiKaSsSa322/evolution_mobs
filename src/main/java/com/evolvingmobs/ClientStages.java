package com.evolvingmobs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Стадии мобов на стороне клиента (id сущности -> стадия). */
public final class ClientStages {
    private static final Map<Integer, Integer> MAP = new ConcurrentHashMap<>();

    public static int get(int id) { return MAP.getOrDefault(id, 0); }
    public static void put(int id, int stage) { MAP.put(id, stage); }
    public static void remove(int id) { MAP.remove(id); }
    public static void clear() { MAP.clear(); }
}
