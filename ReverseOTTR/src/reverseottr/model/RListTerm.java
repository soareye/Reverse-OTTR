package reverseottr.model;

import xyz.ottr.lutra.model.terms.ListTerm;
import xyz.ottr.lutra.model.terms.Term;

import java.util.List;
import java.util.Objects;

public class RListTerm extends ListTerm {
    private boolean unexpanded;

    public RListTerm(List<Term> termList, boolean unexpanded) {
        super(termList);
        this.unexpanded = unexpanded;
    }

    public RListTerm(List<Term> termList) {
        this(termList, false);
    }

    public void setUnexpanded(boolean unexpanded) {
        this.unexpanded = unexpanded;
    }

    public boolean isUnexpanded() {
        return unexpanded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RListTerm rListTerm = (RListTerm) o;
        if (this.unexpanded != rListTerm.unexpanded) return false;
        if (this.asList().size() != rListTerm.asList().size()) return false;
        for (int i = 0; i < this.asList().size(); i++) {
            if (!this.asList().get(i).equals(rListTerm.asList().get(i))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(asList(), unexpanded);
    }
}
