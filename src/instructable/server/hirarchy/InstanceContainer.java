package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;
import instructable.server.dal.InstanceKB;

import java.util.*;

/**
 * Created by Amos Azaria on 21-Apr-15.
 * mutability is on the instance level (the field level is given by the FieldDescription.
 */
public class InstanceContainer
{
    InstanceKB instanceKB;

    ConceptContainer conceptContainer;

    public InstanceContainer(ConceptContainer conceptContainer, String userId)
    {
        this.conceptContainer = conceptContainer;
        instanceKB = new InstanceKB(userId);
    }

    //save last access, and sort according to last access
    public Optional<GenericInstance> getMostPlausibleInstance(ExecutionStatus executionStatus, List<String> conceptOptions, Optional<String> instanceName, boolean mutableOnly)
    {
        List<GenericInstance> allPossibleInstances = getAllPossibleInstances(conceptOptions, instanceName, mutableOnly);
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
        return getAllPossibleInstances(conceptOptions, Optional.empty(), false);
    }

    private List<GenericInstance> getAllPossibleInstances(List<String> conceptOptions, Optional<String> instanceName, boolean mutableOnly)
    {
        List<GenericInstance> allPossibleInstances = new LinkedList<>();
        for (String concept : instanceKB.conceptsWithInstances())
        {
            if (conceptOptions.contains(concept))
            {
                for (GenericInstance genericInstance : instanceKB.getAllInstancesOf(concept))
                {
                    if ((!instanceName.isPresent() || genericInstance.name.equals(instanceName.get())) &&
                            (!mutableOnly || genericInstance.mutable))
                        allPossibleInstances.add(genericInstance);
                }
            }
        }
        return allPossibleInstances;
    }

    public Optional<GenericInstance> getInstance(ExecutionStatus executionStatus, String conceptName, String instanceName)
    {
        if (instanceKB.hasAnyInstancesOfConcept(conceptName))
        {
            if (instanceKB.hasInstanceOfConcept(conceptName, instanceName))
            {
                GenericInstance instance = instanceKB.getInstance(conceptName, instanceName);
                instance.touch();
                return Optional.of(instance);
            }
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

    public void addInstance(ExecutionStatus executionStatus, String conceptName, String instanceName, boolean isMutable)
    {
        if (!conceptContainer.doesConceptExist(conceptName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "there is no concept with the name \"" + "\", please define it first");
            return;
        }
        GenericInstance instance = new GenericInstance(conceptName, instanceName, conceptContainer.getAllFieldDiscriptions(conceptName), isMutable);

        addInstance(executionStatus, instance);
    }

    public void addInstance(ExecutionStatus executionStatus, GenericInstance instance)
    {
        String conceptName = instance.conceptName;
        if (!conceptContainer.doesConceptExist(conceptName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "no concept with the name \"" + conceptName + "\" is defined, please define it first");
        }
        else
        {
            instanceKB.addInstance(conceptName, instance);
        }

    }

    public void renameInstance(ExecutionStatus executionStatus, String conceptName, String instanceOldName, String instanceNewName)
    {
        if (instanceKB.hasAnyInstancesOfConcept(conceptName))
        {
            instanceKB.renameInstance(conceptName, instanceOldName, instanceNewName);
        }
        else
        {
            executionStatus.add(ExecutionStatus.RetStatus.warning, "the instance was not found");
        }
    }

    public void fieldAddedToConcept(ExecutionStatus executionStatus, String conceptName, FieldDescription newFieldDescription)
    {
        if (instanceKB.hasAnyInstancesOfConcept(conceptName))
        {
            //it's ok if no instances are found
            for (GenericInstance instance : instanceKB.getAllInstancesOf(conceptName))
            {
                instance.addFieldToObject(executionStatus, newFieldDescription);
            }
        }
    }

    public void setMutability(ExecutionStatus executionStatus, String conceptName, String instanceName, boolean newMutability)
    {
        if (instanceKB.hasAnyInstancesOfConcept(conceptName))
        {
            GenericInstance reqInstance = instanceKB.getInstance(conceptName,instanceName);
            reqInstance.mutable = newMutability;
        }
        else
        {
            executionStatus.add(ExecutionStatus.RetStatus.warning, "the instance was not found");
        }
    }

    public void fieldRemovedFromConcept(ExecutionStatus executionStatus, String conceptName, String fieldName)
    {
        if (instanceKB.hasAnyInstancesOfConcept(conceptName))
        {
            //it's ok if no instances are found
            for (GenericInstance instance : instanceKB.getAllInstancesOf(conceptName))
            {
                instance.removeFieldFromObject(executionStatus, fieldName);
            }
        }
    }

    public void deleteInstance(ExecutionStatus executionStatus, GenericInstance instance)
    {
        String conceptName = instance.conceptName;
        if (!conceptContainer.doesConceptExist(conceptName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "no concept with the name \"" + conceptName + "\" is defined, please define it first");
        }
        else
        {
            if (instanceKB.hasInstanceOfConcept(conceptName,instance.getName()))
            {
                instanceKB.deleteInstance(conceptName,instance.getName());
            }
            else
            {
                executionStatus.add(ExecutionStatus.RetStatus.error, "the instance could not be found");
            }
        }

    }

    public void conceptUndefined(String conceptName)
    {
        if (instanceKB.hasAnyInstancesOfConcept(conceptName))
            instanceKB.deleteAllInstancesOf(conceptName);
    }
}
