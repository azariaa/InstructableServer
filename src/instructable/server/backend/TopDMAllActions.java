package instructable.server.backend;

import com.google.common.base.Preconditions;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.Consts;
import instructable.server.controllers.CalendarEventController;
import instructable.server.controllers.InboxCommandController;
import instructable.server.controllers.OutEmailCommandController;
import instructable.server.dal.DBUtils;
import instructable.server.hirarchy.*;
import instructable.server.hirarchy.fieldTypes.DateType;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;
import instructable.server.parser.ICommandsToParser;
import instructable.server.senseffect.ICalendarAccessor;
import instructable.server.senseffect.IEmailFetcher;
import instructable.server.senseffect.IEmailSender;
import instructable.server.senseffect.IIncomingEmailControlling;
import instructable.server.utils.InstUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static instructable.server.backend.TextFormattingUtils.noEmailFound;
import static instructable.server.backend.TextFormattingUtils.userFriendlyList;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class TopDMAllActions implements IAllUserActions, IIncomingEmailControlling
{
    ConceptContainer conceptContainer;
    InstanceContainer instanceContainer;
    ICommandsToParser commandsToParser;
    OutEmailCommandController outEmailCommandController;
    CalendarEventController calendarEventController;
    public InboxCommandController inboxCommandController;
    CommandHistory commandHistory;
    Callable<Void> clearUserDBAndReset;

    static private final String ambiguousEmailInstanceName = "email"; //can either be outgoing email, or inbox
    static private final String yesExpression = "(yes)";
    static private final String createEmailExpression = "(createInstanceByConceptName outgoing_email)";
    static private final Function<String, String> runScriptExpression = (scriptName) -> "(runScript \"" + scriptName + "\")";

    String userEmailAddress;
    Optional<JSONObject> previousFieldEval = Optional.empty();
    boolean usePendingResponses = true;

    public TopDMAllActions(String userEmailAddress, String userId, ICommandsToParser commandsToParser, IEmailSender emailSender, boolean usePendingResponses,
                           Optional<IEmailFetcher> emailFetcher,
                           Optional<ICalendarAccessor> calendarAccessor)
    {
        commandHistory = new CommandHistory();
        this.userEmailAddress = userEmailAddress;
        this.usePendingResponses = usePendingResponses;
        reset(userEmailAddress, userId, emailSender, emailFetcher, calendarAccessor);
        this.commandsToParser = commandsToParser;
        internalState = new InternalState();
        commandHistory.startRecording();
        clearUserDBAndReset = () -> {
            DBUtils.clearUserData(userId);
            reset(userEmailAddress, userId, emailSender, emailFetcher, calendarAccessor);
            return null;
        };
    }

    private void reset(String userEmailAddress, String userId, IEmailSender emailSender, Optional<IEmailFetcher> emailFetcher, Optional<ICalendarAccessor> calendarAccessor)
    {
        conceptContainer = new ConceptContainer(userId);
        instanceContainer = new InstanceContainer(conceptContainer, userId);
        // the following command will also define the concept outgoing_email, however, the previous command may already try fetching the email being composed. This used to raise an exception.
        outEmailCommandController = new OutEmailCommandController(userEmailAddress, conceptContainer, instanceContainer, emailSender);
        inboxCommandController = new InboxCommandController(conceptContainer, instanceContainer, emailFetcher);
        calendarEventController = new CalendarEventController(conceptContainer, instanceContainer, calendarAccessor);
    }


    @Override
    public void addEmailMessageToInbox(EmailInfo emailMessage)
    {
        inboxCommandController.addEmailMessageToInbox(emailMessage);
    }

    //TODO: may want to allow to pend on function delegates.
    /*
    this class is in-charge of tracking the user's sentences especially for learning.
     */
    public static class InternalState
    {
        private static final int aLotOfExpressions = 7;

        private static enum InternalLearningStateMode

        {
            none, learnNext, learning
        }

        static enum PendingOn

        {
            none, pendingOnEmailCreation, pendingOnForgetEverything, pendingOnLearning
        }

        private InternalLearningStateMode internalLearningStateMode;
        private PendingOn pendingOn;
        List<Expression2> expressionsBeingLearned = new LinkedList<>();
        int failCount = 0;
        String lastCommandOrLearningCommand = "";
        int lastInfoForCommandHashCode;

        public PendingOn getPendingOn()
        {
            return pendingOn;
        }

        public boolean isInLearningMode()
        {
            return internalLearningStateMode == InternalLearningStateMode.learning;
        }

        public void pendOnEmailCreation()
        {
            pendingOn = PendingOn.pendingOnEmailCreation;
        }

        public void pendOnForgetEverything()
        {
            pendingOn = PendingOn.pendingOnForgetEverything;
        }

        public void pendOnLearning()
        {
            pendingOn = PendingOn.pendingOnLearning;
        }

        public void learnNextCommand()
        {
            internalLearningStateMode = InternalLearningStateMode.learnNext;
        }

        public boolean shouldLearnNext()
        {
            return internalLearningStateMode == InternalLearningStateMode.learnNext;
        }

        private void reset()
        {
            internalLearningStateMode = InternalLearningStateMode.none;
            pendingOn = PendingOn.none;
            expressionsBeingLearned = new LinkedList<>();
            failCount = 0;
        }

        public String startLearning()
        {
            internalLearningStateMode = InternalLearningStateMode.learning;
            return lastCommandOrLearningCommand;
        }

        public void userGaveCommand(InfoForCommand infoForCommand, boolean success, boolean isExecutingUndoNow, boolean preservePending)
        {
            if (!preservePending)
                pendingOn = PendingOn.none;
            if (internalLearningStateMode == InternalLearningStateMode.learning)
            {
                //this saves the infoForCommand hashcode so it uses each expression only once.
                //TODO: not the best way to do this, since some actions in the same expression may succeed and some not. also what if the user end the last command with "and that's it"?
                if (success)
                {
                    if ((infoForCommand.hashCode() != lastInfoForCommandHashCode)
                            && !isExecutingUndoNow) //make sure we don't add the "undo" command
                    {
                        lastInfoForCommandHashCode = infoForCommand.hashCode();
                        expressionsBeingLearned.add(infoForCommand.expression);
                    }
                }
                else //if failed need to remove current expression from list of expressions
                {
                    if (infoForCommand.hashCode() == lastInfoForCommandHashCode)
                    {
                        lastInfoForCommandHashCode = 0;
                        expressionsBeingLearned.remove(expressionsBeingLearned.size() - 1); //remove last
                    }
                    failCount++;
                }
            }
            else
            {
                internalLearningStateMode = InternalLearningStateMode.none;
                lastCommandOrLearningCommand = infoForCommand.userSentence;
            }
        }

        public void removeLastCommandIfLearning()
        {
            if (internalLearningStateMode == InternalLearningStateMode.learning && !expressionsBeingLearned.isEmpty())
            {
                expressionsBeingLearned.remove(expressionsBeingLearned.size() - 1);
            }
        }

        public List<Expression2> endLearningGetSentences()
        {
            internalLearningStateMode = InternalLearningStateMode.none;
            List<Expression2> userSentences = expressionsBeingLearned;
            expressionsBeingLearned = new LinkedList<>();
            failCount = 0;
            return userSentences;
        }

        public boolean learnedSomething()
        {
            return expressionsBeingLearned.size() > 0;
        }

        public boolean shouldFailLearning()
        {
            if (failCount >= 3 && expressionsBeingLearned.size() == 0)
            {
                reset();
                return true;
            }
            return false;
        }

        public boolean userHavingTrouble()
        {
            if (failCount >= 4 || (failCount >= 2 && expressionsBeingLearned.size() < failCount - 1))
                return true;
            return false;
        }

        public boolean isLearningForTooLong()
        {
            return expressionsBeingLearned.size() + failCount >= aLotOfExpressions;
        }

    }

    InternalState internalState;

    @Override
    public ActionResponse sendEmail(InfoForCommand infoForCommand)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();

        outEmailCommandController.sendEmail(executionStatus);
        if (executionStatus.getStatus() == ExecutionStatus.RetStatus.noPswdSet)
        {
            return resetEmailAndPassword(infoForCommand);
        }
        if (executionStatus.isError())
        {
            if (!outEmailCommandController.isAnEmailBeingComposed())
            {
                return TextFormattingUtils.noEmailFound(internalState);
            }
        }

        return testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("Email sent successfully."),
                Optional.of(() -> createNewEmailOrRestore(infoForCommand, true, false)));

