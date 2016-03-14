package instructable.server.hirarchy;

import instructable.server.backend.ExecutionStatus;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
abstract public class EmailMessage
{
    public static final String subjectStr = "subject";
    public static final String bodyStr = "body";
    public static final String senderStr = "sender";
    public static final String recipientListStr = "recipient list";
    public static boolean useCopy = false;//removed copy: not needed for experiments
    public static final String copyListStr = "copy list";

    protected GenericInstance instance;

    protected static List<FieldDescription> getFieldDescriptions(boolean mutable)
    {

        FieldDescription[] fieldDescriptions = new FieldDescription[]
                {
                        new FieldDescription(subjectStr, PossibleFieldType.singleLineString, false, mutable),
                        new FieldDescription(bodyStr, PossibleFieldType.multiLineString, false, mutable),
                        new FieldDescription(senderStr, PossibleFieldType.emailAddress, false, false), //sender is never mutable
                        new FieldDescription(recipientListStr, PossibleFieldType.emailAddress, true, mutable)//,
                        //new FieldDescription(copyListStr, PossibleFieldType.emailAddress, true, mutable)
                };
        if (useCopy)
        {
            List<FieldDescription> fieldDescriptionList = Arrays.asList(fieldDescriptions);
            fieldDescriptionList.add(new FieldDescription(copyListStr, PossibleFieldType.emailAddress, true, mutable));
            fieldDescriptions = fieldDescriptionList.toArray(new FieldDescription[0]);
        }
        return  Arrays.asList(fieldDescriptions);
    }


    public EmailMessage(InstanceContainer instanceContainer, String messageType, String messageId, boolean isMutable)
    {
        //TODO: may want to support adding theInstance to email messages. In that case, the field descriptions would need to come from the concept container
        //instance = GenericInstance.WrapAsGenericInstance(userId, messageType, messageId, isMutable, getFieldDescriptions(isMutable));
        instanceContainer.addInstance(new ExecutionStatus(), messageType, messageId, isMutable);
        instance = instanceContainer.getInstance(new ExecutionStatus(), messageType, messageId).get(); //not checking, since just created.
    }

    public EmailMessage(GenericInstance instance)
    {
        this.instance = instance;
    }


    // should only be called if email had no name at first.
    public void setName(String newEmailName)
    {
        instance.instanceWasRenamed(newEmailName);
    }

    public boolean hasRecipient()
    {
        return !instance.fieldIsEmpty(recipientListStr);
    }

    public boolean hasCopy()
    {
        if (useCopy)
            return !instance.fieldIsEmpty(copyListStr);
        return false;
    }

    public boolean hasBody()
    {
        return !instance.fieldIsEmpty(bodyStr);
    }

    public boolean hasSubject()
    {
        return !instance.fieldIsEmpty(subjectStr);
    }

    public String getRecipient()
    {
        return getFieldWithoutChecking(recipientListStr);
    }

    public String getCopy()
    {
        if (useCopy)
            return getFieldWithoutChecking(copyListStr);
        return "";
    }

    public String getBody()
    {
        return getFieldWithoutChecking(bodyStr);
    }

    public String getSubject()
    {
        return getFieldWithoutChecking(subjectStr);
    }

    private String getFieldWithoutChecking(String fieldName)
    {
        return instance.getField(new ExecutionStatus(), fieldName).get().fieldValForUser();
    }

    public GenericInstance getInstance()
    {
        return instance;
    }
}
