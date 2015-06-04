package instructable;

import instructable.server.IEmailSender;
import instructable.server.IIncomingEmailControlling;
import instructable.server.hirarchy.IncomingEmail;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * Created by Amos Azaria on 02-Jun-15.
 */
public class ExperimentTaskController implements IEmailSender, IAddInboxEmails
{

    //should be written in the expected order of execution
    enum TasksToComplete {
        createEmail,
        sendTestEmail,
        defineContact,
        addEmailToContact,
        createContact,
        readEmailInInbox,
        nextEmailInInbox,
        previousEmailInInbox,
        teachReadNextInbox, //not relevant when not in learning mode.
        eAbbieReply,
        eSpouseReply,
        eForwardToSpouse,
        eForwardToAllWorkers,
        allCompleted
    }
    //static final int numOfTasks = TasksToComplete.values().length;

    boolean unsuccessfulSend = false;
    int unsuccessfulCount = 0;

    LinkedHashSet<TasksToComplete> userTasks = new LinkedHashSet<>();
    String gameId;
    Logger logger;

    ExperimentTaskController(Logger logger, String gameId)
    {
        this.gameId = gameId;
        this.logger = logger;
    }

    //returns the most recent task completed by the user
    //the order in the LinkedHashSet is the order which was entered
    public String getRecentTaskName()
    {
        if (unsuccessfulSend)
            return "unsuccessfulSend" + unsuccessfulCount;
        if (userTasks.size() > 0)
            return userTasks.toArray()[userTasks.size()-1].toString();
        return "none";
    }

    private TasksToComplete currentTask()
    {
        //order of values in enum is guaranteed
        for (TasksToComplete tasksToComplete : TasksToComplete.values())
        {
            if (!userTasks.contains(tasksToComplete))
                return tasksToComplete;
        }
        logger.info("user completed all tasks (is this ok?)");
        return TasksToComplete.allCompleted;
    }

    public int getGameScore()
    {
        return userTasks.size();
        //return (int)userScores.values().stream().filter(x -> x==true).count(); //should work, but need to test it first...
    }

    public String getCurrentTaskText()
    {
        switch (currentTask())
        {
            case createEmail:
                return "Training Task 1: creating a new outgoing email";
            case sendTestEmail:
                return "Training Task 2: sending an outgoing email to Abbie with hello as the body and no subject. Abbie's email appears in the \"notes\" image.";
            case defineContact:
                return "Training Task 3: defining the concept contact";
            case addEmailToContact:
                return "Training Task 4: adding the email field to the concept contact";
            case createContact:
                return "Training Task 5: creating a contact for Abbie and adding her email address to it";
            case readEmailInInbox:
                return "Training Task 6: requesting the agent to read the current email in the inbox";
            case nextEmailInInbox:
                return "Training Task 7: requesting the agent to move to the <b>next</b> email in the inbox";
            case previousEmailInInbox:
                return "Training Task 8: requesting the agent to move to the <b>previous</b> email in the inbox";
            case teachReadNextInbox:
                return "Training Task 9: teaching the agent a <b>new command</b>: having it <b>read</b> the <b>next</b> email";
            case allCompleted:
                //this shouldn't actually ever happen
                return "Congratulations: You have completed all possible tasks!";
            default:
            {
                if (unsuccessfulSend)
                    return "Previously sent email did not complete any task. Check email's subject, body, and recipient address.";
                return "Main Task: reading through all incoming emails and acting accordingly";
            }
        }
    }

