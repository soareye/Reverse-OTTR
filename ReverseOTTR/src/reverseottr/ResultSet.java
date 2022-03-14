package reverseottr;

import xyz.ottr.lutra.model.terms.Term;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResultSet {
    private Set<Map<Term, Term>> set;

    public ResultSet() {
        this.set = new HashSet<>();
    }
}
