package src.Codegen;

public class IRBuilder {
    private int registerCount = 1;
    private final StringBuilder irCode = new StringBuilder();

    public String nextRegister() {
        return "%" + (registerCount++);
    }

    public void appendLine(String line) {
        irCode.append(" ").append(line).append("\n");
    }

    public String getIR() {
        return irCode.toString();
    }

    @Override
    public String toString() {
        return getIR();
    }

    public void reset() {
        registerCount = 1;
        irCode.setLength(0);
    }

    public String emitAlloca(String varName, String llvmType) {
        String ptr = "%" + varName;
        appendLine(ptr + " = alloca " + llvmType);
        return ptr;
    }

    public void emitStore(String val, String llvmType, String ptr) {
        appendLine("store " + llvmType + " " + val + ", ptr " + ptr);
    }

    public String emitLoad(String llvmType, String ptr) {
        String reg = nextRegister();
        appendLine(reg + " = load " + llvmType + ", ptr " + ptr);
        return reg;
    }

    private int labelCount = 0;
    public String nextLabel(String prefix) {
        return prefix + "." + (labelCount++);
    }
}
