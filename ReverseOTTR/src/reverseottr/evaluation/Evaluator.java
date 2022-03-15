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

    public static void main(String[] args) {

        Evaluator e = new Evaluator(GraphReader.read(args[0]));

        List<Parameter> params = OTTR.BaseTemplate.NullableTriple.getParameters();

        Term subVar = params.get(0).getTerm();
        Term predVar = params.get(1).getTerm();
        Term objVar = params.get(2).getTerm();

        Term subVal = new BlankNodeTerm("x");
        subVal.setVariable(true);
        Argument sub = Argument.builder().term(subVal).build();

        Term predVal = new IRITerm(RDF.getURI() + "type");
        Argument pred = Argument.builder().term(predVal).build();

        Term objVal = new BlankNodeTerm("y");
        objVal.setVariable(true);
        Argument obj = Argument.builder().term(objVal).build();

        Map<Term, Argument> map = new HashMap<>();
        map.put(subVar, sub);
        map.put(predVar, pred);
        map.put(objVar, obj);

        e.filter(e.nullableResultSet, map).forEach(System.out::println);
    }
}
