package instructable.server.hirarchy;

import java.util.List;

/**
 * Created by Amos Azaria on 22-Jul-15.
 * Only holds email information for later creation
 */
public class EmailInfo
{
    final String sender;
    final String subject;
    final List<String> recipientList;
    final List<String> copyList;
    final String body;

    public EmailInfo(String sender, String subject, List<String> recipientList, List<String> copyList, String body)
    {
        this.sender = sender;
        this.subject = subject;
        this.recipientList = recipientList;
        this.copyList = copyList;
        this.body = body;
    }

}
