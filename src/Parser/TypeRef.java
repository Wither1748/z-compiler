package src.Parser;

/**
 * Anything a variable or expression can have as a type: a built-in
 * {@link ZType}, or a user-declared {@link StructType}.
 *
 * <p>{@code ZType} is an enum because every built-in type is known at
 * compile time. A struct is not: the user declares it, so it lives in a
 * {@link StructType} object instead. This interface is what the rest of
 * the compiler sees.
 */
public interface TypeRef {
    String zName();
    String llvm();
    String initial();
    int bits();

    boolean isIntegral();
    boolean isFloat();
    boolean isSigned();
    boolean isUnsigned();
    boolean isBoolean();
    boolean isString();
    boolean isVoid();
    boolean isPointer();
    boolean isVararg();

    /** @return true for a user-declared struct */
    boolean isStruct();

    TypeRef pointerTo();
    TypeRef elementType();

    String literal(long value);
    String literal(double value);
}