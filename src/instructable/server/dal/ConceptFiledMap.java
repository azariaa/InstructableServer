package instructable.server.dal;

import instructable.server.hirarchy.FieldDescription;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        //connect to DB and fill map! //TODO: didn't check if works

        try (
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("select " + DBUtils.conceptColName + " from " + DBUtils.conceptsTableName + " where " + DBUtils.userIdColName + "=?");
        )
        {
            pstmt.setString(1, userId);

            try (ResultSet resultSet = pstmt.executeQuery())
            {
                while (resultSet.next())
                {
                    String conceptName = resultSet.getString(DBUtils.conceptColName);
                    conceptFieldMap.put(conceptName, new LinkedList<>());
                }
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }


        //add all field descriptions
        try (
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("select " + DBUtils.conceptColName +"," + DBUtils.fieldColName + "," + DBUtils.fieldTypeCol + "," + DBUtils.isListColName + "," + DBUtils.mutableColName + " from " + DBUtils.conceptFieldTableName + " where " + DBUtils.userIdColName + "=?");
        )
        {
            pstmt.setString(1, userId);

            try (ResultSet resultSet = pstmt.executeQuery())
            {
                while (resultSet.next())
                {
                    String conceptName = resultSet.getString(DBUtils.conceptColName);
                    String fieldName = resultSet.getString(DBUtils.fieldColName);
                    String fieldType = resultSet.getString(DBUtils.fieldTypeCol);
                    boolean isList = resultSet.getBoolean(DBUtils.isListColName);
                    boolean mutable = resultSet.getBoolean(DBUtils.mutableColName);

                    FieldDescription fieldDescription = new FieldDescription(fieldName, PossibleFieldType.valueOf(fieldType),isList,mutable);
                    conceptFieldMap.get(conceptName).add(fieldDescription); //must exist!
                }
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public boolean hasConcept(String concept)
    {
        return conceptFieldMap.containsKey(concept);
    }

    public List<FieldDescription> getAllFieldDescriptions(String concept)
    {
        if (conceptFieldMap.containsKey(concept))
            return conceptFieldMap.get(concept);
        return new LinkedList<>();
    }

    public Set<String> allConcepts()
    {
        return conceptFieldMap.keySet();
    }

    /**
     *    Need to really be new.
     **/
    public void newConcept(String conceptName)
    {
        conceptFieldMap.put(conceptName, new LinkedList<>());

        //update DB!!!
        try (
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("insert into " + DBUtils.conceptsTableName + " (" + DBUtils.userIdColName + "," + DBUtils.conceptColName + ") values (?,?)");
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
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from " + DBUtils.conceptsTableName + " where " + DBUtils.userIdColName + "=? and " + DBUtils.conceptColName + "=?");
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
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from " + DBUtils.conceptFieldTableName + " where " + DBUtils.userIdColName + "=? and " + DBUtils.conceptColName + "=?");

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
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("insert into " + DBUtils.conceptFieldTableName + " (" + DBUtils.userIdColName + "," + DBUtils.conceptColName + "," + DBUtils.fieldColName + "," + DBUtils.fieldTypeCol + "," + DBUtils.isListColName + "," + DBUtils.mutableColName + ") values (?,?,?,?,?,?)");
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
