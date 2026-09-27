package src.Codegen;

public class IRBuilder {
    private int registerCount = 1;
    private StringBuilder irCode = new StringBuilder();

    public String nextRegister() {
        return "%" + (registerCount++);
    }

    public void appendLine(String line) {
        irCode.append(" ").append(line).append("\n");
    }

    public String getIR() {
        return irCode.toString();
    }

    public void reset() {
        registerCount = 1;
        irCode.setLength(0);
    }
}
