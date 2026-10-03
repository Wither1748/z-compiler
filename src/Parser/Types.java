package src.Parser;

import java.util.HashMap;

/**
 * Types helper class to map Z types to LLVM types and get default values
 */
public class Types {
    /**
     * types is an hashmap to store the correlation between Ztypes and LLVM types
     * defaultValues stores the correlation between Ztypes and their default values
     */
    protected static final HashMap<String, String> types = new HashMap<>();
    protected static final HashMap<String, String> defaultValues = new HashMap<>();

    static {
        // Z types to LLVM types
        types.put("int32", "i32");
        types.put("int64", "i64");
        types.put("flt32", "float");
        types.put("flt64", "double");
        types.put("01", "i1");

        // default types
        defaultValues.put("int32", "0");
        defaultValues.put("int64", "0");
        defaultValues.put("flt32", "0.0");
        defaultValues.put("flt64", "0.0");
        defaultValues.put("01", "0");
    }

    /**
     * Getter method for the Ztype
     * @param zType the Ztype
     * @return the corresponding LLVM type
     */
    public static String getLlvmType(final String zType) {
        return types.getOrDefault(zType, "double");
    }

    /**
     * getter method for a default value
     * @param zType with the ztype
     * @return the default value for that type
     */
    public static String getDefaultValue(final String zType) {
        return defaultValues.getOrDefault(zType, "0.0");
    }
}