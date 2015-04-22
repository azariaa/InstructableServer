package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;

import java.util.*;

/**
 * Created by Amos Azaria on 21-Apr-15.
 */
public class InstanceContainer
{

    Map<String,Map<String,GenericConcept>> conceptToInstance = new HashMap<>(); //TODO: should be stored in DB

    public GenericConcept getMostPlausibleInstance(ExecutionStatus executionStatus, List<String> conceptOptions)
    {
        return getMostPlausibleInstance(executionStatus, conceptOptions, null);
    }
    //save last access, and sort according to last access
    public GenericConcept getMostPlausibleInstance(ExecutionStatus executionStatus, List<String> conceptOptions, String optionalInstanceName)
    {
        List<GenericConcept> allPossibleInstances = getAllPossibleInstances(executionStatus, conceptOptions, optionalInstanceName);
        if (allPossibleInstances.isEmpty())
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "no relevant instances were found");
            return null;
        }
        if (allPossibleInstances.size() > 1)
        {
            executionStatus.add(ExecutionStatus.RetStatus.warning, "there is more than one possible instance, using the most recent");
        }
        allPossibleInstances.sort(new Comparator<GenericConcept>()
        {
            @Override
            public int compare(GenericConcept o1, GenericConcept o2)
            {
                if (o1.lastAccess.equals(o2.lastAccess))
                    return 0;
                return o1.lastAccess.before(o2.lastAccess) ? 1 : -1;
            }
        });
        return allPossibleInstances.get(0);
    }

    private List<GenericConcept> getAllPossibleInstances(ExecutionStatus executionStatus, List<String> conceptOptions, String optionalInstanceName)
    {
        List<GenericConcept> allPossibleInstances = new LinkedList<>();
        for (String concept : conceptToInstance.keySet())
        {
            if (conceptOptions.contains(concept))
            {
                for (GenericConcept genericConcept : conceptToInstance.get(concept).values())
                {
                    if (optionalInstanceName == null  || genericConcept.name.equals(optionalInstanceName))
                        allPossibleInstances.add(genericConcept);
                }
            }
        }
        return allPossibleInstances;
    }

    public GenericConcept getInstance(ExecutionStatus executionStatus, String conceptName, String instanceName)
    {
        if (conceptToInstance.containsKey(conceptName))
        {
            Map<String,GenericConcept> instances = conceptToInstance.get(conceptName);
            if (instances.containsKey(instanceName))
                return instances.get(instanceName);
            else
            {
                executionStatus.add(ExecutionStatus.RetStatus.error, "there is no instance of \""+conceptName+"\" with name \"" +instanceName + "\", please create one first");
            }
        }
        else
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "there are no instances of \""+conceptName+"\", please create an instance first (with name \"" +instanceName + "\")");
        }
        return null;
    }

    public void addInstance(ExecutionStatus executionStatus, GenericConcept conceptInstance)
    {
        if (!conceptToInstance.containsKey(conceptInstance.type))
        {
            //TODO: should check validity of conceptName (should have access to conceptContainer) and update executionStatus
            conceptToInstance.put(conceptInstance.type, new HashMap<>());
        }

        conceptToInstance.get(conceptInstance.type).put(conceptInstance.name,conceptInstance);

    }
}
