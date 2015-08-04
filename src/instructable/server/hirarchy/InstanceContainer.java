package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;
import instructable.server.dal.InstanceKB;
import instructable.server.dal.SingleInstance;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
        instanceKB = new InstanceKB(userId, conceptContainer.getConceptFieldMap());
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
                if (o1.getLastAccess() == o2.getLastAccess())
                    return 0;
                return (o1.getLastAccess() < o2.getLastAccess()) ? 1 : -1;
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
                for (SingleInstance singleInstance : instanceKB.getAllInstancesOf(concept))
                {
                    GenericInstance genericInstance = GenericInstance.WrapAsGenericInstance(singleInstance);
                    if ((!instanceName.isPresent() || genericInstance.getName().equals(instanceName.get())) &&
                            (!mutableOnly || genericInstance.getMutable()))
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
                GenericInstance instance = GenericInstance.WrapAsGenericInstance(instanceKB.getInstance(conceptName, instanceName));
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

    public Optional<GenericInstance> addInstance(ExecutionStatus executionStatus, String conceptName, String instanceName, boolean isMutable)
    {
        if (!conceptContainer.doesConceptExist(conceptName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "there is no concept with the name \"" + "\", please define it first");
            return Optional.empty();
        }
        SingleInstance newInstance = instanceKB.addInstance(conceptName, instanceName, isMutable, conceptContainer.getAllFieldDiscriptions(conceptName));
        return Optional.of(GenericInstance.WrapAsGenericInstance(newInstance));
        //GenericInstance instance = GenericInstance.WrapAsGenericInstance(conceptName, instanceName, conceptContainer.getAllFieldDiscriptions(conceptName), isMutable);
        //addInstance(executionStatus, instance);
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
            for (SingleInstance instance : instanceKB.getAllInstancesOf(conceptName))
            {
                GenericInstance.WrapAsGenericInstance(instance).fieldWasAddedToConcept(executionStatus, newFieldDescription);
            }
        }
    }

    public void setMutability(ExecutionStatus executionStatus, String conceptName, String instanceName, boolean newMutability)
    {
        if (instanceKB.hasInstanceOfConcept(conceptName, instanceName))
        {
            GenericInstance reqInstance = GenericInstance.WrapAsGenericInstance(instanceKB.getInstance(conceptName, instanceName));
            reqInstance.changeMutability(newMutability);
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
            for (SingleInstance instance : instanceKB.getAllInstancesOf(conceptName))
            {
                GenericInstance.WrapAsGenericInstance(instance).fieldWasRemovedFromConcept(executionStatus, fieldName);
            }
        }
    }

    public void deleteInstance(ExecutionStatus executionStatus, GenericInstance instance)
    {
        String conceptName = instance.getConceptName();
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
