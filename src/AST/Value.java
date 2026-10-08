package src.AST;

import src.Parser.ZType;

public record Value(String text, ZType type) {

    /** @return true when this is a register rather than a literal */
    public boolean isRegister() {
        return text.startsWith("%");
    }
}
