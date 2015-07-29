package instructable.server.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by Amos Azaria on 29-Jul-15.
 */
public class DBUtils
{
    static final String instancesTableName = "instances";
    static final String mutableColName = "mutable";
    static final String instanceValTableName = "instance_values";
    static final String userIdColName = "user_id";
    static final String conceptColName = "concept_name";
    static final String instanceColName = "instance_name";
    static final String fieldColName = "field_name";
    static final String fieldJSonValColName = "field_jsonval";

    static final String conceptsTableName = "concepts";
    static final String conceptFieldTableName = "concept_fields";
    static final String fieldTypeCol = "field_type";
    static final String isListColName = "isList";

    static final String lexEntriesTable = "lex_entries";
    static final String examplesTable = "parse_examples";
    static final String userIdCol = "user_id";
    static final String lexEntryCol = "lex_entry";
    static final String exampleSentenceCol = "example_sentence";
    static final String exampleLFCol = "example_lf";

    static public void clearUserData(String userId)
    {
        removeUserFromTable(userId, instancesTableName);
        removeUserFromTable(userId, instanceValTableName);
        removeUserFromTable(userId, conceptsTableName);
        removeUserFromTable(userId, conceptFieldTableName);
        removeUserFromTable(userId, lexEntriesTable);
        removeUserFromTable(userId, examplesTable);
    }

    static public void removeUserFromTable(String userId, String tableName)
    {
        try (
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from " + tableName + " where " + DBUtils.userIdColName + "=? ");
        )
        {
            pstmt.setString(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    private DBUtils()
    {
    }
}
