package reverseottr.evaluation;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import reverseottr.model.Mapping;
import reverseottr.model.Placeholder;
import reverseottr.model.RListTerm;
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

    private final Set<Mapping> triples;
    private final Set<Mapping> nullableTriples;
    private final StandardTemplateManager templateManager;
    private int maxRepetitions = 0;

    public Evaluator(Model model, StandardTemplateManager templateManager, int maxRepetitions) {
        this.triples = RDFToOTTR.asResultSet(model, false);
        this.nullableTriples = RDFToOTTR.asResultSet(model, true);
        this.templateManager = templateManager;
        this.maxRepetitions = maxRepetitions;
    }

    public Set<Mapping> evaluateQuery(String IRI) {
        Set<Mapping> result = evaluateTemplate(getTemplate(IRI));
        return result.stream().filter(this::validIDs).collect(Collectors.toSet());
    }

    // A mapping has valid IDs if no list ID is repeated in the mapping.
    private <T> boolean validIDs(Mapping mapping) {
        Set<T> ids = new HashSet<>();

        for (Term var : mapping.domain()) {
            Term term = mapping.get(var);
            if (term instanceof ListTerm
                    && !TermRegistry.isUnexpanded(term)) {
                T id = (T) term.getIdentifier();
                if (ids.contains(id)) {
                    return false;
                } else {
                    ids.add(id);
                }
            }
        }

        return true;
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
        return removeSubMappings(result);
    }

    /** Removes mappings that are less than some other mapping in the input-set
     * according to the partial order in TermRegistry **/
    private Set<Mapping> removeSubMappings(Set<Mapping> mappings) {
        Set<Mapping> result = new HashSet<>();
        for (Mapping mapping : mappings) {
            if (!containsGreater(mappings, mapping)) result.add(mapping);
        }

        return result;
    }

    private boolean containsGreater(Set<Mapping> mappings, Mapping mapping) {
        for (Mapping other : mappings) {
            if (lessOrEqualMapping(mapping, other) && !mapping.equals(other))
                return true;
        }

        return false;
    }

    private boolean lessOrEqualMapping(Mapping m1, Mapping m2) {
        for (Term var : m1.domain()) {
            if (!TermRegistry.lessOrEqual(m1.get(var), m2.get(var)))
                return false;
        }
        return true;
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
        Template template = getTemplate(instance.getIri());

        Set<Mapping> templateResult = evaluateTemplate(template);

        if (instance.hasListExpander()) {
            ListUnexpander unexpander =
                    new ListUnexpander(template.getParameters(),
                            instance.getArguments(), maxRepetitions);

            if (instance.getListExpander().equals(ListExpander.zipMin)) {
                templateResult = unexpander.unzipMin(templateResult);

            } else if (instance.getListExpander().equals(ListExpander.zipMax)) {
                templateResult = unexpander.unzipMax(templateResult);

            } else {
                templateResult = unexpander.uncross(templateResult);
            }
        }

        List<Term> paramVars = template.getParameters().stream()
                .map(Parameter::getTerm).collect(Collectors.toList());

        List<Term> argTerms = instance.getArguments().stream()
                .map(Argument::getTerm).collect(Collectors.toList());

        Mapping argMap = new Mapping(paramVars, argTerms);

        return argFilter(templateResult, argMap);
    }

    private Set<Mapping> argFilter(Set<Mapping> mappings, Mapping argMap) {
        Set<Mapping> result = new HashSet<>();

        for (Mapping map : mappings) {
            if (Mapping.innerCompatible(map, argMap)) {
                result.add(Mapping.transform(map, argMap));
            }
        }

        return result;
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

    /** Checks whether a mapping conforms to a list of parameters,
     * where the mapping-variables are the same as the parameter-variables**/
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

    private Template getTemplate(String templateIRI) {
        if (templateManager.getTemplateStore().containsTemplate(templateIRI)) {
            return templateManager.getTemplateStore().getTemplate(templateIRI).get();
        }
        return templateManager.getStandardLibrary().getTemplate(templateIRI).get();
    }

    public static void main(String[] args) {
        String graphPath = "C:/Users/Erik/Documents/revottr/Reverse-OTTR/ReverseOTTR/src/test/graph2.ttl";
        String libPath = "C:/Users/Erik/Documents/revottr/Reverse-OTTR/ReverseOTTR/src/test/lib.stottr";

        Model model = GraphReader.read(graphPath);
        StandardTemplateManager templateManager = new StandardTemplateManager();
        templateManager.loadStandardTemplateLibrary();
        templateManager.readLibrary(templateManager.getFormat(StandardFormat.stottr.name()), libPath);

        Evaluator e = new Evaluator(model, templateManager, 0);

        String templateIRI = "http://tpl.ottr.xyz/rdfs/0.2/TypedResourceDescription";
        Set<Mapping> s = e.evaluateQuery(templateIRI);

        PrefixMapping prefixes = templateManager.getPrefixes();
        prefixes.setNsPrefix("ph", TermRegistry.ph_ns);

        s.forEach(m -> System.out.println(m.toString(prefixes)));
        System.out.println(s.size());
    }
}
