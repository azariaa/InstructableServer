package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
public class GenericConcept
{

    public GenericConcept(String type, String instanceName, List<FieldDescription> fieldsInType)
    {
        name = instanceName;
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
    must either have val or jsonVal
     */
    public void setField(ExecutionStatus executionStatus, String fieldName, Optional<String> val, Optional<JSONObject> jsonVal)
    {
        lastAccess = new Date();
        if (fields.containsKey(fieldName))
        {
            FieldHolder requestedField = fields.get(fieldName);
            //requestedField shouldn't be null.
            if (jsonVal.isPresent())
                requestedField.setFromJSon(executionStatus, jsonVal.get());
            else
                requestedField.set(executionStatus, val.get());
            return;
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the field \"" + fieldName + " cannot be found");
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
            //requestedField shouldn't be null.
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
        executionStatus.add(ExecutionStatus.RetStatus.error, "the field \"" + fieldName + " cannot be found");
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

    public JSONObject getField(ExecutionStatus executionStatus, String fieldName)
    {
        lastAccess = new Date();
        if (fields.containsKey(fieldName))
        {
            FieldHolder requestedField = fields.get(fieldName);
            //requestedField shouldn't be null.
            return requestedField.getAsJSon();
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the field \"" + fieldName + " cannot be found");
        return null;
    }


//    void removeEmptyStrings(List<String> org)
//    {
//        if (org == null)
//            return;
//        org.removeIf(x -> x.trim().isEmpty());
//    }
}
