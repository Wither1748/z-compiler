package src.Parser;

import java.util.HashMap;

public class Types {
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

    public static String getLlvmType(final String zType) {
        return types.getOrDefault(zType, "double");
    }

    public static String getDefaultValue(final String zType) {
        return defaultValues.getOrDefault(zType, "0.0");
    }
}