package reverseottr.evaluation;

import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.codehaus.plexus.util.StringUtils;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.model.Argument;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.model.terms.*;
import org.apache.jena.vocabulary.RDF;
import java.util.LinkedList;
import java.util.List;

public class RDFToOTTR {

    public static Instance asNullableTriple(Statement s, Model model) {
        Term sub = asTerm(s.getSubject(), model);
        Term pred = asTerm(s.getPredicate(), model);
        Term obj = asTerm(s.getObject(), model);

        return Instance.builder()
                .iri(OTTR.BaseURI.NullableTriple)
                .argument(Argument.builder().term(sub).build())
                .argument(Argument.builder().term(pred).build())
                .argument(Argument.builder().term(obj).build())
                .build();
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
        String datatype = lit.getDatatypeURI();
        String language = lit.getLanguage();
        String value = String.valueOf(lit.getValue());

        if (StringUtils.isNotEmpty(language)) {
            return LiteralTerm.createLanguageTagLiteral(value, language);

        } else if (StringUtils.isNotEmpty(datatype)) {
            return LiteralTerm.createTypedLiteral(value, datatype);

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
