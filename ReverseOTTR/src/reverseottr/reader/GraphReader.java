package reverseottr.reader;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;

import java.util.List;

public class GraphReader {

    /**
     *
     * @param fileName
     * @return
     */
    public Model read(String fileName) {
        Model model = ModelFactory.createDefaultModel();
        model.read(fileName);

        return model;
    }

    public static void main(String[] args) {
        GraphReader reader = new GraphReader();
        List<Statement> list = reader.read(args[0]).listStatements().toList();

        list.forEach(System.out::println);
    }
}
