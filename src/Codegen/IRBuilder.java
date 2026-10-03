package src.Codegen;

import java.util.HashMap;
import java.util.Map;

/**
 * Class with helper methods to generate LLVM IR code using testual strings
 */
public class IRBuilder {
    /**
     * registerCount stores the next available register (es. %1)
     * irCode is the StringBuilder for the code
     * registerTypes stores the LLVM type of each register
     */
    private int registerCount = 1;
    private final StringBuilder irCode = new StringBuilder();
    private final Map<String, String> registerTypes = new HashMap<>();

    /**
     * @return a string with the next register (es. %2)
     */
    public final String nextRegister() {
        return "%" + (registerCount++);
    }

    /**
     * appends a line to the string builder and adds \n
     * @param line the String to append
     */
    public void appendLine(String line) {
        irCode.append(" ").append(line).append("\n");
    }

    /**
     * @return the IR code in a string form
     */
    public final String getIR() {
        return irCode.toString();
    }

    @Override
    /**
     * return the String form of the IR
     */
    public final String toString() {
        return getIR();
    }

    /**
     * reset the count of the registers (es. when entering a new function)
     */
    public void reset() {
        registerCount = 1;
        irCode.setLength(0);
        registerTypes.clear();
    }

    /**
     * emitAlloca emits an allocation instruction for a variable
     * @param varName name of the ptr to use
     * @param llvmType LLVM type of the variable (es. int32 becomes i32)
     * @return a String with the IR code
     */
    public final String emitAlloca(final String varName, final String llvmType) {
        final String ptr = "%" + varName;
        appendLine(ptr + " = alloca " + llvmType);
        return ptr;
    }

    /**
     * emitStore emits the IR code to store a value in a variable
     * @param val the value to store
     * @param llvmType the llvmtype
     * @param ptr the name of the ptr
     */
    public void emitStore(final String val, final String llvmType, final String ptr) {
        appendLine("store " + llvmType + " " + val + ", ptr " + ptr);
    }

    /**
     * emitLoad emits the Ir code to Load the value from a ptr
     * @param llvmType llvmtype
     * @param ptr the name of the ptr
     * @return the register in which the value was loaded
     */
    public final String emitLoad(final String llvmType, final String ptr) {
        final String reg = nextRegister();
        appendLine(reg + " = load " + llvmType + ", ptr " + ptr);
        registerTypes.put(reg, llvmType);
        return reg;
    }

    /**
     * Gets the LLVM type of a register
     * @param reg the register name (e.g., "%1")
     * @return the LLVM type or null if not tracked
     */
    public final String getRegisterType(final String reg) {
        return registerTypes.get(reg);
    }

    /**
     * nextLabel emits the next available label
     * @param prefix the prefix of the label
     * @return the label
     */
    private int labelCount = 0;
    public final String nextLabel(final String prefix) {
        return prefix + "." + (labelCount++);
    }


    /**
     * setter method for registerTypes
     * @param reg name of the register
     * @param type type to set
     */
    public void setRegisterType(final String reg, final String type) {
        registerTypes.put(reg, type);
    }
}
