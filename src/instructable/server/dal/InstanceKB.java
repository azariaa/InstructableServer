package instructable.server.dal;

import instructable.server.hirarchy.FieldDescription;
import instructable.server.hirarchy.GenericInstance;

import java.util.*;

/**
 * Created by Amos Azaria on 21-Jul-15.
 *  Gets all information from DB upon construction (can be changed to work directly with DB, so it can become stateless, if has too many users, or runs on multiple servers)
 *  Does not perform any checking, operates on objects and DB as requested.
 */
public class InstanceKB
{
    String userId;
    Map<String, Map<String, GenericInstance>> conceptToInstance; //map from conceptNames to a map holding all instances of that concept

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

    public GenericInstance getInstance(String conceptName, String instanceName)
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


    public Collection<GenericInstance> getAllInstancesOf(String conceptName)
    {
        return conceptToInstance.get(conceptName).values();
    }

    public void firstUseOfConcept(String conceptName)
    {
        conceptToInstance.put(conceptName, new HashMap<>());
    }

    public void addInstance(String conceptName, GenericInstance instance)
    {
        if (!conceptToInstance.containsKey(conceptName))
        {
            conceptToInstance.put(conceptName, new HashMap<>());
        }
        //TODO: update DB?! maybe was already added when created GenericInstance???
        conceptToInstance.get(conceptName).put(instance.getName(), instance);
    }

    public void renameInstance(String conceptName, String instanceOldName, String instanceNewName)
    {
        Map<String, GenericInstance> instances = conceptToInstance.get(conceptName);
        if (instances.containsKey(instanceOldName))
        {
            GenericInstance reqInstance = instances.get(instanceOldName);
            reqInstance.instanceWasRenamed(instanceNewName);
            instances.remove(instanceOldName);
            instances.put(instanceNewName, reqInstance);
            //TODO: update DB!!!
            return;
        }
    }

    public void deleteInstance(String conceptName, String instanceName)
    {
        conceptToInstance.get(conceptName).remove(instanceName);
        //TODO: update DB!!!
    }

    public void addInstance(String conceptName, String instanceName, boolean isMutable, List<FieldDescription> fieldDiscriptionList)
    {
        GenericInstance instance = GenericInstance.CreateNewGenericInstance(userId, conceptName, instanceName, isMutable, fieldDiscriptionList);

        addInstance(conceptName, instance);
    }
}
