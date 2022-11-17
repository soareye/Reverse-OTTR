package reverseottr.evaluation;

import reverseottr.model.Mapping;
import reverseottr.model.RListTerm;
import reverseottr.model.TermRegistry;
import xyz.ottr.lutra.model.Argument;
import xyz.ottr.lutra.model.Parameter;
import xyz.ottr.lutra.model.terms.ListTerm;
import xyz.ottr.lutra.model.terms.NoneTerm;
import xyz.ottr.lutra.model.terms.Term;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implements the reverse of OTTR list expansion.**/
public class ListUnexpander {

    private final List<Term> markedVariables;
    private final List<Term> unmarkedVariables;
    private final Set<Mapping> minEmpty;
    private final Set<Mapping> maxEmpty;
    private final int maxRepetitions;

    public ListUnexpander(List<Parameter> parameters, List<Argument> arguments, int repetitions) {
        this.markedVariables = getMarkedVars(parameters, arguments);
        this.unmarkedVariables = getUnmarkedVars(parameters, arguments);
        this.minEmpty = minUnfltr();
        this.maxEmpty = maxUnfltr();
        this.maxRepetitions = repetitions;
    }

    /**
     * The reverse of OTTR cross.
     *
     * @param mappings Takes a set of mappings that is the result of evaluating a template as a query.
     * @return all lists that could be crossed in OTTR to produce the input-set of mappings,
     * limited by the maximum number of repetitions.**/
    public Set<Mapping> uncross(Set<Mapping> mappings) {
        var compSets = findCompatibleSets(mappings);

        Set<Mapping> result = new HashSet<>();

        for (Set<Mapping> compSet : compSets) {
            var uncrossed = toMappings(compactifyAll(compSet));

            Mapping glb = GLBSet(compSet);

            for (Mapping mapping : uncrossed) {
                for (Mapping mapPermutation : mapPermutations(mapping)) {
                    Mapping resultMap = Mapping.union(glb, mapPermutation);
                    result.add(resultMap);
                }
            }
        }

        result.addAll(this.minEmpty);

        return result.stream()
                .map(this::ListTermsToRListTerms)
                .collect(Collectors.toSet());
    }

    /** Generate all possible orderings and sublists for the uncrossed lists in a mapping.
     *
     * @param mapping A mapping that is a solution to uncross.
     * @return All permutations of the lists in the range of the input-mapping, such that these
     * are also solutions to uncross.**/
    private Set<Mapping> mapPermutations(Mapping mapping) {
        Set<Set<Mapping>> tempSet = new HashSet<>();

        for (Term var : mapping.domain()) {
            Set<Mapping> varSet = new HashSet<>();

            List<Term> list = ((ListTerm) mapping.get(var)).asList();

            for (var sublist : sublists(list)) {
                for (var repeated : repeat(sublist)) {
                    for (var order : permutations(repeated.size())) {
                        Mapping m = new Mapping();
                        ListTerm term = new ListTerm(applyOrder(order, repeated));
                        m.put(var, term);
                        varSet.add(m);
                    }
                }
            }

            tempSet.add(varSet);
        }

        return Mapping.joinAll(tempSet);
    }

    private <T> Set<List<T>> sublists(List<T> list) {
        Set<List<T>> result = new HashSet<>();
        List<T> resultList = new LinkedList<>();
        for (var t : list) {
            resultList.add(t);
            result.add(resultList);
            resultList = new LinkedList<>(resultList);
        }

        return result;
    }

    private Set<Mapping> toMappings(Set<Map<Term, Set<Term>>> set) {
        Set<Mapping> result = new HashSet<>();
        for (Map<Term, Set<Term>> map : set) {
            Mapping mapping = new Mapping();
            for (Term var : map.keySet()) {
                List<Term> termList = new LinkedList<>(map.get(var));
                mapping.put(var, new ListTerm(termList));
                result.add(mapping);
            }
        }

        return result;
    }

    /** The main function of uncross, where elements of possible lists in the solution
     * are determined.
     *
     * @param mappings A set of mappings that are compatible for the unmarked variables.
     * @return A set of mappings from terms to sets of terms. The sets contain elements
     * that may be present in list-solutions for uncross.
     * @see "compactify-method" **/
    private Set<Map<Term, Set<Term>>> compactifyAll(Set<Mapping> mappings) {
        Set<Map<Term, Set<Term>>> compacted = uncrossMin(mappings);
        Set<Map<Term, Set<Term>>> nextCompacted = new HashSet<>();

        for (Term markedVar : markedVariables) {
            for (var m1 : compacted) {
                var map = m1;
                for (var m2 : compacted) {
                    var temp = compactify(map, m2, markedVar);
                    if (temp != null) {
                        map = temp;
                    }
                }
                nextCompacted.add(map);
            }
            compacted = nextCompacted;
            nextCompacted = new HashSet<>();
        }

        return compacted;
    }