//        //this will also mark as sent
//        Optional<OutgoingEmail> emailToSend = outEmailCommandController.getEmailToSend(executionStatus);
//        List<ActionResponse> listOfActions = new LinkedList<>();
//        if (executionStatus.isError() || !emailToSend.isPresent())
//        {
//            if (!outEmailCommandController.isAnEmailBeingComposed())
//            {
//                internalState.userGaveCommand(infoForCommand, false, commandHistory.isExecutingAnUndoNow());
//                return TextFormattingUtils.noEmailFound(internalState);
//            }
//        }
//        else
//        {
//            listOfActions.add(sugExecClick(infoForCommand, "GMail"));
//            //TODO: may need to click on the back button (or "Open navigation drawer" button)
//            listOfActions.add(sugExecClick(infoForCommand, "Compose"));
//            if (emailToSend.get().hasSubject())
//                listOfActions.add(sugExecSetText(infoForCommand, "Subject", emailToSend.get().getSubject()));
//            if (emailToSend.get().hasBody())
//                listOfActions.add(sugExecSetText(infoForCommand, "Compose email", emailToSend.get().getBody()));
//            listOfActions.add(sugExec(infoForCommand, "SET_TEXT", "0 0 964 196"/*"To"*/, "parent", emailToSend.get().getRecipient()));
//            //listOfActions.add(sugExec(infoForCommand, "SET_TEXT", "0 0 964 196"/*"To"*/, "parent", "amos.azaria@gmail.co;fdsa.dsfa@fdsa.com"));
//            listOfActions.add(sugExecClick(infoForCommand, "Send"));
//        }
//        listOfActions.add(
//                testOkAndFormat(infoForCommand,
//                executionStatus,
//                false,
//                true,
//                Optional.of("Now sending Email!"), //TODO: should really see if managed to send email. "Email sent successfully."),
//                false,//true,
//                Optional.of(() -> createNewEmailOrRestore(infoForCommand, true, false)))
//        );
//        return ActionResponse.createFromList(listOfActions);
    }

    @Override
    public ActionResponse send(InfoForCommand infoForCommand, String instanceName)
    {
        //instanceName can actually also be a conceptName (probably outgoing_email), for example when saying: "send an email", or "send one email"
        if (instanceName.equals(ambiguousEmailInstanceName) || instanceName.equals(OutgoingEmail.strOutgoingEmailTypeAndName))
        {
            return sendEmail(infoForCommand);
        }
        return failWithMessage(infoForCommand, "I don't know how to send " + instanceName);
    }

//    @Override
//    public ActionResponse reset(InfoForCommand infoForCommand, String instanceName)
//    {
//        //instanceName can actually also be a conceptName (probably outgoing_email), for example when saying: "send an email", or "send one email"
//        if (instanceName.equals(ambiguousEmailInstanceName) || instanceName.equals(OutgoingEmail.strOutgoingEmailTypeAndName))
//        {
//            return resetEmailAndPassword(infoForCommand);
//        }
//        return failWithMessage(infoForCommand, "I don't know how to reset " + instanceName);
//    }

    @Override
    public ActionResponse resetEmailAndPassword(InfoForCommand infoForCommand)
    {
        return ActionResponse.reqEmailAndPswd();
    }

    @Override
    public ActionResponse saveCalendarEvent(InfoForCommand infoForCommand)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        calendarEventController.saveEvent(executionStatus);
        //TODO:
