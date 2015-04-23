package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
public class OutgoingEmail extends EmailMessage
{
    public static final String strOutgoingEmailTypeAndName = "outgoing email";
    boolean warnIfNoBody = true;
    boolean warnIfNoSubject = true;

    public OutgoingEmail(ExecutionStatus executionStatus, String myEmail)
    {
        super(strOutgoingEmailTypeAndName, strOutgoingEmailTypeAndName);
        setField(executionStatus, senderStr, myEmail);
    }


    public void sendEmail(ExecutionStatus executionStatus)
    {
        checkSendingPrerequisites(executionStatus,true);
        if (!executionStatus.isError())
        {
            //TODO: sendEmail

            return;
        }
        return;
    }


    private void checkSendingPrerequisites(ExecutionStatus executionStatus, boolean testWarnings)
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
                    executionStatus.add(ExecutionStatus.RetStatus.warning,  "the message has no body");
                    return;
                }
            }
        }
        executionStatus.add(ExecutionStatus.RetStatus.error, "the message has no recipient");
        return;
    }

}
