package reverseottr.evaluation;

import org.apache.commons.validator.Arg;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import reverseottr.reader.GraphReader;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.model.Argument;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.model.Parameter;
import xyz.ottr.lutra.model.Template;
import xyz.ottr.lutra.model.terms.BlankNodeTerm;
import xyz.ottr.lutra.model.terms.IRITerm;
import xyz.ottr.lutra.model.terms.ListTerm;
import xyz.ottr.lutra.model.terms.Term;

import java.util.*;

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
            return null;
        }
    }

    public Set<Map<Term, Term>> evaluateInstance(Instance instance) {

        if (instance.hasListExpander()) {


        } else {
            // return filter(evaluateTemplate(instance.getIri()));
        }

        return null;
    }

    private Set<Map<Term, Term>> filter(Set<Map<Term, Term>> set, Map<Term, Argument> argMap) {
        Set<Map<Term, Term>> result = new HashSet<>();

        for (Map<Term, Term> map : set) {
            Map<Term, Term> resultMap = new HashMap<>();
            boolean keep = true;

            for (Term key : argMap.keySet()) {
                Term argVal = argMap.get(key).getTerm();
                Term val = map.get(key);

                if (argVal.isVariable()) {
                    resultMap.put(argVal, val);

                } else if (argVal instanceof ListTerm) {
                    if (val instanceof ListTerm) {
                        Map<Term, Term> listMap =
                                filterLists(((ListTerm) argVal).asList(), ((ListTerm) val).asList());

                        if (listMap == null) {
                            keep = false;

                        } else {
                            Map<Term, Term> newMap = combineLeft(resultMap, listMap);

                            if (newMap == null) {
                                keep = false;

                            } else {
                                resultMap = newMap;
                            }
                        }

                    } else {
                        keep = false;
                    }

                } else if (!argVal.equals(val)) {
                    keep = false;
                }
            }

            if (keep) {
                result.add(resultMap);
            }
        }

        return result;
    }

    private Map<Term, Term> filterLists(List<Term> argList, List<Term> list) {
        if (argList.size() != list.size()) {
            return null;
        }

        Map<Term, Term> result = new HashMap<>();

        for (int i = 0; i < argList.size(); i++) {
            Term key = argList.get(i);
            Term value = list.get(i);

            if (key.isVariable()) {
                Term existingValue = result.putIfAbsent(key, value);

                if (existingValue != null && existingValue != value) {
                    return null;
                }

            } else if (key instanceof ListTerm && value instanceof ListTerm) {
                Map<Term, Term> innerMap =
                        filterLists(((ListTerm) key).asList(), ((ListTerm) value).asList());

                if (innerMap == null) {
                    return null;
                }

                result = combineLeft(result, innerMap);
                if (result == null) {
                    return null;
                }

            } else if (!key.equals(value)) {
                return null;
            }
        }

        return result;
    }

    private Map<Term, Term> combineLeft(Map<Term, Term> m1, Map<Term, Term> m2) {
        for (Term innerKey : m2.keySet()) {
            Term innerValue = m2.get(innerKey);
            Term existingValue = m1.putIfAbsent(innerKey, innerValue);

            if (existingValue != null && existingValue != innerValue) {
                return null;
            }
        }

        return m1;
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

        //e.resultSet.forEach(System.out::println);

        e.filter(e.nullableResultSet, map).forEach(System.out::println);
    }
}