//        if (executionStatus.isError())
//        {
//            if (!outEmailCommandController.isAnEmailBeingComposed())
//            {
//                return TextFormattingUtils.noEmailFound(internalState);
//            }
//        }

        return testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("Event saved to calendar successfully."),
                Optional.of(() -> createNewEventOrRestore(infoForCommand, true, false)));
    }

    @Override
    public ActionResponse save(InfoForCommand infoForCommand, String instanceName)
    {
        //instanceName can actually also be a conceptName (probably outgoing_email), for example when saying: "send an email", or "send one email"
        if (instanceName.equals(CalendarEvent.strCalendarEventTypeAndName))
        {
            return saveCalendarEvent(infoForCommand);
        }
        return failWithMessage(infoForCommand, "I don't know how to save " + instanceName);
    }

    public ActionResponse yes(InfoForCommand infoForCommand)
    {
        switch (internalState.pendingOn)
        {
            case none:
                return failWithMessage(infoForCommand, "I did not understand what you said yes to, please give the full request");
            case pendingOnEmailCreation:
                //the following code replaces the "yes" command with the "create email" command for learning purposes
                //not elegant at all...
                if (internalState.isInLearningMode() && infoForCommand.expression.toString().equals(yesExpression))
                {
                    infoForCommand.expression = ExpressionParser.expression2().parseSingleExpression(createEmailExpression);
                }
                return createNewEmailOrRestore(infoForCommand, false, true);//createInstanceByConceptName(infoForCommand, OutgoingEmail.strOutgoingEmailTypeAndName);//identical
            case pendingOnForgetEverything:
                return actuallyForgetEverything(infoForCommand);
            case pendingOnLearning:
                commandHistory.push(infoForCommand, () -> cancel(infoForCommand));
                String lastCommand = internalState.startLearning();
                return new ActionResponse("Great! When you say, for example: \"" + lastCommand + "\", what shall I do first? (Please give me a command)", //(Either tell me what to do or say demonstrate to demonstrate)",
                        true, Optional.empty());
        }
        return new ActionResponse("That should never happen!", false, Optional.empty());
    }

    @Override
    public ActionResponse no(InfoForCommand infoForCommand)
    {
        internalState.reset();
        return new ActionResponse("Ok, I won't do anything.", true, Optional.empty());
    }

    /*
        stop learning.
     */
    @Override
    public ActionResponse cancel(InfoForCommand infoForCommand)
    {
        if (internalState.isInLearningMode() || internalState.shouldLearnNext())
        {
            internalState.reset();
            return new ActionResponse("Ok, I won't learn it.", true, Optional.empty());
        }
        return new ActionResponse("There is nothing that I can cancel.", true, Optional.empty());
    }

    @Override
    public ActionResponse getInstance(InfoForCommand infoForCommand, String conceptName, String instanceName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        //instanceName = AliasMapping.instanceNameMapping(instanceName);
        if (inboxCommandController.isInboxInstanceName(instanceName))
        {
            conceptName = IncomingEmail.incomingEmailType; //make sure is asking for the right concept
            instanceName = inboxCommandController.getCurrentEmailName(executionStatus);
            if (executionStatus.getStatus() == ExecutionStatus.RetStatus.noPswdSet)
            {
                return resetEmailAndPassword(infoForCommand);
            }
        }

        Optional<GenericInstance> instance = instanceContainer.getInstance(executionStatus, conceptName, instanceName);
        if (!instance.isPresent())
        {
            if (conceptName.equals(IncomingEmail.incomingEmailType)) //this might happen if for some reason didn't use current inbox message
            {
                instanceName = inboxCommandController.getCurrentEmailName(executionStatus);
                if (executionStatus.getStatus() == ExecutionStatus.RetStatus.noPswdSet)
                {
                    return resetEmailAndPassword(infoForCommand);
                }
                instance = instanceContainer.getInstance(executionStatus, IncomingEmail.incomingEmailType, OutgoingEmail.strOutgoingEmailTypeAndName);
            }
            if (instanceName.contains(InboxCommandController.emailMessageNameStart)) //this might happen if concept mismatches the instanceName
            {
                instance = instanceContainer.getInstance(executionStatus, IncomingEmail.incomingEmailType, instanceName);
            }
            if (conceptName.equals(OutgoingEmail.strOutgoingEmailTypeAndName) || instanceName.equals(OutgoingEmail.strOutgoingEmailTypeAndName)) //this might happen if concept mismatches the instanceName
            {
                instance = instanceContainer.getInstance(executionStatus, OutgoingEmail.strOutgoingEmailTypeAndName, OutgoingEmail.strOutgoingEmailTypeAndName);
                if (!instance.isPresent())
                {
                    StringBuilder retSentences = new StringBuilder();
                    return TextFormattingUtils.noEmailFound(internalState);
                }
            }
        }
        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("Got instance \"" + instanceName + "\" of concept \"" + conceptName + "\"."),
                Optional.empty()); //nothing to undo here
        if (actionResponse.isSuccess())
        {
            actionResponse.addInstance(instance.get());
        }
        return actionResponse;
    }

    @Override
    public ActionResponse getFieldFromInstance(InfoForCommand infoForCommand, GenericInstance instance, String fieldName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<FieldHolder> field = instance.getField(executionStatus, fieldName);

        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("Got field \"" + fieldName + "\" from instance \"" + instance.getName() + "\"."),
                Optional.empty()); //nothing to undo here
        if (actionResponse.isSuccess())
        {
            actionResponse.addField(field.get());
        }
        return actionResponse;
    }

    @Override
    public ActionResponse getProbInstance(InfoForCommand infoForCommand)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<GenericInstance> instance = getMostPlausibleInstance(executionStatus, Optional.empty(), Optional.empty(), false, infoForCommand.userSentence);

        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("Got instance \"" + (instance.isPresent() ? instance.get().getName() : "") + "\"."),
                Optional.empty()); //nothing to undo here
        if (actionResponse.isSuccess())
        {
            actionResponse.addInstance(instance.get());
        }
        return actionResponse;
    }

    @Override
    public ActionResponse getProbInstanceByName(InfoForCommand infoForCommand, String instanceName)
    {
        if (instanceName.equals(ambiguousEmailInstanceName)) //if got ambiguous "email" instance, select the better choice according to mutability.
        {
            instanceName = InboxCommandController.emailMessageNameStart; //it seems that Instance is mostly immutable.
        }

        ExecutionStatus executionStatus = new ExecutionStatus();
        if (inboxCommandController.isInboxInstanceName(instanceName))
        {
            instanceName = inboxCommandController.getCurrentEmailName(executionStatus);
            if (executionStatus.getStatus() == ExecutionStatus.RetStatus.noPswdSet)
            {
                return resetEmailAndPassword(infoForCommand);
            }
        }

        Optional<GenericInstance> instance = getMostPlausibleInstance(executionStatus, Optional.of(instanceName), Optional.empty(), false, infoForCommand.userSentence);

        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("Got instance \"" + instanceName + "\"."),
                Optional.empty()); //nothing to undo here
        if (actionResponse.isSuccess())
        {
            actionResponse.addInstance(instance.get());
        }
        return actionResponse;
    }

    @Override
    public ActionResponse getProbFieldByInstanceNameAndFieldName(InfoForCommand infoForCommand, String instanceName, String fieldName)
    {
        return getProbField(infoForCommand, Optional.of(instanceName), fieldName, false);
    }

    @Override
    public ActionResponse getProbFieldByFieldName(InfoForCommand infoForCommand, String fieldName)
    {
        return getProbField(infoForCommand, Optional.empty(), fieldName, false);
    }

    private ActionResponse getProbField(InfoForCommand infoForCommand, Optional<String> instanceName, String fieldName, boolean mutableOnly)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (instanceName.isPresent() && instanceName.get().equals(ambiguousEmailInstanceName)) //if got ambiguous "email" instance, select the better choice according to mutability.
        {
            if (mutableOnly)
                instanceName = Optional.of(OutgoingEmail.strOutgoingEmailTypeAndName);
            else
            {
                instanceName = Optional.of(inboxCommandController.getCurrentEmailName(executionStatus));
                if (executionStatus.getStatus() == ExecutionStatus.RetStatus.noPswdSet)
                {
                    return resetEmailAndPassword(infoForCommand);
                }
            }
        }

        if (instanceName.isPresent() && inboxCommandController.isInboxInstanceName(instanceName.get()))
        {
            if (mutableOnly) //inbox is not mutable, so user probably wanted outgoing message. remove it, and let the system decide what to use.
                instanceName = Optional.empty();
            else
            {
                //instanceName = Optional.of(AliasMapping.instanceNameMapping(instanceName.get()));
                instanceName = Optional.of(inboxCommandController.getCurrentEmailName(executionStatus));
                if (executionStatus.getStatus() == ExecutionStatus.RetStatus.noPswdSet)
                {
                    return resetEmailAndPassword(infoForCommand);
                }
            }
        }

        Optional<GenericInstance> instance = getMostPlausibleInstance(executionStatus, instanceName, Optional.of(fieldName), mutableOnly, infoForCommand.userSentence);
        if (instance.isPresent() && instance.get().getConceptName().equals(OutgoingEmail.strOutgoingEmailTypeAndName) && !instanceName.isPresent() && !mutableOnly) //since the user didn't need mutable, and didn't explicitly mention the outgoing email, he probably wants the inbox
        {
            instanceName = Optional.of(inboxCommandController.getCurrentEmailName(executionStatus));
            if (executionStatus.getStatus() == ExecutionStatus.RetStatus.noPswdSet)
            {
                return resetEmailAndPassword(infoForCommand);
            }
            instance = getMostPlausibleInstance(executionStatus, instanceName, Optional.of(fieldName), mutableOnly, infoForCommand.userSentence);
        }
        Optional<FieldHolder> field = Optional.empty();
        String successStr = "";
        if (instance.isPresent())
        {
            field = instance.get().getField(executionStatus, fieldName);
            if (field.isPresent())
                successStr = "Got field \"" + fieldName + "\" from instance \"" + field.get().getParentInstanceName() + "\".";
        }

        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of(successStr),
                Optional.empty()); //nothing to undo here
        if (actionResponse.isSuccess())
        {
            actionResponse.addField(field.get());
        }
        else
        {
            //if failed, but is trying to set to outgoing_email (mutableOnly==true), and there is no email being composed, ask if would like to create a new email
            if (mutableOnly && !outEmailCommandController.isAnEmailBeingComposed() &&
                    ((instanceName.isPresent() && instanceName.get().equals(OutgoingEmail.strOutgoingEmailTypeAndName))
                            || conceptContainer.findConceptsForField(new ExecutionStatus(), fieldName, mutableOnly, false).contains(OutgoingEmail.strOutgoingEmailTypeAndName)))
            {
                actionResponse = noEmailFound(internalState, actionResponse);
            }
        }
        return actionResponse;
    }

    @Override
    public ActionResponse getProbMutableFieldByInstanceNameAndFieldName(InfoForCommand infoForCommand, String instanceName, String fieldName)
    {
        return getProbField(infoForCommand, Optional.of(instanceName), fieldName, true);
    }

    @Override
    public ActionResponse getProbMutableFieldByFieldName(InfoForCommand infoForCommand, String fieldName)
    {
        return getProbField(infoForCommand, Optional.empty(), fieldName, true);
    }

    @Override
    public ActionResponse getProbFieldVal(InfoForCommand infoForCommand)
    {
        if (previousFieldEval.isPresent())
        {
            ActionResponse actionResponse = new ActionResponse("It is: " + FieldHolder.fieldFromJSonForUser(previousFieldEval.get()), true, Optional.empty()); //TODO: what if learning?
            actionResponse.addValue(previousFieldEval.get());
            return actionResponse;
        }

        return failWithMessage(infoForCommand, "there is no previously evaluated field");
    }

    @Override
    public ActionResponse evalField(InfoForCommand infoForCommand, FieldHolder field)
    {
        JSONObject requestedField;

        requestedField = field.getFieldVal();

        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                new ExecutionStatus(),
                Optional.of("It is: " + FieldHolder.fieldFromJSonForUser(requestedField)),
                //false,//changed to false, but this might be ok being true, (all other except unknownCommand are false.)
                Optional.empty() //nothing to undo here
        );
        if (actionResponse.isSuccess())
            previousFieldEval = Optional.of(requestedField);
        actionResponse.addValue(requestedField);
        return actionResponse;
    }

    @Override
    public ActionResponse readInstance(InfoForCommand infoForCommand, GenericInstance instance)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        StringBuilder instanceContent = new StringBuilder();
        if (!instance.getConceptName().equals(IncomingEmail.incomingEmailType)) //TODO: should be done in a more modular way
            instanceContent.append("instance: \"" + instance.getName() + "\" (of concept \"" + instance.getConceptName() + "\").\n");
        for (String fieldName : instance.getAllFieldNames())
        {
            Optional<FieldHolder> field = instance.getField(executionStatus, fieldName);
            if (field.isPresent())
            {
                instanceContent.append(fieldName + ": " + field.get().fieldValForUser() + "\n");
            }
        }

        return testOkAndFormat(infoForCommand,
                new ExecutionStatus(),
                Optional.of(instanceContent.toString()),
                Optional.empty() //nothing to undo here
        );
    }

    @Override
    public ActionResponse setFieldFromString(InfoForCommand infoForCommand, FieldHolder field, String val)
    {
        return setAndAdd(infoForCommand, field, Optional.of(val), Optional.empty(), false, true);
    }

    @Override
    public ActionResponse setFieldFromFieldVal(InfoForCommand infoForCommand, FieldHolder field, JSONObject jsonVal)
    {
        return setAndAdd(infoForCommand, field, Optional.empty(), Optional.of(jsonVal), false, true);
    }

//    @Override
//    public ActionResponse setFieldWithMissingArg(InfoForCommand infoForCommand, FieldHolder field)
//    {
//        return failWithMessage(infoForCommand, "I don't know what to set " + field.getParentInstanceName() + "'s " + field.getFieldName() + " to. " +
//                "Please repeat and tell me what to set it to (e.g. set " + field.getParentInstanceName() + "'s " + field.getFieldName() + " to something)");
//    }
//
//    @Override
//    public ActionResponse setWhatFromString(InfoForCommand infoForCommand, String val)
//    {
//        return failWithMessage(infoForCommand, "I don't know what should be set to " + val + ". " +
//                "Please repeat and tell me what should be set to it (e.g. set example to " + val + ")");
//    }
//
//    @Override
//    public ActionResponse setWhatFromField(InfoForCommand infoForCommand, FieldHolder field)
//    {
//        String parentNiceName = field.getParentInstanceName();
//        if (parentNiceName.contains(InboxCommandController.emailMessageNameStart))
//        {
//            parentNiceName = InboxCommandController.emailMessageNameStart;
//        }
//        return failWithMessage(infoForCommand, "I don't know what should be set to " + parentNiceName + "'s " + field.getFieldName() + ". " +
//                "Please repeat and tell me what should be set to it (e.g. set example to " + parentNiceName + "'s " + field.getFieldName() + ")");
//    }


    @Override
    public ActionResponse addToFieldFromString(InfoForCommand infoForCommand, FieldHolder field, String val)
    {
        return setAndAdd(infoForCommand, field, Optional.of(val), Optional.empty(), true, true);
    }

    @Override
    public ActionResponse addToFieldFromFieldVal(InfoForCommand infoForCommand, FieldHolder field, JSONObject jsonVal)
    {
        return setAndAdd(infoForCommand, field, Optional.empty(), Optional.of(jsonVal), true, true);
    }


    @Override
    public ActionResponse addToFieldAtStartFromString(InfoForCommand infoForCommand, FieldHolder field, String val)
    {
        return setAndAdd(infoForCommand, field, Optional.of(val), Optional.empty(), true, false);
    }

    @Override
    public ActionResponse addToFieldAtStartFromFieldVal(InfoForCommand infoForCommand, FieldHolder field, JSONObject jsonVal)
    {
        return setAndAdd(infoForCommand, field, Optional.empty(), Optional.of(jsonVal), true, false);
    }

