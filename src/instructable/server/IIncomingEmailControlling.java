package instructable.server;

import instructable.server.hirarchy.IncomingEmail;

/**
 * Created by Amos Azaria on 28-Apr-15.
 */
public interface IIncomingEmailControlling
{
    void addEmailMessageToInbox(IncomingEmail emailMessage);
}
