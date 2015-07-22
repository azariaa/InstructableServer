package instructable.server;

import instructable.server.hirarchy.*;

import java.util.Optional;

/**
 * Created by Amos Azaria on 19-May-15.
 */
public class InboxCommandController
{
    int currentIncomingEmailIdx = 0;
    static public final String emailMessageNameStart = "inbox";
    String userId;
    ConceptContainer conceptContainer;
    InstanceContainer instanceContainer;

    public InboxCommandController(ConceptContainer conceptContainer, InstanceContainer instanceContainer)
    {
        userId = conceptContainer.getUserId();
        this.conceptContainer = conceptContainer;
        this.instanceContainer = instanceContainer;
        conceptContainer.defineConcept(new ExecutionStatus(), IncomingEmail.incomingEmailType, IncomingEmail.getFieldDescriptions());

    }

    public void addEmailMessageToInbox(EmailInfo emailMessage)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        IncomingEmail incomingEmail = new IncomingEmail(userId, emailMessage, instanceName(inboxSize()));
        instanceContainer.addInstance(executionStatus, incomingEmail.getInstance());
    }

    public boolean isInboxInstanceName(String instanceName)
    {
        return instanceName.equals(emailMessageNameStart);
    }


    public String getCurrentEmailName()
    {
        return emailMessageNameStart + currentIncomingEmailIdx;
    }

    public Optional<GenericInstance> getCurrentIncomingEmail(ExecutionStatus executionStatus)
    {
        return instanceContainer.getInstance(executionStatus, IncomingEmail.incomingEmailType, instanceName(currentIncomingEmailIdx));
    }

    public void setToNextEmail(ExecutionStatus executionStatus)
    {
        int inboxSize = inboxSize();
        if (currentIncomingEmailIdx < inboxSize - 1)
            currentIncomingEmailIdx++;
        else
            executionStatus.add(ExecutionStatus.RetStatus.error, "there are no more emails. You have probably missed some earlier emails. You may want to request a previous email (say \"previous email\")"); //TODO: shouldn't be in the production version
    }

    public void setToPrevEmail(ExecutionStatus executionStatus)
    {
        if (currentIncomingEmailIdx > 0)
            currentIncomingEmailIdx--;
        else
            executionStatus.add(ExecutionStatus.RetStatus.error, "there is no previous email");
    }

    private String instanceName(int emailIdx)
    {
        return emailMessageNameStart + emailIdx;
    }

    private int inboxSize()
    {
        return instanceContainer.getAllInstances(IncomingEmail.incomingEmailType).size();
    }
    //TODO: delete?
}
