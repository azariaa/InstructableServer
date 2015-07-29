package instructable;

import instructable.server.IEmailSender;
import instructable.server.IIncomingEmailControlling;
import instructable.server.hirarchy.EmailInfo;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Amos Azaria on 02-Jun-15.
 */
public class ExperimentTaskController implements IEmailSender, IAddInboxEmails
{

    //should be written in the expected order of execution
    enum TasksToComplete {
        defineContact,
        addEmailToContact,
        createContact,
        setMomsEmail,
        seeMomsEmail,
        createEmail,
        sendTestEmail,
        readEmailInInbox,
        setRecpToSender,
        sendTestBody,
        nextEmailInInbox,
        previousEmailInInbox,
        teachReadNextInbox, //not relevant when not in learning mode.
        eRepMomShirt,
        eRepBossTask,
        eRepW2,
        eRepW1,
        eRepMomAtWork,
        eRepW3,
        tellBoss,
        eForwardMToBoss,
        eForwardToMom,
        eForwardToW2,
        eForwardToWParty,
        eForwardToWWork,
        eForwardToWRest,
        eForwardWToBoss,
        eRepW3Ignore,
        eRepW1Attention,
        allCompleted
    }
    //static final int numOfTasks = TasksToComplete.values().length;


    static final String bossName = "Alex";
    static final String momName = "Mom"; //"Ash"
    static final String worker1Name = "Casey";
    static final String worker2Name = "Charlie";
    //static final String worker3Name = "Aaron";
    //static final String friendName = "Remy";

    static final String bossEmail = "alextimetowork@myworkplace.com";
    static final String momEmail = "momthebest7@bestforyou.com";//"momthebest7@bestemailsforall.com";
    static final String worker1Email = "caseyousoon8@myworkplace.com";
    static final String worker2Email = "charlieisasleep4@myworkplace.com";
    static final String worker3Email = "aaronworkshard3@myworkplace.com";
    static final String myEmail = "you@myworkplace.com";//TopDMAllActions.userEmailAddress; //in production every user should have a different email

    static final String familyEventDate = "September 28th";

    //or the notes appear bossEmail, momEmail and CharlieEmail only!

    enum LastEmailSent {ok, noTask, oldTask}
    LastEmailSent lastEmailSent = LastEmailSent.ok;
    int unsuccessfulCount = 0;

    LinkedHashSet<TasksToComplete> userTasks = new LinkedHashSet<>();
    String gameId;
    Logger logger;

    public ExperimentTaskController(Logger logger, String gameId)
    {
        this.gameId = gameId;
        this.logger = logger;
    }

