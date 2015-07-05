package instructable.server;

import org.json.simple.JSONArray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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


    public enum EmailAddressRelation {noEmailFound, isOneEmail, startsWithEmailsButHasMore, listOfEmails}; //containsEmailAndMore: requires reading all tokens (not time efficient)

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private static Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);

    public static boolean isEmailAddress(String addr)
    {
        Matcher matcher = emailPattern.matcher(addr);
        return matcher.matches();
    }

    static String ignoredAnd = "and";

    //ignores the word "and" (unless isOneEmail)
    public static EmailAddressRelation getEmailAddressRelation(List<String> tokens)
    {
        int emailCount = 0;
        int nonEmailCount = 0;
        boolean hasEmailBeforeAndAfterAllAnds = false;
        for (String token : tokens)
        {
            if (token.equals(ignoredAnd))
            {
                if (!hasEmailBeforeAndAfterAllAnds)
                    break;
                hasEmailBeforeAndAfterAllAnds = false;
            }
            else if (isEmailAddress(token))
            {
                hasEmailBeforeAndAfterAllAnds = true;
                emailCount++;
            }
            else
            {
                nonEmailCount++;
                break; //this break is added for performance, may want to remove in order to support containsEmailAndMore
            }
        }
        if (nonEmailCount == 0 && hasEmailBeforeAndAfterAllAnds)
        {
            if (tokens.size() == 1)
                return EmailAddressRelation.isOneEmail;
            if (emailCount > 1)
                return EmailAddressRelation.listOfEmails;
        }
        if (emailCount > 0)
            return EmailAddressRelation.startsWithEmailsButHasMore;
        return EmailAddressRelation.noEmailFound;
    }

    /*
    should be called only if known to be an EmailAddressRelation.listOfEmails
     */
    public static List<String> getEmailsFromEmailList(List<String> tokens)
    {
        LinkedList<String> emailList = new LinkedList<>(tokens);
        emailList.removeAll(Arrays.asList(ignoredAnd));
        return emailList;
    }


    public static List<String> convertJArrToStrList(JSONArray jsonArray)
    {
        List<String> list = new ArrayList<String>();
        for (int i=0; i<jsonArray.size(); i++) {
            list.add(jsonArray.get(i).toString());
        }
        return list;
    }

    private InstUtils()
    {
        //static class
    }
}
