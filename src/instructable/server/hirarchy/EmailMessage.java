package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;

import java.util.Arrays;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
abstract public class EmailMessage extends GenericInstance
{
    public static final String subjectStr = "subject";
    public static final String bodyStr = "body";
    public static final String senderStr = "sender";
    public static final String recipientListStr = "recipient list";
    public static final String copyListStr = "copy list";

    protected static FieldDescription[] getFieldDescriptions(boolean mutable)
    {
        return new FieldDescription[]
                {
                        new FieldDescription(subjectStr, PossibleFieldType.singleLineString, false, mutable),
                        new FieldDescription(bodyStr, PossibleFieldType.multiLineString, false, mutable),
                        new FieldDescription(senderStr, PossibleFieldType.emailAddress, false, mutable),
                        new FieldDescription(recipientListStr, PossibleFieldType.emailAddress, true, mutable),
                        new FieldDescription(copyListStr, PossibleFieldType.emailAddress, true, mutable)
                };
    }


    // should only be called if email had no name at first.
    public void setName(String newEmailName)
    {
        name = newEmailName;
    }


    public EmailMessage(String messageType, String messageId, FieldDescription[] fieldDescriptions)
    {
        //TODO: may want to support adding fields to email messages. In that case, the field descriptions would need to come from the concept container
        super(messageType, messageId, Arrays.asList((fieldDescriptions)));
    }

    public boolean hasRecipient()
    {
        return !fieldIsEmpty(recipientListStr);
    }

    public boolean hasCopy()
    {
        return !fieldIsEmpty(copyListStr);
    }

    public boolean hasBody()
    {
        return !fieldIsEmpty(bodyStr);
    }

    public boolean hasSubject()
    {
        return !fieldIsEmpty(subjectStr);
    }

    public String getRecipient()
    {
        return getFieldWithoutChecking(recipientListStr);
    }

    public String getCopy()
    {
        return getFieldWithoutChecking(copyListStr);
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
        return getField(new ExecutionStatus(), fieldName).get().fieldValForUser();
    }
}
