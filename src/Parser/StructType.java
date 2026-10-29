package src.Parser;

import java.util.List;

/**
 * A user-declared struct: {@code struct Point { int32 x | int32 y }}.
 *
 * <p>LLVM sees it as {@code %Point = type { i32, i32 }}. A value of this
 * type is the whole aggregate, passed by value; a variable of this type
 * holds the aggregate in a stack slot.
 */
public final class StructType implements TypeRef {

    public record Field(String name, TypeRef type) { }

    private final String name;
    private final List<Field> fields;

    public StructType(final String name, final List<Field> fields) {
        this.name = name;
        this.fields = List.copyOf(fields);
    }

    public List<Field> fields() { return fields; }

    /** @return the index of {@code fieldName} in the aggregate, or -1 */
    public int indexOf(final String fieldName) {
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).name().equals(fieldName)) return i;
        }
        return -1;
    }

    public TypeRef fieldType(final String fieldName) {
        final int i = indexOf(fieldName);
        return i < 0 ? null : fields.get(i).type();
    }

    @Override public String zName() { return name; }
    @Override public String llvm() { return "%" + name; }
    @Override public String initial() { return "zeroinitializer"; }
    @Override public int bits() { return 0; }
    @Override public boolean isIntegral() { return false; }
    @Override public boolean isFloat() { return false; }
    @Override public boolean isSigned() { return false; }
    @Override public boolean isUnsigned() { return false; }
    @Override public boolean isBoolean() { return false; }
    @Override public boolean isString() { return false; }
    @Override public boolean isVoid() { return false; }
    @Override public boolean isPointer() { return false; }
    @Override public boolean isVararg() { return false; }
    @Override public boolean isStruct() { return true; }
    @Override public TypeRef pointerTo() { return ZType.PTR; }
    @Override public TypeRef elementType() { return null; }
    @Override public String literal(final long value) {
        throw new IllegalStateException("struct has no literal");
    }
    @Override public String literal(final double value) {
        throw new IllegalStateException("struct has no literal");
    }
}