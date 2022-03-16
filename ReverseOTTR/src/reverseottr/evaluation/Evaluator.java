package reverseottr.evaluation;

import org.apache.commons.validator.Arg;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import reverseottr.reader.GraphReader;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.model.*;
import xyz.ottr.lutra.model.terms.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Evaluator {

    private Set<Map<Term, Term>> resultSet;
    private Set<Map<Term, Term>> nullableResultSet;

    public Evaluator(Model model) {
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
            List<Term> nonOptVars = paramsToVarsFilter(parameters, p -> !p.isOptional());
            List<Term> defaultVars = paramsToVarsFilter(parameters, Parameter::hasDefaultValue);
            List<Term> nonBlankVars = paramsToVarsFilter(parameters, Parameter::isNonBlank);



            // join all of generate possible solutions of eval of each instance in pattern.
            return null;
        }
    }

    private List<Term> paramsToVarsFilter(List<Parameter> parameters,
                                          Predicate<Parameter> predicate) {
        return parameters.stream()
                .filter(predicate)
                .map(Parameter::getTerm)
                .collect(Collectors.toList());
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

    private Set<Map<Term, Term>> nonOptSolutions(Map<Term, Term> map, List<Term> nonOptVars) {
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

    private Set<Map<Term, Term>> allSolutions(Map<Term, Term> map) {
        if (map.size() == 0) {
            return new HashSet<>();
        } else {
            Set<Map<Term, Term>> resultSet = new HashSet<>();
            Set<Map<Term, Term>> subSet = allSolutions(next(map));

            for (Map<Term, Term> m : subSet) {
                temp1 = copy(m);
                temp1.put(var, map.get(var));
                temp2 = copy(m);
                temp2.put(var, new NoneTerm());

                resultSet.add(temp1);
                resultSet.add(temp2);
            }

            return resultSet;
        }
    }

    private Set<Map<Term, Term>> defaultSolutions(Map<Term, Term> map, List<Parameter> defaultVars) {
        Set<Map<Term, Term>> resultSet = new HashSet<>();

        return resultSet;
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

        e.argFilter(e.resultSet, map2).forEach(System.out::println);
    }
}
