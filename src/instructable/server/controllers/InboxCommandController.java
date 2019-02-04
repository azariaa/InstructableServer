package instructable.server.controllers;

import instructable.server.backend.ExecutionStatus;
import instructable.server.hirarchy.*;
import instructable.server.senseffect.IEmailFetcher;

import java.util.Optional;

/**
 * Created by Amos Azaria on 19-May-15.
 */
public class InboxCommandController
{
    int currentIncomingEmailIdx;
    static public final String emailMessageNameStart = "inbox";
    ConceptContainer conceptContainer;
    InstanceContainer instanceContainer;
    Optional<IEmailFetcher> emailFetcher;

    public InboxCommandController(ConceptContainer conceptContainer, InstanceContainer instanceContainer, Optional<IEmailFetcher> emailFetcher)
    {
        //userId = conceptContainer.getUserId();
        this.conceptContainer = conceptContainer;
        this.instanceContainer = instanceContainer;
        this.emailFetcher = emailFetcher;
        if (emailFetcher.isPresent() && emailFetcher.get().shouldUseLastEmail())
            currentIncomingEmailIdx = emailFetcher.get().getLastEmailIdx(new ExecutionStatus());
        else
            currentIncomingEmailIdx = 0;
        conceptContainer.defineConcept(new ExecutionStatus(), IncomingEmail.incomingEmailType, IncomingEmail.getFieldDescriptions());
    }

    /**
     * for use only for experiments. In real world will use EmailFetcher
     * @param emailMessage
     */
    public void addEmailMessageToInbox(EmailInfo emailMessage)
    {
        new IncomingEmail(instanceContainer, emailMessage, instanceName(inboxSize(new ExecutionStatus())));
    }

    public boolean isInboxInstanceName(String instanceName)
    {
        return instanceName.equals(emailMessageNameStart);
    }


    public String getCurrentEmailName(ExecutionStatus executionStatus)
    {
        if (currentIncomingEmailIdx == 0 && emailFetcher.isPresent() && emailFetcher.get().shouldUseLastEmail())
            setToNewestEmail(new ExecutionStatus());
        makeSureEmailIsPresentInDb(executionStatus);
        return emailMessageNameStart + currentIncomingEmailIdx;
    }

    public Optional<GenericInstance> getCurrentIncomingEmail(ExecutionStatus executionStatus)
    {
        boolean ok = makeSureEmailIsPresentInDb(executionStatus);
        if (ok)
            return instanceContainer.getInstance(executionStatus, IncomingEmail.incomingEmailType, instanceName(currentIncomingEmailIdx));
        executionStatus.add(ExecutionStatus.RetStatus.error, "the email was not found");
        return Optional.empty();
    }

    /**
     *
     * @return true is ok.
     */
    private boolean makeSureEmailIsPresentInDb(ExecutionStatus executionStatus)
    {
        //TODO: what happens if email was deleted? Maybe current email is different?
        if (emailFetcher.isPresent() && !instanceContainer.getInstance(new ExecutionStatus(), IncomingEmail.incomingEmailType, instanceName(currentIncomingEmailIdx)).isPresent())
        {
            Optional<EmailInfo> emailInfo = emailFetcher.get().getEmailInfo(executionStatus, currentIncomingEmailIdx);
            if (emailInfo.isPresent())
            {
                new IncomingEmail(instanceContainer, emailInfo.get(), instanceName(currentIncomingEmailIdx));
                return true;
            }
            return false;
        }
        return true;
    }

    public void setToNextEmail(ExecutionStatus executionStatus)
    {
        int inboxSize = inboxSize(executionStatus);
        if (currentIncomingEmailIdx < inboxSize - 1)
            currentIncomingEmailIdx++;
        else
            executionStatus.add(ExecutionStatus.RetStatus.error, "there are no more emails. You may want to set to a previous email");// You have probably missed some earlier emails. You may want to request a previous email (say \"previous email\")"); //shouldn't be in the production version
    }

    public void setToPrevEmail(ExecutionStatus executionStatus)
    {
        if (currentIncomingEmailIdx > 0)
            currentIncomingEmailIdx--;
        else
            executionStatus.add(ExecutionStatus.RetStatus.error, "there is no previous email");
    }

    /**
     * sets to last email, returns index before setting.
     * @return
     */
    public int setToNewestEmail(ExecutionStatus executionStatus)
    {
        return setToIndex(inboxSize(executionStatus) - 1);
    }

    /**
     * sets to index, returns index before setting.
     * @return
     */
    public int setToIndex(int newIdx)
    {
        int previousIdx = currentIncomingEmailIdx;
        currentIncomingEmailIdx = newIdx;
        return previousIdx;
    }

    private String instanceName(int emailIdx)
    {
        return emailMessageNameStart + emailIdx;
    }

    private int inboxSize(ExecutionStatus executionStatus)
    {
        if (emailFetcher.isPresent())
            return emailFetcher.get().getLastEmailIdx(executionStatus) + 1;
        return instanceContainer.getAllInstances(IncomingEmail.incomingEmailType).size();
    }
    //TODO: delete?
}