//    @Override
//    public ActionResponse addToWhat(InfoForCommand infoForCommand, String toAdd)
//    {
//        return failWithMessage(infoForCommand, "I don't know what I should add \"" + toAdd + "\" to. Say: add " + toAdd + " to something");
//    }


    private Optional<GenericInstance> getMostPlausibleInstance(ExecutionStatus executionStatus, Optional<String> optionalInstanceName, Optional<String> fieldName, boolean mutableOnly, String userSentence)
    {
        boolean userUsedTheWordAs = userSentence.contains(" as "); //the word "as" maybe confusing. Would be better to check if the token "as" exists though.
        //find intersection of all instances that have requested instanceName and fieldName
        List<String> conceptOptions;
        if (fieldName.isPresent())
        {
            conceptOptions = conceptContainer.findConceptsForField(executionStatus, fieldName.get(), mutableOnly, userUsedTheWordAs);
        }
        else
        {
            conceptOptions = conceptContainer.getAllConceptNames();
        }

        return instanceContainer.getMostPlausibleInstance(executionStatus, conceptOptions, optionalInstanceName, mutableOnly);
    }

    /*
     must either have val or jsonVal
     */
    private ActionResponse setAndAdd(InfoForCommand infoForCommand, FieldHolder theField, Optional<String> val, Optional<JSONObject> jsonVal, boolean addToExisting, boolean appendToEnd)
    {
        if (!val.isPresent() && !jsonVal.isPresent())
        {
            return new ActionResponse("I don't know what I should set it to, please say set " + theField.getParentInstanceName() + "'s " + theField.getFieldName() + " to something", false, Optional.empty()); //TODO: again, what if learning
        }

        JSONObject oldVal = theField.getFieldVal();

        ExecutionStatus executionStatus = new ExecutionStatus();
        if (jsonVal.isPresent())
        {
            theField.setFromJSon(executionStatus, jsonVal.get(), addToExisting, appendToEnd, false);
        }
        else
        {
            if (addToExisting)
                theField.appendTo(executionStatus, val.get(), appendToEnd, false);
            else
                theField.set(executionStatus, val.get(), false);
        }


        String successStr;
        if (executionStatus.noError() && !addToExisting)
        {
            String valForSet = FieldHolder.fieldFromJSonForUser(theField.getFieldVal());
            successStr = "The \"" + theField.getFieldName() + "\" field in \"" + theField.getParentInstanceName() + "\" was set to: \"" + valForSet + "\".";
        }
        else
        {
            String valForOutput;
            if (jsonVal.isPresent())
                valForOutput = FieldHolder.fieldFromJSonForUser(jsonVal.get());
            else
                valForOutput = val.get();
            successStr = "\"" + valForOutput + "\" was added to the \"" + theField.getFieldName() + "\" field in \"" + theField.getParentInstanceName() + "\".";
        }

        return testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of(successStr),
                //false,//Don't want to teach, since it might keep failing, by parsing again to the same original command
                Optional.of(() -> setAndAdd(infoForCommand, theField, Optional.empty(), Optional.of(oldVal), false, false)));
    }

    @Override
    public ActionResponse defineConcept(InfoForCommand infoForCommand, String newConceptName)
    {
        if (InstUtils.isEmailAddress(newConceptName))
        {
            return failWithMessage(infoForCommand, "concept names cannot be email addresses");
        }

        //TODO: remember what was the last concept defined, and add fields to is if no concept is given.
        ExecutionStatus executionStatus = new ExecutionStatus();
        conceptContainer.defineConcept(executionStatus, newConceptName);

        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                false,
                Optional.of("Concept \"" + newConceptName + "\" was defined successfully. Please add fields to it."),
                Optional.empty(),
                false,
                Optional.of(() -> undefineConcept(infoForCommand, newConceptName)));
        if (actionResponse.isSuccess())
        {
            commandsToParser.newConceptDefined(newConceptName);
        }
        return actionResponse;
    }

    @Override
    public ActionResponse addFieldToProbConcept(InfoForCommand infoForCommand, String newFieldName)
    {
        //TODO: use timestamp (just like with the instances) to resolve ambiguity
        return null;
    }

    @Override
    public ActionResponse addFieldToConcept(InfoForCommand infoForCommand, String conceptName, String fieldName)
    {
        //TODO: need to infer the field type in a smarter way...
        PossibleFieldType possibleFieldType = PossibleFieldType.multiLineString;
        boolean isList = false;
        if (fieldName.contains("email"))
            possibleFieldType = PossibleFieldType.emailAddress;
        if (fieldName.endsWith(" list"))
            isList = true;
        else
        {
            InstUtils.Plurality plurality = InstUtils.wordPlurality(fieldName);
            //TODO: we may want to alert the user and ask for confirmation if the fieldName is not in the dictionary (unknown).
            if (plurality == InstUtils.Plurality.plural || plurality == InstUtils.Plurality.unknown && fieldName.endsWith("s"))
                isList = true;
        }

        return addFieldToConceptWithType(infoForCommand, conceptName, fieldName, possibleFieldType, isList, true);
    }

    @Override
    public ActionResponse addFieldToConceptWithType(InfoForCommand infoForCommand, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList, boolean mutable)
    {
        if (InstUtils.isEmailAddress(fieldName))
        {
            return failWithMessage(infoForCommand, "field names cannot be email addresses");
        }
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (conceptName.equals(IncomingEmail.incomingEmailType) || conceptName.equals(OutgoingEmail.strOutgoingEmailTypeAndName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "fields cannot be added to email messages");
        }

        FieldDescription fieldDescription = new FieldDescription(fieldName, possibleFieldType, isList, mutable);
        if (executionStatus.noError())
            conceptContainer.addFieldToConcept(executionStatus, conceptName, fieldDescription);
        if (executionStatus.noError())
            instanceContainer.fieldAddedToConcept(executionStatus, conceptName, fieldDescription);

        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("Field \"" + fieldName + "\" was added to concept \"" + conceptName + "\"."),
                Optional.of(() -> removeFieldFromConcept(infoForCommand, conceptName, fieldName)));
        if (actionResponse.isSuccess())
        {
            commandsToParser.newFieldDefined(fieldName);
        }
        return actionResponse;
    }

    @Override
    public ActionResponse removeFieldFromConcept(InfoForCommand infoForCommand, String conceptName, String fieldName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (conceptName.equals(IncomingEmail.incomingEmailType) || conceptName.equals(OutgoingEmail.strOutgoingEmailTypeAndName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "fields cannot be removed from email messages");
        }
        if (executionStatus.noError())
            conceptContainer.removeFieldFromConcept(executionStatus, conceptName, fieldName);
        if (executionStatus.noError())
            instanceContainer.fieldRemovedFromConcept(executionStatus, conceptName, fieldName);

        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("Field \"" + fieldName + "\" was removed from concept \"" + conceptName + "\"."),
                Optional.of(() -> failWithMessage(infoForCommand, "undo is currently not supported for delete commands")));//could just add the field, but this function also deletes all usages of this field.
        if (actionResponse.isSuccess())
        {
            //if this field doesn't appear at any other concept
            if (conceptContainer.findConceptsForField(new ExecutionStatus(), fieldName, false, false).size() == 0)
                commandsToParser.removeField(fieldName);
        }
        return actionResponse;
    }

    @Override
    public ActionResponse createInstanceEmail(InfoForCommand infoForCommand, String emailStr)
    {
        if (emailStr.equals(OutgoingEmail.strOutgoingEmailTypeAndName) ||
                emailStr.equals(ambiguousEmailInstanceName)) //this is actually an instance name, but user's intention is clear.
        {
            return createNewEmailOrRestore(infoForCommand, false, true);
        }
//        if (conceptName.equals(CalendarEvent.strCalendarEventTypeAndName))
//        {
//            return createNewEventOrRestore(infoForCommand, false, true);
//        }
        return failWithMessage(infoForCommand, "creating an instance of \"" + emailStr + "\" requires a name (please repeat command and provide a name)");
    }

    /**
     * @param restore          undo an email creation or email sent
     * @param restoreFromDraft if true restore from draft, if false restore from sent email
     * @return
     */
    private ActionResponse createNewEmailOrRestore(InfoForCommand infoForCommand, boolean restore, boolean restoreFromDraft)
    {
        String conceptName = OutgoingEmail.strOutgoingEmailTypeAndName;
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (restore)
        {
            outEmailCommandController.restoreEmailFrom(executionStatus, restoreFromDraft);
        }
        else
        {
            outEmailCommandController.createNewEmail(executionStatus);
        }

        String successSentence;
        if (restore)
        {
            if (restoreFromDraft)
                successSentence = "Draft restored successfully.";
            else
                successSentence = "Sent email restored successfully (but was still sent).";
        }
        else
        {
            List<String> emailFieldNames = outEmailCommandController.changeToRelevantComposedEmailFields(conceptContainer.getAllFieldNames(conceptName));
            successSentence = "Composing new email. " + "\"" + conceptName + "\" fields are: " + userFriendlyList(emailFieldNames) + ".";
        }

        return testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of(successSentence),
                Optional.of(() -> createNewEmailOrRestore(infoForCommand, !restore, restoreFromDraft)));
    }


    /**
     * @param restore          undo an event creation or event saved
     * @param restoreFromDraft if true restore from draft, if false restore from saved event (and delete event)
     * @return
     */
    private ActionResponse createNewEventOrRestore(InfoForCommand infoForCommand, boolean restore, boolean restoreFromDraft)
    {
        String conceptName = CalendarEvent.strCalendarEventTypeAndName;
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (restore)
        {
            calendarEventController.restoreEventFrom(executionStatus, restoreFromDraft);
        }
        else
        {
            calendarEventController.createNewEvent(executionStatus);
        }

        String successSentence;
        if (restore)
        {
            if (restoreFromDraft)
                successSentence = "Draft restored successfully.";
            else
                successSentence = "Event restored successfully (and deleted).";
        }
        else
        {
            List<String> emailFieldNames = conceptContainer.getAllFieldNames(conceptName);
            successSentence = "Creating new event. " + "\"" + conceptName + "\" fields are: " + userFriendlyList(emailFieldNames) + ".";
        }

        return testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of(successSentence),
                Optional.of(() -> createNewEventOrRestore(infoForCommand, !restore, restoreFromDraft)));
    }

    @Override
    public ActionResponse createInstanceByFullNames(InfoForCommand infoForCommand, String conceptName, String newInstanceName)
    {
        if (InstUtils.isEmailAddress(newInstanceName))
        {
            return failWithMessage(infoForCommand, "instance names cannot be email addresses");
        }
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (conceptName.equals(OutgoingEmail.strOutgoingEmailTypeAndName))
        {
            return createNewEmailOrRestore(infoForCommand, false, true);
        }
        Optional<GenericInstance> instanceAdded = instanceContainer.addInstance(executionStatus, conceptName, newInstanceName, true);

        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("Instance \"" + newInstanceName + "\" (of concept \"" + conceptName + "\") was created. " + listFieldsOfConcept(conceptName)), //listFieldsOfConcept is safe.
                Optional.of(() -> deleteInstance(infoForCommand, instanceAdded.get())));//instanceAdded should contain a value if no error
        if (actionResponse.isSuccess())
        {
            commandsToParser.newInstanceDefined(newInstanceName);
        }
        return actionResponse;
    }


    /**
     * safe call.
     *
     * @param conceptName
     * @return returns empty string if concept does not exist
     */
    private String listFieldsOfConcept(String conceptName)
    {
        if (conceptContainer.doesConceptExist(conceptName))
            return "\"" + conceptName + "\" fields are: " + userFriendlyList(conceptContainer.getAllFieldNames(conceptName)) + ".";
        return "";
    }

    @Override
    public ActionResponse createInstanceByInstanceName(InfoForCommand infoForCommand, String newInstanceName)
    {
        return null;
    }

    @Override
    public ActionResponse setFieldTypeKnownConcept(InfoForCommand infoForCommand, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList)
    {
        return null;
    }

    @Override
    public ActionResponse setFieldType(InfoForCommand infoForCommand, String fieldName, PossibleFieldType possibleFieldType, boolean isList)
    {
        return null;
    }

    @Override
    public ActionResponse unknownCommand(InfoForCommand infoForCommand)
    {
        if (internalState.shouldLearnNext())
        {
            internalState.userGaveCommand(infoForCommand, false, false, false);
            internalState.pendOnLearning();
            return yes(infoForCommand);
        }

        ExecutionStatus executionStatus = new ExecutionStatus();
        executionStatus.add(ExecutionStatus.RetStatus.error, "I heard \"" + infoForCommand.userSentence + "\" and I don't know what to do");

        return testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                false,
                Optional.empty(), //will fail anyway, because added error above.
                Optional.empty(),
                true,
                Optional.empty());//shouldn't be used because failed
    }

    /**
     * This will go into learning mode and cause the parser to fail next time (but only on new commands).
     * Even if the user says she wants to teach a new command, she can't teach new commands which is identical to commands that appear in the examples (either already tought, or from data)
     */
    @Override
    public ActionResponse teachNewCommand(InfoForCommand infoForCommand)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (internalState.isInLearningMode())
        {
            return new ActionResponse("I'm already trying to learn a command, if you want me to end and learn this new command, say \"end\". If you want me to cancel this command say \"cancel\".", false, Optional.empty());
        }

        internalState.learnNextCommand();
        commandsToParser.failNextCommand();
        return new ActionResponse("I'm happy to learn a new command. Now say the command the way you would use it, then I will ask you what exactly to do. I will try to generalize to similar sentences (if you don't want me to learn say \"cancel\")", true, Optional.empty());
    }

    /*
        We excecute all sentences as we go. no need to execute them again, just update the parser for next time.
     */
    @Override
    public ActionResponse end(InfoForCommand infoForCommand)
    {
        if (!internalState.isInLearningMode())
        {
            return new ActionResponse("Not sure what you are talking about.", false, Optional.empty());
        }
        String commandBeingLearnt = internalState.lastCommandOrLearningCommand;
        List<Expression2> commandsLearnt = internalState.endLearningGetSentences();

        //make sure learnt at least one successful sentence
        if (commandsLearnt.size() > 0)
        {
            boolean learnedAnyGeneralization = commandsToParser.addTrainingEg(
                    commandBeingLearnt,
                    commandsLearnt);
            final String learningSuccessStr = "I now know what to do when you say "+(learnedAnyGeneralization? "(for example)":"")+": \"" + commandBeingLearnt + "\"!";
            if (usePendingResponses)
            {
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        commandsToParser.retrain(
                                Optional.of(new ActionResponse(learningSuccessStr, true, Optional.empty())));

                    }
                }.start();
                return new ActionResponse("I'm currently learning the new command (\"" + commandBeingLearnt + "\"), " + resendNewRequest +
                        ". " + (learnedAnyGeneralization? "I'm also trying to generalize to other similar commands!" : "I did not find any way to generalize this new command to other commands, but you can use the new command and it should work!"), true, Optional.empty());
            }
            else
            {
                commandsToParser.retrain(Optional.empty());
                return new ActionResponse(learningSuccessStr, true, Optional.empty());
            }
        }
        return new ActionResponse("I'm afraid that I didn't learn anything.", false, Optional.empty());

    }


    @Override
    public ActionResponse endOrCancel(InfoForCommand infoForCommand)
    {
        if (!internalState.isInLearningMode())
        {
            return new ActionResponse("Not sure what you are talking about.", false, Optional.empty());
        }
        if (internalState.learnedSomething())
        {
            return new ActionResponse("I'm not sure what to do. If you want me to learn \"" + internalState.lastCommandOrLearningCommand + "\", say \"end\". If you want me to cancel say \"cancel\".", false, Optional.empty());
        }
        return cancel(infoForCommand); //didn't learn anything anyway.
    }


    @Override
    public ActionResponse undefineConcept(InfoForCommand infoForCommand, String conceptName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (conceptName.equals(IncomingEmail.incomingEmailType) || conceptName.equals(ambiguousEmailInstanceName) || conceptName.equals(OutgoingEmail.strOutgoingEmailTypeAndName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "emails concepts are built-in and cannot be undefined");
        }
        if (executionStatus.noError())
        {
            conceptContainer.undefineConcept(executionStatus, conceptName);
            //delete all instances
            instanceContainer.conceptUndefined(conceptName);
        }

        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("concept \"" + conceptName + "\" was undefined and all its instances were deleted."), //TODO: very harsh, should ask if sure before doing this.
                Optional.of(() -> failWithMessage(infoForCommand, "undo is currently not supported for delete commands")));
        if (actionResponse.isSuccess())
        {
            commandsToParser.undefineConcept(conceptName);
        }
        return actionResponse;
    }

    @Override
    public ActionResponse deleteInstance(InfoForCommand infoForCommand, GenericInstance instance)
    {
        String instanceName = instance.getName();
        String conceptName = instance.getConceptName();
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (instance.getConceptName().equals(IncomingEmail.incomingEmailType) || conceptName.equals(ambiguousEmailInstanceName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "emails in the inbox can't be deleted. Instead move to the next email (say \"next email\")");
        }
        if (executionStatus.noError())
            instanceContainer.deleteInstance(executionStatus, instance);

        ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("Instance \"" + instanceName + "\" of concept \"" + conceptName + "\" was deleted."),
                Optional.of(() -> failWithMessage(infoForCommand, "undo is currently not supported for delete commands"))); //in order to support "undo" of delete command, may create an additional column in the DB of deleted which holds which command deleted it.
        if (actionResponse.isSuccess())
        {
            //if this instance name doesn't appear at any other concept
            if (!instanceContainer.getMostPlausibleInstance(new ExecutionStatus(), conceptContainer.getAllConceptNames(), Optional.of(instanceName), false).isPresent() &&
                    (!instance.getConceptName().equals(IncomingEmail.incomingEmailType)) &&
                    (!conceptName.equals(ambiguousEmailInstanceName)) &&
                    (!instanceName.equals(OutgoingEmail.strOutgoingEmailTypeAndName))
                    )
                commandsToParser.removeInstance(instanceName);
        }
        return actionResponse;
    }


    @Override
    public ActionResponse next(InfoForCommand infoForCommand, String instanceName)
    {
        return nextPrevLastIdx(infoForCommand, instanceName, new CNextPrevLastIdx(CNextPrevLastIdx.ENextPrevLastIdx.next));
    }

    @Override
    public ActionResponse previous(InfoForCommand infoForCommand, String instanceName)
    {
        return nextPrevLastIdx(infoForCommand, instanceName, new CNextPrevLastIdx(CNextPrevLastIdx.ENextPrevLastIdx.previous));
    }

    @Override
    public ActionResponse newest(InfoForCommand infoForCommand, String instanceName)
    {
        return nextPrevLastIdx(infoForCommand, instanceName, new CNextPrevLastIdx(CNextPrevLastIdx.ENextPrevLastIdx.newest));
    }

    private static class CNextPrevLastIdx
    {
        public CNextPrevLastIdx(ENextPrevLastIdx nextPrevLastIdx)
        {
            this.nextPrevLastIdx = nextPrevLastIdx;
        }

        public CNextPrevLastIdx(ENextPrevLastIdx nextPrevLastIdx, int idx)
        {
            this.nextPrevLastIdx = nextPrevLastIdx;
            this.idx = idx;
        }

        enum ENextPrevLastIdx
        {
            next, previous, newest, requested
        }

        ;
        public ENextPrevLastIdx nextPrevLastIdx;
        public int idx;
    }

    private ActionResponse nextPrevLastIdx(InfoForCommand infoForCommand, String instanceName, CNextPrevLastIdx nextPrevLastIdx)
    {
        String nextOrPrev = nextPrevLastIdx.nextPrevLastIdx.name();
        if (instanceName.equals(ambiguousEmailInstanceName) || inboxCommandController.isInboxInstanceName(instanceName))
        {
            final CNextPrevLastIdx opposite;
            ExecutionStatus executionStatus = new ExecutionStatus();
            if (nextPrevLastIdx.nextPrevLastIdx == CNextPrevLastIdx.ENextPrevLastIdx.next)
            {
                inboxCommandController.setToNextEmail(executionStatus);
                opposite = new CNextPrevLastIdx(CNextPrevLastIdx.ENextPrevLastIdx.previous);
            }
            else if (nextPrevLastIdx.nextPrevLastIdx == CNextPrevLastIdx.ENextPrevLastIdx.previous)
            {
                inboxCommandController.setToPrevEmail(executionStatus);
                opposite = new CNextPrevLastIdx(CNextPrevLastIdx.ENextPrevLastIdx.next);
            }
            else if (nextPrevLastIdx.nextPrevLastIdx == CNextPrevLastIdx.ENextPrevLastIdx.newest)
            {
                int prevIdx = inboxCommandController.setToNewestEmail(executionStatus);
                opposite = new CNextPrevLastIdx(CNextPrevLastIdx.ENextPrevLastIdx.requested, prevIdx);
            }
            else if (nextPrevLastIdx.nextPrevLastIdx == CNextPrevLastIdx.ENextPrevLastIdx.requested)
            {
                int prevIdx = inboxCommandController.setToIndex(nextPrevLastIdx.idx);
                opposite = new CNextPrevLastIdx(CNextPrevLastIdx.ENextPrevLastIdx.requested, prevIdx);
            }
            else
            {
                opposite = null;
                Preconditions.checkState(opposite != null);
            }

            instanceContainer.getInstance(executionStatus, IncomingEmail.incomingEmailType, inboxCommandController.getCurrentEmailName(new ExecutionStatus())); //just touching it to update last instance being touched (for "it", etc.).

            return testOkAndFormat(infoForCommand,
                    executionStatus,
                    Optional.of("Set to " + nextOrPrev + " incoming email successfully."),
                    Optional.of(() -> nextPrevLastIdx(infoForCommand, instanceName, opposite)));
        }
        else if (instanceName.equals(CalendarEvent.strCalendarEventTypeAndName))
        {
            final CNextPrevLastIdx opposite;
            GenericInstance instance = null;
            ExecutionStatus executionStatus = new ExecutionStatus();
            if ((nextPrevLastIdx.nextPrevLastIdx == CNextPrevLastIdx.ENextPrevLastIdx.next) ||
                    (nextPrevLastIdx.nextPrevLastIdx == CNextPrevLastIdx.ENextPrevLastIdx.previous))
            {
                boolean wantsNext = (nextPrevLastIdx.nextPrevLastIdx == CNextPrevLastIdx.ENextPrevLastIdx.next);

                Optional<GenericInstance> ret = calendarEventController.getNextPrevCalendarEvent(executionStatus, wantsNext);
                if (ret.isPresent())
                {
                    instance = ret.get();
                    if (wantsNext)
                        opposite = new CNextPrevLastIdx(CNextPrevLastIdx.ENextPrevLastIdx.previous);
                    else
                        opposite = new CNextPrevLastIdx(CNextPrevLastIdx.ENextPrevLastIdx.next);
                }
                else
                    opposite = null; //must set to something, will anyway fail so won't undo
            }
            else
            {
                executionStatus.add(ExecutionStatus.RetStatus.error, "Cannot get latest meeting");
                opposite = new CNextPrevLastIdx(CNextPrevLastIdx.ENextPrevLastIdx.previous);
            }

            instanceContainer.getInstance(executionStatus, IncomingEmail.incomingEmailType, inboxCommandController.getCurrentEmailName(executionStatus));

            ActionResponse actionResponse = testOkAndFormat(infoForCommand,
                    executionStatus,
                    Optional.of("Set to " + nextOrPrev + " incoming email successfully."),
                    Optional.of(() -> nextPrevLastIdx(infoForCommand, instanceName, opposite)));
            if (executionStatus.noError())
            {
                //actionResponse.addInstance(instance);
                return readInstance(infoForCommand, instance);
            }
            return actionResponse;
        }
        return failWithMessage(infoForCommand, "I don't know how to give you the " + nextOrPrev + " " + instanceName);
    }

    @Override
    public ActionResponse say(InfoForCommand infoForCommand, String toSay)
    {
        return testOkAndFormat(infoForCommand,
                new ExecutionStatus(),
                Optional.of(toSay),
                Optional.of(() -> say(infoForCommand, "I'm taking back my words")));
    }

    @Override
    public ActionResponse undo(InfoForCommand infoForCommand)
    {
        internalState.removeLastCommandIfLearning(); //undo learning.
        Optional<ActionResponse> response = commandHistory.undo();
        if (response.isPresent())
            return response.get();
        return failWithMessage(infoForCommand, "undo failed");
    }

