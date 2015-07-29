package instructable.server.dal;

import instructable.server.hirarchy.FieldDescription;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Amos Azaria on 21-Jul-15.
 *  Gets all information from DB upon construction (can be changed to work directly with DB, so it can become stateless, if has too many users, or runs on multiple servers)
 *  Does not perform any checking, operates on objects and DB as requested.
 */
public class InstanceKB
{
    String userId;
    Map<String, Map<String, SingleInstance>> conceptToInstance; //map from conceptNames to a map holding all instances of that concept

    public InstanceKB(String userId, ConceptFiledMap conceptFiledMap)
    {
        this.userId = userId;
        fillMap(conceptFiledMap);
    }

    private void fillMap(ConceptFiledMap conceptFiledMap)
    {
        conceptToInstance =  new HashMap<>();
        //connect to DB and fill map! //TODO: didn't check if works

        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("select " + DBUtils.conceptColName + "," + DBUtils.instanceColName + "," + DBUtils.mutableColName + " from " + DBUtils.instancesTableName + " where " + DBUtils.userIdColName + "=?");
        )
        {
            pstmt.setString(1, userId);

            try (ResultSet resultSet = pstmt.executeQuery())
            {
                while (resultSet.next())
                {
                    String conceptName = resultSet.getString(DBUtils.conceptColName);
                    String instanceName = resultSet.getString(DBUtils.instanceColName);
                    boolean mutable = resultSet.getBoolean(DBUtils.mutableColName);

                    SingleInstance instance = SingleInstance.loadInstanceFieldsFromDB(userId, conceptName, instanceName, mutable, conceptFiledMap.getAllFieldDescriptions(conceptName));
                    if (!conceptToInstance.containsKey(conceptName))
                        conceptToInstance.put(conceptName, new HashMap<>());
                    Map<String, SingleInstance> allInstancesOfConcept = conceptToInstance.get(conceptName);
                    allInstancesOfConcept.put(instanceName, instance);
                }
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public boolean hasAnyInstancesOfConcept(String conceptName)
    {
        return conceptToInstance.containsKey(conceptName);
    }

    public boolean hasInstanceOfConcept(String conceptName, String instanceName)
    {
        if (conceptToInstance.containsKey(conceptName) && conceptToInstance.get(conceptName).containsKey(instanceName))
            return true;
        return false;
    }

    public SingleInstance getInstance(String conceptName, String instanceName)
    {
        return conceptToInstance.get(conceptName).get(instanceName);
    }

    public Set<String> conceptsWithInstances()
    {
        return conceptToInstance.keySet();
    }

    public void deleteAllInstancesOf(String conceptName)
    {
        conceptToInstance.remove(conceptName);
    }


    public Collection<SingleInstance> getAllInstancesOf(String conceptName)
    {
        return conceptToInstance.get(conceptName).values();
    }

    public void renameInstance(String conceptName, String instanceOldName, String instanceNewName)
    {
        Map<String, SingleInstance> instances = conceptToInstance.get(conceptName);
        if (instances.containsKey(instanceOldName))
        {
            SingleInstance reqInstance = instances.get(instanceOldName);
            reqInstance.instanceWasRenamed(instanceNewName); //will also update DB
            instances.remove(instanceOldName);
            instances.put(instanceNewName, reqInstance);
            return;
        }
    }

    public void deleteInstance(String conceptName, String instanceName)
    {
        SingleInstance instanceToDelete = conceptToInstance.get(conceptName).get(instanceName);
        //call removeFromDB to update DB
        instanceToDelete.removeFromDB();
        conceptToInstance.get(conceptName).remove(instanceName);

    }

    public void addInstance(String conceptName, String instanceName, boolean isMutable, List<FieldDescription> fieldDiscriptionList)
    {
        SingleInstance singleInstance = SingleInstance.createNewInstance(userId, conceptName, instanceName, isMutable, fieldDiscriptionList); //also updates DB
        //GenericInstance instance = GenericInstance.WrapAsGenericInstance(singleInstance);

        if (!conceptToInstance.containsKey(conceptName))
        {
            conceptToInstance.put(conceptName, new HashMap<>());
        }
        conceptToInstance.get(conceptName).put(instanceName, singleInstance);
    }
}
