package experiment;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Factory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.shared.PrefixMapping;
import reverseottr.evaluation.Evaluator;
import reverseottr.model.Mapping;
import reverseottr.model.TermRegistry;
import reverseottr.reader.GraphReader;
import reverseottr.reader.RDFToOTTR;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.api.StandardFormat;
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.io.Format;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.model.Parameter;
import xyz.ottr.lutra.model.terms.*;
import xyz.ottr.lutra.system.ResultStream;
import xyz.ottr.lutra.wottr.WOTTR;
import xyz.ottr.lutra.wottr.parser.WTermParser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.*;

public class Experiments {

    private static final Map<BlankNodeTerm, Resource> createdBlankNodes = new HashMap<>();
    private final static String ns = "http://example.com/";
    private final static String[] templateIRIs = {
            "http://tpl.ottr.xyz/rdfs/0.2/TypedResourceDescription",
            "http://tpl.ottr.xyz/rdf/0.1/StatementTriple",
            "http://tpl.ottr.xyz/owl/declaration/0.1/Ontology",
            "http://tpl.ottr.xyz/owl/declaration/0.1/ObjectProperty",
            "http://tpl.ottr.xyz/owl/util/0.1/ObjectCardinality",
            "http://tpl.ottr.xyz/owl/restriction/0.1/ObjectAllValuesFrom",
            "http://tpl.ottr.xyz/owl/axiom/0.1/SubObjectPropertyOf",
            "http://tpl.ottr.xyz/owl/axiom/0.1/EquivalentObjectProperty",
            "http://tpl.ottr.xyz/owl/axiom/0.1/EquivalentDataProperty",
            "http://tpl.ottr.xyz/owl/axiom/0.1/EquivalentClass"};

    private static void run(Model model, StandardTemplateManager manager, String queryIRI, int repetitions) {
        int n = 0;
        long time = 0;

        Model testModel = model;

        while (time < 60000) {
            System.out.println("Iteration: " + n);
            System.out.println("Graph size: " + model.size());

            Evaluator evaluator = new Evaluator(model, manager, repetitions);

            long t1 = System.currentTimeMillis();
            Set<Mapping> result = evaluator.evaluateQuery(queryIRI);
            long t2 = System.currentTimeMillis();

            time = t2 - t1;
            System.out.println("Execution time: " + time + " ms");
            System.out.println("Result size: " + result.size());

            //PrefixMapping prefixes = manager.getPrefixes();
            //prefixes.setNsPrefix("ph", TermRegistry.ph_ns);
            //result.forEach(m -> System.out.println(m.toString(prefixes)));

            testModel = scramble(testModel);
            model.add(testModel);
            n++;
        }
    }

    private static Model generateTestGraph(Model model, int n) {
        if (n > 0) {
            model.add(generateTestGraph(scramble(model), n - 1));
        }
        return model;
    }

    private static Model scramble(Model model) {
        Model result = ModelFactory.createDefaultModel();
        result.setNsPrefixes(model.getNsPrefixMap());

        Set<Mapping> mappings = RDFToOTTR.asResultSet(model, false);
        for (Mapping mapping : mappings) {
            result.add(mappingToStatement(result, scrambleTriple(mapping)));
        }

        return result;
    }

    private static Mapping scrambleTriple(Mapping mapping) {
        List<Parameter> params = OTTR.BaseTemplate.Triple.getParameters();
        Term sVar = params.get(0).getTerm();
        Term pVar = params.get(1).getTerm();
        Term oVar = params.get(2).getTerm();

        Term s = mapping.get(sVar);
        Term p = mapping.get(pVar);
        Term o = mapping.get(oVar);

        if (Math.random() < 0.33) s = randomIRITerm();
        if (Math.random() < 0.33) p = randomIRITerm();
        if (Math.random() < 0.33) o = randomIRITerm();

        Mapping result = new Mapping();
        result.put(sVar, s);
        result.put(pVar, p);
        result.put(oVar, o);

        return result;
    }

    private static Term randomIRITerm() {
        int random = (int) (Math.random()*Integer.MAX_VALUE);
        return new IRITerm(ns + "r" + random);
    }

    private static Statement mappingToStatement(Model model, Mapping mapping) {
        List<Parameter> params = OTTR.BaseTemplate.Triple.getParameters();

        Term sVar = params.get(0).getTerm();
        Term pVar = params.get(1).getTerm();
        Term oVar = params.get(2).getTerm();

        Term s = mapping.get(sVar);
        Term p = mapping.get(pVar);
        Term o = mapping.get(oVar);

        return model.createStatement(term(model, s).asResource(),
                term(model, p).as(Property.class),
                term(model, o));
    }

    static RDFNode term(Model model, Term term) {
        if (term instanceof ListTerm) {
            return listTerm(model, (ListTerm) term);
        } else if (term instanceof IRITerm) {
            return iriTerm(model, (IRITerm) term);
        } else if (term instanceof LiteralTerm) {
            return literalTerm(model, (LiteralTerm) term);
        } else if (term instanceof BlankNodeTerm) {
            return blankNodeTerm(model, (BlankNodeTerm) term);
        } else if (term instanceof NoneTerm) {
            return none(model);
        }

        return null;
    }

    private static RDFList listTerm(Model model, ListTerm term) {
        Iterator<RDFNode> iterator = term.asList().stream()
                .map(t -> term(model, t))
                .iterator();
        return model.createList(iterator);
    }

    private static Resource iriTerm(Model model, IRITerm term) {
        return model.createResource(term.getIri());
    }

    private static Literal literalTerm(Model model, LiteralTerm term) {
        String val = term.getValue();

        if (term.getLanguageTag() != null) { // Literal with language tag
            String tag = term.getLanguageTag();
            return model.createLiteral(val, tag);
        } else if (term.getDatatype() != null) { // Typed literal
            String type = term.getDatatype();
            TypeMapper tm = TypeMapper.getInstance();
            return model.createTypedLiteral(val, tm.getSafeTypeByName(type));
        } else {
            return model.createLiteral(val);
        }
    }

    private static Resource blankNodeTerm(Model model, BlankNodeTerm term) {
        return createdBlankNodes.computeIfAbsent(term, l -> model.createResource());
    }

    private static Resource none(Model model) {
        return WOTTR.none.inModel(model);
    }

    private static void printInstances() {
        int e = 0;
        int s = 0;
        int n = 0;
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("o-owl-ut:ObjectCardinality(");
            sb.append("ex:e").append(e++).append(",");
            sb.append("ex:e").append(e++).append(",");
            //sb.append("\"s").append(s++).append("\",");
            //sb.append("(ex:e").append(e++).append("),");
            sb.append("\"").append(n++).append("\"^^xsd:nonNegativeInteger,");
            sb.append("ex:e").append(e++).append(",");
            sb.append("ex:e").append(e++);
            sb.append(") .");
            System.out.println(sb);
        }
    }

    public static void main(String[] args) {
        for (int i = 3; i < templateIRIs.length; i++) {
            String relGraphPath = "graphs/graph" + (i + 1) + ".ttl";
            URL url = Experiments.class.getResource(relGraphPath);
            String graphPath = url.getPath();

            StandardTemplateManager templateManager = new StandardTemplateManager();
            templateManager.loadStandardTemplateLibrary();
            String templateIRI = templateIRIs[i];

            Model model = GraphReader.read(graphPath);
            model.setNsPrefixes(templateManager.getPrefixes());
            model.setNsPrefix("ex", ns);

            System.out.println(templateIRI);
            run(model, templateManager, templateIRI, 1);
            System.out.println();
        }
    }
}
