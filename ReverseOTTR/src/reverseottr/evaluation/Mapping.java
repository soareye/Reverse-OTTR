package reverseottr.evaluation;

import xyz.ottr.lutra.model.terms.ListTerm;
import xyz.ottr.lutra.model.terms.Term;
import java.util.*;

public class Mapping {

    public static Set<Map<Term, Term>> joinAll(Set<Set<Map<Term, Term>>> sets) {
        return sets.stream().reduce(new HashSet<>(), Mapping::join);
    }

    public static Set<Map<Term, Term>> join(Set<Map<Term, Term>> a, Set<Map<Term, Term>> b) {
        Set<Map<Term, Term>> resultSet = new HashSet<>();

        if (a.size() == 0) return b;
        if (b.size() == 0) return a;

        for (Map<Term, Term> mapA : a) {
            for (Map<Term, Term> mapB : b) {
                if (compatible(mapA, mapB)) {
                    resultSet.add(union(mapA, mapB));
                }
            }
        }

        return resultSet;
    }

    public static Map<Term, Term> union(Map<Term, Term> a, Map<Term, Term> b) {
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

            if (b.containsKey(key) && !b.get(key).equals(value)) {
                return false;
            }
        }

        return true;
    }

    public static Map<Term, Term> transform(Map<Term, Term> termMap, Map<Term, Term> argMap) {
        Map<Term, Term> result = new HashMap<>();
        if (innerCompatible(termMap, argMap)) {
            for (Term key : argMap.keySet()) {
                Term argValue = argMap.get(key);
                Term termValue = termMap.get(key);

                Map<Term, Term> tempMap = termTransform(termValue, argValue);

                if (compatible(result, tempMap)) result = union(result, tempMap);
            }
        }

        return result;
    }

    public static Map<Term, Term> termTransform(Term term, Term arg) {
        Map<Term, Term> result = new HashMap<>();

        if (termCompatible(term, arg)) {
            if (arg instanceof ListTerm && term instanceof ListTerm) {
                List<Term> argList = ((ListTerm) arg).asList();
                List<Term> termList = ((ListTerm) term).asList();

                return listTransform(termList, argList);

            } else if (arg.isVariable()) {
                result.put(arg, term);
            }
        }

        return result;
    }

    public static Map<Term, Term> listTransform(List<Term> termList, List<Term> argList) {
        Map<Term, Term> result = new HashMap<>();

        if (listCompatible(termList, argList)) {
            for (int i = 0; i < argList.size(); i++) {
                Term arg = argList.get(i);
                Term term = termList.get(i);

                Map<Term, Term> tempMap = termTransform(term, arg);

                if (compatible(result, tempMap)) {
                    result = union(result, tempMap);
                }
            }
        }

        return result;
    }

    public static boolean innerCompatible(Map<Term, Term> termMap, Map<Term, Term> argMap) {
        if (!termMap.keySet().equals(argMap.keySet())) return false;

        for (Term key : argMap.keySet()) {
            Term argValue = argMap.get(key);
            Term termValue = termMap.get(key);

            if (!termCompatible(termValue, argValue)) {
                return false;
            }
        }

        return true;
    }

    public static boolean termCompatible(Term term, Term arg) {
        if (arg instanceof ListTerm && term instanceof ListTerm) {
            List<Term> argList = ((ListTerm) arg).asList();
            List<Term> termList = ((ListTerm) term).asList();

            return listCompatible(termList, argList);

        } else {
            return arg.isVariable() || arg.equals(term);
        }
    }

    public static boolean listCompatible(List<Term> termList, List<Term> argList) {
        if (termList.size() != argList.size()) return false;

        for (int i = 0; i < argList.size(); i++) {
            Term key = argList.get(i);
            Term value = termList.get(i);

            if (!termCompatible(value, key)) {
                return false;
            }
        }

        return true;
    }
}
