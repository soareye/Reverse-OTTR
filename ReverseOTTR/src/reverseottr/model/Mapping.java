package reverseottr.model;

import org.apache.jena.shared.PrefixMapping;
import xyz.ottr.lutra.model.terms.BlankNodeTerm;
import xyz.ottr.lutra.model.terms.ListTerm;
import xyz.ottr.lutra.model.terms.NoneTerm;
import xyz.ottr.lutra.model.terms.Term;
import java.util.*;

public class Mapping {

    private final Map<Term, Term> map;

    public Mapping() {
        this.map = new HashMap<>();
    }

    public Mapping(Term var, Term term) {
        this.map = new HashMap<>();
        this.put(var, term);
    }

    public Mapping(List<Term> vars, List<Term> terms) {
        this.map = new HashMap<>();
        this.put(vars, terms);
    }

    public void put(Term var, Term value) {
        this.map.put(var, value);
    }

    public void put(List<Term> vars, List<Term> terms) {
        for (int i = 0; i < vars.size(); i++) {
            this.put(vars.get(i), terms.get(i));
        }
    }

    public Term get(Term var) {
        return this.map.get(var);
    }

    public List<Term> get(List<Term> vars) {
        List<Term> list = new LinkedList<>();

        for (Term var : vars) {
            list.add(this.get(var));
        }

        return list;
    }

    public Set<Term> domain() {
        return this.map.keySet();
    }

    public boolean containsVar(Term var) {
        return this.map.containsKey(var);
    }

    public String toString() {
        return this.map.toString();
    }

    public static Set<Mapping> joinAll(Set<Set<Mapping>> sets) {
        return sets.stream().reduce(Mapping::join).orElse(new HashSet<>());
    }

    public static Set<Mapping> join(Set<Mapping> a, Set<Mapping> b) {
        Set<Mapping> resultSet = new HashSet<>();

        for (Mapping mapA : a) {
            for (Mapping mapB : b) {
                if (compatible(mapA, mapB)) {
                    resultSet.add(union(mapA, mapB));
                }
            }
        }

        return resultSet;
    }

    public static Mapping union(Mapping a, Mapping b) {
        Mapping result = new Mapping();
        Set<Term> domain = new HashSet<>();
        domain.addAll(a.domain());
        domain.addAll(b.domain());

        for (Term var : domain) {
            if (!a.containsVar(var)) {
                result.put(var, b.get(var));
            } else if (!b.containsVar(var)) {
                result.put(var, a.get(var));
            } else {
                result.put(var, TermRegistry.GLB(a.get(var), b.get(var)));
            }
        }

        return result;
    }

    public static boolean compatible(Mapping a, Mapping b) {
        for (Term var : a.domain()) {
            if (b.containsVar(var) && TermRegistry.GLB(a.get(var), b.get(var)) == null) {
                return false;
            }
        }
        return true;
    }

    public static Mapping transform(Mapping termMap, Mapping argMap) {
        Mapping result = new Mapping();
        for (Term var : argMap.domain()) {
            Term argValue = argMap.get(var);
            Term termValue = termMap.get(var);
            Mapping tempMap = termTransform(termValue, argValue);
            if (compatible(result, tempMap)) result = union(result, tempMap);
        }

        return result;
    }

    public static Mapping termTransform(Term term, Term arg) {
        Mapping result = new Mapping();

        if (arg instanceof ListTerm && term instanceof ListTerm) {
            return listTransform(((ListTerm) term).asList(), ((ListTerm) arg).asList());

        } else if (arg.isVariable()) {
            result.put(arg, term);
        }

        return result;
    }

    public static Mapping listTransform(List<Term> termList, List<Term> argList) {
        int size = argList.size();

        if (termList.get(termList.size() - 1).equals(TermRegistry.any_trail)) {
            size = termList.size() - 1;

        }

        Mapping result = new Mapping();

        for (int i = 0; i < size; i++) {
            Term arg = argList.get(i);
            Term term = termList.get(i);

            Mapping tempMap = termTransform(term, arg);

            if (compatible(result, tempMap)) {
                result = union(result, tempMap);
            }
        }

        for (int i = size; i < argList.size(); i++) {
            Term arg = argList.get(i);
            if (arg.isVariable()) {
                Mapping tempMap = new Mapping();
                tempMap.put(arg, TermRegistry.any);
                result = union(result, tempMap);
            }
        }

        return result;
    }

    public static boolean innerCompatible(Mapping termMap, Mapping argMap) {
        if (!termMap.domain().equals(argMap.domain())) return false;

        for (Term var : argMap.domain()) {
            Term arg = argMap.get(var);
            Term term = termMap.get(var);

            if (!arg.isVariable() && TermRegistry.GLB(arg, term) == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Mapping mapping = (Mapping) o;

        if (!mapping.domain().equals(this.domain())) return false;

        for (Term var : this.domain()) {
            Term t1 = this.get(var);
            Term t2 = mapping.get(var);
            if (!t1.equals(t2) && !(t1 instanceof NoneTerm && t2 instanceof NoneTerm))
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (Term var : domain()) {
            Term term = get(var);
            hash += term.hashCode();
        }

        return hash;
    }

    /*
    private int listHash(List<Term> termList) {
        int hash = 0;

        for (Term term : termList) {
            if (term instanceof ListTerm) {
                hash += listHash(((ListTerm) term).asList());
            } else {
                hash += term.hashCode();
            }
        }

        return hash;
    }
     */

    public String toString(PrefixMapping prefixes) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (Term var : domain()) {
            stringBuilder.append("?");
            stringBuilder.append(var.getIdentifier());
            stringBuilder.append("=");

            Term term = get(var);
            String id;
            if (term instanceof ListTerm) {
                id = listToString(term, prefixes);
            } else {
                id = prefixes.shortForm(get(var).getIdentifier().toString());
            }

            stringBuilder.append(id);
            stringBuilder.append(", ");
        }
        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        stringBuilder.append("}");

        return stringBuilder.toString();
    }

    private String listToString(Term term, PrefixMapping prefixes) {
        List<Term> list = ((ListTerm) term).asList();

        if (list.size() == 0) return "[]";

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (Term t : list) {
            String id;
            if (t instanceof ListTerm) {
                id = listToString(t, prefixes);
            } else {
                id = prefixes.shortForm(t.getIdentifier().toString());
            }

            stringBuilder.append(id);
            stringBuilder.append(", ");
        }
        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        stringBuilder.append("]");
        stringBuilder.append("{").append(term.getIdentifier()).append("}");

        return stringBuilder.toString();
    }
}
