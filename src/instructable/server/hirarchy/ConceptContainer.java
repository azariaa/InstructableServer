package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;

import java.util.*;

import static instructable.server.TextFormattingUtils.userFriendlyList;

/**
 * Created by Amos Azaria on 21-Apr-15.
 *
 * This class has access to all defined concepts and their fields and types.
 * This class has the required information for creating an instance of a concept.
 * This class can access the data base
 */
public class ConceptContainer
{
    Map<String,List<FieldDescription>> conceptFieldMap;

    public ConceptContainer()
    {
        conceptFieldMap = new HashMap<>();
        conceptFieldMap.put("email", Arrays.asList(EmailMessage.fieldDescriptions));
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

    public List<String> findConceptsForField(ExecutionStatus executionStatus, String fieldName)
    {
        List<String> candidates = new LinkedList<>();
        for (String concept : conceptFieldMap.keySet())
        {
            List<FieldDescription> fieldDescriptionList = conceptFieldMap.get(concept);
            for (FieldDescription fieldDescription : fieldDescriptionList)
            {
                if (fieldDescription.fieldName.equals(fieldName))
                {
                    candidates.add(concept);
                    break;
                }
            }
        }
        if (candidates.size() == 0)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "I am not familiar with any concept with a field \"" + fieldName + "\". Please define it first, or use a different field.");
        }
        return candidates;
    }

    public void defineConcept(ExecutionStatus executionStatus, String conceptName)
    {
        if (conceptFieldMap.containsKey(conceptName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.warning, "the concept \""+conceptName + "\" is already defined. "
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
        addFieldsToConcept(executionStatus, conceptName, new FieldDescription[] {fieldDescription});
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
                executionStatus.add(ExecutionStatus.RetStatus.warning, "the concept \"" + conceptName + "\" already has the field \""+fieldDescription.fieldName + "\"");
            }
            else
                conceptFields.add(fieldDescription);
        }
    }

    public void removeFieldFromConcept(ExecutionStatus executionStatus, String conceptName, String fieldName)
    {
        //TODO: check all these...
        List<FieldDescription> currentFields = conceptFieldMap.get(conceptName);
        currentFields.removeIf( x -> x.fieldName == fieldName);
    }


    public List<String> getFields(String conceptName)
    {
        //I wish I could use LinQ...
        LinkedList<String> fieldNames = new LinkedList<>();
        List<FieldDescription> fields = conceptFieldMap.get(conceptName);
        if (fields != null)
        {
            for (FieldDescription fieldDescription : fields)
            {
                fieldNames.add(fieldDescription.fieldName);
            }
        }
        return  fieldNames;
    }

    //can't fail, but can return empty list
    public List<String> getAllConceptNames()
    {
        List<String> conceptNames = new LinkedList<>();
        conceptNames.addAll(conceptFieldMap.keySet());
        return  conceptNames;
    }
}
