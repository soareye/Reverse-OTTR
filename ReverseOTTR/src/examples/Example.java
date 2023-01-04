package examples;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.PrefixMapping;
import reverseottr.evaluation.Evaluator;
import reverseottr.model.Mapping;
import reverseottr.model.TermRegistry;
import xyz.ottr.lutra.api.StandardFormat;
import xyz.ottr.lutra.api.StandardTemplateManager;
import java.util.Set;

public class Example {

    public static void main(String[] args) {

        String graphPath = Example.class.getResource("exampleGraph.ttl").getPath();
        Model model = ModelFactory.createDefaultModel().read(graphPath);

        StandardTemplateManager templateManager = new StandardTemplateManager();
        String libPath = Example.class.getResource("exampleLib.stottr")
                .getPath().substring(1);
        templateManager.readLibrary(
                templateManager.getFormat(StandardFormat.stottr.name()), libPath);
        templateManager.loadStandardTemplateLibrary();

        Evaluator evaluator = new Evaluator(model, templateManager, 0);

        String queryIRI = "http://example.org/example/Person";
        Set<Mapping> mappings = evaluator.evaluateQuery(queryIRI);

        PrefixMapping prefixMapping = templateManager.getPrefixes();
        prefixMapping.setNsPrefix("ph", TermRegistry.ph_ns);

        mappings.forEach(m -> {
            System.out.println(m.toString(prefixMapping));
        });
    }
}
