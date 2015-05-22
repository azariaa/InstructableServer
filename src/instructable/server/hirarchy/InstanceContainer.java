package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;

import java.util.*;

/**
 * Created by Amos Azaria on 21-Apr-15.
 */
public class InstanceContainer
{

    Map<String, Map<String, GenericInstance>> conceptToInstance = new HashMap<>(); //TODO: should be stored in DB

    ConceptContainer conceptContainer;

    public InstanceContainer(ConceptContainer conceptContainer)
    {
        this.conceptContainer = conceptContainer;
    }

    //save last access, and sort according to last access
    public Optional<GenericInstance> getMostPlausibleInstance(ExecutionStatus executionStatus, List<String> conceptOptions, Optional<String> instanceName)
    {
        List<GenericInstance> allPossibleInstances = getAllPossibleInstances(conceptOptions, instanceName);
        if (allPossibleInstances.isEmpty())
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "no relevant instances were found");
            return Optional.empty();
        }
        if (allPossibleInstances.size() > 1)
        {
            executionStatus.add(ExecutionStatus.RetStatus.warning, "there is more than one possible instance, using the most recent");
        }
        allPossibleInstances.sort(new Comparator<GenericInstance>()
        {
            @Override
            public int compare(GenericInstance o1, GenericInstance o2)
            {
                if (o1.lastAccess == o2.lastAccess)
                    return 0;
                return (o1.lastAccess < o2.lastAccess) ? 1 : -1;
            }
        });
        return Optional.of(allPossibleInstances.get(0));
    }

    public List<GenericInstance> getAllInstances(String concept)
    {
        List<String> conceptOptions = new LinkedList<>();
        conceptOptions.add(concept);
        return getAllPossibleInstances(conceptOptions, Optional.empty());
    }

    private List<GenericInstance> getAllPossibleInstances(List<String> conceptOptions, Optional<String> instanceName)
    {
        List<GenericInstance> allPossibleInstances = new LinkedList<>();
        for (String concept : conceptToInstance.keySet())
        {
            if (conceptOptions.contains(concept))
            {
                for (GenericInstance genericInstance : conceptToInstance.get(concept).values())
                {
                    if (!instanceName.isPresent() || genericInstance.name.equals(instanceName.get()))
                        allPossibleInstances.add(genericInstance);
                }
            }
        }
        return allPossibleInstances;
    }

    public Optional<GenericInstance> getInstance(ExecutionStatus executionStatus, String conceptName, String instanceName)
    {
        if (conceptToInstance.containsKey(conceptName))
        {
            Map<String, GenericInstance> instances = conceptToInstance.get(conceptName);
            if (instances.containsKey(instanceName))
                return Optional.of(instances.get(instanceName));
            else
            {
                executionStatus.add(ExecutionStatus.RetStatus.error, "there is no instance of \"" + conceptName + "\" with name \"" + instanceName + "\", please create one first");
            }
        }
        else
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "there are no instances of \"" + conceptName + "\", please create an instance first (with name \"" + instanceName + "\")");
        }
        return Optional.empty();
    }

    public void addInstance(ExecutionStatus executionStatus, String conceptName, String instanceName)
    {
        if (!conceptContainer.doesConceptExist(conceptName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "there is no concept with the name \"" + "\", please define it first");
            return;
        }
        GenericInstance instance = new GenericInstance(conceptName, instanceName, conceptContainer.conceptFieldMap.get(conceptName));

        addInstance(executionStatus, instance);
    }

    public void addInstance(ExecutionStatus executionStatus, GenericInstance conceptInstance)
    {
        String conceptName = conceptInstance.conceptName;
        if (!conceptContainer.doesConceptExist(conceptName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "there is no concept with the name \"" + conceptName + "\" is defined, please define it first");
        }
        else
        {
            if (!conceptToInstance.containsKey(conceptName))
            {
                conceptToInstance.put(conceptInstance.conceptName, new HashMap<>());
            }

            conceptToInstance.get(conceptInstance.conceptName).put(conceptInstance.name, conceptInstance);
        }

    }

    public void renameInstance(ExecutionStatus executionStatus, String conceptName, String instanceOldName, String instanceNewName)
    {
        if (conceptToInstance.containsKey(conceptName))
        {
            Map<String, GenericInstance> instances = conceptToInstance.get(conceptName);
            if (instances.containsKey(instanceOldName))
            {
                GenericInstance reqInstance = instances.get(instanceOldName);
                reqInstance.name = instanceNewName;
                instances.remove(instanceOldName);
                instances.put(instanceNewName, reqInstance);
                return;
            }
        }
        executionStatus.add(ExecutionStatus.RetStatus.warning, "the instance was not found");
    }

    public void fieldAddedToConcept(ExecutionStatus executionStatus, String conceptName, FieldDescription newFieldDescription)
    {
        if (conceptToInstance.containsKey(conceptName))
        {
            //it's ok if no instances are found
            Map<String, GenericInstance> instances = conceptToInstance.get(conceptName);
            for (GenericInstance instance : instances.values())
            {
                instance.addFieldToObject(executionStatus, newFieldDescription);
            }
        }
    }
}
