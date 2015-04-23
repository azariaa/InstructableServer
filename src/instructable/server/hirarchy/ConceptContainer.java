package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;

import java.util.*;

import static instructable.server.StaticUtils.userFriendlyList;

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

    public List<String> findConceptsForField(String fieldName)
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
        //TODO: should test if concept exists, wants to add, etc.
        conceptFieldMap.put(conceptName, Arrays.asList(fieldDescriptions));
    }

    public void addFieldToConcept(ExecutionStatus executionStatus, String conceptName, FieldDescription fieldDescription)
    {
        addFieldsToConcept(executionStatus, conceptName, new FieldDescription[] {fieldDescription});
    }

    public void addFieldsToConcept(ExecutionStatus executionStatus, String conceptName, FieldDescription[] fieldDescriptions)
    {
        //TODO: check all these...
        //TODO: check no duplicate fields!!!
        List<FieldDescription> currentFields = conceptFieldMap.get(conceptName);
        currentFields.addAll(Arrays.asList(fieldDescriptions));
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
}
