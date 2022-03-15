package reverseottr.evaluation;

import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.codehaus.plexus.util.StringUtils;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.model.Parameter;
import xyz.ottr.lutra.model.terms.*;
import org.apache.jena.vocabulary.RDF;

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
            Term sub = asTerm(s.getSubject(), model);
            Term pred = asTerm(s.getPredicate(), model);
            Term obj = asTerm(s.getObject(), model);

            Map<Term, Term> map = new HashMap<>();
            map.put(subVar, sub);
            map.put(predVar, pred);
            map.put(objVar, obj);

            resultSet.add(map);
        }

        return resultSet;
    }

    public static Term asTerm(RDFNode node, Model model) {
        if (isList(node, model)) {
            List<RDFNode> nodes = model.getList(node.asResource()).asJavaList();

            List<Term> terms = new LinkedList<>();

            for (RDFNode n : nodes) {
                terms.add(asTerm(n, model));
            }

            return new ListTerm(terms);

        } else if (node.isURIResource()) {
            return new IRITerm(node.asResource().getURI());

        } else if (node.isLiteral()) {
            return asLiteral(node);

        } else {
            return new BlankNodeTerm();
        }
    }

    private static Term asLiteral(RDFNode node) {
        Literal lit = node.asLiteral();
        String language = lit.getLanguage();
        String value = String.valueOf(lit.getValue());

        if (StringUtils.isNotEmpty(language)) {
            return LiteralTerm.createLanguageTagLiteral(value, language);

        } else {
            return LiteralTerm.createPlainLiteral(value);
        }
    }

    private static boolean isList(RDFNode node, Model model) {
        Property first = new PropertyImpl(RDF.first.getURI());
        Property rest = new PropertyImpl(RDF.rest.getURI());

        boolean isEmptyList = false;

        if (node.isURIResource()) {
            if (node.asResource().getURI().equals(RDF.nil.getURI())) {
                isEmptyList = true;
            }
        }

        return isEmptyList || (node.isAnon() &&
                model.contains(node.asResource(), first) &&
                model.contains(node.asResource(), rest));
    }

    public static void main(String[] args) {
        Model model = ModelFactory.createDefaultModel();
        model.read(args[0]);

        List<Statement> list = model.listStatements().toList();

        for (Statement s : list) {
            System.out.println(asTerm(s.getSubject(), model));
            System.out.println(asTerm(s.getPredicate(), model));
            System.out.println(asTerm(s.getObject(), model));
            System.out.println();
        }
    }
}
