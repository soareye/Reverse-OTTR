package reverseottr.model;

import reverseottr.model.Placeholder;
import xyz.ottr.lutra.model.Parameter;
import xyz.ottr.lutra.model.terms.*;

import java.util.*;

public enum TermRegistry {
    ;

    public static final String ph_ns = "http://example.org/placeholder/";
    private static final String any_IRI = ph_ns + "any";
    private static final String any_nb_IRI = ph_ns + "any-nb";
    private static final String any_no_IRI = ph_ns + "any-no";
    private static final String any_nbno_IRI = ph_ns + "any-nbno";
    private static final String any_trail_IRI = ph_ns + "any-trail";

    public static final Term any = new Placeholder(any_IRI);
    public static final Term any_nb = new Placeholder(any_nb_IRI);
    public static final Term any_no = new Placeholder(any_no_IRI);
    public static final Term any_nbno = new Placeholder(any_nbno_IRI);
    public static final Term any_trail = new Placeholder(any_trail_IRI);

    public static final Set<Mapping> lattice = populateLattice();

    public static Term GLB(Term t1, Term t2) {
        if (lessOrEqual(t1, t2)) return t1;

        if (lessOrEqual(t2, t1)) return t2;

        if (t1 instanceof ListTerm && t2 instanceof ListTerm) {
            if (t1.getIdentifier().equals(t2.getIdentifier())) return t1;

            List<Term> list = GLBList(((ListTerm) t1).asList(), ((ListTerm) t2).asList());
            if (list == null) return null;

            if (isUnexpanded(t1) && !isUnexpanded(t2))
                return t2;

            if (isUnexpanded(t2) && !isUnexpanded(t1))
                return t1;

            if (isUnexpanded(t1) && isUnexpanded(t2))
                return new RListTerm(list, true);

            return null;
        }

        if ((t1.equals(any_nb) && t2.equals(any_nb)) ||
                (t2.equals(any_nb) && t1.equals(any_nb))) {
            return any_nbno;
        }

        return null;
    }

    private static List<Term> GLBList(List<Term> l1, List<Term> l2) {

        if (l1.isEmpty() && l2.isEmpty()) return l1;

        if (l1.isEmpty() && !l2.get(l2.size() - 1).equals(any_trail)) return null;

        if (l2.isEmpty() && !l1.get(l1.size() - 1).equals(any_trail)) return null;

        if ((l1.size() < l2.size() && !l1.get(l1.size() - 1).equals(any_trail)) ||
                (l2.size() < l1.size() && !l2.get(l2.size() - 1).equals(any_trail)))
            return null;

        int size = Math.min(l1.size(), l2.size());
        List<Term> resultList = new LinkedList<>();

        for (int i = 0; i < size - 1; i++) {
            Term currentTerm = GLB(l1.get(i), l2.get(i));
            if (currentTerm == null) return null;
            resultList.add(currentTerm);
        }

        List<Term> longer = l1;
        List<Term> shorter = l2;

        if (l1.get(l1.size() - 1).equals(any_trail)) {
            shorter = l1;
            longer = l2;

        } else if (!l2.get(l2.size() - 1).equals(any_trail)) {
            Term lastTerm = GLB(l1.get(l1.size() - 1), l2.get(l2.size() - 1));
            if (lastTerm == null) return null;
            resultList.add(lastTerm);
        }

        for (int i = shorter.size() - 1; i < longer.size(); i++) {
            resultList.add(longer.get(i));
        }

        return resultList;
    }

    public static boolean lessOrEqual(Term t1, Term t2) {
        if (t1.equals(t2)) return true;

        if (lattice.contains(new Mapping(t1, t2))) return true;

        if (t1 instanceof NoneTerm && t2 instanceof NoneTerm) return true;

        if (t1 instanceof NoneTerm && lessOrEqual(any_nb, t2)) return true;

        if (t1 instanceof BlankNodeTerm && lessOrEqual(any_no, t2)) return true;

        if (t1 instanceof IRITerm && lessOrEqual(any_nbno, t2)) return true;

        if (t1 instanceof LiteralTerm && lessOrEqual(any_nbno, t2)) return true;

        if (t1 instanceof ListTerm && lessOrEqual(any_nbno, t2)) return true;

        if (t1 instanceof ListTerm && t2 instanceof ListTerm)
            return lessOrEqualList((ListTerm) t1, (ListTerm) t2);

        if (t1 instanceof Placeholder && t2 instanceof Placeholder) {
            for (Mapping map : lattice) {
                if (map.containsVar(t1)) {
                    return lessOrEqual(map.get(t1), t2);
                }
            }
        }

        return false;
    }

    private static boolean lessOrEqualList(ListTerm t1, ListTerm t2) {
        if (t1.getIdentifier().equals(t2.getIdentifier())) return true;

        if (!isUnexpanded(t2)) return false;

        List<Term> list1 = t1.asList();
        List<Term> list2 = t2.asList();

        if (list1.isEmpty() && list2.isEmpty()) return true;

        if (list1.size() < list2.size()) return false;

        if (list2.isEmpty() && !list1.isEmpty()) return false;

        if (list1.size() > list2.size() &&
                !list2.get(list2.size() - 1).equals(any_trail))
            return false;

        for (int i = 0; i < list2.size(); i++) {
            if (!lessOrEqual(list1.get(i), list2.get(i))) {
                if (!list2.get(i).equals(any_trail))
                    return false;
            }
        }

        return true;
    }

    public static boolean isUnexpanded(Term t) {
        return t instanceof RListTerm && ((RListTerm) t).isUnexpanded();
    }

    public static Term paramPlaceholder(Parameter parameter) {
        if (parameter.isNonBlank() && !parameter.isOptional()) {
            return any_nbno;
        } else if (parameter.isNonBlank()) {
            return any_nb;
        } else if (!parameter.isOptional()) {
            return any_no;
        } else {
            return any;
        }
    }

    private static Set<Mapping> populateLattice() {
        Set<Mapping> maps = new HashSet<>();
        maps.add(new Mapping(any_nb, any));
        maps.add(new Mapping(any_no, any));
        maps.add(new Mapping(any_nbno, any_nb));
        maps.add(new Mapping(any_nbno, any_no));

        return maps;
    }
}
