package instructable.server.dal;

import instructable.server.hirarchy.FieldDescription;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Amos Azaria on 21-Jul-15.
 * Has a map holding all concepts and the list of their fields
 * Gets all information from DB upon construction (can be changed to work directly with DB, so it can become stateless, if has too many users, or runs on multiple servers)
 * Does not perform any checking, operates on objects and DB as requested.
 */
public class ConceptFiledMap
{
    static final String conceptsTableName = "concepts";
    static final String userIdColName = "user_id";
    static final String conceptColName = "concept_name";
    static final String conceptFieldTableName = "concept_fields";
    static final String fieldColName = "field_name";
    static final String fieldTypeCol = "field_type";
    static final String isListColName = "isList";
    static final String mutableColName = "mutable";

    Map<String, List<FieldDescription>> conceptFieldMap;
    String userId;

    public ConceptFiledMap(String userId)
    {
        this.userId = userId;
        fillMap();
    }

    private void fillMap()
    {
        conceptFieldMap = new HashMap<>();
        //TODO: connect to DB and fill map!
    }

    public boolean hasConcept(String concept)
    {
        return conceptFieldMap.containsKey(concept);
    }

    public List<FieldDescription> getAllFieldDescriptions(String concept)
    {
        return conceptFieldMap.get(concept);
    }

    public Set<String> allConcepts()
    {
        return conceptFieldMap.keySet();
    }

    public void newConcept(String conceptName)
    {
        conceptFieldMap.put(conceptName, new LinkedList<>());

        //update DB!!!
        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("insert into " + conceptsTableName + " (" + userIdColName + "," + conceptColName + ") values (?,?)");

        )
        {
            pstmt.setString(1, userId);
            pstmt.setString(2, conceptName);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void removeConcept(String conceptName)
    {
        conceptFieldMap.remove(conceptName);

        //update DB!
        //first delete concept
        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from " + conceptsTableName + " where " + userIdColName + "=? and " + conceptColName + "=?");

        )
        {
            pstmt.setString(1, userId);
            pstmt.setString(2, conceptName);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

        //then delete all fields
        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from " + conceptFieldTableName + " where " + userIdColName + "=? and " + conceptColName + "=?");

        )
        {
            pstmt.setString(1, userId);
            pstmt.setString(2, conceptName);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void addFieldToConcept(String conceptName, FieldDescription fieldDescription)
    {
        conceptFieldMap.get(conceptName).add(fieldDescription);

        //update DB!!!
        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("insert into " + conceptFieldTableName + " (" + userIdColName + "," + conceptColName + "," + fieldColName + "," + fieldTypeCol + "," + isListColName + "," + mutableColName + ") values (?,?,?,?,?,?)");

        )
        {
            pstmt.setString(1, userId);
            pstmt.setString(2, conceptName);
            pstmt.setString(3, fieldDescription.fieldName);
            pstmt.setString(4, fieldDescription.fieldType.name());
            pstmt.setBoolean(5, fieldDescription.isList);
            pstmt.setBoolean(6, fieldDescription.mutable);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}
