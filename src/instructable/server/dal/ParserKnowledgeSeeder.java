package instructable.server.dal;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 24-Jul-15.
 * In this class the DB is just used as a seed and backup. This class does not actually store the usable data.
 * No checking
 */
public class ParserKnowledgeSeeder
{
    final String forAllUsers = "all";

    final Optional<String> userId;
    final private List<String> lexiconEntries;
    final private List<String> synonyms;
    private List<String> userDefinedEntries;
    final private String[] unaryRules;
    final private List<String[]> globalExamples;
    private List<String[]> userExamples;

//    public ParserKnowledgeSeeder(Optional<String> userId)
//    {
//        this.userId = userId;
//        //TODO: first read all general input from DB.
//        fillUserSpecificFromDB();
//    }

    public ParserKnowledgeSeeder(Optional<String> userId, List<String> lexiconEntries, List<String> synonyms, String[] unaryRules, List<String[]> globalExamples)
    {
        this.userId = userId;
        this.lexiconEntries = lexiconEntries;
        this.unaryRules = unaryRules;
        this.synonyms = synonyms;
        this.globalExamples = globalExamples;
        this.userDefinedEntries = new LinkedList<>();
        this.userExamples = new LinkedList<>();

        fillUserSpecificFromDB();
    }

    public ParserKnowledgeSeeder(ParserKnowledgeSeeder parserKnowledgeSeeder, String userId)
    {
        this.userId = Optional.of(userId);
        this.lexiconEntries = parserKnowledgeSeeder.lexiconEntries; //immutable
        this.unaryRules = parserKnowledgeSeeder.unaryRules; //immutable
        this.synonyms = parserKnowledgeSeeder.synonyms; //immutable
        this.globalExamples = parserKnowledgeSeeder.globalExamples; //immutable
        this.userDefinedEntries = new LinkedList<>();
        this.userExamples = new LinkedList<>();
    }

    private void fillUserSpecificFromDB()
    {
        //TODO: fill from DB
        if (!isGeneralUser())
        {

        }
    }

    public List<String> getInitialLexiconEntries()
    {
        return lexiconEntries;
    }

    public List<String> getSynonyms()
    {
        return synonyms;
    }

    public String[] getUnaryRules()
    {
        return unaryRules;
    }

    public List<String> getUserDefinedEntries()
    {
        return userDefinedEntries;
    }

    public List<String[]> getGlobalExamples()
    {
        return globalExamples;
    }

    public List<String[]> getUserExamples()
    {
        return userExamples;
    }

    public List<String[]> getAllExamples()
    {
        List<String[]> allExamples = new LinkedList<>();
        allExamples.addAll(globalExamples);
        allExamples.addAll(userExamples);
        return allExamples;
    }

    public boolean isGeneralUser()
    {
        return !userId.isPresent();
    }

    public void addNewUserLexicons(List<String> newLexiconEntries)
    {
        userDefinedEntries.addAll(newLexiconEntries);
        //TODO: update DB!!!
    }

    public void addNewUserExample(String[] newExample)
    {
        userExamples.add(newExample);
        //TODO: add to DB!!!
    }

    public void removeUserDefinedLex(String lexiconToRemove)
    {
        userDefinedEntries.remove(lexiconToRemove);
        //TODO: update DB!!!
    }

    public boolean hasUserDefinedLex(String lexiconToRemove)
    {
        return userDefinedEntries.stream().anyMatch(s->s.startsWith(lexiconToRemove));
    }
}
