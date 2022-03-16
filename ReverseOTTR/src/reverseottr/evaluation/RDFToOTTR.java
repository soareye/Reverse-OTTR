package reverseottr.evaluation;

import org.apache.jena.rdf.model.*;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.model.Parameter;
import xyz.ottr.lutra.model.terms.*;
import xyz.ottr.lutra.wottr.parser.WTermParser;
import java.util.*;

public class RDFToOTTR {

    public static Set<Map<Term, Term>> asResultSet(Model model, boolean nullable) {
        List<Parameter> params = OTTR.BaseTemplate.Triple.getParameters();

        if (nullable) {
            params = OTTR.BaseTemplate.NullableTriple.getParameters();
        }

        Term subVar = params.get(0).getTerm();
        Term predVar = params.get(1).getTerm();
        Term objVar = params.get(2).getTerm();

        List<Statement> list = model.listStatements().toList();

        Set<Map<Term, Term>> resultSet = new HashSet<>();

        for (Statement s : list) {
            Term sub = WTermParser.toTerm(s.getSubject()).get();
            Term pred = WTermParser.toTerm(s.getPredicate()).get();
            Term obj = WTermParser.toTerm(s.getObject()).get();

            Map<Term, Term> map = new HashMap<>();
            map.put(subVar, sub);
            map.put(predVar, pred);
            map.put(objVar, obj);

            resultSet.add(map);
        }

        return resultSet;
    }
}
