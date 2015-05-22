package instructable.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private InstUtils()
    {
        //static class
    }
}
