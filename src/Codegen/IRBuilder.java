package src.Codegen;

public class IRBuilder {
    private int registerCount = 1;
    private final StringBuilder irCode = new StringBuilder();

    public final String nextRegister() {
        return "%" + (registerCount++);
    }

    public void appendLine(String line) {
        irCode.append(" ").append(line).append("\n");
    }

    public final String getIR() {
        return irCode.toString();
    }

    @Override
    public final String toString() {
        return getIR();
    }

    public void reset() {
        registerCount = 1;
        irCode.setLength(0);
    }

    public final String emitAlloca(final String varName, final String llvmType) {
        final String ptr = "%" + varName;
        appendLine(ptr + " = alloca " + llvmType);
        return ptr;
    }

    public void emitStore(final String val, final String llvmType, final String ptr) {
        appendLine("store " + llvmType + " " + val + ", ptr " + ptr);
    }

    public final String emitLoad(final String llvmType, final String ptr) {
        final String reg = nextRegister();
        appendLine(reg + " = load " + llvmType + ", ptr " + ptr);
        return reg;
    }

    private int labelCount = 0;
    public final String nextLabel(final String prefix) {
        return prefix + "." + (labelCount++);
    }
}
