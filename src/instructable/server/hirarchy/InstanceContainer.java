package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;

import java.util.*;

/**
 * Created by Amos Azaria on 21-Apr-15.
 */
public class InstanceContainer
{

    Map<String,Map<String,GenericConcept>> conceptToInstance = new HashMap<>(); //TODO: should be stored in DB

    ConceptContainer conceptContainer;

    public InstanceContainer(ConceptContainer conceptContainer)
    {
        this.conceptContainer = conceptContainer;
    }

    //save last access, and sort according to last access
    public Optional<GenericConcept> getMostPlausibleInstance(ExecutionStatus executionStatus, List<String> conceptOptions, Optional<String> instanceName)
    {
        List<GenericConcept> allPossibleInstances = getAllPossibleInstances(conceptOptions, instanceName);
        if (allPossibleInstances.isEmpty())
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "no relevant instances were found");
            return Optional.empty();
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
        return Optional.of(allPossibleInstances.get(0));
    }

    public List<GenericConcept> getAllInstances(String concept)
    {
        List<String> conceptOptions = new LinkedList<>();
        conceptOptions.add(concept);
        return getAllPossibleInstances(conceptOptions, Optional.empty());
    }

    private List<GenericConcept> getAllPossibleInstances(List<String> conceptOptions, Optional<String> instanceName)
    {
        List<GenericConcept> allPossibleInstances = new LinkedList<>();
        for (String concept : conceptToInstance.keySet())
        {
            if (conceptOptions.contains(concept))
            {
                for (GenericConcept genericConcept : conceptToInstance.get(concept).values())
                {
                    if (!instanceName.isPresent()  || genericConcept.name.equals(instanceName.get()))
                        allPossibleInstances.add(genericConcept);
                }
            }
        }
        return allPossibleInstances;
    }

    public Optional<GenericConcept> getInstance(ExecutionStatus executionStatus, String conceptName, String instanceName)
    {
        if (conceptToInstance.containsKey(conceptName))
        {
            Map<String,GenericConcept> instances = conceptToInstance.get(conceptName);
            if (instances.containsKey(instanceName))
                return Optional.of(instances.get(instanceName));
            else
            {
                executionStatus.add(ExecutionStatus.RetStatus.error, "there is no instance of \""+conceptName+"\" with name \"" +instanceName + "\", please create one first");
            }
        }
        else
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "there are no instances of \""+conceptName+"\", please create an instance first (with name \"" +instanceName + "\")");
        }
        return Optional.empty();
    }

    public void addInstance(ExecutionStatus executionStatus, String conceptName, String instanceName)
    {
        if (!conceptContainer.doesConceptExist(conceptName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error,"there is no concept with the name \"" + "\", please define it first");
            return;
        }
        GenericConcept instance = new GenericConcept(conceptName, instanceName, conceptContainer.conceptFieldMap.get(conceptName));

        addInstance(executionStatus, instance);
    }

    public void addInstance(ExecutionStatus executionStatus, GenericConcept conceptInstance)
    {
        String conceptName = conceptInstance.type;
        if (!conceptContainer.doesConceptExist(conceptName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error,"there is no concept with the name \"" + conceptName + "\" is defined, please define it first");
        }
        else
        {
            if (!conceptToInstance.containsKey(conceptName))
            {
                conceptToInstance.put(conceptInstance.type, new HashMap<>());
            }

            conceptToInstance.get(conceptInstance.type).put(conceptInstance.name, conceptInstance);
        }

    }

    public void renameInstance(ExecutionStatus executionStatus, String conceptName, String instanceOldName, String instanceNewName)
    {
        if (conceptToInstance.containsKey(conceptName))
        {
            Map<String, GenericConcept> instances = conceptToInstance.get(conceptName);
            if (instances.containsKey(instanceOldName))
            {
                GenericConcept reqInstance = instances.get(instanceOldName);
                reqInstance.name = instanceNewName;
                instances.remove(instanceOldName);
                instances.put(instanceNewName, reqInstance);
                return;
            }
        }
        executionStatus.add(ExecutionStatus.RetStatus.warning, "the instance was not found");
    }
}
