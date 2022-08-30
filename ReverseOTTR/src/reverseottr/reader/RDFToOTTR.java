package reverseottr.reader;

import org.apache.jena.rdf.model.*;
import reverseottr.model.Mapping;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.model.Parameter;
import xyz.ottr.lutra.model.terms.*;
import xyz.ottr.lutra.wottr.parser.WTermParser;
import java.util.*;

public class RDFToOTTR {

    // TODO: filter out none-values if nullable==false.
    // TODO: Maybe add general method of dealing with "none" according to parameters.
    // TODO: "asTriples" and "asNullableTriples".
    public static Set<Mapping> asResultSet(Model model, boolean nullable) {
        List<Parameter> params = OTTR.BaseTemplate.Triple.getParameters();

        if (nullable) {
            params = OTTR.BaseTemplate.NullableTriple.getParameters();
        }

        Term subVar = params.get(0).getTerm();
        Term predVar = params.get(1).getTerm();
        Term objVar = params.get(2).getTerm();

        List<Statement> list = model.listStatements().toList();

        Set<Mapping> resultSet = new HashSet<>();

        for (Statement s : list) {
            Term sub = WTermParser.toTerm(s.getSubject()).get();
            Term pred = WTermParser.toTerm(s.getPredicate()).get();
            Term obj = WTermParser.toTerm(s.getObject()).get();

            Mapping map = new Mapping();
            map.put(subVar, sub);
            map.put(predVar, pred);
            map.put(objVar, obj);

            resultSet.add(map);
        }

        return resultSet;
    }
}
