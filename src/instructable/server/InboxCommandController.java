package instructable.server;

import instructable.server.hirarchy.ConceptContainer;
import instructable.server.hirarchy.GenericInstance;
import instructable.server.hirarchy.IncomingEmail;
import instructable.server.hirarchy.InstanceContainer;

import java.util.Optional;

/**
 * Created by Amos Azaria on 19-May-15.
 */
public class InboxCommandController
{
    int currentIncomingEmailIdx = 0;
    static final String emailMessageNameStart = "inbox";
    ConceptContainer conceptContainer;
    InstanceContainer instanceContainer;

    public InboxCommandController(ConceptContainer conceptContainer, InstanceContainer instanceContainer)
    {
        this.conceptContainer = conceptContainer;
        this.instanceContainer = instanceContainer;
        conceptContainer.defineConcept(new ExecutionStatus(), IncomingEmail.incomingEmailType, IncomingEmail.getFieldDescriptions());

    }

    public void addEmailMessageToInbox(IncomingEmail emailMessage)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        emailMessage.setName(instanceName(inboxSize()));
        instanceContainer.addInstance(executionStatus, emailMessage);
    }

    public String addCounterToEmailMessageIdIfRequired(String instanceName)
    {
        if (instanceName.equals(emailMessageNameStart))
            return emailMessageNameStart + currentIncomingEmailIdx;
        return instanceName;
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
            executionStatus.add(ExecutionStatus.RetStatus.error, "there are no more messages");
    }

    public void setToPrevEmail(ExecutionStatus executionStatus)
    {
        if (currentIncomingEmailIdx > 0)
            currentIncomingEmailIdx--;
        else
            executionStatus.add(ExecutionStatus.RetStatus.error, "there is no previous messages");
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
