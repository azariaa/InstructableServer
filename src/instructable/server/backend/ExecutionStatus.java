package instructable.server.backend;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 20-Apr-15.
 * <p>
 * Message style should adhere to a sentence saying: "I see that ..."
 */
public class ExecutionStatus
{

    public enum RetStatus
    {
        ok, comment, warning, error, noPswdSet
    }

    ;
    //TODO: should have the messages strings in a separate file and have an enum (or similar) defining all messages

    static public class StatusAndMessage
    {
        StatusAndMessage(RetStatus retStatus, Optional<String> message)
        {
            this.retStatus = retStatus;
            this.message = message;
        }

        RetStatus retStatus;
        Optional<String> message;
    }

    /*
     if everything is ok.
     */
    public ExecutionStatus()
    {
    }

    public ExecutionStatus(RetStatus retStatus, String message)
    {
        add(retStatus, message);
    }

    List<StatusAndMessage> internalList = new LinkedList<StatusAndMessage>();

    public StatusAndMessage getStatusAndMessage()
    {
        StatusAndMessage retStatusAndMessage = new StatusAndMessage(RetStatus.ok, null);
        for (StatusAndMessage statusAndMessage : internalList)
        {
            if (statusAndMessage.retStatus == RetStatus.error)
            {
                retStatusAndMessage = statusAndMessage;
                break;
            }
            else if (statusAndMessage.retStatus == RetStatus.noPswdSet)
            {
                retStatusAndMessage = statusAndMessage;
                break;
            }
            else if (statusAndMessage.retStatus == RetStatus.warning && retStatusAndMessage.retStatus != RetStatus.error)
            {
                retStatusAndMessage = statusAndMessage;
            }
            else if (statusAndMessage.retStatus == RetStatus.comment && retStatusAndMessage.retStatus != RetStatus.error && retStatusAndMessage.retStatus != RetStatus.warning)
            {
                retStatusAndMessage = statusAndMessage;
            }
        }
        return retStatusAndMessage;

    }

    public boolean isError()
    {
        RetStatus retStatus = getStatus();
        return retStatus == RetStatus.error || retStatus == RetStatus.noPswdSet;
    }

    public boolean noError()
    {
        return !isError();
    }


    public boolean isOkOrComment()
    {
        RetStatus retStatus = getStatus();
        return retStatus == RetStatus.ok || retStatus == RetStatus.comment;
    }

    public RetStatus getStatus()
    {
        return getStatusAndMessage().retStatus;
    }

    public void add(RetStatus retStatus, String message)
    {
        internalList.add(new StatusAndMessage(retStatus, Optional.of(message)));
    }
}
