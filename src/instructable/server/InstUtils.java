package instructable.server;

import opennlp.tools.tokenize.DetokenizationDictionary;
import opennlp.tools.tokenize.DictionaryDetokenizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Amos Azaria on 19-May-15.
 */
public class InstUtils
{
    static Set<String> pluralWords = null;
    static Set<String> singularWords = null;
    public enum Plurality {unknown,singular,plural};
    public static Plurality wordPlurality(String word)
    {
        if (pluralWords == null)
            readData();
        if (pluralWords.contains(word))
            return Plurality.plural;
        if (singularWords.contains(word))
            return Plurality.singular;
        return Plurality.unknown;
    }

    private static void readData()
    {

        pluralWords = read("resources/plural.unigrams.txt");
        singularWords = read("resources/singular.unigrams.txt");

    }

    private static Set<String> read(String file)
    {
        List<String> fileAsList = null;
        try
        {
            fileAsList = Files.readAllLines(Paths.get(file));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return new HashSet<>(fileAsList);
    }

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private static Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    public static boolean isEmailAddress(String addr)
    {
        Matcher matcher = pattern.matcher(addr);
        return matcher.matches();
    }


    static String tokensToMove[] = new String[]{".", "!", "?", ",", "$", "(", ")", "[", "]", "\"", "'", ":", "n't", "'m", "'s", "n't"};
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
            DetokenizationDictionary.Operation.MOVE_LEFT
    };
    static DictionaryDetokenizer detokenizer = new DictionaryDetokenizer(new DetokenizationDictionary(tokensToMove, operations));
    /*
    This function gets a list of tokens which was joint using " ", and hopefully returns the string that originated these tokens.
    This uses DictionaryDetokenizer from OpenNLP, however, I couldn't find a public DetokenizationDictionary.
     */
    public static String detokenizer(String malDetokenizedStr)
    {
        StringBuilder outputStr = new StringBuilder();
        String[] tokens = malDetokenizedStr.split(" ");
        return detokenizer.detokenize(tokens, null);
    }

    private InstUtils()
    {
        //static class
    }
}