    //returns the most recent task completed by the user
    //the order in the LinkedHashSet is the order which was entered
    public String getRecentTaskName()
    {
        switch (lastEmailSent)
        {
            case ok:
            {
                if (userTasks.size() > 0)
                    return userTasks.toArray()[userTasks.size() - 1].toString();
                return "none";
            }
            case noTask:
                return "unsuccessfulSend" + unsuccessfulCount;
            case oldTask:
                return "oldTask" + unsuccessfulCount;
        }
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
            case defineContact:
                return "Your 1st training task is to have the agent define/create the concept \"contact\" (simply say: define the concept contact)";
            case addEmailToContact:
                return "Your 2nd training task is to have the agent add the \"email\" field to the concept \"contact\" (simply say: add email field to concept contact)";
            case createContact:
                return "Your 3rd training task is to <b>create</b> a contact for "+ momName +" (simply tell the agent to create a contact for mom)";
            case setMomsEmail:
                return "Your 4th training task is to <b>set</b> mom's email to correctly. (Set "+momName+"'s email to the email that appears in the \"notes\" image. Make sure to type it in correctly!)";
            case seeMomsEmail:
                return "Your 5th training task is to ask the agent for "+momName+"'s email";
            case createEmail:
                return "Your 6th training task is to create a new outgoing email";
            case sendTestEmail:
                return "Your 7th training task is to set the recipient to "+ momName +"'s email and set the subject to hello and send the email (instead of retyping mom's email, you can say: \"set the recipient to " + momName +"'s email\")";
            case readEmailInInbox:
                return "Your 8th training task is to read the current email (in the inbox). Ignore the email's content for now (don't read the next email yet, you'll do it later).";
            case setRecpToSender:
                return "Your 9th training task is to create a new email and set the recipient to the current email's sender (don't type the email yourself, use: \"current email's sender\" instead)";
            case sendTestBody:
                return "Your 10th training task is to set the body to the current email's body (don't type in the body yourself, just use the current email's body), and send the email";
            case nextEmailInInbox:
                return "Your 11th training task is to move to the <b>next</b> email (in the inbox). (No need to read it now.)";
            case previousEmailInInbox:
                return "Your 12th training task is to move to the <b>previous</b> email (in the inbox). (No need to read it now.)";
            case teachReadNextInbox:
                return "Your 13th (and last) training task is to teach the agent a <b>new command</b> (e.g. \"go\"): having it both moving to the next email and reading it (say: \"go\", then answer \"yes\", give the proper commands, then say \"end\")";
            case allCompleted:
                //this shouldn't actually ever happen
                return "Congratulations: You have completed all possible tasks!";
            default:
            {
                switch (lastEmailSent)
                {
                    case ok:
                        return "Main Task: read each of the incoming emails (next email, etc.). For each email do as it says (follow the sender's request). (Remember that teaching new commands may save you time!)";
                    case noTask:
                        return "Sent email did not complete any task. Check email's subject, body, and recipient address.";
                    case oldTask:
                        return "Sent email completed a task which you have already completed in the past. Move to the next email.";
                }
            }
        }
        return "Error.";
    }

    public void responseSentToUser(String agentResponse)
    {
        if (agentResponse.contains("Composing new email") && !userTasks.contains(TasksToComplete.createEmail))
            userTasks.add(TasksToComplete.createEmail);
        else if (agentResponse.contains("Concept \"contact\" was defined successfully"))
            userTasks.add(TasksToComplete.defineContact);
        else if (agentResponse.contains("Field \"email\" was added to concept \"contact\""))
            userTasks.add(TasksToComplete.addEmailToContact);
        else if (agentResponse.contains("Instance \"mom\" (of concept \"contact\") was created") && !userTasks.contains(TasksToComplete.createContact))
            userTasks.add(TasksToComplete.createContact);
        else if (agentResponse.contains("\"email\"") && agentResponse.contains(momEmail))//\"abbie\" was set to:"))//("Instance \"abbie\" (of concept \"contact\") was created."))
            userTasks.add(TasksToComplete.setMomsEmail);
        else if (agentResponse.contains("It is: "+momEmail))
            userTasks.add(TasksToComplete.seeMomsEmail);
        else if (agentResponse.contains("subject:") && agentResponse.contains("sender:") && !userTasks.contains(TasksToComplete.readEmailInInbox))
            userTasks.add(TasksToComplete.readEmailInInbox);
        else if (agentResponse.contains("recipient") && agentResponse.contains(worker1Email))
            userTasks.add(TasksToComplete.setRecpToSender);
        else if (agentResponse.contains("Set to next incoming email successfully"))
            userTasks.add(TasksToComplete.nextEmailInInbox);
        else if (agentResponse.contains("Set to previous incoming email successfully"))
            userTasks.add(TasksToComplete.previousEmailInInbox);
        else if (agentResponse.contains("I now know what to do when you say (for example): "))
        {
            //make sure that it didn't teach something (probably not relevant) earlier.
            if (userTasks.contains(TasksToComplete.nextEmailInInbox) && userTasks.contains(TasksToComplete.readEmailInInbox) && userTasks.contains(TasksToComplete.previousEmailInInbox))
                userTasks.add(TasksToComplete.teachReadNextInbox);
        }
        else if (agentResponse.contains("amos password is this"))
        {
            userTasks.addAll(Arrays.asList(TasksToComplete.class.getEnumConstants()));
        }
    }

    //should actually be named "emailSent"
    @Override
    public void sendEmail(String subject1, String body1, String copyList, String recipientList)
    {
        String subject = subject1.toLowerCase();
        String body = body1.toLowerCase();

        Set<TasksToComplete> completedInThisEmail = new HashSet<>();
        //may want to actually send the email in real environment right here.
        if (subject.contains("hello") && recipientList.contains(momEmail) && !userTasks.contains(TasksToComplete.sendTestEmail))
            completedInThisEmail.add(TasksToComplete.sendTestEmail);
        if ((body.contains("feeling well today") || body.contains("felt like")) && !userTasks.contains(TasksToComplete.sendTestBody))
            completedInThisEmail.add(TasksToComplete.sendTestBody);
        if (subject.contains("shirt color") && recipientList.contains(momEmail) && !body.isEmpty())
            completedInThisEmail.add(TasksToComplete.eRepMomShirt);
        if (subject.contains("task")  && subject.contains("asked") && recipientList.contains(bossEmail) && !body.isEmpty())
            completedInThisEmail.add(TasksToComplete.eRepBossTask);
        if (subject.contains("working tomorrow") && recipientList.contains(worker2Email) && !body.isEmpty())
            completedInThisEmail.add(TasksToComplete.eRepW2);
        if (subject.contains("what to do") && recipientList.contains(worker1Email) && !body.isEmpty())
            completedInThisEmail.add(TasksToComplete.eRepW1);
        if (subject.contains("are you still") && !body.isEmpty() && recipientList.contains(momEmail))
            completedInThisEmail.add(TasksToComplete.eRepMomAtWork);
        if (subject.contains("work") && recipientList.contains(worker3Email) && !body.isEmpty())
            completedInThisEmail.add(TasksToComplete.eRepW3);
        if (recipientList.contains(bossEmail) && (body.contains(" way") || body.contains(" late")|| subject.contains(" way") || subject.contains(" late") || subject.contains(worker2Name.toLowerCase()) || body.contains(worker2Name.toLowerCase()) || body.contains("coming")|| subject.contains("coming")))
            completedInThisEmail.add(TasksToComplete.tellBoss);
        if (subject.contains("family event") && recipientList.contains(bossEmail) && body.contains("vacation"))
            completedInThisEmail.add(TasksToComplete.eForwardMToBoss);
        if (subject.contains("vacation") && recipientList.contains(momEmail) && body.contains("vacation"))
            completedInThisEmail.add(TasksToComplete.eForwardToMom);
        if (subject.contains(worker2Name.toLowerCase()) && recipientList.contains(worker2Email) && body.contains("do what"))
            completedInThisEmail.add(TasksToComplete.eForwardToW2);
        if (subject.contains("party time") && body.contains("thursday") && recipientList.contains(worker2Email))
            completedInThisEmail.add(TasksToComplete.eForwardToWParty);
        if (subject.contains("work before parting") && body.contains("monday") && recipientList.contains(worker2Email))
            completedInThisEmail.add(TasksToComplete.eForwardToWWork);
        if (subject.contains("rest") && body.contains("well") && recipientList.contains(worker2Email))
            completedInThisEmail.add(TasksToComplete.eForwardToWRest);
        if (subject.contains("cannot attend") && body.contains("sorry") && recipientList.contains(bossEmail))
            completedInThisEmail.add(TasksToComplete.eForwardWToBoss);
        if (subject.contains("everyone ignores me") && !body.isEmpty() && recipientList.contains(worker3Email))
            completedInThisEmail.add(TasksToComplete.eRepW3Ignore);
        if (subject.contains("i like attention") && !body.isEmpty() && recipientList.contains(worker1Email))
            completedInThisEmail.add(TasksToComplete.eRepW1Attention);


        if (completedInThisEmail.isEmpty())
        {
            lastEmailSent = LastEmailSent.noTask;
            unsuccessfulCount++;
        }
        else
        {
            if (userTasks.containsAll(completedInThisEmail))
            {
                lastEmailSent = LastEmailSent.oldTask;
                unsuccessfulCount++;
            }
            else
            {
                lastEmailSent = LastEmailSent.ok;
                userTasks.addAll(completedInThisEmail);
            }
        }

    }

    @Override
    public void addInboxEmails(IIncomingEmailControlling incomingEmailControlling)
    {
        //no mission
        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker1Email,
                "Hi there",
                Arrays.asList(myEmail),
                new LinkedList<String>(),
                "I'm feeling well today. I hope I will also feel well tomorrow and anytime! Please ignore this email and read the next one."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker1Email,
                "Another email",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "I felt like sending you another email. I hope that you don't mind. Please ignore this email too and read the next one."
        ));



        //replying

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(momEmail,
                "Shirt color",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "I need to know your favorite color for a shirt. Please reply as soon as possible (make sure to use the same subject, as you should always do when replying to emails :)."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(bossEmail,
                "Task I asked",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "Are you working on the task that I asked you to work on? Please reply immediately ."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker2Email,
                "Working tomorrow",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "I don't feel like working tomorrow, do I have to? Please reply as soon as possible."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker1Email,
                "What to do?",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "I'm done with all my tasks, what should I do next? Please reply as soon as possible."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(momEmail,
                "Are you still at work?",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "I must know if you are still at work. Please reply as soon as possible."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker3Email,
                "Do you like work?",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "I like my job. Please reply and let me know what you think, as soon as possible."
        ));

        //plain send
        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker2Email,
                "Tell Alex that I'm on my way",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "Please email Alex saying that I'm on my way. " + worker2Name
        ));


        //forwarding
        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(momEmail,
                "Family event",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "You must ask your boss to approve your vacation for the family event on "+familyEventDate+". Forward this email to your boss (make sure to use the same subject, and include the whole body, as you should always do when forwarding an email :)."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(bossEmail,
                "Your vacation",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "Your vacation has been approved. Please forward this email to your mom."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker1Email,
                worker2Name,
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "I asked "+worker2Name+" to do what you said, but I see that it must come from you. Please forward this email to "+worker2Name + "."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(bossEmail,
                "Party time!",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "We will have a party next Thursday at 4:00pm. Please forward this email to " + worker2Name + "."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(bossEmail,
                "Work before parting",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "We will all have to work very hard next Monday, Tuesday and Wednesday. Please forward this email to " + worker2Name + "."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(bossEmail,
                "Rest during the weekend",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "Don't forget to rest well on the weekend so you can work well on Monday, Tuesday and Wednesday. Please forward this email to " + worker2Name + "."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker2Email,
                "Cannot attend the party",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "I am sorry, but I can't attend next week's party. Please forward this email to " + bossName + "."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker3Email,
                "Everyone ignores me",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "Thank you so much for not ignoring this email and reading it. Please reply and tell me that you saw it."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker1Email,
                "I like attention",
                Arrays.asList(myEmail),
                new LinkedList<>(),
                "I'm happy that you are reading my email. Please reply and tell me that you saw it."
        ));


        //tell someone something???

    }

}
