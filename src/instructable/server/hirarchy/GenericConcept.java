package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;

import java.util.*;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
public class GenericConcept
{

    public GenericConcept(String type, List<FieldDescription> fieldsInType)
    {
        lastAccess = new Date();
        this.type = type;
        fields = new HashMap<String, FieldHolder>();
        for (FieldDescription fieldDescription : fieldsInType)
        {
            fields.put(fieldDescription.fieldName, new FieldHolder(fieldDescription));
        }
    }

    String name;
    String type; //class, concept
    Date lastAccess;
    private Map<String, FieldHolder> fields;

    public String getName()
    {
        return name;
    }

    /*
    replaces existing (use add to add or create new).
    must have field defined in this object.
     */
    public void setField(ExecutionStatus executionStatus, String fieldName, String val)
    {
        lastAccess = new Date();
        if (fields.containsKey(fieldName))
        {
            FieldHolder requestedField = fields.get(fieldName);
            if (requestedField == null)
            {
                executionStatus.add(ExecutionStatus.RetStatus.error, "the field cannot be empty");
                return;
            }
            requestedField.set(executionStatus, val);
            return;
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the field cannot be found");
    }

    /*
        adds to existing field.
        must have field defined in this object.
     */
    public void addToField(ExecutionStatus executionStatus, String fieldName, String val, boolean appendToEnd)
    {
        lastAccess = new Date();
        if (fields.containsKey(fieldName))
        {
            FieldHolder requestedField = fields.get(fieldName);
            if (requestedField == null)
            {
                executionStatus.add(ExecutionStatus.RetStatus.error, "the field cannot be empty");
                return;
            }
            if (appendToEnd)
            {
                requestedField.appendToEnd(executionStatus, val);
                return;
            }
            else
            {
                requestedField.addToBeginning(executionStatus, val);
                return;
            }
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the field cannot be found");
    }

    /*
    should be called only if a new field was added to the type
     */
    public ExecutionStatus addFieldToObject(FieldDescription fieldToAdd)
    {
        lastAccess = new Date();
        if (!fields.containsKey(fieldToAdd.fieldName))
        {
            fields.put(fieldToAdd.fieldName, new FieldHolder(fieldToAdd));
            return new ExecutionStatus();
        }
        return new ExecutionStatus(ExecutionStatus.RetStatus.error, "the field cannot be found");
    }

    public boolean fieldExists(String fieldName)
    {
        lastAccess = new Date();
        return fields.containsKey(fieldName);
    }

    public boolean fieldIsEmpty(String fieldName)
    {
        lastAccess = new Date();
        if (fields.containsKey(fieldName))
        {
            return fields.get(fieldName).isEmpty();
        }

        return true;
    }

    public Set<String> getAllFieldNames()
    {
        lastAccess = new Date();
        return fields.keySet();
    }


//    void removeEmptyStrings(List<String> org)
//    {
//        if (org == null)
//            return;
//        org.removeIf(x -> x.trim().isEmpty());
//    }
}
