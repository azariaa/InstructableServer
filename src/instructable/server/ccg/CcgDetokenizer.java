package instructable.server.ccg;

import com.google.common.base.Function;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import opennlp.tools.tokenize.DetokenizationDictionary;
import opennlp.tools.tokenize.DictionaryDetokenizer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Amos Azaria on 03-Jun-15.
 */
public class CcgDetokenizer
{
    static String[][] detokenizeConverter = new String[][]{new String[]{"i","I"}};

    static String tokensToMove[] = new String[]{".", "!", "?", ",", "$", "(", ")", "[", "]", "\"", "'", ":", "n't", "'m", "'s", "n't", "'ll"};
    static DetokenizationDictionary.Operation operations[] = new DetokenizationDictionary.Operation[]{
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
            DetokenizationDictionary.Operation.MOVE_LEFT,
            DetokenizationDictionary.Operation.MOVE_LEFT
    };
    static DictionaryDetokenizer detokenizer = new DictionaryDetokenizer(new DetokenizationDictionary(tokensToMove, operations));

    static String detokenizeOneToken(String token)
    {
        for (String[] rule : detokenizeConverter)
        {
            if (token.equals(rule[0]))
                return rule[1];
        }
        return token;
    }

    /*
    This function gets a list of tokens which was joint using " ", and hopefully returns the string that originated these tokens.
    This uses DictionaryDetokenizer from OpenNLP, however, I couldn't find a public DetokenizationDictionary.
     */
    private static Expression2 detokenizeToExpression(List<String> tokenizedStr)
    {
        //tokenizedStr.replaceAll(CcgDetokenizer::detokenizeOneToken);
        List<String> replacedRules = tokenizedStr.stream().map(CcgDetokenizer::detokenizeOneToken).collect(Collectors.toList());

        return Expression2.constant("\"" + detokenizer.detokenize(replacedRules.toArray(new String[0]), null) + "\"");
    }

    public static Function<List<String>, Expression2> getDetokenizer()
    {
        return CcgDetokenizer::detokenizeToExpression;
    }
}
