package src.Parser;

import java.util.HashMap;
import java.util.Map;

/**
 * The structs the parser has seen so far, by name.
 *
 * <p>A struct is a global declaration, so a static registry is fine here.
 * {@link #clear()} is called at the start of every compilation, so one
 * test does not see another test's structs.
 */
public final class StructRegistry {

    private static final Map<String, StructType> STRUCTS = new HashMap<>();

    private StructRegistry() { }

    public static void register(final StructType type) {
        STRUCTS.put(type.zName(), type);
    }

    public static StructType lookup(final String name) {
        return STRUCTS.get(name);
    }

    public static boolean isStruct(final String name) {
        return STRUCTS.containsKey(name);
    }

    public static void clear() {
        STRUCTS.clear();
    }

    public static java.util.Collection<StructType> all() {
        return STRUCTS.values();
    }
}