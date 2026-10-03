package src.Codegen;

import java.util.*;

public class SymbolTable {

    public static class SymbolInfo {
        private final String name;
        private final String llvmType;
        private final String pointerReg;
        private final boolean isConstant;
        private boolean isUsed;
        private int callCount;

        public SymbolInfo(String name, String llvmType, String pointerReg, boolean isConstant) {
            this.name = name;
            this.llvmType = llvmType;
            this.pointerReg = pointerReg;
            this.isConstant = isConstant;
            this.isUsed = false;
            this.callCount = 0;
        }

        public final String getName() { return name; }
        public final String getLlvmType() { return llvmType; }
        public final String getPointerReg() { return pointerReg; }
        public final boolean isConstant() { return isConstant; }
        public final boolean isUsed() { return isUsed; }
        public void setUsed(boolean used) { this.isUsed = used; }
        public final int getCallCount() { return callCount; }
        public void incrementCallCount() { this.callCount++; }
    }

    private final Deque<Map<String, SymbolInfo>> scopes = new ArrayDeque<>();

    public SymbolTable() {
        enterScope(); // initial global scope
    }

    /**
     * enters a new scope inside the parent
     */
    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    /**
     * exit the current scope to go back at the parent
     */
    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        }
    }

    /**
     * defines a symbol in the current scope
     * returns false if the symbol is already present
     */
    public final boolean define(final String name, final SymbolInfo info) {
        Map<String, SymbolInfo> current = scopes.peek();
        if (current == null || current.containsKey(name)) {
            return false;
        }
        current.put(name, info);
        return true;
    }

    /**
     * searches a symbol starting from the current scope and going up
     */
    public final SymbolInfo lookup(final String name) {
        for (Map<String, SymbolInfo> scope : scopes) {
            final SymbolInfo info = scope.get(name);
            if (info != null) {
                info.setUsed(true); // for optimization uses /warning
                return info;
            }
        }
        return null; // symbol not found
    }

    /**
     * searches for a symbol only in the current scope
     */
    public final SymbolInfo lookupCurrentScope(String name) {
        Map<String, SymbolInfo> current = scopes.peek();
        return current != null ? current.get(name) : null;
    }

    public final int getDepth() {
        return scopes.size();
    }
}
