package instructable.server;

import org.json.simple.JSONArray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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

    public static List<String> convertJArrToStrList(JSONArray jsonArray)
    {
        List<String> list = new ArrayList<String>();
        for (int i=0; i<jsonArray.size(); i++) {
            list.add( jsonArray.get(i).toString() );
        }
        return list;
    }

    private InstUtils()
    {
        //static class
    }
}
