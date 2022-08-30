package reverseottr.evaluation;

import reverseottr.model.Mapping;
import xyz.ottr.lutra.model.Argument;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.model.Parameter;
import xyz.ottr.lutra.model.terms.ListTerm;
import xyz.ottr.lutra.model.terms.Term;

import java.util.*;
import java.util.stream.Collectors;

public class ListUnexpander {

    private List<Term> markedVariables;
    private List<Term> unmarkedVariables;

    public ListUnexpander(List<Parameter> parameters, List<Argument> arguments) {
        this.markedVariables = getMarkedVars(parameters, arguments);
        this.unmarkedVariables = getUnmarkedVars(parameters, arguments);
    }

    public Set<Mapping> uncross(Set<Mapping> mappings) {

        Set<Mapping> resultSet = new HashSet<>();

        for (Mapping pick : mappings) {
            Map<Term, List<Term>> listMap = new HashMap<>();

            for (Term var : markedVariables) {
                List<Term> list = new LinkedList<>();
                list.add(pick.get(var));
                listMap.put(var, list);
            }

            /*
            Set<Mapping> validMaps = findEqualForVars(mappings, pick, unmarkedVariables);
            validMaps.remove(pick);

            for (Term var : markedVariables) {
                List<Term> fixedVars = new LinkedList<>(markedVariables);
                fixedVars.remove(var);

                for (Mapping map : findEqualForVars(validMaps, pick, fixedVars)) {
                    listMap.get(var).add(map.get(var));


                    check if map.get(var) works with all combinations of fixedVars.
                    something like:
                    combinations = new LinkedList<>(new LinkedList).
                    for combination in combinations
                        if combination is not empty
                            blah blah


                }
            }
            */

            Mapping resultMap = new Mapping();

            for (Term var : markedVariables) {
                resultMap.put(var, new ListTerm(listMap.get(var)));
            }

            resultMap.put(unmarkedVariables, pick.get(unmarkedVariables));

            resultSet.add(resultMap);
        }

        return resultSet;
    }

    private boolean hasUncrossedMap(Set<Mapping> mappings, Mapping mapFromUncross) {
        for (Mapping map : mappings) {
            for (Term var : map.domain()) {
                Term term = mapFromUncross.get(var);
                if (term instanceof ListTerm) {
                    if (((ListTerm) term).asList().contains(map.get(var))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isUncrossedMap(Mapping map, Mapping mapFromUncross) {
        for (Term var : map.domain()) {
            Term term = mapFromUncross.get(var);
            if (term instanceof ListTerm) {
                if (!((ListTerm) term).asList().contains(map.get(var))) {
                    return false;
                }
            }
        }

        return true;
    }

    public Set<Mapping> unzipMin(Set<Mapping> mappings, List<Argument> args) {
        return null;
    }

    public Set<Mapping> unzipMax(Set<Mapping> mappings) {
        Set<Mapping> result = new HashSet<>();

        Set<Mapping> unzipped = unzip(mappings);



        return unzipped;
    }

    private Set<Mapping> allOrdersMap(Mapping mapping) {
        Set<Mapping> result = new HashSet<>();

        int size = 0;



        return result;
    }

    private List<Term> applyOrder(List<Integer> order, List<Term> list) {
        return order.stream().map(list::get).collect(Collectors.toList());
    }

    private Set<List<Integer>> allOrders(int size) {
        Set<List<Integer>> result = new HashSet<>();

        if (size == 1) {
            List<Integer> list = new LinkedList<>();
            list.add(1);
        }

        for (List<Integer> order : allOrders(size - 1)) {

        }

        return result;
    }

    private Set<Mapping> unzip(Set<Mapping> mappings) {
        Set<Mapping> resultMaps = new HashSet<>();

        for (Mapping map : mappings) {
            Mapping unzippedMap = semiUnzip(findEqualForVars(mappings, map));
            unzippedMap.put(unmarkedVariables, map.get(unmarkedVariables));

            resultMaps.add(unzippedMap);
        }

        return resultMaps;
    }

    private Mapping semiUnzip(Set<Mapping> mappings) {
        Map<Term, List<Term>> resultMap = new HashMap<>();

        for (Mapping map : mappings) {
            for (Term var : markedVariables) {
                if (resultMap.containsKey(var)) {
                    resultMap.get(var).add(map.get(var));

                } else {
                    List<Term> terms = new LinkedList<>();
                    terms.add(map.get(var));
                    resultMap.put(var, terms);
                }
            }
        }

        Mapping result = new Mapping();

        for (Term var : resultMap.keySet()) {
            result.put(var, new ListTerm(resultMap.get(var)));
        }

        return result;
    }

    private Set<Mapping> findEqualForVars
            (Set<Mapping> mappings, Mapping map) {

        return mappings.stream()
                .filter(m -> equalForVars(m, map))
                .collect(Collectors.toSet());
    }

    private boolean equalForVars(Mapping m1, Mapping m2) {
        for (Term var : unmarkedVariables) {
            if (!m1.get(var).equals(m2.get(var))) {
                return false;
            }
        }

        return true;
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
}
