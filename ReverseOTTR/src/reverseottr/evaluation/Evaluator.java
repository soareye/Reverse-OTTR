package reverseottr.evaluation;

import org.apache.jena.rdf.model.Model;
import reverseottr.reader.GraphReader;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.model.*;
import xyz.ottr.lutra.model.terms.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Evaluator {

    private Set<Map<Term, Term>> resultSet;
    private Set<Map<Term, Term>> nullableResultSet;

    public Evaluator(Model model) {
        // TODO: apply nonOptSolutionsAll to resultSet.
        this.resultSet = RDFToOTTR.asResultSet(model, false);
        this.nullableResultSet = RDFToOTTR.asResultSet(model, true);
    }

    public Set<Map<Term, Term>> evaluateTemplate(Template template) {
        if (template.getIri().equals(OTTR.BaseURI.Triple)) {
            return this.resultSet;

        } else if (template.getIri().equals(OTTR.BaseURI.NullableTriple)) {
            return this.nullableResultSet;

        } else {
            List<Parameter> parameters = template.getParameters();

            Set<Set<Map<Term, Term>>> instanceEvaluations =
                    template.getPattern().stream()
                            .map(this::evaluateInstance)
                            .map(s -> paramFilter(s, parameters))
                            .map(s -> defaultSolutionsAll(s, parameters))
                            // TODO: for each set of mappings, do nonOptSolutionsAll(mappings)
                            .collect(Collectors.toSet());

            return Mapping.joinAll(instanceEvaluations);
        }
    }

    public Set<Map<Term, Term>> evaluateInstance(Instance instance) {
        if (instance.hasListExpander()) {
            if (instance.getListExpander().equals(ListExpander.zipMin)) {
                // filter of unzipMin of eval of template

            } else if (instance.getListExpander().equals(ListExpander.zipMax)) {
                // filter of unzipMax of eval of template

            } else {
                // filter of uncross of eval of template
            }

        } else {
            // return filter(evaluateTemplate(instance.getIri()));
        }

        return null;
    }

    private Set<Map<Term, Term>> argFilter(Set<Map<Term, Term>> set, Map<Term, Term> argMap) {
        Set<Map<Term, Term>> resultSet = new HashSet<>();

        for (Map<Term, Term> map : set) {
            if (Mapping.innerCompatible(map, argMap)) {
                resultSet.add(Mapping.transform(map, argMap));
            }
        }

        return resultSet;
    }

    private Set<Map<Term, Term>> paramFilter(Set<Map<Term, Term>> maps, List<Parameter> parameters) {
        return maps.stream()
                .filter(m -> conforms(m, parameters))
                .collect(Collectors.toSet());
    }

    private boolean conforms(Map<Term, Term> map, List<Parameter> parameters) {
        for (Parameter param : parameters) {
            Term var = param.getTerm();
            if (map.containsKey(var)) {
                if ((map.get(var) instanceof NoneTerm
                        && (param.hasDefaultValue() || !param.isOptional()))
                        || (param.isNonBlank() && map.get(var) instanceof BlankNodeTerm)) {
                    return false;
                }
            }
        }

        return true;
    }

    private Set<Map<Term, Term>> nonOptSolutions(Map<Term, Term> map, List<Parameter> parameters) {
        List<Term> nonOptVars = parameters.stream()
                .filter(p -> !p.isOptional())
                .map(Parameter::getTerm)
                .collect(Collectors.toList());

        Set<Map<Term, Term>> resultSet = new HashSet<>();

        Map<Term, Term> baseMap = new HashMap<>();

        for (Term var : map.keySet()) {
            if (nonOptVars.contains(var) && map.get(var) instanceof NoneTerm) {

            } else {
                baseMap.put(var, map.get(var));
            }
        }

        return resultSet;
    }

    private Set<Map<Term, Term>> defaultSolutionsAll(Set<Map<Term, Term>> maps, List<Parameter> parameters) {
        List<Parameter> defaultParams = parameters.stream()
                .filter(Parameter::hasDefaultValue)
                .collect(Collectors.toList());

        Set<Map<Term, Term>> resultSet = new HashSet<>();
        maps.forEach(m -> resultSet.addAll(defaultSolutions(m, defaultParams)));

        return resultSet;
    }

    private Set<Map<Term, Term>> defaultSolutions(Map<Term, Term> map, List<Parameter> parameters) {
        List<Term> defaultVars = new LinkedList<>();

        for (Parameter param : parameters) {
            Term var = param.getTerm();
            if (map.containsKey(var) && map.get(var).equals(param.getDefaultValue())) {
                defaultVars.add(var);
            }
        }

        return altSolutions(map, defaultVars);
    }

    private Set<Map<Term, Term>> altSolutions(Map<Term, Term> map, List<Term> vars) {
        Map<Term, Term> baseMap = new HashMap<>();
        Map<Term, Term> subMap = new HashMap<>();

        for (Term var : map.keySet()) {
            if (vars.contains(var)) {
                subMap.put(var, map.get(var));
            } else {
                baseMap.put(var, map.get(var));
            }
        }

        return allCombinationsNone(subMap).stream()
                .map(m -> Mapping.union(m, baseMap))
                .collect(Collectors.toSet());
    }

    private Set<Map<Term, Term>> allCombinationsNone(Map<Term, Term> map) {
        Set<Set<Map<Term, Term>>> combinations = new HashSet<>();

        for (Term var : map.keySet()) {
            Map<Term, Term> subMap = new HashMap<>();
            subMap.put(var, map.get(var));

            Map<Term, Term> noneMap = new HashMap<>();
            noneMap.put(var, new NoneTerm());

            Set<Map<Term, Term>> combination = new HashSet<>();
            combination.add(subMap);
            combination.add(noneMap);

            combinations.add(combination);
        }

        return Mapping.joinAll(combinations);
    }

    public static void main(String[] args) {

        Evaluator e = new Evaluator(GraphReader.read(args[0]));

        List<Parameter> params = OTTR.BaseTemplate.NullableTriple.getParameters();

        Term subVar = params.get(0).getTerm();
        Term predVar = params.get(1).getTerm();
        Term objVar = params.get(2).getTerm();

        Term subVal = new IRITerm("http://sws.ifi.uio.no/inf3580/v14/oblig/6/racehorse#test");
        Argument sub = Argument.builder().term(subVal).build();

        Term predVal = new IRITerm("http://sws.ifi.uio.no/inf3580/v14/oblig/6/racehorse#is");
        Argument pred = Argument.builder().term(predVal).build();

        Term objVal1 = new BlankNodeTerm("x");
        objVal1.setVariable(true);

        Term objVal2 = new BlankNodeTerm("y");
        objVal2.setVariable(true);

        Term objVal3 = new BlankNodeTerm("z");
        objVal3.setVariable(true);

        Term objVal = new ListTerm(objVal1, objVal2, objVal3);

        Argument obj = Argument.builder().term(objVal).build();

        Map<Term, Argument> map = new HashMap<>();
        map.put(subVar, sub);
        map.put(predVar, pred);
        map.put(objVar, obj);

        Map<Term, Term> map2 = new HashMap<>();
        map2.put(subVar, subVal);
        map2.put(predVar, predVal);
        map2.put(objVar, objVal);

        // e.argFilter(e.resultSet, map2).forEach(System.out::println);

        e.allCombinationsNone(map2).forEach(System.out::println);
    }
}
