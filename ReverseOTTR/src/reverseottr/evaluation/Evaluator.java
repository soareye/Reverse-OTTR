package reverseottr.evaluation;

import xyz.ottr.lutra.OTTR;
import xyz.ottr.lutra.model.Argument;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.model.Template;
import xyz.ottr.lutra.model.terms.Term;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Evaluator {

    public static Instance evaluateTemplate(Template template, List<Argument> arguments) {
        if (template.getIri().equals(OTTR.BaseURI.Triple)) {
            // Compare args to statements (with filter?), discard statements with ottr:none

        } else if (template.getIri().equals(OTTR.BaseURI.NullableTriple)) {
            // Compare args to statements (with filter?)

        } else {

        }

        return null;
    }

    public static Instance evaluateInstance(Instance instance) {

        return null;
    }
}
