package reverseottr.reader;

import org.apache.jena.rdf.model.*;
import reverseottr.model.Mapping;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.model.Parameter;
import xyz.ottr.lutra.model.terms.*;
import xyz.ottr.lutra.wottr.parser.WTermParser;
import java.util.*;

public class RDFToOTTR {

    // TODO: "asTripleMappings" and "asNullableMappings".
    public static Set<Mapping> asResultSet(Model model, boolean nullable) {
        List<Parameter> params = OTTR.BaseTemplate.Triple.getParameters();
        // TODO: to make sure that lists have correct id,...
        // ... first create terms from graph, then assign template vars from nullable and triple.
        if (nullable) {
            params = OTTR.BaseTemplate.NullableTriple.getParameters();
        }

        Term subVar = params.get(0).getTerm();
        Term predVar = params.get(1).getTerm();
        Term objVar = params.get(2).getTerm();

        List<Statement> list = model.listStatements().toList();

        Set<Mapping> resultSet = new HashSet<>();

        String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        String first = rdf + "first";
        String rest = rdf + "rest";

        for (Statement s : list) {

            Term sub = WTermParser.toTerm(s.getSubject()).get();
            Term pred = WTermParser.toTerm(s.getPredicate()).get();
            Term obj = WTermParser.toTerm(s.getObject()).get();

            String predString = pred.getIdentifier().toString();

            // TODO: Use of list predicates in templates

            // Ensures no funny list-triples:
            if (predString.equals(first) || predString.equals(rest)) {
                if (sub instanceof ListTerm && !((ListTerm) sub).asList().isEmpty()) {
                    sub = WTermParser.toBlankNodeTerm(
                            s.getSubject().asNode().getBlankNodeId()).get();
                }
                if (obj instanceof ListTerm && !((ListTerm) obj).asList().isEmpty()) {
                    obj = WTermParser.toBlankNodeTerm(
                            s.getObject().asResource().asNode().getBlankNodeId()).get();
                }
            }

            Mapping map = new Mapping();
            map.put(subVar, sub);
            map.put(predVar, pred);
            map.put(objVar, obj);

            resultSet.add(map);
        }

        return resultSet;
    }
}
