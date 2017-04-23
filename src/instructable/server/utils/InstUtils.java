package instructable.server.utils;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import instructable.server.Credentials;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Amos Azaria on 19-May-15.
 */
public class InstUtils
{
    static Set<String> pluralWords = null;
    static Set<String> singularWords = null;
    static Parser parser = new Parser();

    public static Optional<Date> getDate(String val)
    {
        List<DateGroup> dates = parser.parse(val);
        if (dates.size() > 0)
            return Optional.of(dates.get(0).getDates().get(0));
        else
            return Optional.empty();
    }

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
        for (int i=0; i<jsonArray.length(); i++) {
            try
            {
                list.add(jsonArray.get(i).toString());
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static String alphaNumLower(String org) //this is problematic for multilingual
    {
        return org.replaceAll("[^a-zA-Z0-9' ]", "").toLowerCase().trim();
    }


    public static String callServer(String fullUrlWithQueries) throws Exception
    {
        return callServer(fullUrlWithQueries, new HashMap<>());
    }

    public static String callServer(String fullUrlWithQueries, Map<String, String> keyValueToAddToHeader) throws Exception
    {
        HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead

        HttpGet request = new HttpGet(fullUrlWithQueries);
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        for (Map.Entry<String,String> entry: keyValueToAddToHeader.entrySet())
        {
            request.addHeader(entry.getKey(), entry.getValue());
        }

        HttpResponse httpResponse = httpClient.execute(request);

        if (httpResponse.getStatusLine().getStatusCode() != 200)
        {
            System.out.println("S: error. (response code is: " + httpResponse.getStatusLine().getStatusCode() + ")");
        }

        String response = new BasicResponseHandler().handleResponse(httpResponse);//httpResponse.getEntity().toString();
        return response;
    }
    public static String getFirstYoutubeResponse(String spaceSeparatedTerms)
    {
        try
        {
            String url = "https://www.googleapis.com/youtube/v3/search?key="+ Credentials.youTubeApiKey + "&part=snippet&q=" + spaceSeparatedTerms.replace(" ", "+");
            JSONObject jsonObj = new JSONObject(callServer(url));
            String video = (jsonObj.getJSONArray("items").getJSONObject(0)).getJSONObject("id").getString("videoId");
            return video;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

    public static class NewsInfo
    {
        public NewsInfo(String title, String webLink)
        {
            this.title = title;
            this.webLink = webLink;
        }
        public String title = "";
        public String webLink = "";
        public String summary = "";
    }

    static Map<String, List<String>> theGuardianSections = new HashMap<>();
    static Date sectionLastUpdate = null;
    static Stemmer stemmer = new Stemmer(); //should really be a static class so won't have multithreading problems

    public static List<NewsInfo> getGuardianLinks(String spaceSeparatedTerms, int numOfTitles)
    {
        List<NewsInfo> newsInfos = new LinkedList<>();
        try
        {
            checkIfNeedToUpdateGuardianSections();
            List<String> allTerms = Arrays.asList(spaceSeparatedTerms.split(" "));
            //find all terms that match (a part of) section name, and add all relevant sections.
            List<String> matchingSections = new LinkedList<>();
            List<String> termsLeft = new LinkedList<>();
            for (String term : allTerms)
            {
                String stemmedTerm = stemmer.stem(term.toLowerCase());
                if (theGuardianSections.containsKey(stemmedTerm))
                    matchingSections.addAll(theGuardianSections.get(stemmedTerm));
                else
                    termsLeft.add(term);
            }

            String url = "https://content.guardianapis.com/search?api-key="+ Credentials.theGuardianKey +
                    "&order-by=newest" +
                    (termsLeft.size() > 0 ? "&q=" + String.join("%20AND%20"/*"%20"*/, termsLeft) : "") + //requiring all remaining terms (AND)
                    (matchingSections.size() > 0 ? "&section=" + String.join("%7C", matchingSections) : ""); //"from-date=2017-04-12" (24 hours?)
            JSONObject jsonObj = new JSONObject(callServer(url));
            JSONArray results = jsonObj.getJSONObject("response").getJSONArray("results");
            for (int i = 0; i < numOfTitles && i < results.length(); i++)
            {
                JSONObject newsEntry = results.getJSONObject(i);
                newsInfos.add(new NewsInfo(newsEntry.getString("webTitle"), newsEntry.getString("webUrl")));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return newsInfos;
    }

    private static void checkIfNeedToUpdateGuardianSections() throws Exception
    {
        final long DAY_IN_MS = 1000 * 60 * 60 * 24;
        final String[] stop_words = {"the", "in", "and", "s", "&"};
        if (sectionLastUpdate == null || sectionLastUpdate.before(new Date(System.currentTimeMillis() - (365 * DAY_IN_MS))))
        {
            sectionLastUpdate = new Date();
            theGuardianSections.clear();
            String url = "https://content.guardianapis.com/sections?api-key="+ Credentials.theGuardianKey;
            JSONObject jsonObj = new JSONObject(callServer(url));
            JSONArray allSections = jsonObj.getJSONObject("response").getJSONArray("results");
            for (int i = 0; i < allSections.length(); i++)
            {
                JSONObject currSection = allSections.getJSONObject(i);
                String webTitle = currSection.getString("webTitle");
                String id = currSection.getString("id");
                List<String> sections = Arrays.stream(webTitle.split("[ ']")).
                        filter(a -> Arrays.stream(stop_words).noneMatch(s -> s.equals(a.toLowerCase()))).
                        map(String::toLowerCase).
                        map(s -> stemmer.stem(s)). //stemming all sections
                        collect(Collectors.toList());
                for (String section : sections)
                {
                    if (!theGuardianSections.containsKey(section))
                        theGuardianSections.put(section, new LinkedList<>());
                    theGuardianSections.get(section).add(id);
                }
            }
        }
    }

    public static void getSummaries(List<NewsInfo> newsInfos, int numOfSentences)
    {
        try
        {
            for (int i = 0; i < newsInfos.size(); i++)
            {
                String url = "https://api.aylien.com/api/v1/summarize?sentences_number="+ numOfSentences + "&url=" + newsInfos.get(i).webLink;
                Map<String, String> cred = new HashMap<>();
                cred.put("X-AYLIEN-TextAPI-Application-ID", Credentials.aylienAppId);
                cred.put("X-AYLIEN-TextAPI-Application-Key", Credentials.aylienKey);
                JSONObject jsonObj = new JSONObject(callServer(url, cred));
                JSONArray summaryArr = jsonObj.getJSONArray("sentences");
                StringBuilder summary = new StringBuilder();
                for (int sentenceIdx = 0; sentenceIdx < summaryArr.length(); sentenceIdx++)
                {
                    summary.append(summaryArr.getString(sentenceIdx).replaceAll("\\s+", " ")).append("\n");
                }
                newsInfos.get(i).summary = summary.toString();

            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private InstUtils()
    {
        //static class
    }
}