//    @Override
//    public ActionResponse runScript(InfoForCommand infoForCommand, String scriptToRun)
//    {
//        return testOkAndFormat(infoForCommand,
//                new ExecutionStatus(),
//                false,
//                true,
//                Optional.of(runScriptPre + scriptToRun),
//                false,//can't fail
//                Optional.of(() -> failWithMessage(infoForCommand, "undo is currently not supported for scripts"))
//        );
//    }

    @Override
    public ActionResponse demonstrate(InfoForCommand infoForCommand)
    {
        if (!internalState.isInLearningMode())
            return teachNewCommand(infoForCommand);
        //if (internalState.learnedSomething())
        //return failWithMessage(infoForCommand, "speech commands can only be combined with already demonstrated commands. Please cancel current command, and teach me by demonstration a new command. Then you can combine it with speech commands");

        String scriptName = internalState.lastCommandOrLearningCommand;
//        InfoForCommand infoForDemonstrate = new InfoForCommand(scriptName, ExpressionParser.expression2().parseSingleExpression(runScriptExpression.apply(scriptName)));
//        internalState.userGaveCommand(infoForDemonstrate, true, false);
//        List commandsLearnt = internalState.endLearningGetSentences();

//        //make sure learnt at least one successful sentence
//        if (commandsLearnt.size() > 0)
//        {
//            if (usePendingResponses)
//            {
//                new Thread()
//                {
//                    @Override
//                    public void run()
//                    {
//                        commandsToParser.addTrainingEg(
//                                scriptName,
//                                commandsLearnt,
//                                Optional.empty());
//
//                    }
//                }.start();
//            }
//            else
//            {
//                commandsToParser.addTrainingEg(
//                        scriptName,
//                        commandsLearnt,
//                        Optional.empty()
//                );
//            }
//        }

        return new ActionResponse(Consts.demonstrateStr + scriptName, true, Optional.empty());
    }

