package reverseottr.reader;

import org.apache.jena.rdf.model.*;
import reverseottr.model.Mapping;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.model.Parameter;
import xyz.ottr.lutra.model.terms.*;
import xyz.ottr.lutra.wottr.parser.WTermParser;
import java.util.*;

public class RDFToOTTR {

    private static final String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String first = rdf + "first";
    private static final String rest = rdf + "rest";

    public static Set<Mapping> asResultSet(Model model, boolean nullable) {
        List<Parameter> params = OTTR.BaseTemplate.Triple.getParameters();
        if (nullable) {
            params = OTTR.BaseTemplate.NullableTriple.getParameters();
        }

        Term sVar = params.get(0).getTerm();
        Term pVar = params.get(1).getTerm();
        Term oVar = params.get(2).getTerm();

        List<Statement> list = model.listStatements().toList();

        Set<Mapping> resultSet = new HashSet<>();

        for (Statement statement : list) {

            Term s = WTermParser.toTerm(statement.getSubject()).get();
            Term p = WTermParser.toTerm(statement.getPredicate()).get();
            Term o = WTermParser.toTerm(statement.getObject()).get();

            String pString = p.getIdentifier().toString();

            // Ensures no funny list-triples:
            /*
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
             */
            Mapping map = new Mapping();
            map.put(sVar, s);
            map.put(pVar, p);
            map.put(oVar, o);

            if (!pString.equals(first) && !pString.equals(rest))
                resultSet.add(map);
        }

        return resultSet;
    }
}
