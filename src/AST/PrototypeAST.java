package src.AST;

import java.util.List;

// maybe we should convert this to a record
public final class PrototypeAST {

    /** One declared parameter. */
    public static final class Param {
        public final String type;
        public final String name;

        public Param(final String type, final String name) {
            this.type = type;
            this.name = name;
        }
    }

    private final String name;
    private final List<Param> params;

    public PrototypeAST(final String name, final List<Param> params) {
        this.name = name;
        this.params = List.copyOf(params);
    }

    /** @return the function name */
    public String getName() {
        return name;
    }

    /** @return the declared parameters */
    public List<Param> getParams() {
        return params;
    }

    public boolean isMain() {
        return name.equals("main");
    }
}
