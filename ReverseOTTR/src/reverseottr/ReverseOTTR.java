package reverseottr;

import xyz.ottr.lutra.model.Argument;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.model.Template;
import xyz.ottr.lutra.model.terms.IRITerm;
import xyz.ottr.lutra.model.terms.ListTerm;
import xyz.ottr.lutra.model.terms.Term;

import java.io.File;
import java.util.*;

public class ReverseOTTR {

    public Set<List<Term>> generatePossibleLists(Set<Term> knownTerms, int maxRep) {
        Set<List<Term>> lists = new HashSet<>();
        List<Term> fullList = new LinkedList<>();

        for (Term knownTerm : knownTerms) {
            for (int i = 0; i <= maxRep; i++) {
                fullList.add(knownTerm);
            }
        }

        lists.add(fullList);

        return lists;
    }

    private List<Term> replaceVars(List<Term> terms, Term knownTerm) {
        List<Term> result = new LinkedList<>();
        for (Term term : terms) {
            if (term.isVariable()) {
                result.add(knownTerm);

            } else if (term instanceof ListTerm) {

                //result.add();

            } else {
                result.add(term);
            }
        }

        return result;
    }

    private Set<List<Term>> allSublists(List<Term> list) {
        Set<List<Term>> result = new HashSet<>();

        if (list.size() == 1) {
            List<Term> temp = new LinkedList<>();
            temp.add(list.get(0));
            result.add(temp);

        } else if (list.size() > 1) {
            List<Term> nextList = new LinkedList<>(list.subList(0, list.size() - 1));
            Set<List<Term>> sublists = allSublists(nextList);

            Term last = list.get(list.size() - 1);

            List<Term> single = new LinkedList<>();
            single.add(last);
            result.add(single);

            for (List<Term> sublist : sublists) {
                List<Term> temp = new LinkedList<>();
                temp.addAll(sublist);
                temp.add(last);

                result.add(sublist);
                result.add(temp);
            }
        }

        return result;
    }

    private Set<List<Term>> allPermutations(List<Term> terms) {
        Set<List<Term>> result = new HashSet<>();



        return result;
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
        Set<Term> set = new HashSet<>();
        instance.getArguments().stream()
                .map(Argument::getTerm)
                .map(this::extractTerm)
                .forEach(set::addAll);

        return set;
    }

    private Set<Term> extractTerm(Term term) {
        Set<Term> terms = new HashSet<>();
        if (term instanceof ListTerm) {
            terms.add(term);
            List<Term> listTerm = ((ListTerm) term).asList();
            listTerm.stream().map(this::extractTerm).forEach(terms::addAll);

        } else {
            if (!term.isVariable()) {
                terms.add(term);
            }
        }

        return terms;
    }

    public static void main(String[] args) {
        ReverseOTTR r = new ReverseOTTR();

        String fileName = "C:/Users/Erik/Documents/revottr/Reverse-OTTR/ReverseOTTR/src/test/lib1.stottr";



        Set<Term> testSet = new HashSet<>();
        testSet.add(new IRITerm("a"));
        testSet.add(new IRITerm("b"));
        testSet.add(new IRITerm("c"));

        for (List<Term> list : r.generatePossibleLists(testSet, 1)) {
            //r.allSublists(list).forEach(System.out::println);
        }
    }
}
