package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
public class GenericInstance
{

    public GenericInstance(String conceptName, String instanceName, List<FieldDescription> fieldsInType)
    {
        name = instanceName;
        lastAccess = System.nanoTime(); //System.currentTimeMillis(); doesn't work because in test may execute several commands in same millisecond
        this.conceptName = conceptName;
        fields = new HashMap<String, FieldHolder>();
        for (FieldDescription fieldDescription : fieldsInType)
        {
            fields.put(fieldDescription.fieldName, new FieldHolder(fieldDescription, this));
        }
    }

    String name;
    String conceptName; //type, class, concept
    long lastAccess;
    private Map<String, FieldHolder> fields;

    public String getName()
    {
        return name;
    }

    public String getConceptName()
    {
        return conceptName;
    }

    /*
    replaces existing (use add to add or create new).
    must have field defined in this object.
    must either have val or jsonVal
     */
    public void setField(ExecutionStatus executionStatus, String fieldName, Optional<String> val, Optional<JSONObject> jsonVal, boolean addToExisting, boolean appendToEnd, boolean setAlsoImmutable)
    {
        lastAccess = System.nanoTime();
        if (fields.containsKey(fieldName))
        {
            FieldHolder requestedField = fields.get(fieldName);
            //requestedField shouldn't be null.
            if (jsonVal.isPresent())
            {
                requestedField.setFromJSon(executionStatus, jsonVal.get(), addToExisting, appendToEnd, setAlsoImmutable);
            }
            else
            {
                if (addToExisting)
                    requestedField.appendTo(executionStatus, val.get(), appendToEnd, setAlsoImmutable);
                else
                    requestedField.set(executionStatus, val.get(), setAlsoImmutable);
            }
            return;
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the field \"" + fieldName + " cannot be found");
    }


    /*
    should be called only if a new field was added to the conceptName
     */
    public ExecutionStatus addFieldToObject(FieldDescription fieldToAdd)
    {
        lastAccess = System.nanoTime();
        if (!fields.containsKey(fieldToAdd.fieldName))
        {
            fields.put(fieldToAdd.fieldName, new FieldHolder(fieldToAdd, this));
            return new ExecutionStatus();
        }
        return new ExecutionStatus(ExecutionStatus.RetStatus.error, "the field cannot be found");
    }

    public boolean fieldExists(String fieldName)
    {
        lastAccess = System.nanoTime();
        return fields.containsKey(fieldName);
    }

    public boolean fieldIsEmpty(String fieldName)
    {
        lastAccess = System.nanoTime();
        if (fields.containsKey(fieldName))
        {
            return fields.get(fieldName).isEmpty();
        }

        return true;
    }

    public Set<String> getAllFieldNames()
    {
        lastAccess = System.nanoTime();
        return fields.keySet();
    }

    public Optional<FieldHolder> getField(ExecutionStatus executionStatus, String fieldName)
    {
        lastAccess = System.nanoTime();
        if (fields.containsKey(fieldName))
        {
            return Optional.of(fields.get(fieldName));
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the field \"" + fieldName + " cannot be found");
        return Optional.empty();
    }

    public Optional<JSONObject> getFieldVal(ExecutionStatus executionStatus, String fieldName)
    {
        lastAccess = System.nanoTime();
        Optional<FieldHolder> requestedField = getField(executionStatus, fieldName);
        if (requestedField.isPresent())
        {
            return Optional.of(requestedField.get().getFieldVal());
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the field \"" + fieldName + " cannot be found");
        return Optional.empty();
    }


//    void removeEmptyStrings(List<String> org)
//    {
//        if (org == null)
//            return;
//        org.removeIf(x -> x.trim().isEmpty());
//    }
}
