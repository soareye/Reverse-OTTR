package reverseottr.evaluation;

import xyz.ottr.lutra.model.terms.Term;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Mapping {
    private Map<Term, Term> map;

    public Mapping(Map<Term, Term> map) {
        this.map = map;
    }

    public static Map<Term, Term> join(Map<Term, Term> a, Map<Term, Term> b) {
        Map<Term, Term> result = new HashMap<>();

        if (compatible(a, b)) {
            Set<Term> keys = new HashSet<>();
            keys.addAll(a.keySet());
            keys.addAll(b.keySet());

            for (Term key : keys) {
                if (!a.containsKey(key)) {
                    result.put(key, b.get(key));
                } else {
                    result.put(key, a.get(key));
                }
            }
        }

        return result;
    }

    public static boolean compatible(Map<Term, Term> a, Map<Term, Term> b) {
        for (Term key : a.keySet()) {
            Term value = a.get(key);

            if (b.containsKey(key) && b.get(key) != value) {
                return false;
            }
        }

        return true;
    }
}