    public void responseSentToUser(String agentResponse)
    {
        if (agentResponse.contains("Composing new email"))
            userTasks.add(TasksToComplete.createEmail);
        //else if (agentResponse.contains("Email sent successfully")) //this is done below in the emailSent function.
            //userTasks.add(TasksToComplete.sendTestEmail);
        else if (agentResponse.contains("Concept \"contact\" was defined successfully"))
            userTasks.add(TasksToComplete.defineContact);
        else if (agentResponse.contains("Field \"email\" was added to concept \"contact\""))
            userTasks.add(TasksToComplete.addEmailToContact);
        else if (agentResponse.contains("The \"email\" field in \"abbie\" was set to:"))//("Instance \"abbie\" (of concept \"contact\") was created."))
            userTasks.add(TasksToComplete.createContact);
        else if (agentResponse.contains("subject:") && agentResponse.contains("sender:"))
            userTasks.add(TasksToComplete.readEmailInInbox);
        else if (agentResponse.contains("Set to next incoming email successfully"))
            userTasks.add(TasksToComplete.nextEmailInInbox);
        else if (agentResponse.contains("Set to previous incoming email successfully"))
            userTasks.add(TasksToComplete.previousEmailInInbox);
        else if (agentResponse.contains("I now know what to do when you say (for example): ")) //might want to check in between, that actually taught correct task.
            userTasks.add(TasksToComplete.teachReadNextInbox);
    }

    //should actually be named "emailSent"
    @Override
    public void sendEmail(String subject1, String body1, String copyList, String recipientList)
    {
        String subject = subject1.toLowerCase();
        String body = body1.toLowerCase();
        unsuccessfulSend = false;
        //may want to actually send the email in real environment right here.
        if (body.contains("hello"))// && recipientList.contains("abemail7@bestemails.com"))
            userTasks.add(TasksToComplete.sendTestEmail);
        else if (subject.contains("working hours") && recipientList.contains("abemail7@myworkplace.com") && !body.isEmpty())
            userTasks.add(TasksToComplete.eAbbieReply);
        else if (subject.contains("are you") && !body.isEmpty() && recipientList.contains("caseyousoon@bestemails.com"))
            userTasks.add(TasksToComplete.eSpouseReply);
        else if (subject.contains("your vacation") && recipientList.contains("caseyousoon@bestemails.com") && body.contains("approved"))
            userTasks.add(TasksToComplete.eForwardToSpouse);
        else if (subject.contains("department party") && body.contains("department party") && body.contains("wednesday") &&
                recipientList.contains("bobtheman4@myworkplace.com") && recipientList.contains("annthebest3@myworkplace.com") && recipientList.contains("samlikestodrum@myworkplace.com"))
            userTasks.add(TasksToComplete.eForwardToAllWorkers);
        else
        {
            unsuccessfulSend = true;
            unsuccessfulCount++;
        }

    }

    @Override
    public void addInboxEmails(IIncomingEmailControlling incomingEmailControlling)
    {
        incomingEmailControlling.addEmailMessageToInbox(new IncomingEmail("manor73@myworkplace.com",
                "Hi there",
                Arrays.asList(new String[]{"you@myjob.com"}),
                new LinkedList<String>(),
                "I'm feeling well today."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new IncomingEmail("manor73@myworkplace.com",
                "Another email",
                Arrays.asList(new String[]{"you@myworkplace.com"}),
                new LinkedList<String>(),
                "I felt like sending you another email."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new IncomingEmail("abemail7@myworkplace.com",
                "Working hours",
                Arrays.asList(new String[]{"you@myworkplace.com"}),
                new LinkedList<String>(),
                "I need to know if you will be working next week. Please reply immediately (make sure to use the same subject, as you should always do when replying to emails :)."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new IncomingEmail("caseyousoon@bestemails.com",
                "Are you ok?",
                Arrays.asList(new String[]{"you@myworkplace.com"}),
                new LinkedList<String>(),
                "I didn't hear from you in a while, is everything ok? Please reply as soon as possible."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new IncomingEmail("bob7@bestemails.com",
                "Your vacation",
                Arrays.asList(new String[]{"you@myworkplace.com"}),
                new LinkedList<String>(),
                "Your vacation has been approved. Please forward this email to your spouse (make sure to use the same subject, and include the whole body, as you should always do when forwarding an email :)."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new IncomingEmail("abemail7@myworkplace.com",
                "department party",
                Arrays.asList(new String[]{"you@myworkplace.com"}),
                new LinkedList<String>(),
                "We will have our department party next Wednesday at 4:00pm. Please forward this email to all people who report to you (make sure that they all appear together as recipients in the email)."
        ));

    }

}
