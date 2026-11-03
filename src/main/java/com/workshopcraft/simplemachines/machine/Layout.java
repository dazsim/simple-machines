package com.workshopcraft.simplemachines.machine;

/** Slot positions of a powered machine screen, in container slot order. big is the slot drawn as the large result slot. */
public record Layout(String name, int[][] slots, int arrowX, int arrowY, int big) {
    public static final Layout PROCESS = new Layout("process", new int[][]{{56, 35}, {116, 35}}, 79, 34, 1);
    public static final Layout ALLOY = new Layout("alloy_smelter", new int[][]{{38, 35}, {56, 35}, {116, 35}}, 79, 34, 2);
    public static final Layout BATTERY = new Layout("battery", new int[0][], -1, -1, -1);
    public static final Layout STONE = new Layout("stone_generator", new int[][]{{116, 35}}, 79, 34, 0);
    public static final Layout HIVE = new Layout("mechanical_hive", new int[][]{{56, 35}, {112, 35}, {130, 35}}, 79, 34, -1);
    public static final Layout LAVA = new Layout("lava_generator", new int[][]{{44, 35}, {128, 35}}, -1, -1, -1);
    public static final Layout GRID = new Layout("grid", grid(), -1, -1, -1);

    private static int[][] grid() {
        final int[][] s = new int[9][];
        for (int i = 0; i < 9; i++) s[i] = new int[]{62 + i % 3 * 18, 17 + i / 3 * 18};
        return s;
    }
}
