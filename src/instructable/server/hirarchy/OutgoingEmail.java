package instructable.server.hirarchy;

import instructable.server.backend.ExecutionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
public class OutgoingEmail extends EmailMessage
{
    public static final String strOutgoingEmailTypeAndName = "outgoing email";
    boolean warnIfNoBody = true;
    boolean warnIfNoSubject = true;

    public OutgoingEmail(ExecutionStatus executionStatus, InstanceContainer instanceContainer, String myEmail)
    {
        super(instanceContainer, strOutgoingEmailTypeAndName, strOutgoingEmailTypeAndName, true);
        instance.setField(executionStatus, senderStr, Optional.of(myEmail), Optional.empty(), false, true, true);
    }

    public OutgoingEmail(GenericInstance instance)
    {
        super(instance);
    }


    static public List<FieldDescription> getFieldDescriptions()
    {
        return getFieldDescriptions(true);
    }

    public void checkSendingPrerequisites(ExecutionStatus executionStatus, boolean testWarnings)
    {
        if ((hasRecipient() || hasCopy()))
        {
            if (testWarnings)
            {
                if (!warnIfNoBody || hasBody())
                {
                    if ((!warnIfNoSubject || hasSubject()))
                    {
                        return;
                    }
                    else
                    {
                        executionStatus.add(ExecutionStatus.RetStatus.warning, "the message has no subject");
                        return;
                    }
                }
                else
                {
                    executionStatus.add(ExecutionStatus.RetStatus.warning, "the message has no body");
                    return;
                }
            }
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the message has no recipient. Set the recipient to an email address (e.g. set recipient to email@example.com)");
    }

}
