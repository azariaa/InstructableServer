package instructable.server.dal;

import instructable.server.hirarchy.FieldDescription;

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

    public InstanceKB(String userId)
    {
        this.userId = userId;
        fillMap();
    }

    private void fillMap()
    {
        conceptToInstance =  new HashMap<>();
        //TODO: connect to DB and fill map!
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

    public void firstUseOfConcept(String conceptName)
    {
        conceptToInstance.put(conceptName, new HashMap<>());
    }

    public void renameInstance(String conceptName, String instanceOldName, String instanceNewName)
    {
        Map<String, SingleInstance> instances = conceptToInstance.get(conceptName);
        if (instances.containsKey(instanceOldName))
        {
            SingleInstance reqInstance = instances.get(instanceOldName);
            reqInstance.instanceWasRenamed(instanceNewName);
            instances.remove(instanceOldName);
            instances.put(instanceNewName, reqInstance);
            //TODO: update DB!!!
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
