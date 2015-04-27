package instructable.server;

import instructable.server.hirarchy.*;

import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
public class OutEmailCommandController
{
    private String myEmail;
    InstanceContainer instanceContainer;
    int numOfEmailsSent = 0;

    OutEmailCommandController(String myEmail, ConceptContainer conceptContainer, InstanceContainer instanceContainer)
    {
        this.myEmail = myEmail;
        conceptContainer.defineConcept(new ExecutionStatus(), OutgoingEmail.strOutgoingEmailTypeAndName, EmailMessage.fieldDescriptions);
        this.instanceContainer = instanceContainer;
    }

    public Optional<OutgoingEmail> getEmailBeingComposed(ExecutionStatus executionStatus)
    {
        Optional<GenericConcept> emailBeingComposed = instanceContainer.getInstance(executionStatus, OutgoingEmail.strOutgoingEmailTypeAndName, OutgoingEmail.strOutgoingEmailTypeAndName);
        if (emailBeingComposed.isPresent())
        {
            return Optional.of((OutgoingEmail) emailBeingComposed.get());
        }
        else
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "there is no email being composed");
            return Optional.empty();
        }
    }

    public void sendEmail(ExecutionStatus executionStatus)
    {
        Optional<OutgoingEmail> email = getEmailBeingComposed(executionStatus);
        if (email.isPresent())
        {
            email.get().sendEmail(executionStatus);
            if (executionStatus.noError())
            {
                numOfEmailsSent++;
                instanceContainer.renameInstance(executionStatus, OutgoingEmail.strOutgoingEmailTypeAndName, OutgoingEmail.strOutgoingEmailTypeAndName, "sent" + numOfEmailsSent);
            }
        }
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
