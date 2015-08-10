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
    String userId;
    String conceptName;
    String instanceName;
    boolean mutable;
    private Map<String, FieldHolder> fields;

    long lastAccess; //not kept in DB

    private SingleInstance(String userId, String conceptName, String instanceName, boolean mutable)
    {
        this.userId = userId;
        this.conceptName = conceptName;
        this.instanceName = instanceName;
        this.mutable = mutable;
        lastAccess = System.nanoTime();
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
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("insert into " + DBUtils.instancesTableName + " (" + DBUtils.userIdColName + "," + DBUtils.conceptColName + "," + DBUtils.instanceColName + "," + DBUtils.mutableColName + ") values (?,?,?,?)");
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
            fields.put(fieldDescription.fieldName, new FieldHolder(fieldDescription, instanceName, new FieldChanged(this, fieldDescription.fieldName), Optional.empty()));
        }

    }

    private void loadFieldsFromDB(List<FieldDescription> fieldsInType)
    {
        fields = new HashMap<>();
        //fill map from DB!         // reads all fields at once.
        //TODO: check if works

        try (
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("select " + DBUtils.fieldColName + "," + DBUtils.fieldJSonValColName + " from " + DBUtils.instanceValTableName + " where " + DBUtils.userIdColName + "=?" + " and " + DBUtils.conceptColName + "=?" + " and " + DBUtils.instanceColName + "=?");
        )
        {
            pstmt.setString(1, userId);
            pstmt.setString(2, conceptName);
            pstmt.setString(3, instanceName);

            try (ResultSet resultSet = pstmt.executeQuery())
            {
                Map<String, Optional<JSONObject>> fieldsVals = new HashMap<>();
                while (resultSet.next())
                {
                    String fieldResName = resultSet.getString(DBUtils.fieldColName);
                    String jsonValAsStr = resultSet.getString(DBUtils.fieldJSonValColName);
                    Optional<JSONObject> jsonValue = Optional.empty();
                    try
                    {
                        if (jsonValAsStr != null)
                        {
                            jsonValue = Optional.of(new JSONObject(jsonValAsStr));
                        }
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                    fieldsVals.put(fieldResName, jsonValue);
                }
                for (FieldDescription fieldDescription : fieldsInType)
                {
                    //need to create a FieldHolder also if has no JSon value
                    String fieldName = fieldDescription.fieldName;
                    Optional<JSONObject> jsonValue = Optional.empty();
                    if (fieldsVals.containsKey(fieldName))
                    {
                        jsonValue = fieldsVals.get(fieldName);
                    }
                    FieldHolder fieldHolder = new FieldHolder(fieldDescription, instanceName, new FieldChanged(this, fieldName), jsonValue);
                    fields.put(fieldDescription.fieldName, fieldHolder);
                }
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public String getInstanceName()
    {
        return instanceName;
    }

    public void instanceWasRenamed(String newName)
    {
        String oldName = instanceName;

        //update DB instance
        try (
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("update " + DBUtils.instancesTableName + " set " + DBUtils.instanceColName + " = ? where " + DBUtils.userIdColName + "=?" + " and " + DBUtils.conceptColName + "=?" + " and " + DBUtils.instanceColName + "=?");
        )
        {
            pstmt.setString(1, newName);
            pstmt.setString(2, userId);
            pstmt.setString(3, conceptName);
            pstmt.setString(4, oldName);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

        //update all fields in DB
        try (
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("update " + DBUtils.instanceValTableName + " set " + DBUtils.instanceColName + " = ? where " + DBUtils.userIdColName + "=?" + " and " + DBUtils.conceptColName + "=?" + " and " + DBUtils.instanceColName + "=?");
        )
        {
            pstmt.setString(1, newName);
            pstmt.setString(2, userId);
            pstmt.setString(3, conceptName);
            pstmt.setString(4, oldName);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }


        //update local
        instanceName = newName;
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
        fields.put(fieldDescription.fieldName, new FieldHolder(fieldDescription, instanceName, new FieldChanged(this, fieldDescription.fieldName), Optional.empty()));
    }

    public void fieldWasRemovedFromConcept(String fieldName)
    {
        fields.remove(fieldName);
        //update the DB!!!
        try (
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from " + DBUtils.instancesTableName + " where " + DBUtils.userIdColName + "=?" + " and " + DBUtils.conceptColName + "=?" + " and " + DBUtils.instanceColName + "=?" + " and " + DBUtils.fieldColName + "=?");
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
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("update " + DBUtils.instancesTableName + " set " + DBUtils.mutableColName + " = ? where " + DBUtils.userIdColName + "=?" + " and " + DBUtils.conceptColName + "=?" + " and " + DBUtils.instanceColName + "=?");

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
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from " + DBUtils.instancesTableName + " where " + DBUtils.userIdColName + "=?" + " and " + DBUtils.conceptColName + "=?" + " and " + DBUtils.instanceColName + "=?");
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
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("delete from " + DBUtils.instanceValTableName + " where " + DBUtils.userIdColName + "=?" + " and " + DBUtils.conceptColName + "=?" + " and " + DBUtils.instanceColName + "=?");
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

    public void accessed(long l)
    {

    }

    public long getLastAccess()
    {
        return lastAccess;
    }
}