    /**
     * Combines two mappings that are potential solutions to uncross to create another potential solution.
     *
     * This method is based on idea that two mappings from terms to sets of terms, that represents solutions
     * to uncross, with the same domain i.e. terms that represent variables, can be combined by the union of
     * of one variable and the disjunction of the other variables in their domain.
     *
     * @param m1 A mapping from terms to sets of terms, which represents a solution to uncross.
     * @param m2 A mapping from terms to sets of terms, which represents a solution to uncross.
     * @param var The selected variable to combine m1 and m2 over.
     * @return The combination of the two mappings that must also represent a solution to uncross.**/
    private Map<Term, Set<Term>> compactify(Map<Term, Set<Term>> m1, Map<Term, Set<Term>> m2, Term var) {
        Map<Term, Set<Term>> combination = new HashMap<>();
        combination.put(var, union(m1.get(var), m2.get(var)));
        for (Term otherVar : markedVariables) {
            if (!otherVar.equals(var)) {
                combination.put(otherVar, intersection(m1.get(otherVar), m2.get(otherVar)));
            }
        }

        if (hasEmptySet(combination)) return null;

        return combination;
    }

    private boolean hasEmptySet(Map<Term, Set<Term>> map) {
        for (Term var : map.keySet()) {
            if (map.get(var).isEmpty()) return true;
        }
        return false;
    }

    private Set<Term> intersection(Set<Term> s1, Set<Term> s2) {
        Set<Term> result = new HashSet<>();
        for (Term t1 : s1) {
            for (Term t2 : s2) {
                Term glb = TermRegistry.GLB(t1, t2);
                if (glb != null) {
                    result.add(glb);
                }
            }
        }
        return result;
    }

    private Set<Term> union(Set<Term> s1, Set<Term> s2) {
        Set<Term> result = new HashSet<>();

        for (Term t1 : s1) {
            for (Term t2 : s2) {
                Term glb = TermRegistry.GLB(t1, t2);
                if (glb != null) {
                    result.add(glb);
                } else {
                    result.add(t1);
                    result.add(t2);
                }
            }
        }

        return result;
    }

    /**
     * The smallest mappings that represents solutions to uncross.
     *
     * @param mappings A set of mappings that compatible over the unmarked variables.
     * @return The smallest mappings that represents solutions to uncross.**/
    private Set<Map<Term, Set<Term>>> uncrossMin(Set<Mapping> mappings) {
        Set<Map<Term, Set<Term>>> result = new HashSet<>();
        for (Mapping mapping : mappings) {
            Map<Term, Set<Term>> uncrossed = new HashMap<>();
            for (Term var : markedVariables) {
                Set<Term> set = new HashSet<>();
                set.add(mapping.get(var));
                uncrossed.put(var, set);
            }
            result.add(uncrossed);
        }

        return result;
    }

    /**
     * The reverse of zipMin in OTTR.
     *
     * Generates mappings with lists, such that applying zipMin re-obtains the input-set.
     * Since zipMin ignores elements in lists past the index of the last element of the
     * shortest list, unzipMin appends the placeholder TermRegistry.any_trail, to signify
     * that the remaining elements of lists other than the shortest could be anything.
     *
     * @param mappings A set of mappings that were obtained from the evaluateTemplate-method
     *                 in the Evaluator-class.
     * @return A set of mappings that could be zipMined to produce the input-set.**/
    public Set<Mapping> unzipMin(Set<Mapping> mappings) {
        Set<Mapping> result = new HashSet<>();

        Set<Mapping> unzipped = unzip(mappings);
        for (Mapping mapping : unzipped) {
            for (Term pick : markedVariables) {
                Mapping trailing = new Mapping(unmarkedVariables, mapping.get(unmarkedVariables));
                trailing.put(pick, mapping.get(pick));

                for (Term var : markedVariables) {
                    if (!pick.equals(var)) {
                        List<Term> termList = new LinkedList<>(((ListTerm)mapping.get(var)).asList());
                        termList.add(TermRegistry.any_trail);
                        trailing.put(var, new ListTerm(termList));
                    }
                }

                result.add(trailing);
            }
        }

        result.addAll(this.minEmpty);

        return result.stream().map(this::ListTermsToRListTerms).collect(Collectors.toSet());
    }

