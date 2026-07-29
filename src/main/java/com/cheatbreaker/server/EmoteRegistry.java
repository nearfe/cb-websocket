package com.cheatbreaker.server;

import java.util.*;

/**
 * Maps emote names to their integer IDs, matching the client's EmoteManager BiMap:
 *   0 = wave, 1 = handsup, 2 = floss, 3 = dab, 4 = tpose,
 *   5 = shrug, 6 = facepalm, 7 = narutorun, 8 = superfacepalm
 */
public class EmoteRegistry {

    private static final Map<String, Integer> NAME_TO_ID = new LinkedHashMap<>();
    private static final Map<Integer, String> ID_TO_NAME = new LinkedHashMap<>();

    static {
        register(0, "wave");
        register(1, "handsup", "hands_up", "hands-up");
        register(2, "floss");
        register(3, "dab");
        register(4, "tpose", "t_pose", "t-pose");
        register(5, "shrug");
        register(6, "facepalm");
        register(7, "narutorun", "naruto_run", "naruto-run", "naruto");
        register(8, "superfacepalm", "super_facepalm", "super-facepalm");
    }

    private static void register(int id, String... aliases) {
        ID_TO_NAME.put(id, aliases[0]);
        for (String alias : aliases) {
            NAME_TO_ID.put(alias.toLowerCase(), id);
        }
    }

    public static Integer getId(String name) {
        return NAME_TO_ID.get(name.toLowerCase().trim());
    }

    public static String getName(int id) {
        return ID_TO_NAME.getOrDefault(id, "unknown_" + id);
    }

    public static Map<Integer, String> getAll() {
        return Collections.unmodifiableMap(ID_TO_NAME);
    }

    public static boolean isValid(String name) {
        return NAME_TO_ID.containsKey(name.toLowerCase().trim());
    }

    public static String listAll() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> entry : ID_TO_NAME.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
