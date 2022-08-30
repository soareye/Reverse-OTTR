package reverseottr.evaluation;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import reverseottr.model.Mapping;
import reverseottr.model.Placeholder;
import reverseottr.model.TermRegistry;
import reverseottr.reader.GraphReader;
import reverseottr.reader.RDFToOTTR;
import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.api.StandardFormat;
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.model.*;
import xyz.ottr.lutra.model.terms.*;

import java.util.*;
import java.util.stream.Collectors;

public class Evaluator {

    private Set<Mapping> triples;
    private Set<Mapping> nullableTriples;
    private TemplateManager templateManager;
    private int maxRepetitions;

    public Evaluator(Model model, TemplateManager templateManager) {
        this.triples = RDFToOTTR.asResultSet(model, false);
        this.nullableTriples = RDFToOTTR.asResultSet(model, true);
        this.templateManager = templateManager;
    }

    public Set<Mapping> evaluateTemplate(Template template) {
        Set<Mapping> templateMappings;

        if (template.getIri().equals(OTTR.BaseURI.Triple)) {
            templateMappings = this.triples;

        } else if (template.getIri().equals(OTTR.BaseURI.NullableTriple)) {
            templateMappings = this.nullableTriples;

        } else {
            Set<Set<Mapping>> instanceEvaluations =
                    template.getPattern().stream()
                            .map(this::evaluateInstance)
                            .collect(Collectors.toSet());

            templateMappings = Mapping.joinAll(instanceEvaluations);
        }

        List<Parameter> parameters = template.getParameters();

        Set<Mapping> result = paramFilter(templateMappings, parameters);

        result = defaultAlternativesAll(result, parameters);

        result = placeholderFilter(result, parameters);

        result.addAll(generateNonOptSolutions(parameters));
        // TODO: Filter placeholders in nonOptSolutions.
        return result;
    }

    private Set<Mapping> placeholderFilter(Set<Mapping> mappings, List<Parameter> parameters) {
        Set<Mapping> result = new HashSet<>();
        for (Mapping mapping : mappings) {
            Mapping resultMap = new Mapping();
            for (Parameter parameter : parameters) {
                Term var = parameter.getTerm();
                if (mapping.get(var) instanceof Placeholder) {
                    resultMap.put(var, TermRegistry.GLB(mapping.get(var),
                            TermRegistry.paramPlaceholder(parameter)));
                } else {
                    resultMap.put(var, mapping.get(var));
                }
            }

            result.add(resultMap);
        }

        return result;
    }

    public Set<Mapping> evaluateInstance(Instance instance) {
        Template template = templateManager.getTemplateStore()
                .getTemplate(instance.getIri()).get();

        Set<Mapping> templateResult = evaluateTemplate(template);

        List<Term> paramVars = template.getParameters().stream()
                .map(Parameter::getTerm).collect(Collectors.toList());

        List<Term> argTerms = instance.getArguments().stream()
                .map(Argument::getTerm).collect(Collectors.toList());

        Mapping argMap = new Mapping(paramVars, argTerms);

        if (instance.hasListExpander()) {
            ListUnexpander listUnexpander =
                    new ListUnexpander(template.getParameters(), instance.getArguments());

            if (instance.getListExpander().equals(ListExpander.zipMin)) {
                // filter of unzipMin of eval of template
                return null;

            } else if (instance.getListExpander().equals(ListExpander.zipMax)) {
                Set<Mapping> unexpanded = listUnexpander.unzipMax(templateResult);
                return argFilter(unexpanded, argMap);

            } else {
                // filter of uncross of eval of template
                return null;
            }

        } else {
            return argFilter(templateResult, argMap);
        }
    }

    private Set<Mapping> argFilter(Set<Mapping> maps, Mapping argMap) {
        Set<Mapping> resultMaps = new HashSet<>();

        for (Mapping map : maps) {
            if (Mapping.innerCompatible(map, argMap)) {
                resultMaps.add(Mapping.transform(map, argMap));
            }
        }

        return resultMaps;
    }