//    @Override
//    public ActionResponse sugExecFromJSON(InfoForCommand infoForCommand, String jsonBlock)
//    {
//        try
//        {
//            JSONObject asJson = new JSONObject(jsonBlock);
//            return new ActionResponse(asJson, true, Optional.empty());
//        } catch (JSONException e)
//        {
//            e.printStackTrace();
//        }
//        return failWithMessage(infoForCommand, "there is a problem with the json");
//    }

    @Override
    public ActionResponse sugExecClick(InfoForCommand infoForCommand, String buttonText)
    {
        return sugExec(infoForCommand, "CLICK", buttonText, "false", Optional.empty());
    }

    //@Override currently not accessible from outside
    public ActionResponse sugExecSetText(InfoForCommand infoForCommand, String buttonText, String stringToSet)
    {
        return sugExec(infoForCommand, "SET_TEXT", buttonText, "false", stringToSet);
    }

    @Override
    public ActionResponse sugExec(InfoForCommand infoForCommand, String actionType, String buttonText, String fromLocation, String actionPrm)
    {
        Optional<String> optionalActionParam = Optional.empty();
        if (!actionPrm.isEmpty())
            optionalActionParam = Optional.of(actionPrm);
        return sugExec(infoForCommand, actionType, buttonText, fromLocation, optionalActionParam);
    }

    private ActionResponse sugExec(InfoForCommand infoForCommand, String actionType, String buttonText, String isLocation, Optional<String> actionParam)
    {
        Optional<JSONObject> jsonToExec = createJSONForSug(actionType, buttonText, isLocation, actionParam);
        if (jsonToExec.isPresent())
        {
            return testOkAndFormat(
                    infoForCommand,
                    new ExecutionStatus(),
                    false,
                    true,
                    false,
                    Optional.empty(),
                    jsonToExec,
                    false,
                    Optional.of(() -> failWithMessage(infoForCommand, "I can't undo this"))
                    );
        }
        return failWithMessage(infoForCommand, "there is a problem with the json");
    }

    /**
     * @param actionType
     * @param buttonText should be the text of the button, or location e.g. "1188 1944 1384 2140"
     * @param isLocation
     * @return
     */
    private Optional<JSONObject> createJSONForSug(String actionType, String buttonText, String isLocation, Optional<String> actionParam)
    {
        try
        {
            //buttonText = buttonText.substring(0, 1).toUpperCase() + buttonText.substring(1);//capitilize first letter for debug purposes
            JSONObject asBlock = new JSONObject();
            asBlock.put("actionType", actionType);
            if (actionParam.isPresent())
                asBlock.put("actionParameter", actionParam.get());
            JSONObject filter = new JSONObject();
            if (isLocation.equals("parent") || isLocation.equals("screen"))
            {
                if (isLocation.equals("screen"))
                    filter.put("boundsInScreen", buttonText);
                else if (isLocation.equals("parent"))
                    filter.put("boundsInParent", buttonText);
            }
            else
            {
                filter.put("textOrChildTextOrContentDescription", buttonText);
            }
            if (actionType.equals("CLICK")) //don't need this for LONG_CLICK
                filter.put("isClickable", "true");
            //filter.put("text", buttonText);
            asBlock.put("filter", filter);
            JSONObject asJson = new JSONObject();
            asJson.put("nextBlock", asBlock);
            return Optional.of(asJson);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return Optional.empty();
    }


    @Override
    public ActionResponse userHasDemonstrated(InfoForCommand infoForCommand, JSONObject json)
    {
        System.out.print(json.toString());
        if (!internalState.isInLearningMode())
            return failWithMessage(infoForCommand, "I wasn't expecting a demonstration from you");

        //String commandBeingLearned = internalState.lastCommandOrLearningCommand;
        //String cmdForType = commandBeingLearned.replace(" ", "_");
        String previousClickForType = "topLvl";
        //pull out all alternatives
        try
        {
            JSONObject nextBlock = json.getJSONObject("nextBlock");
            //int blockNum = 0;
            while (nextBlock != null)
            {
                List<String> allAlternatives = new LinkedList<>();
                if (nextBlock.getJSONObject("filter").has("alternativeLabels"))
                {
                    JSONArray altList = nextBlock.getJSONObject("filter").getJSONArray("alternativeLabels");
                    //add all alternatives as new types
                    for (int i = 0; i < altList.length(); i++)
                    {
                        try
                        {
                            String singleAlt = altList.getJSONObject(i).getString("value");
                            singleAlt = InstUtils.alphaNumLower(singleAlt);
                            //it is not likely that the user will use very long alternatives, so we shorten them-up
                            //if (singleAlt.split(" ").length > 3)
                            //{
                            //String[] singleAltWords = singleAlt.split(" ");
                            //singleAlt = singleAltWords[0] + " " + singleAltWords[1] + " " + singleAltWords[2];
                            //}
                            //if (singleAlt.length() > 50)
                            //singleAlt = singleAlt.substring(0, 50);
                            if (!singleAlt.equals(""))
                                allAlternatives.add(InstUtils.alphaNumLower(singleAlt));
                        } catch (Exception e)
                        {
                            System.out.print("error when adding alt, i:" + i);
                            try
                            {
                                String alt = altList.getJSONObject(i).getString("value");
                                System.out.print(". alt: " + alt);
                            } catch (Exception ignored)
                            {
                                System.out.print(". error fetching alternative");
                            }
                        }
                    }
                }

                //add command demonstrated
                //convert json to a json with only one command
                String actionType = nextBlock.getString("actionType");

                JSONObject filter = null;
                boolean isLocation = false;
                boolean locationParent = false;
                Optional<String> actionParameter = Optional.empty();
                if (nextBlock.has("actionParameter"))
                    actionParameter = Optional.of(InstUtils.alphaNumLower(nextBlock.getString("actionParameter")));
                if (nextBlock.has("filter"))
                    filter = nextBlock.getJSONObject("filter");
                if (filter != null)
                {
                    String buttonText = null;

                    if (filter.has("text"))
                        buttonText = filter.getString("text");
                    if (buttonText == null || buttonText.equals(""))
                    {
                        if (filter.has("contentDescription"))
                            buttonText = filter.getString("contentDescription");
                    }
                    if (buttonText == null || buttonText.equals(""))
                    {
                        JSONObject childFilter = null;
                        if (filter.has("childFilter"))
                            childFilter = filter.getJSONObject("childFilter");
                        if (childFilter != null)
                            buttonText = childFilter.getString("text");
                    }

                    if (buttonText == null || buttonText.equals(""))
                    {
                        if (filter.has("boundsInScreen"))
                        {
                            buttonText = filter.getString("boundsInScreen");
                            isLocation = true;
                            locationParent = false;
                        }
                        if (filter.has("boundsInParent"))
                        {
                            buttonText = filter.getString("boundsInParent");
                            isLocation = true;
                            locationParent = true;
                        }
                    }
                    //commandsToParser.newDemonstrateAlt("SugOption", filterText); //just double checking we added this alternative
                    if (buttonText != null && !buttonText.equals(""))
                    {
                        buttonText = InstUtils.alphaNumLower(buttonText);
                        String expression;
                        if (isLocation)
                        {
                            expression = "(sugExec \"" + actionType + "\" \"" + buttonText + "\"" + " \"" + (locationParent ? "parent" : "screen") + "\"";
                        }
                        else
                        {
                            if (!allAlternatives.contains(InstUtils.alphaNumLower(buttonText)))
                            {
                                allAlternatives.add(InstUtils.alphaNumLower(buttonText));
                            }
                            commandsToParser.newDemonstrateAlt(previousClickForType, allAlternatives, true);
                            expression = "(sugExec \"" + actionType + "\" " + buttonText.replace(" ", "_") + " \"false\"";
                        }
                        //top level should have packageName: "com.google.android.googlequicksearchbox"
                        String textForType = buttonText.trim();
                        if (textForType.contains(" "))
                            textForType = textForType.split(" ")[0];
                        textForType = InstUtils.alphaNumLower(textForType);
                        previousClickForType += "_" + textForType; //should work even if textForType end-up  being empty
                        expression += " " + (actionParameter.isPresent() ? "(stringValue \"" + actionParameter.get() + "\")" : "\"\"") + ")";
                        internalState.userGaveCommand(new InfoForCommand("n/a", ExpressionParser.expression2().parseSingleExpression(expression), Optional.empty()), true, false, false);
                    }
                }
                if (nextBlock.has("nextBlock"))
                    nextBlock = nextBlock.getJSONObject("nextBlock");
                else
                    nextBlock = null;
                //blockNum++;
            }
            //learn
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return end(infoForCommand);
    }

    @Override
    public ActionResponse removeLastLearnedCommand(InfoForCommand infoForCommand)
    {
        if (internalState.isInLearningMode())
            return undo(infoForCommand);
        return failWithMessage(infoForCommand, "I'm not learning anything right now.");
    }


    private ActionResponse failWithMessage(InfoForCommand infoForCommand, String sentence)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        executionStatus.add(ExecutionStatus.RetStatus.error, sentence);

        return testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.empty(), //will fail for sure
                Optional.empty() //shouldn't be used because failed
        );
    }

    public ActionResponse testOkAndFormat(InfoForCommand infoForCommand,
                                          ExecutionStatus executionStatus,
                                          Optional<String> optionalSuccessSentence,
                                          Optional<Callable<ActionResponse>> callableForUndo)
    {
        return testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                false,
                optionalSuccessSentence,
                Optional.empty(),
                false,
                callableForUndo);
    }

    /*
        internalState can be null if askToTeachIfFails is false
        returns success.
     */
    public ActionResponse testOkAndFormat(InfoForCommand infoForCommand,
                                          ExecutionStatus executionStatus,
                                          boolean failWithWarningToo,
                                          boolean ignoreComments,
                                          boolean preservePending,
                                          Optional<String> optionalSuccessSentence,
                                          Optional<JSONObject> optionalSuccessExecJson, //can't have both a sentence and JSon
                                          boolean askToTeachIfFails,
                                          Optional<Callable<ActionResponse>> callableForUndo)
    {
        StringBuilder response = new StringBuilder();
        Optional<String> learningSentence = Optional.empty();
        boolean isInLearningPhase = internalState.isInLearningMode();
        ExecutionStatus.RetStatus retStatus = executionStatus.getStatus();
        boolean success = retStatus == ExecutionStatus.RetStatus.ok || retStatus == ExecutionStatus.RetStatus.comment ||
                (retStatus == ExecutionStatus.RetStatus.warning && !failWithWarningToo);

        internalState.userGaveCommand(infoForCommand, success, commandHistory.isExecutingAnUndoNow(), preservePending); //this also clears pending on learning or  email creation

        if (retStatus == ExecutionStatus.RetStatus.error || retStatus == ExecutionStatus.RetStatus.warning ||
                (retStatus == ExecutionStatus.RetStatus.comment && !ignoreComments))
        {
            ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
            if (statusAndMessage.message.isPresent())
            {
                if (success)
                {
                    response.append("I see that " + statusAndMessage.message.get() + ". ");
                }
                else
                {
                    response.append("Sorry, but " + statusAndMessage.message.get() + ".");

                    if (isInLearningPhase)
                    {
                        learningSentence = Optional.of("What should I do instead (when executing: \"" + internalState.lastCommandOrLearningCommand + "\")?");
                    }
                }
            }
            else if (executionStatus.isError())
            {
                response.append("There was some kind of error.");
            }
        }

        if (!success && askToTeachIfFails && !isInLearningPhase)
        {
            //response.append("\nWould you like to teach me what to do in this case (either say yes or simply ignore this question)?");
            response.append("\nWould you like to teach me (say yes or just ignore)?");
            internalState.pendOnLearning();
        }

        if (success && optionalSuccessSentence.isPresent())
        {
            response.append(optionalSuccessSentence.get());
            if (isInLearningPhase)
                learningSentence = Optional.of("What shall I do next (when executing: \"" + internalState.lastCommandOrLearningCommand + "\")?");
        }

        if (learningSentence.isPresent())
        {
            if (internalState.shouldFailLearning())
                learningSentence = Optional.of("I didn't learn anything. If you want to teach me what to do when you say \"" + internalState.lastCommandOrLearningCommand + "\", say it again, and answer \"yes\" when I ask if you want to teach me.");
            else if (internalState.isLearningForTooLong() || internalState.userHavingTrouble())
            {
                //learningSentence = Optional.of(learningSentence.get() + "\nI noticed that you are teaching me a command for a while now, it's ok with me and you may continue, but if you want me to end and learn this new command, say \"end\". If you want me to cancel this command say \"cancel\".");
                learningSentence = Optional.of(learningSentence.get() + "\nYou may continue teaching me this command, when you want me to end and learn this new command, say \"end\". If you want me to cancel this command say \"cancel\".");
            }
        }

        if (success && callableForUndo.isPresent())
            commandHistory.push(infoForCommand, callableForUndo.get());

        if (optionalSuccessExecJson.isPresent())
            return new ActionResponse(optionalSuccessExecJson.get(), success, learningSentence);
        else
            return new ActionResponse(response.toString(), success, learningSentence);
    }

    @Override
    public ActionResponse forgetAllLearned(InfoForCommand infoForCommand)
    {
        //ask if sure!!!!
        internalState.pendOnForgetEverything();

        return testOkAndFormat(infoForCommand,
                new ExecutionStatus(),
                false,
                true,
                true,
                Optional.of("Forget all I know, are you really sure about that?"),
                Optional.empty(),
                false,
                Optional.of(() -> say(infoForCommand, "I didn't do anything, so there is nothing to undo")));
    }

    private ActionResponse actuallyForgetEverything(InfoForCommand infoForCommand)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        try
        {
            clearUserDBAndReset.call();
            boolean success = commandsToParser.forgetEverythingLearned();
            if (!success)
            {
                executionStatus.add(ExecutionStatus.RetStatus.error, "deletion from DB has failed");
            }
        } catch (Exception e)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "there was an error while trying to forget");
        }

        return testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of("I forgot everything. Everything!"),
                Optional.of(() -> failWithMessage(infoForCommand, "I really forgot everything, I can't undo it")));
    }

    @Override
    public ActionResponse searchYoutube(InfoForCommand infoForCommand, String searchTerm)
    {
        Optional<String> optionalVideoId = InstUtils.getFirstYoutubeResponse(searchTerm);
        String videoId;
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (optionalVideoId.isPresent())
            videoId = optionalVideoId.get();
        else
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "YouTube video not found");
            videoId = "";
        }

        return testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of(Consts.playYouTubeStr + videoId),
                Optional.of(() -> say(infoForCommand, "Forget that..."))
                );
    }

    @Override
    public ActionResponse getNewsGuardian(InfoForCommand infoForCommand, String searchTerm)//, int numOfStories)
    {
        final int numOfTitles = 3;
        final int numOfSentencesPerTitle = 2;
        List<InstUtils.NewsInfo> newsInfos = InstUtils.getGuardianLinks(searchTerm, numOfTitles);
        InstUtils.getSummaries(newsInfos, numOfSentencesPerTitle);
        String newsForUser = newsInfos.stream().map(e -> "Title: " + e.title + "\n" + e.summary).reduce("", (x,y) -> x + "\n" + y, (x,y) -> x + y);
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (newsInfos.size() == 0)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "No matching titles found for: " + searchTerm);
            newsForUser = "";
        }

        return testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of(newsForUser),
                Optional.of(() -> say(infoForCommand, "Forget that..."))
        );
    }

    @Override
    public ActionResponse getAnswerToFactoid(InfoForCommand infoForCommand, String searchTerm)
    {
        searchTerm = infoForCommand.userSentence; //modifying searchTerm to include the whole question!
        String result = InstUtils.getAnswerToFactoid(searchTerm);
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (result == null || result.equals(""))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "no answer was found");
            result = "";
        }

        return testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                false,
                Optional.of(result),
                Optional.empty(),
                true,
                Optional.of(() -> say(infoForCommand, "Just forget that...")));
    }

    @Override
    public ActionResponse spell(InfoForCommand infoForCommand, String word)
    {
        String spelledOutWord = word.replaceAll(".(?=.)", "$0 ");
        return say(infoForCommand, word + " is spelled " + spelledOutWord);
    }

    /**
     * @param infoForCommand
     * @param type : read/setTimer/setAlarm
     * @param arg1 : if read: whatToRead: currentTime/currentDate/allTimers/allAlarms.
     *             alarmDateTime: yyyy-MM-dd HH:mm:ss
     * @param arg2 : if setTimer/setAlarm: toSay: what to say when time
     * @param arg3 : if setTimer/setAlarm: alarmType: bell/#timesToRepeat
     * @return
     */
    @Override
    public ActionResponse timeFuncs(InfoForCommand infoForCommand, String type, String arg1, String arg2, String arg3)
    {
        JSONObject jsonForTimer = new JSONObject();
        ExecutionStatus executionStatus = new ExecutionStatus();
        try
        {
            jsonForTimer.put("type", type);
            if (type.equals("read"))
            {
                String whatToRead = "";
                if (arg1.equals("time"))
                    whatToRead = "currentTime";
                else if (arg1.equals("date"))
                    whatToRead = "currentDate";
                else if (arg1.equals("alarm"))
                    whatToRead = "allAlarms";
                else if (arg1.equals("timer"))
                    whatToRead = "allTimers";
                jsonForTimer.put("whatToRead", whatToRead);
            }
            else if (type.equals("setAlarm") || type.equals("setTimer"))
            {
                Optional<Date> alarmDateTime = InstUtils.getDate(arg1, infoForCommand.currentTimeOnUsersPhone);
                if (alarmDateTime.isPresent())
                {
                    jsonForTimer.put("alarmTimerDate", DateType.dateFormat.format(alarmDateTime.get()));
                }
                else
                {
                    executionStatus.add(ExecutionStatus.RetStatus.error, "I couldn't understand when the alarm should be set for");
                }
                String toSay = arg2;
                jsonForTimer.put("toSay", toSay);
                boolean bell = !arg3.equals("false"); //if the user doesn't explicitly ask for no bell, there will be a bell
                jsonForTimer.put("bell", bell);
            }
            else if (type.equals("del"))
            {
                if (arg1.equals("alarm"))
                    jsonForTimer.put("type", "delAlarm");
                else
                    jsonForTimer.put("type", "delTimer");
                Optional<Date> alarmDateTime = InstUtils.getDate(arg2, infoForCommand.currentTimeOnUsersPhone);
                if (alarmDateTime.isPresent())
                {
                    jsonForTimer.put("alarmTimerDate", DateType.dateFormat.format(alarmDateTime.get()));
                }
                else
                {
                    executionStatus.add(ExecutionStatus.RetStatus.error, "I couldn't understand which alarm should be canceled");
                }
            }
        }
        catch (Exception ex)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "something went wrong");
        }

        return testOkAndFormat(infoForCommand,
                executionStatus,
                Optional.of(Consts.timerFunctions + jsonForTimer.toString()),
                Optional.of(() -> say(infoForCommand, "Forget that..."))
        );
    }


    @Override
    public ActionResponse plusAction(InfoForCommand infoForCommand, String arg1, String arg2)
    {
        return new ActionResponse("I don't know how much is " + arg1 + " and " + arg2, true, Optional.empty());
    }
}