    /**
     * The reverse of zipMax in OTTR.
     *
     * @param mappings A set of mappings that were obtained from evaluating a template as a query.
     * @return A set of mappings that could be zipMaxed to produce the input-set.**/
    public Set<Mapping> unzipMax(Set<Mapping> mappings) {
        Set<Mapping> result = unzip(mappings).stream().map(this::noneTrailAlternatives)
                .reduce((s1, s2) -> {s1.addAll(s2); return s1;})
                .orElse(new HashSet<>())
                .stream().map(this::ListTermsToRListTerms)
                .collect(Collectors.toSet());

        result.addAll(maxEmpty);
        return result;
    }

    /** One or more trailing NoneTerm may be removed to produce another solution to unzipMax.
     * This method finds all such solutions.
     *
     * @param mapping A mapping that is a solution to unzipMax.
     * @return A set of mappings that are also solutions to unzipMax, obtained by removing
     * trailing none-values**/
    private Set<Mapping> noneTrailAlternatives(Mapping mapping) {
        Set<Set<Mapping>> altLists = new HashSet<>();

        for (Term var : markedVariables) {
            altLists.add(noneTrailLists(mapping, var));
        }

        Set<Mapping> markedMappings = Mapping.joinAll(altLists);

        for (Mapping m : markedMappings) {
            m.put(unmarkedVariables, mapping.get(unmarkedVariables));
        }

        return markedMappings;
    }

    private Set<Mapping> noneTrailLists(Mapping mapping, Term var) {
        Set<Mapping> result = new HashSet<>();
        Mapping original = new Mapping(var, mapping.get(var));
        result.add(original);

        List<Term> termList = ((ListTerm) mapping.get(var)).asList();

        for (int i = termList.size() - 1; i >= 0; i--) {
            Term term = termList.get(i);

            if (term instanceof NoneTerm) {
                Mapping altMap = new Mapping();
                List<Term> altList = new LinkedList<>(termList.subList(0, i));
                ListTerm altListTerm = new ListTerm(altList);
                altMap.put(var, altListTerm);
                result.add(altMap);
            } else {
                break;
            }
        }

        return result;
    }

    private <T> List<T> applyOrder(List<Integer> order, List<T> list) {
        return order.stream().map(list::get).collect(Collectors.toList());
    }

    private Set<List<Integer>> permutations(int size) {
        Set<List<Integer>> result = new HashSet<>();

        if (size == 1) {
            List<Integer> list = new LinkedList<>();
            list.add(0);
            result.add(list);
            return result;
        }

        for (List<Integer> list : permutations(size - 1)) {
            for (int i = 0; i <= list.size(); i++) {
                List<Integer> newList = new LinkedList<>(list);
                newList.add(i, size - 1);
                result.add(newList);
            }
        }

        return result;
    }

    /**
     * Finds the relevant lists for unzipMin and unzipMax.
     *
     * It finds the mappings with lists, such that when either of zipMin or zipMax
     * are applied, the input-set of mappings is re-obtained. These mappings must
     * be compatible over the unmarked variables.
     *
     * Additionally, it generates all possible orders of such lists, because order
     * doesn't matter for zipMin and zipMax.
     *
     * @param mappings A set of mappings obtained from the evaluateTemplate-method
     *                 in the Evaluator-class.
     * @return A set of mappings with solutions for both unzipMin and unzipMax.
     * **/
    private Set<Mapping> unzip(Set<Mapping> mappings) {
        Set<Mapping> result = new HashSet<>();

        Set<Set<Mapping>> compSets = findCompatibleSets(mappings);

        for (Set<Mapping> compSet : compSets) {
            List<Mapping> compList = new LinkedList<>(compSet);
            for (List<Mapping> repeat : repeat(compList)) {
                for (List<Integer> order : permutations(repeat.size())) {
                    result.add(unzipList(applyOrder(order, repeat)));
                }
            }
        }

        return result;
    }

    private <T> Set<List<T>> repeat(List<T> list) {
        Set<List<T>> result = new HashSet<>();
        result.add(list);

        for (T t : list) {
            for (int i = 0; i < this.maxRepetitions; i++) {
                Set<List<T>> temp = new HashSet<>();
                for (List<T> resultList : result) {
                    List<T> other = new LinkedList<>(resultList);
                    other.add(t);
                    temp.add(other);
                }
                result.addAll(temp);
            }
        }

        return result;
    }

