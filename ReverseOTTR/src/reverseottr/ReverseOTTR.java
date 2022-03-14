package reverseottr;

import xyz.ottr.lutra.model.Argument;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.model.Template;
import xyz.ottr.lutra.model.terms.IRITerm;
import xyz.ottr.lutra.model.terms.Term;
import java.util.*;
import java.util.stream.Collectors;

public class ReverseOTTR {

    public Set<List<Term>> generatePossibleLists(LinkedHashSet<Term> knownTerms, int maxRep) {
        Set<List<Term>> set = new HashSet<>();
        List<Term> list = new LinkedList<>();

        return set;
    }

    private List<List<Term>> allPermutations(List<Term> list) {

        if (list.size() == 1) {
            List<List<Term>> result = new LinkedList<>();
            List<Term> temp = new LinkedList<>();
            temp.add(list.get(0));
            result.add(temp);

            return result;

        } else if (list.size() > 1) {
            List<Term> sublist = new LinkedList<>();
            sublist.addAll(list.subList(0, list.size() - 1));
            List<List<Term>> subPerms = allPermutations(sublist);

            Term last = list.get(list.size() - 1);

            List<List<Term>> result = new LinkedList<>();

            for (List<Term> perm : subPerms) {
                List<Term> temp = new LinkedList<>();
                temp.addAll(perm);
                temp.add(last);

                result.add(perm);
                result.add(temp);
            }

            return result;

        } else {
            return new LinkedList<>();
        }
    }

    public Set<Term> extractTerms(List<Template> templates) {
        Set<Term> set = new HashSet<>();
        templates.stream()
                .map(this::extractTerms)
                .forEach(set::addAll);

        return set;
    }

    private Set<Term> extractTerms(Template template) {
        Set<Term> set = new HashSet<>();
        template.getPattern().stream()
                .map(this::extractTerms)
                .forEach(set::addAll);

        return set;
    }

    private Set<Term> extractTerms(Instance instance) {
        return instance.getArguments().stream()
                .map(Argument::getTerm)
                .filter(t -> !t.isVariable())
                .collect(Collectors.toCollection(HashSet::new));
    }

    public static void main(String[] args) {
        ReverseOTTR r = new ReverseOTTR();

        List<Term> test = new LinkedList<>();
        test.add(new IRITerm("a"));
        test.add(new IRITerm("b"));
        test.add(new IRITerm("c"));

        System.out.println(r.allPermutations(test));
    }
}
