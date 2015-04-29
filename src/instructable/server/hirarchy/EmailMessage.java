package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
public class EmailMessage extends GenericConcept
{
    public static final String emailMessageType = "email message";
    public static final String subjectStr = "subject";
    public static final String bodyStr = "body";
    public static final String senderStr = "sender";
    public static final String recipientListStr = "recipient list";
    public static final String copyListStr = "copy list";
    public static final FieldDescription[] fieldDescriptions = new FieldDescription[]
            {
                    new FieldDescription(subjectStr, PossibleFieldType.singleLineString, false),
                    new FieldDescription(bodyStr, PossibleFieldType.multiLineString, false),
                    new FieldDescription(senderStr, PossibleFieldType.emailAddress, false),
                    new FieldDescription(recipientListStr, PossibleFieldType.emailAddress, true),
                    new FieldDescription(copyListStr, PossibleFieldType.emailAddress, true)
            };

    public EmailMessage(String sender, String subject, List<String> recipientList, List<String> copyList, String body)
    {
        this("TBD");
        ExecutionStatus executionStatus = new ExecutionStatus();
        setField(executionStatus, senderStr, Optional.of(sender), Optional.empty(), false, true);
        setField(executionStatus, subjectStr, Optional.of(subject), Optional.empty(), false, true);
        setField(executionStatus, bodyStr, Optional.of(body), Optional.empty(), false, true);
        for (String recipient : recipientList)
        {
            setField(executionStatus, recipientListStr, Optional.of(recipient), Optional.empty(), true, true);
        }

        for (String copy : copyList)
        {
            setField(executionStatus, recipientListStr, Optional.of(copy), Optional.empty(), true, true);
        }
    }

    // should only be called if email had no name at first.
    public void setName(String newEmailName)
    {
        name = newEmailName;
    }

    public EmailMessage(String messageId)
    {
        this(emailMessageType, messageId);
    }

    public EmailMessage(String messageType, String messageId)
    {
        super(messageType, messageId, Arrays.asList(fieldDescriptions));
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
}
