package instructable.server.dal;


import instructable.server.hirarchy.FieldDescription;
import instructable.server.hirarchy.FieldHolder;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Amos Azaria on 21-Jul-15.
 * No checking at all (callers responsibility).
 */
public class SingleInstance
{
    static final String instancesTableName = "instances";
    static final String mutableColName = "mutable";
    static final String instanceValTableName = "instance_values";
    static final String userIdColName = "user_id";
    static final String conceptColName = "concept_name";
    static final String instanceColName = "instance_name";
    static final String fieldColName = "field_name";
    static final String fieldJSonValColName = "field_jsonval";

    String userId;
    String conceptName;
    String instanceName;
    boolean mutable;
    private Map<String, FieldHolder> fields;

    private SingleInstance(String userId, String conceptName, String instanceName, boolean mutable)
    {
        this.userId = userId;
        this.conceptName = conceptName;
        this.instanceName = instanceName;
        this.mutable = mutable;
    }

    static SingleInstance loadInstanceFieldsFromDB(String userId, String conceptName, String instanceName, boolean mutable, List<FieldDescription> fieldsInType)
    {
        //TODO: must be called on initialization!!!
        SingleInstance singleInstance = new SingleInstance(userId, conceptName, instanceName, mutable);
        singleInstance.loadFieldsFromDB(fieldsInType);
        return singleInstance;
    }

    /**
     * instance shouldn't already exist in DB.
     */
    static SingleInstance createNewInstance(String userId, String conceptName, String instanceName, boolean mutable, List<FieldDescription> fieldsInType)
    {
        //update DB!!!
        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("insert into " + instancesTableName + " (" + userIdColName + "," + conceptColName + "," + instanceColName+","+mutableColName + ") values (?,?,?,?)");

        )
        {
            pstmt.setString(1, userId);
            pstmt.setString(2, conceptName);
            pstmt.setString(3, instanceName);
            pstmt.setBoolean(4, mutable);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

        SingleInstance singleInstance = new SingleInstance(userId, conceptName, instanceName, mutable);
        singleInstance.createNewFields(fieldsInType);
        return singleInstance;
    }

    private void createNewFields(List<FieldDescription> fieldsInType)
    {
        fields = new HashMap<>();

        //no need to update DB. because DB only holds fields with values in them.
        for (FieldDescription fieldDescription : fieldsInType)
        {
            fields.put(fieldDescription.fieldName, new FieldHolder(fieldDescription, instanceName, new FieldChanged(this,fieldDescription.fieldName), Optional.empty()));
        }

    }

    private void loadFieldsFromDB(List<FieldDescription> fieldsInType)
    {
        fields = new HashMap<>();
        //fill map from DB!
        //TODO: check if works
        for (FieldDescription fieldDescription : fieldsInType)
        {
            Optional<JSONObject> jsonValue = Optional.empty();
            try (
                    Connection connection = InMindDataSource.getDataSource().getConnection();
                    PreparedStatement pstmt = connection.prepareStatement("select " + fieldJSonValColName + " from "+instanceValTableName+" where " + userIdColName + "=?" + " and "+conceptColName+"=?"+" and "+instanceColName+"=?" + " and " + fieldColName +"=?");
            )
            {
                pstmt.setString(1, userId);
                pstmt.setString(2, conceptName);
                pstmt.setString(3, instanceName);
                pstmt.setString(4, fieldDescription.fieldName);

                try (ResultSet resultSet = pstmt.executeQuery())
                {
                    //may be empty
                    String jsonValAsStr = resultSet.getString(fieldJSonValColName);
                    if (jsonValAsStr != null)
                    {
                        jsonValue = Optional.of(new JSONObject(jsonValAsStr));
                    }
                } catch (JSONException e)
                {
                    e.printStackTrace();
                }
            } catch (SQLException e)
            {
                e.printStackTrace();
            }
            FieldHolder fieldHolder = new FieldHolder(fieldDescription, instanceName, new FieldChanged(this,fieldDescription.fieldName), jsonValue);
            fields.put(fieldDescription.fieldName, fieldHolder);
        }

    }

    public String getInstanceName()
    {
        return instanceName;
    }

    public void instanceWasRenamed(String newName)
    {
        instanceName = newName;
        //TODO: do we need to update the DB? I think someone else already did...
    }

    public String getConceptName()
    {
        return conceptName;
    }

    public boolean containsField(String fieldName)
    {
        return fields.containsKey(fieldName);
    }

    public FieldHolder getField(String fieldName)
    {
        return fields.get(fieldName);
    }

    public void fieldWasAdded(FieldDescription fieldDescription)
    {
        //no need to update DB, because the field is still empty.
        fields.put(fieldDescription.fieldName, new FieldHolder(fieldDescription, instanceName, new FieldChanged(this,fieldDescription.fieldName), Optional.empty()));
    }

    public void fieldWasRemovedFromConcept(String fieldName)
    {
        fields.remove(fieldName);
        //update the DB!!!
        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from "+instancesTableName+" where " + userIdColName + "=?" + " and "+conceptColName+"=?"+" and "+instanceColName+"=?" + " and " + fieldColName +"=?");
        )
        {
            pstmt.setString(1, userId);
            pstmt.setString(2, conceptName);
            pstmt.setString(3, instanceName);
            pstmt.setString(4, fieldName);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public Set<String> getAllFieldNames()
    {
        return fields.keySet();
    }

    public boolean getMutable()
    {
        return mutable;
    }

    /**
     * Instance must exist
     *
     * @param newMutability
     */
    public void changeMutability(boolean newMutability)
    {
        mutable = newMutability;
        //update DB!!!
        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("update " + instancesTableName + " set " + mutableColName + " = ?, where " + userIdColName + "=?" + " and "+conceptColName+"=?"+" and "+instanceColName+"=?");

        )
        {
            pstmt.setBoolean(1, newMutability);
            pstmt.setString(2, userId);
            pstmt.setString(3, conceptName);
            pstmt.setString(4, instanceName);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    void removeFromDB()
    {
        //delete instance!!!
        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from " + instancesTableName + " where " + userIdColName + "=?" + " and "+conceptColName+"=?"+" and "+instanceColName+"=?");

        )
        {
            pstmt.setString(1, userId);
            pstmt.setString(2, conceptName);
            pstmt.setString(3, instanceName);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

        //delete all fields that exist
        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from "+instancesTableName+" where " + userIdColName + "=?" + " and "+conceptColName+"=?"+" and "+instanceColName+"=?");

        )
        {
            pstmt.setString(1, userId);
            pstmt.setString(2, conceptName);
            pstmt.setString(3, instanceName);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}
