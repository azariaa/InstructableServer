package instructable.server;

import instructable.server.hirarchy.EmailMessage;

/**
 * Created by Amos Azaria on 28-Apr-15.
 */
public interface IIncomingEmailControlling
{
    void addEmailMessageToInbox(EmailMessage emailMessage);
}
