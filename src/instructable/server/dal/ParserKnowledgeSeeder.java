package instructable.server.dal;

import com.google.common.base.Preconditions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    //final String forAllUsers = "all";

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
        if (userId.isPresent())
        {
            try (
                    Connection connection = InMindDataSource.getDataSource().getConnection();
                    PreparedStatement pstmt = connection.prepareStatement("select " + DBUtils.lexEntryCol + " from " + DBUtils.lexEntriesTable + " where " + DBUtils.userIdCol + "=?");
            )
            {
                pstmt.setString(1, userId.get());

                try (ResultSet resultSet = pstmt.executeQuery())
                {
                    while (resultSet.next())
                    {
                        String lexEntry = resultSet.getString(DBUtils.lexEntryCol);
                        userDefinedEntries.add(lexEntry);
                    }
                }
            } catch (SQLException e)
            {
                e.printStackTrace();
            }

            try (
                    Connection connection = InMindDataSource.getDataSource().getConnection();
                    PreparedStatement pstmt = connection.prepareStatement("select " + DBUtils.exampleSentenceCol + "," + DBUtils.exampleLFCol + " from " + DBUtils.examplesTable + " where " + DBUtils.userIdCol + "=?");
            )
            {
                pstmt.setString(1, userId.get());

                try (ResultSet resultSet = pstmt.executeQuery())
                {
                    while (resultSet.next())
                    {
                        String exampleSentence = resultSet.getString(DBUtils.exampleSentenceCol);
                        String exampleLF = resultSet.getString(DBUtils.exampleLFCol);
                        userExamples.add(new String[]{exampleSentence, exampleLF});
                    }
                }
            } catch (SQLException e)
            {
                e.printStackTrace();
            }

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
        Preconditions.checkState(!isGeneralUser());
        userDefinedEntries.addAll(newLexiconEntries);
        //update DB!
        for (String newLex : newLexiconEntries)
        {
            try (
                    Connection connection = InMindDataSource.getDataSource().getConnection();
                    PreparedStatement pstmt = connection.prepareStatement("insert into " + DBUtils.lexEntriesTable + " (" + DBUtils.userIdCol + "," + DBUtils.lexEntryCol + ") values (?,?)");
            )
            {
                pstmt.setString(1, userId.get());
                pstmt.setString(2, newLex);
                pstmt.executeUpdate();
            } catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void addNewUserExample(String[] newExample)
    {
        Preconditions.checkState(!isGeneralUser());
        userExamples.add(newExample);
        //add to DB!
        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("insert into " + DBUtils.examplesTable + " (" + DBUtils.userIdCol + "," + DBUtils.exampleSentenceCol + "," + DBUtils.exampleLFCol + ") values (?,?,?)");
        )
        {
            pstmt.setString(1, userId.get());
            String exampleSentence = newExample[0];
            pstmt.setString(2, exampleSentence);
            String exampleLF = newExample[1];
            pstmt.setString(3, exampleLF);
            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void removeUserDefinedLex(String lexiconToRemove)
    {
        Preconditions.checkState(!isGeneralUser());
        userDefinedEntries.remove(lexiconToRemove);

        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from " + DBUtils.lexEntriesTable + " where " + DBUtils.userIdCol + "=? and " + DBUtils.lexEntryCol + "=?");
        )
        {
            pstmt.setString(1, userId.get());
            pstmt.setString(2, lexiconToRemove);
            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

    }

    //should also have remove user example...

    public boolean hasUserDefinedLex(String lexiconToRemove)
    {
        return userDefinedEntries.stream().anyMatch(s -> s.startsWith(lexiconToRemove));
    }
}
