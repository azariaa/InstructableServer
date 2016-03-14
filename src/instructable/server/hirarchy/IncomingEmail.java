package instructable.server.hirarchy;

import instructable.server.backend.ExecutionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 11-May-15.
 */
public class IncomingEmail extends EmailMessage
{
    public static final String incomingEmailType = "incoming email";

    public IncomingEmail(InstanceContainer instanceContainer, EmailInfo emailInfo, String name)
    {
        super(instanceContainer, incomingEmailType, name, false);
        ExecutionStatus executionStatus = new ExecutionStatus();
        instance.setField(executionStatus, senderStr, Optional.of(emailInfo.sender), Optional.empty(), false, true, true);
        instance.setField(executionStatus, subjectStr, Optional.of(emailInfo.subject), Optional.empty(), false, true, true);
        instance.setField(executionStatus, bodyStr, Optional.of(emailInfo.body), Optional.empty(), false, true, true);
        for (String recipient : emailInfo.recipientList)
        {
            instance.setField(executionStatus, recipientListStr, Optional.of(recipient), Optional.empty(), true, true, true);
        }

        for (String copy : emailInfo.copyList)
        {
            instance.setField(executionStatus, recipientListStr, Optional.of(copy), Optional.empty(), true, true, true);
        }
    }

//    public IncomingEmail(String userId, String sender, String subject, List<String> recipientList, List<String> copyList, String body)
//    {
//    }

    static public List<FieldDescription> getFieldDescriptions()
    {
        return getFieldDescriptions(false);
    }

}