    private Mapping unzipList(List<Mapping> mappings) {
        Map<Term, List<Term>> unzipMap = new HashMap<>();

        for (Term var : markedVariables) {
            List<Term> termList = new LinkedList<>();
            unzipMap.put(var, termList);
        }

        for (Mapping mapping : mappings) {
            for (Term var : markedVariables) {
                unzipMap.get(var).add(mapping.get(var));
            }
        }

        Mapping result = GLBSet(new HashSet<>(mappings));

        for (Term var : markedVariables) {
            ListTerm listTerm = new ListTerm(unzipMap.get(var));
            result.put(var, listTerm);
        }

        return result;
    }

    private Set<Set<Mapping>> findCompatibleSets(Set<Mapping> mappings) {
        Set<Set<Mapping>> result = new HashSet<>();

        for (Mapping mapping : mappings) {
            Set<Mapping> compSet = new HashSet<>();
            compSet.add(mapping);
            result.add(compSet);
        }

        for (Mapping mapping : mappings) {
            Set<Set<Mapping>> workSet = new HashSet<>();
            for (Set<Mapping> compSet : result) {
                Mapping glb = GLBSet(compSet);
                if (glb != null) {
                    if (Mapping.compatible(glb, mapping)) {
                        Set<Mapping> fresh = new HashSet<>(compSet);
                        fresh.add(mapping);
                        workSet.add(fresh);
                    }
                }
            }

            result.addAll(workSet);
        }

        return result;
    }

    private Mapping GLBSet(Set<Mapping> mappings) {
        Mapping result = new Mapping();
        for (Term var : unmarkedVariables) {
            result.put(var, TermRegistry.any);
        }

        for (Mapping mapping : mappings) {
            result = GLBMapping(result, mapping);
            if (result == null) return null;
        }

        return result;
    }

    private Mapping GLBMapping(Mapping m1, Mapping m2) {
        Mapping result = new Mapping();
        for (Term var : unmarkedVariables) {
            Term GLB = TermRegistry.GLB(m1.get(var), m2.get(var));
            if (GLB != null) {
                result.put(var, GLB);
            } else {
                return null;
            }
        }

        return result;
    }

    private List<Term> getMarkedVars(List<Parameter> parameters, List<Argument> arguments) {
        List<Term> result = new LinkedList<>();

        for (int i = 0; i < parameters.size(); i++) {
            if (arguments.get(i).isListExpander())
                result.add(parameters.get(i).getTerm());
        }

        return result;
    }

    private List<Term> getUnmarkedVars(List<Parameter> parameters, List<Argument> arguments) {
        List<Term> result = new LinkedList<>();

        for (int i = 0; i < parameters.size(); i++) {
            if (!arguments.get(i).isListExpander())
                result.add(parameters.get(i).getTerm());
        }

        return result;
    }

    private Mapping ListTermsToRListTerms(Mapping mapping) {
        Mapping result = new Mapping();
        for (Term var : markedVariables) {
            List<Term> termList = ((ListTerm) mapping.get(var)).asList();
            result.put(var, new RListTerm(termList, true));
        }

        for (Term var : unmarkedVariables) {
            result.put(var, mapping.get(var));
        }

        return result;
    }

    private Set<Mapping> minUnfltr() {
        Set<Mapping> result = new HashSet<>();

        for (Term marked : this.markedVariables) {
            Mapping mapping = new Mapping();
            mapping.put(marked, new RListTerm(new LinkedList<>(), true));
            for (Term other : this.markedVariables) {
                if (!other.equals(marked)) {
                    List<Term> list = new LinkedList<>();
                    list.add(TermRegistry.any_trail);
                    mapping.put(other, new RListTerm(list, true));
                }
            }

            for (Term unmarked : this.unmarkedVariables) {
                mapping.put(unmarked, TermRegistry.any);
            }

            result.add(mapping);
        }

        return result;
    }

    private Set<Mapping> maxUnfltr() {
        Set<Mapping> result = new HashSet<>();
        Mapping mapping = new Mapping();

        for (Term unmarked : this.unmarkedVariables) {
            mapping.put(unmarked, TermRegistry.any);
        }

        for (Term marked : this.markedVariables) {
            mapping.put(marked, new RListTerm(new LinkedList<>(), true));
        }

        result.add(mapping);

        return result;
    }
}
