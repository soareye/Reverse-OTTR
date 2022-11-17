package reverseottr.model;

import org.apache.jena.shared.PrefixMapping;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.model.Substitution;
import xyz.ottr.lutra.model.terms.Term;
import xyz.ottr.lutra.model.types.Type;
import xyz.ottr.lutra.model.types.TypeRegistry;
import java.util.Objects;
import java.util.Optional;

/**Placeholders are used to represent infinite classes of terms. **/
public class Placeholder implements Term {

    private final String identifier;
    private Type type;
    private boolean variable;

    public Placeholder(String identifier) {
        this.identifier = identifier;
        this.type = TypeRegistry.LUB_TOP;
    }

    @Override
    public Object getIdentifier() {
        return this.identifier;
    }

    @Override
    public void setVariable(boolean b) {
        this.variable = b;
    }

    @Override
    public boolean isVariable() {
        return this.variable;
    }

    @Override
    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public Optional<Term> unify(Term term) {
        return Optional.of(term);
    }

    @Override
    public Placeholder shallowClone() {
        Placeholder term = new Placeholder(this.identifier);
        term.setVariable(this.variable);
        return term;
    }

    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || Objects.nonNull(o)
                && getClass() == o.getClass()
                && this.variable == ((Term) o).isVariable()
                && Objects.equals(this.identifier, ((Term) o).getIdentifier());
    }

    @Override
    public Term apply(Substitution substitution) {
        return Objects.requireNonNullElse(substitution.get(this), this);
    }

    public String toString(PrefixMapping prefixes) {

        StringBuilder strBuilder = new StringBuilder();

        if (this.variable) {
            strBuilder.append("?");
        }

        strBuilder.append(prefixes.shortForm(this.identifier));

        if (Objects.nonNull(this.type)) {
            strBuilder.append(" : ").append(prefixes.shortForm(this.type.toString()));
        }

        return strBuilder.toString();
    }

    public String toString() {
        return toString(OTTR.getDefaultPrefixes());
    }

}
