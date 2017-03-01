package instructable.server.senseffect;

import instructable.server.backend.ExecutionStatus;
import instructable.server.hirarchy.EmailInfo;

import java.util.Optional;

/**
 * Created by Amos Azaria on 28-Feb-17.
 */
public class EmptyEmailOperations implements IEmailSender, IEmailFetcher
{
    String emailAndPasswordNotSet = "email and password are not set";

    @Override
    public int getLastEmailIdx(ExecutionStatus executionStatus)
    {
        executionStatus.add(ExecutionStatus.RetStatus.noPswdSet, emailAndPasswordNotSet);
        return 0;
    }

    @Override
    public Optional<EmailInfo> getEmailInfo(ExecutionStatus executionStatus, int emailIdx)
    {
        executionStatus.add(ExecutionStatus.RetStatus.noPswdSet, emailAndPasswordNotSet);
        return Optional.empty();
    }

    @Override
    public void sendEmail(ExecutionStatus executionStatus, String subject, String body, String copyList, String recipientList)
    {
        executionStatus.add(ExecutionStatus.RetStatus.noPswdSet, emailAndPasswordNotSet);
    }
}
