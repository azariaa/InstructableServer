package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;
import instructable.server.dal.SingleInstance;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by Amos Azaria on 15-Apr-15.
 * Any changes to instance (not to fields) must be reported to theInstance
 */
public class GenericInstance
{

    //String name;
    //String conceptName; //type, class, concept
    //boolean mutable;
    //long lastAccess;
    SingleInstance theInstance;

    private GenericInstance()
    {
        //lastAccess = System.nanoTime(); //System.currentTimeMillis(); doesn't work because in test may execute several commands in same millisecond
    }

    public static GenericInstance WrapAsGenericInstance(SingleInstance interfaceWithInstanceData)
    {
        GenericInstance genericInstance = new GenericInstance();
        genericInstance.theInstance = interfaceWithInstanceData;
        return genericInstance;
        //name = instanceName;

        //this.conceptName = conceptName;
        //this.mutable = mutable;
    }

    SingleInstance getDataInterface()
    {
        return theInstance;
    }

    public String getName()
    {
        return theInstance.getInstanceName();
    }

    public void instanceWasRenamed(String newName)
    {
        theInstance.instanceWasRenamed(newName);
    }

    public String getConceptName()
    {
        return theInstance.getConceptName();
    }

    /*
    replaces existing (use add to add or create new).
    must have field defined in this object.
    must either have val or jsonVal
     */
    public void setField(ExecutionStatus executionStatus, String fieldName, Optional<String> val, Optional<JSONObject> jsonVal, boolean addToExisting, boolean appendToEnd, boolean setAlsoImmutable)
    {
        touch();
        if (theInstance.containsField(fieldName))
        {
            FieldHolder requestedField = theInstance.getField(fieldName);
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

    public void touch()
    {
        theInstance.accessed(System.nanoTime());
    }

    /*
    should be called only if a new field was added to the conceptName
     */
    public void fieldWasAddedToConcept(ExecutionStatus executionStatus, FieldDescription fieldToAdd)
    {
        touch();
        if (!theInstance.containsField(fieldToAdd.fieldName))
        {
            theInstance.fieldWasAdded(fieldToAdd);
            return ;
        }
    }


    public void fieldWasRemovedFromConcept(ExecutionStatus executionStatus, String fieldName)
    {
        touch();
        if (theInstance.containsField(fieldName))
        {
            theInstance.fieldWasRemovedFromConcept(fieldName);
            return ;
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the field cannot be found");
    }

//    public boolean fieldExists(String fieldName)
//    {
//        touch();
//        return theInstance.containsField(fieldName);
//    }

    public boolean fieldIsEmpty(String fieldName)
    {
        touch();
        if (theInstance.containsField(fieldName))
        {
            return theInstance.getField(fieldName).isEmpty();
        }

        return true;
    }

    public Set<String> getAllFieldNames()
    {
        touch();
        return theInstance.getAllFieldNames();
    }

    public Optional<FieldHolder> getField(ExecutionStatus executionStatus, String fieldName)
    {
        touch();
        if (theInstance.containsField(fieldName))
        {
            return Optional.of(theInstance.getField(fieldName));
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the field \"" + fieldName + " cannot be found");
        return Optional.empty();
    }

    public Optional<JSONObject> getFieldVal(ExecutionStatus executionStatus, String fieldName)
    {
        touch();
        Optional<FieldHolder> requestedField = getField(executionStatus, fieldName);
        if (requestedField.isPresent())
        {
            return Optional.of(requestedField.get().getFieldVal());
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the field \"" + fieldName + " cannot be found");
        return Optional.empty();
    }

    public boolean getMutable()
    {
        return theInstance.getMutable();
    }

    public void changeMutability(boolean newMutability)
    {
        theInstance.changeMutability(newMutability);
    }

    public long getLastAccess()
    {
        return theInstance.getLastAccess();
    }

//    void removeEmptyStrings(List<String> org)
//    {
//        if (org == null)
//            return;
//        org.removeIf(x -> x.trim().isEmpty());
//    }
}
