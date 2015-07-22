package instructable.server.dal;

import instructable.server.hirarchy.FieldDescription;
import instructable.server.hirarchy.FieldHolder;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Amos Azaria on 21-Jul-15.
 * No checking.
 */
public class SingleInstance
{
    String userId;
    String conceptName;
    String instanceName;
    boolean mutable;
    private Map<String, FieldHolder> fields;

    private SingleInstance(String userId, String conceptName, String instanceName, boolean mutable)
    {
        this.userId = userId;
        this.conceptName = conceptName;
        this.instanceName = instanceName;
        this.mutable = mutable;
    }

    public static SingleInstance loadInstanceFieldsFromDB(String userId, String conceptName, String instanceName, boolean mutable, List<FieldDescription> fieldsInType)
    {
        SingleInstance singleInstance = new SingleInstance(userId,conceptName,instanceName,mutable);
        singleInstance.loadFieldsFromDB(fieldsInType);
        return singleInstance;
    }

    public static SingleInstance createNewInstance(String userId, String conceptName, String instanceName, boolean mutable, List<FieldDescription> fieldsInType)
    {
        SingleInstance singleInstance = new SingleInstance(userId,conceptName,instanceName,mutable);
        singleInstance.createNewFields(fieldsInType);
        return singleInstance;
    }

    private void createNewFields(List<FieldDescription> fieldsInType)
    {
        fields = new HashMap<>();

        //TODO: no need to update DB. because DB only holds fields with values in them.
        for (FieldDescription fieldDescription : fieldsInType)
        {
            fields.put(fieldDescription.fieldName, new FieldHolder(fieldDescription, instanceName, this));
        }

    }

    private void loadFieldsFromDB(List<FieldDescription> fieldsInType)
    {
        fields = new HashMap<>();
        //TODO: fill map from DB!!!

        //FieldHolder.createFromDBdata()

    }

    public String getInstanceName()
    {
        return instanceName;
    }

    public void instanceWasRenamed(String newName)
    {
        instanceName = newName;
        //TODO: do we need to update the DB? I think someone else already did...
    }

    public String getConceptName()
    {
        return conceptName;
    }

    public boolean containsField(String fieldName)
    {
        return fields.containsKey(fieldName);
    }

    public FieldHolder getField(String fieldName)
    {
        return fields.get(fieldName);
    }

    public void fieldWasAdded(FieldDescription fieldDescription)
    {
        //no need to update DB, because the field is still empty.
        fields.put(fieldDescription.fieldName, new FieldHolder(fieldDescription, instanceName, this));
    }

    public void fieldWasRemovedFromConcept(String fieldName)
    {
        //TODO: update the DB!!!
        fields.remove(fieldName);
    }

    public Set<String> getAllFieldNames()
    {
        return fields.keySet();
    }

    public boolean getMutable()
    {
        return mutable;
    }

    public void changeMutability(boolean newMutability)
    {
        //TODO: update DB!!!
        mutable = newMutability;
    }

    public void fieldChanged(String fieldName, JSONObject fieldVal)
    {
        //TODO: update DB!!!!
        //update instance_values set field_jsonval = fieldVal.toString() where user_id=userId and concept_name=conceptName and instance_name=instanceName;
    }
}
