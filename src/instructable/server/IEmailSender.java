package instructable.server;

/**
 * Created by Amos Azaria on 02-Jun-15.
 */
public interface IEmailSender
{
    void sendEmail(String subject, String body, String copyList, String recipientList);
}
