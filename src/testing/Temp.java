package testing;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.shiftreduce.ShiftReduceParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;
import instructable.server.ccg.CcgUtils;
import opennlp.tools.tokenize.DetokenizationDictionary;
import opennlp.tools.tokenize.DictionaryDetokenizer;

import java.io.StringReader;
import java.util.*;

/**
 * Created by Amos Azaria on 25-May-15.
 */
public class Temp
{
    public static void main(String[] args) throws Exception
    {
        Scanner scanIn = new Scanner(System.in);

        while (true)
        {
            String text = scanIn.nextLine();
            if (text.equals("exit"))
                break;

            List<String> tokens = new LinkedList<>();
            List<String> POSs = new LinkedList<>();
            Set<String> dummy = new HashSet<>();
            CcgUtils.tokenizeAndPOS(text, tokens,POSs, true, dummy);
            POSs.stream().forEach(x->System.out.print(x + " "));
            System.out.println();
        }
        //detokenizing();
        //constituency(args);
    }

    private static void detokenizing()
    {
        String tokensToMove[] = new String[]{".", "!", "?", ",", "$", "(", ")", "[", "]", "\"", "'", ":", "n't", "'m", "'s", "n't"};
        DetokenizationDictionary.Operation operations[] = new DetokenizationDictionary.Operation[]{
                DetokenizationDictionary.Operation.MOVE_LEFT,
                DetokenizationDictionary.Operation.MOVE_LEFT,
                DetokenizationDictionary.Operation.MOVE_LEFT,
                DetokenizationDictionary.Operation.MOVE_LEFT,
                DetokenizationDictionary.Operation.MOVE_RIGHT,
                DetokenizationDictionary.Operation.MOVE_RIGHT,
                DetokenizationDictionary.Operation.MOVE_LEFT,
                DetokenizationDictionary.Operation.MOVE_RIGHT,
                DetokenizationDictionary.Operation.MOVE_LEFT,
                DetokenizationDictionary.Operation.RIGHT_LEFT_MATCHING,
                DetokenizationDictionary.Operation.MOVE_LEFT,
                DetokenizationDictionary.Operation.MOVE_LEFT,
                DetokenizationDictionary.Operation.MOVE_LEFT,
                DetokenizationDictionary.Operation.MOVE_LEFT,
                DetokenizationDictionary.Operation.MOVE_LEFT,
                DetokenizationDictionary.Operation.MOVE_LEFT
        };
        DictionaryDetokenizer detokenizer = new DictionaryDetokenizer(new DetokenizationDictionary(tokensToMove, operations));
        String org = detokenizer.detokenize(new String[]{"I", "said",":", "\"", "I","'m", "thinking", "that", "I", "ca", "n't", "\"",".", "What", "did", "you", "?"}, null);
        System.out.println(org);
    }

    private static void constituency(String[] args)
    {
        String modelPath = "resources/englishSR.ser.gz";
        String taggerPath = "resources/english-left3words-distsim.tagger";

        for (int argIndex = 0; argIndex < args.length; ) {
            switch (args[argIndex]) {
                case "-tagger":
                    taggerPath = args[argIndex + 1];
                    argIndex += 2;
                    break;
                case "-model":
                    modelPath = args[argIndex + 1];
                    argIndex += 2;
                    break;
                default:
                    throw new RuntimeException("Unknown argument " + args[argIndex]);
            }
        }

        //String text = "My dog likes to shake his stuffed chickadee toy.";
        MaxentTagger tagger = null;
        try
        {
            tagger = new MaxentTagger(taggerPath);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        ShiftReduceParser model = ShiftReduceParser.loadModel(modelPath);
        Scanner scanIn = new Scanner(System.in);

        while (true)
        {
            String text = scanIn.nextLine();
            if (text.equals("exit"))
                break;

            DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
            for (List<HasWord> sentence : tokenizer) {
                List<TaggedWord> tagged = tagger.tagSentence(sentence);
                Tree tree = model.apply(tagged);
                System.err.println(tree);
            }
        }
    }
}

