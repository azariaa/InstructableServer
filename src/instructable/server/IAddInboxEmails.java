package instructable.server;

import instructable.server.backend.IIncomingEmailControlling;

/**
 * Created by Amos Azaria on 02-Jun-15.
 */
public interface IAddInboxEmails
{
    void addInboxEmails(IIncomingEmailControlling incomingEmailControlling);
}
