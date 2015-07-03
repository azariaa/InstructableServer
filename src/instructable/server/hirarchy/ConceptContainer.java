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
    Map<String, List<FieldDescription>> conceptFieldMap; // a map holding all concepts and the list of their fields

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


    /**
     *
     * @param executionStatus
     * @param fieldName
     * @param mutableOnly
     * @param userUsedTheWordAs The word "as" may be confusing, especially when setting. If someone says set recipient as sender, they probably mean set recipient to sender. The agent will express this if required.
     * @return
     */
    public List<String> findConceptsForField(ExecutionStatus executionStatus, String fieldName, boolean mutableOnly, boolean userUsedTheWordAs)
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
            {
                String failSentence = "the field \"" + fieldName + "\" of the concept \"" + foundImmutableConcept.get() + "\" is immutable.";
                if (userUsedTheWordAs)
                    failSentence = failSentence + "\nThe word \"as\" in your command, may be confusing; try replacing it with \"to\"";
                else
                    failSentence =  failSentence + "\nIf you want to set a mutable field to the "+ fieldName + ", say: \"set field name to " + fieldName + "\"";

                executionStatus.add(ExecutionStatus.RetStatus.error, failSentence);
            }
            else
                executionStatus.add(ExecutionStatus.RetStatus.error, "I am not familiar with any concept with a field \"" + fieldName + "\". Please define it first, or use a different field");
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
        if (conceptFieldMap.containsKey(conceptName))
        {
            List<FieldDescription> currentFields = conceptFieldMap.get(conceptName);
            if (currentFields.stream().anyMatch(x->x.fieldName.equals(fieldName)))
            {
                currentFields.removeIf(x -> x.fieldName.equals(fieldName));
                //success
                return;
            }
        }

        executionStatus.add(ExecutionStatus.RetStatus.error, "the concept \"" + conceptName + "\" does not have a field named \"" + fieldName + "\"");
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

    public void undefineConcept(ExecutionStatus executionStatus, String conceptName)
    {
        if (conceptFieldMap.containsKey(conceptName))
            conceptFieldMap.remove(conceptName);
        else
            executionStatus.add(ExecutionStatus.RetStatus.error, "the concept \"" + conceptName + "\" was not found");
    }
}
