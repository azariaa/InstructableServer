package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 11-May-15.
 */
public class IncomingEmail extends EmailMessage
{
    public static final String incomingEmailType = "incoming email";

    public IncomingEmail(String sender, String subject, List<String> recipientList, List<String> copyList, String body)
    {
        super(incomingEmailType, "TBD", getFieldDescriptions(), false);
        ExecutionStatus executionStatus = new ExecutionStatus();
        setField(executionStatus, senderStr, Optional.of(sender), Optional.empty(), false, true, true);
        setField(executionStatus, subjectStr, Optional.of(subject), Optional.empty(), false, true, true);
        setField(executionStatus, bodyStr, Optional.of(body), Optional.empty(), false, true, true);
        for (String recipient : recipientList)
        {
            setField(executionStatus, recipientListStr, Optional.of(recipient), Optional.empty(), true, true, true);
        }

        for (String copy : copyList)
        {
            setField(executionStatus, recipientListStr, Optional.of(copy), Optional.empty(), true, true, true);
        }
    }

    static public FieldDescription[] getFieldDescriptions()
    {
        return getFieldDescriptions(false);
    }

}
