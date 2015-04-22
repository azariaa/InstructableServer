package instructable.server;

import instructable.server.hirarchy.ConceptContainer;
import instructable.server.hirarchy.EmailMessage;
import instructable.server.hirarchy.InstanceContainer;
import instructable.server.hirarchy.OutgoingEmail;

import java.util.List;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
public class OutEmailCommandController
{
    private String myEmail;
    InstanceContainer instanceContainer;

    OutEmailCommandController(String myEmail, ConceptContainer conceptContainer, InstanceContainer instanceContainer)
    {
        this.myEmail = myEmail;
        conceptContainer.defineConcept(OutgoingEmail.strOutgoingEmailTypeAndName, EmailMessage.fieldDescriptions);
        this.instanceContainer = instanceContainer;
    }

    public OutgoingEmail getEmailBeingComposed(ExecutionStatus executionStatus)
    {
        OutgoingEmail emailBeingComposed = (OutgoingEmail)instanceContainer.getInstance(executionStatus, OutgoingEmail.strOutgoingEmailTypeAndName, OutgoingEmail.strOutgoingEmailTypeAndName);
        if (emailBeingComposed != null)
            return emailBeingComposed;
        else
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "there is no email being composed");
            return null;
        }
    }

    public void sendEmail(ExecutionStatus executionStatus)
    {
        OutgoingEmail email = getEmailBeingComposed(executionStatus);
        if (email != null)
            email.sendEmail(executionStatus);
    }

    public void createNewEmail(ExecutionStatus executionStatus)
    {
        instanceContainer.addInstance(executionStatus, new OutgoingEmail(executionStatus, myEmail));
    }

    public List<String> changeToRelevantComposedEmailFields(List<String> allEmailFields)
    {
        allEmailFields.remove(EmailMessage.senderStr);
        return allEmailFields;
    }
}
