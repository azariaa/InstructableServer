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
    IEmailSender emailSender;

    OutEmailCommandController(String myEmail, ConceptContainer conceptContainer, InstanceContainer instanceContainer, IEmailSender emailSender)
    {
        this.myEmail = myEmail;
        conceptContainer.defineConcept(new ExecutionStatus(), OutgoingEmail.strOutgoingEmailTypeAndName, OutgoingEmail.getFieldDescriptions());
        this.instanceContainer = instanceContainer;
        this.emailSender = emailSender;
    }

    public boolean isAnEmailBeingComposed()
    {
        return getEmailBeingComposed(new ExecutionStatus()).isPresent();
    }

    public Optional<OutgoingEmail> getEmailBeingComposed(ExecutionStatus executionStatus)
    {
        Optional<GenericInstance> emailBeingComposed = instanceContainer.getInstance(executionStatus, OutgoingEmail.strOutgoingEmailTypeAndName, OutgoingEmail.strOutgoingEmailTypeAndName);
        if (emailBeingComposed.isPresent())
        {
            return Optional.of(new OutgoingEmail(emailBeingComposed.get()));
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
            email.get().checkSendingPrerequisites(executionStatus, true);
            if (executionStatus.noError())
            {
                sendThisEmail(executionStatus, email.get());
                if (executionStatus.noError())
                {
                    numOfEmailsSent++;
                    instanceContainer.setMutability(executionStatus, OutgoingEmail.strOutgoingEmailTypeAndName, OutgoingEmail.strOutgoingEmailTypeAndName, false);
                    instanceContainer.renameInstance(executionStatus, OutgoingEmail.strOutgoingEmailTypeAndName, OutgoingEmail.strOutgoingEmailTypeAndName, "sent" + numOfEmailsSent);
                }
            }
        }
    }

    private void sendThisEmail(ExecutionStatus executionStatus, OutgoingEmail outgoingEmail)
    {
        emailSender.sendEmail(outgoingEmail.getSubject(),outgoingEmail.getBody(),outgoingEmail.getCopy(),outgoingEmail.getRecipient());
    }

    public void createNewEmail(ExecutionStatus executionStatus)
    {
        //first delete old one if exists.
        Optional<GenericInstance> emailBeingComposed = instanceContainer.getInstance(new ExecutionStatus(), OutgoingEmail.strOutgoingEmailTypeAndName, OutgoingEmail.strOutgoingEmailTypeAndName);
        if (emailBeingComposed.isPresent())
        {
            instanceContainer.deleteInstance(new ExecutionStatus(), emailBeingComposed.get());
        }

        //now create a new one
        new OutgoingEmail(executionStatus, instanceContainer, myEmail);
    }

    public List<String> changeToRelevantComposedEmailFields(List<String> allEmailFields)
    {
        allEmailFields.remove(EmailMessage.senderStr);
        return allEmailFields;
    }
}
