package src.AST;

import src.Parser.TypeRef;

public record Value(String text, TypeRef type) {

    /** @return true when this is a register rather than a literal */
    public boolean isRegister() {
        return text.startsWith("%");
    }
}