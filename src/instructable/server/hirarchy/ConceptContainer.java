package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;

import java.util.*;
import java.util.stream.Collectors;

import static instructable.server.TextFormattingUtils.userFriendlyList;

/**
 * Created by Amos Azaria on 21-Apr-15.
 * <p>
 * This class has access to all defined concepts and their fields and types.
 * This class has the required information for creating an instance of a concept.
 * This class can access the data base
 * mutability on the field level.
 */
public class ConceptContainer
{
    Map<String, List<FieldDescription>> conceptFieldMap;

    public ConceptContainer()
    {
        conceptFieldMap = new HashMap<>();
        //conceptFieldMap.put("email", Arrays.asList(EmailMessage.fieldDescriptions));
    }

    public boolean doesConceptExist(String concept)
    {
        return conceptFieldMap.containsKey(concept);
    }

    public boolean doesFieldExistInConcept(String concept, String fieldName)
    {
        if (conceptFieldMap.containsKey(concept))
        {
            List<FieldDescription> fieldDescriptionList = conceptFieldMap.get(concept);
            for (FieldDescription fieldDescription : fieldDescriptionList)
            {
                if (fieldDescription.fieldName.equals(fieldName))
                    return true;
            }
        }
        return false;
    }

    public List<String> findConceptsForField(ExecutionStatus executionStatus, String fieldName, boolean mutableOnly)
    {
        Optional<String> foundImmutableConcept = Optional.empty();
        List<String> candidates = new LinkedList<>();
        for (String concept : conceptFieldMap.keySet())
        {
            List<FieldDescription> fieldDescriptionList = conceptFieldMap.get(concept);
            for (FieldDescription fieldDescription : fieldDescriptionList)
            {
                if (fieldDescription.fieldName.equals(fieldName))
                {
                    if (!mutableOnly || fieldDescription.mutable)
                        candidates.add(concept);
                    else
                        foundImmutableConcept = Optional.of(concept);
                    break;
                }
            }
        }
        if (candidates.size() == 0)
        {
            if (mutableOnly && foundImmutableConcept.isPresent())
                executionStatus.add(ExecutionStatus.RetStatus.error, "the field \"" + fieldName + "\" of the concept \""+ foundImmutableConcept.get() + "\" is immutable.");
            else
                executionStatus.add(ExecutionStatus.RetStatus.error, "I am not familiar with any concept with a field \"" + fieldName + "\". Please define it first, or use a different field.");
        }
        return candidates;
    }

    public void defineConcept(ExecutionStatus executionStatus, String conceptName)
    {
        if (conceptFieldMap.containsKey(conceptName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.warning, "the concept \"" + conceptName + "\" is already defined. "
                    + "Its fields are: " + userFriendlyList(getFields(conceptName)));
        }
        else
        {
            conceptFieldMap.put(conceptName, new LinkedList<>());
        }
    }

    public void defineConcept(ExecutionStatus executionStatus, String conceptName, FieldDescription[] fieldDescriptions)
    {
        defineConcept(executionStatus, conceptName);
        if (executionStatus.isOkOrComment())
        {
            addFieldsToConcept(executionStatus, conceptName, fieldDescriptions);
        }
    }

    public void addFieldToConcept(ExecutionStatus executionStatus, String conceptName, FieldDescription fieldDescription)
    {
        addFieldsToConcept(executionStatus, conceptName, new FieldDescription[]{fieldDescription});
    }

    public void addFieldsToConcept(ExecutionStatus executionStatus, String conceptName, FieldDescription[] fieldDescriptions)
    {
        //TODO: check all these...
        //TODO: check no duplicate fields!!!
        if (!conceptFieldMap.containsKey(conceptName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "the concept \"" + conceptName + "\" is not defined, please define it first");
            return;
        }
        List<FieldDescription> conceptFields = conceptFieldMap.get(conceptName);
        for (FieldDescription fieldDescription : fieldDescriptions)
        {
            if (doesFieldExistInConcept(conceptName, fieldDescription.fieldName))
            {
                executionStatus.add(ExecutionStatus.RetStatus.warning, "the concept \"" + conceptName + "\" already has the field \"" + fieldDescription.fieldName + "\"");
            }
            else
                conceptFields.add(fieldDescription);
        }
    }

    public void removeFieldFromConcept(ExecutionStatus executionStatus, String conceptName, String fieldName)
    {
        //TODO: check all these...
        List<FieldDescription> currentFields = conceptFieldMap.get(conceptName);
        currentFields.removeIf(x -> x.fieldName.equals(fieldName));
    }


    public List<String> getFields(String conceptName)
    {
        return new LinkedList<>(conceptFieldMap.get(conceptName).stream().map(fieldDescription -> fieldDescription.fieldName).collect(Collectors.toList()));
    }

    //can't fail, but can return empty list
    public List<String> getAllConceptNames()
    {
        List<String> conceptNames = new LinkedList<>();
        conceptNames.addAll(conceptFieldMap.keySet());
        return conceptNames;
    }
}
