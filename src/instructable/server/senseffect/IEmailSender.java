package instructable.server.senseffect;

import instructable.server.backend.ExecutionStatus;

/**
 * Created by Amos Azaria on 02-Jun-15.
 */
public interface IEmailSender
{
    void sendEmail(ExecutionStatus executionStatus, String subject, String body, String copyList, String recipientList);
}