    /** Generates additional mappings which have ottr:none for non-optional vars
     * and thus are ignored in forward OTTR **/
    private Set<Mapping> generateNonOptSolutions(List<Parameter> parameters) {
        Set<Mapping> result = new HashSet<>();

        for (Parameter noParam : parameters) {
            if (!noParam.isOptional()) {
                Mapping m = new Mapping();
                m.put(noParam.getTerm(), new NoneTerm());
                for (Parameter parameter : parameters) {
                    if (!parameter.equals(noParam)) {
                        if (parameter.isNonBlank()) {
                            m.put(parameter.getTerm(), TermRegistry.any_nb);
                        } else {
                            m.put(parameter.getTerm(), TermRegistry.any);
                        }
                    }
                }

                result.add(m);
            }
        }

        return result;
    }

    /** Filters mappings according to parameter modifiers **/
    private Set<Mapping> paramFilter(Set<Mapping> maps, List<Parameter> parameters) {
        return maps.stream()
                .filter(m -> conforms(m, parameters))
                .collect(Collectors.toSet());
    }

    /** Checks if a mapping conforms to a list of parameters **/
    private boolean conforms(Mapping map, List<Parameter> parameters) {
        for (Parameter param : parameters) {
            Term var = param.getTerm();
            if (map.containsVar(var)) {
                if ((map.get(var) instanceof NoneTerm && !param.isOptional())
                        || (param.isNonBlank() && map.get(var) instanceof BlankNodeTerm)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Set<Mapping> defaultAlternativesAll(Set<Mapping> maps, List<Parameter> parameters) {
        Set<Mapping> resultMaps = new HashSet<>();
        maps.forEach(m -> resultMaps.addAll(defaultAlternatives(m, parameters)));

        return resultMaps;
    }

    private Set<Mapping> defaultAlternatives(Mapping map, List<Parameter> parameters) {
        Mapping baseMap = new Mapping();
        Mapping subMap = new Mapping();

        for (Parameter param : parameters) {
            Term var = param.getTerm();
            if (param.hasDefaultValue()
                    && map.get(var).equals(param.getDefaultValue())) {
                subMap.put(var, map.get(var));

            } else {
                baseMap.put(var, map.get(var));
            }
        }

        if (subMap.domain().isEmpty()) {
            Set<Mapping> result = new HashSet<>();
            result.add(baseMap);
            return result;
        }

        return allCombinationsNone(subMap).stream()
                .map(m -> Mapping.union(m, baseMap))
                .collect(Collectors.toSet());
    }

    private Set<Mapping> allCombinationsNone(Mapping map) {
        Set<Set<Mapping>> combinations = new HashSet<>();

        for (Term var : map.domain()) {
            Mapping subMap = new Mapping();
            subMap.put(var, map.get(var));

            Mapping noneMap = new Mapping();
            noneMap.put(var, new NoneTerm());

            Set<Mapping> combination = new HashSet<>();
            combination.add(subMap);
            combination.add(noneMap);

            combinations.add(combination);
        }

        return Mapping.joinAll(combinations);
    }

    public static void main(String[] args) {

        String graphPath = "C:/Users/Erik/Documents/revottr/Reverse-OTTR/ReverseOTTR/src/test/graph.ttl";
        String libPath = "C:/Users/Erik/Documents/revottr/Reverse-OTTR/ReverseOTTR/src/test/lib.stottr";

        Model model = GraphReader.read(graphPath);
        TemplateManager templateManager = new StandardTemplateManager();
        templateManager.readLibrary(templateManager.getFormat(StandardFormat.stottr.name()), libPath);

        Evaluator e = new Evaluator(model, templateManager);

        String templateIRI = "http://test.com/Test";

        Set<Mapping> s = e.evaluateTemplate(
                templateManager.getTemplateStore().getTemplate(templateIRI).get()
        );

        //s.forEach(System.out::println);

        PrefixMapping prefixes = templateManager.getPrefixes();
        prefixes.setNsPrefix("ph", TermRegistry.ph_ns);
        //System.out.println(prefixes);

        for (Mapping m : s) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{");
            for (Term var : m.domain()) {
                stringBuilder.append(var.getIdentifier().toString());
                stringBuilder.append("=");
                stringBuilder.append(m.get(var).toString(prefixes));
                stringBuilder.append(",");
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.append("}");
            System.out.println(stringBuilder);
        }
    }
}
