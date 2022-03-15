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
    public static Model read(String fileName) {
        Model model = ModelFactory.createDefaultModel();
        model.read(fileName);

        return model;
    }
}
