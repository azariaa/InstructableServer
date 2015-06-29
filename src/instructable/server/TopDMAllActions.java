package instructable.server;

import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.hirarchy.*;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;
import org.json.simple.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static instructable.server.TextFormattingUtils.noEmailFound;
import static instructable.server.TextFormattingUtils.userFriendlyList;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class TopDMAllActions implements IAllUserActions, IIncomingEmailControlling
{
    ConceptContainer conceptContainer;
    InstanceContainer instanceContainer;
    ICommandsToParser commandsToParser;
    OutEmailCommandController outEmailCommandController;
    InboxCommandController inboxCommandController;
    static public final String userEmailAddress = "you@myworkplace.com";

    static private final String ambiguousEmailInstanceName = "email"; //can either be outgoing email, or inbox
    static private final String yesExpression = "(yes)";
    static private final String createEmailExpression = "(createInstanceByConceptName outgoing_email)";

    Optional<JSONObject> previousFieldEval = Optional.empty();

    public TopDMAllActions(ICommandsToParser commandsToParser, IEmailSender emailSender)
    {
        conceptContainer = new ConceptContainer();
        instanceContainer = new InstanceContainer(conceptContainer);
        outEmailCommandController = new OutEmailCommandController(userEmailAddress, conceptContainer, instanceContainer, emailSender);
        inboxCommandController = new InboxCommandController(conceptContainer, instanceContainer);
        this.commandsToParser = commandsToParser;
        internalState = new InternalState();
    }

    @Override
    public void addEmailMessageToInbox(IncomingEmail emailMessage)
    {
        inboxCommandController.addEmailMessageToInbox(emailMessage);
    }

    //TODO: may want to allow to pend on function delegates.
    /*
    this class is in-charge of tracking the user's sentences especially for learning.
     */
    public static class InternalState
    {
        private static final int aLotOfExpressions = 8;

        private static enum InternalLearningStateMode

        {
            none, pendOnLearning, learning
        }

        private InternalLearningStateMode internalLearningStateMode;
        private boolean pendingOnEmailCreation;
        List<Expression2> expressionsLearnt = new LinkedList<>();
        int failCount = 0;
        String lastCommandOrLearningCommand = "";
        int lastInfoForCommandHashCode;

        public boolean isPendingOnEmailCreation()
        {
            return pendingOnEmailCreation;
        }

        public boolean isPendingOnLearning()
        {
            return internalLearningStateMode == InternalLearningStateMode.pendOnLearning;
        }

        public boolean isInLearningMode()
        {
            return internalLearningStateMode == InternalLearningStateMode.learning;
        }

        public void pendOnEmailCreation()
        {
            pendingOnEmailCreation = true;
        }

        public void pendOnLearning()
        {
            internalLearningStateMode = InternalLearningStateMode.pendOnLearning;
        }

        public void reset()
        {
            internalLearningStateMode = InternalLearningStateMode.none;
            pendingOnEmailCreation = false;
            expressionsLearnt = new LinkedList<>();
            failCount = 0;
        }

        public String startLearning()
        {
            internalLearningStateMode = InternalLearningStateMode.learning;
            return lastCommandOrLearningCommand;
        }

        public void userGaveCommand(InfoForCommand infoForCommand, boolean success)
        {
            pendingOnEmailCreation = false;
            if (internalLearningStateMode == InternalLearningStateMode.learning)
            {
                //this saves the infoForCommand hashcode so it uses each expression only once.
                //TODO: not the best way to do this, since some actions in the same expression may succeed and some not. also what if the user end the last command with "and that's it"?
                if (success)
                {
                    if(infoForCommand.hashCode() != lastInfoForCommandHashCode)
                    {
                        lastInfoForCommandHashCode = infoForCommand.hashCode();
                        expressionsLearnt.add(infoForCommand.expression);
                    }
                }
                else //if failed need to remove current expression from list of expressions
                {
                    if(infoForCommand.hashCode() == lastInfoForCommandHashCode)
                    {
                        lastInfoForCommandHashCode = 0;
                        expressionsLearnt.remove(expressionsLearnt.size() - 1); //remove last
                    }
                    failCount++;
                }
            }
            else
            {
                lastCommandOrLearningCommand = infoForCommand.userSentence;
            }
        }

        public List<Expression2> endLearningGetSentences()
        {
            internalLearningStateMode = InternalLearningStateMode.none;
            List<Expression2> userSentences = expressionsLearnt;
            expressionsLearnt = new LinkedList<>();
            return userSentences;
        }

        public boolean learnedSomething()
        {
            return expressionsLearnt.size() > 0;
        }

        public boolean shouldFailLearning()
        {
            if (failCount >= 3 && expressionsLearnt.size() == 0)
            {
                reset();
                return true;
            }
            return false;
        }

        public boolean userHavingTrouble()
        {
            if (failCount >= 4 || (failCount >= 2 && expressionsLearnt.size() < failCount - 1))
                return true;
            return false;
        }

        public boolean isLearningForTooLong()
        {
            return expressionsLearnt.size() + failCount > aLotOfExpressions;
        }

    }

    InternalState internalState;
    String currentConcept = ""; //in use when update concept.

    @Override
    public ActionResponse sendEmail(InfoForCommand infoForCommand)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        outEmailCommandController.sendEmail(executionStatus);
        ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
        if (executionStatus.isError())
        {
            if (!outEmailCommandController.isAnEmailBeingComposed())
            {
                return TextFormattingUtils.noEmailFound(internalState);
            }
        }


        StringBuilder response = new StringBuilder();
        return TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                Optional.of("Email sent successfully."),
                false,//true,
                internalState);
    }

    public ActionResponse yes(InfoForCommand infoForCommand)
    {
        if (internalState.isPendingOnEmailCreation())
        {
            //the following code replaces the "yes" command with the "create email" command for learning purposes
            //not elegant at all...
            if (internalState.isInLearningMode() && infoForCommand.expression.toString().equals(yesExpression))
            {
                infoForCommand.expression = ExpressionParser.expression2().parseSingleExpression(createEmailExpression);
            }
            //composeEmail(infoForCommand);
            return createInstanceByConceptName(infoForCommand, OutgoingEmail.strOutgoingEmailTypeAndName);
        }
        else if (internalState.isPendingOnLearning())
        {
            String lastCommand = internalState.startLearning();
            return new ActionResponse("Great! When you say, for example: \"" + lastCommand + "\", what shall I do first?", true, Optional.empty());
        }
        else
        {
            return new ActionResponse("I did not understand what you said yes to, please give the full request.", false, Optional.empty());
        }
    }

    @Override
    public ActionResponse no(InfoForCommand infoForCommand)
    {
        return new ActionResponse("Ok, I won't do anything.", true, Optional.empty());
    }

    /*
        stop learning.
     */
    @Override
    public ActionResponse cancel(InfoForCommand infoForCommand)
    {
        if (internalState.isInLearningMode())
        {
            internalState.reset();
            return new ActionResponse("Ok, I won't learn it.", true, Optional.empty());
        }
        return new ActionResponse("There is nothing that I can cancel.", true, Optional.empty());
    }

    @Override
    public ActionResponse getInstance(InfoForCommand infoForCommand, String conceptName, String instanceName)
    {
        //instanceName = AliasMapping.instanceNameMapping(instanceName);
        if (inboxCommandController.isInboxInstanceName(instanceName))
        {
            conceptName = IncomingEmail.incomingEmailType; //make sure is asking for the right concept
            instanceName = inboxCommandController.addCounterToEmailMessageIdIfRequired(instanceName);
        }

        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<GenericInstance> instance = instanceContainer.getInstance(executionStatus, conceptName, instanceName);
        if (!instance.isPresent())
        {
            if (conceptName.equals(IncomingEmail.incomingEmailType)) //this might happen if for some reason didn't use current inbox message
            {
                instanceName = inboxCommandController.addCounterToEmailMessageIdIfRequired(InboxCommandController.emailMessageNameStart);
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
        ActionResponse actionResponse = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                Optional.of("Got instance \"" + instanceName + "\" of concept \"" + conceptName + "\"."),
                false,
                internalState);
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

        ActionResponse actionResponse = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                Optional.of("Got field \"" + fieldName + "\" from instance \"" + instance.getName() + "\"."),
                false,
                internalState);
        if (actionResponse.isSuccess())
        {
            actionResponse.addField(field.get());
        }
        return actionResponse;
    }

    @Override
    public ActionResponse getProbInstanceByName(InfoForCommand infoForCommand, String instanceName)
    {
        boolean mutableOnly = false; //it seems that Instance is always immutable.
        if (instanceName.equals(ambiguousEmailInstanceName)) //if got ambiguous "email" instance, select the better choice according to mutability.
        {
            if (mutableOnly)
                instanceName = OutgoingEmail.strOutgoingEmailTypeAndName;
            else
                instanceName = InboxCommandController.emailMessageNameStart;
        }

        if (inboxCommandController.isInboxInstanceName(instanceName))
            instanceName = inboxCommandController.addCounterToEmailMessageIdIfRequired(instanceName);

        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<GenericInstance> instance = getMostPlausibleInstance(executionStatus, Optional.of(instanceName), Optional.empty(), false);

        ActionResponse actionResponse = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                Optional.of("Got instance \"" + instanceName + "\"."),
                false,
                internalState);
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
        if (instanceName.isPresent() && instanceName.get().equals(ambiguousEmailInstanceName)) //if got ambiguous "email" instance, select the better choice according to mutability.
        {
            if (mutableOnly)
                instanceName = Optional.of(OutgoingEmail.strOutgoingEmailTypeAndName);
            else
                instanceName = Optional.of(inboxCommandController.addCounterToEmailMessageIdIfRequired(InboxCommandController.emailMessageNameStart));
        }

        if (instanceName.isPresent() && inboxCommandController.isInboxInstanceName(instanceName.get()))
        {
            if (mutableOnly) //inbox is not mutable, so user probably wanted outgoing message. remove it, and let the system decide what to use.
                instanceName = Optional.empty();
            else
            {
                //instanceName = Optional.of(AliasMapping.instanceNameMapping(instanceName.get()));
                instanceName = Optional.of(inboxCommandController.addCounterToEmailMessageIdIfRequired(instanceName.get()));
            }
        }

        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<GenericInstance> instance = getMostPlausibleInstance(executionStatus, instanceName, Optional.of(fieldName), mutableOnly);
        if (instance.isPresent() && instance.get().getConceptName().equals(OutgoingEmail.strOutgoingEmailTypeAndName)&& !instanceName.isPresent() && !mutableOnly) //since the user didn't need mutable, and didn't explicitly mention the outgoing email, he probably wants the inbox
        {
            instanceName = Optional.of(inboxCommandController.addCounterToEmailMessageIdIfRequired(InboxCommandController.emailMessageNameStart));
            instance = getMostPlausibleInstance(executionStatus, instanceName, Optional.of(fieldName), mutableOnly);
        }
        Optional<FieldHolder> field = Optional.empty();
        String successStr = "";
        if (instance.isPresent())
        {
            field = instance.get().getField(executionStatus, fieldName);
            if (field.isPresent())
                successStr = "Got field \"" + fieldName + "\" from instance \"" + field.get().getParentInstanceName() + "\".";
        }

        ActionResponse actionResponse = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                Optional.of(successStr),
                false,
                internalState);
        if (actionResponse.isSuccess())
        {
            actionResponse.addField(field.get());
        }
        else
        {
            //if failed, but is trying to set to outgoing_email (mutableOnly==true), and there is no email being composed, ask if would like to create a new email
            if (mutableOnly && !outEmailCommandController.isAnEmailBeingComposed() &&
                    ((instanceName.isPresent() && instanceName.get().equals(OutgoingEmail.strOutgoingEmailTypeAndName))
                            || conceptContainer.findConceptsForField(new ExecutionStatus(), fieldName, mutableOnly).contains(OutgoingEmail.strOutgoingEmailTypeAndName)))
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

    private ActionResponse failWithMessage(InfoForCommand infoForCommand, String sentence)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        executionStatus.add(ExecutionStatus.RetStatus.error, sentence);

        return TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                Optional.empty(), //will fail for sure
                false,
                internalState);
    }

    @Override
    public ActionResponse evalField(InfoForCommand infoForCommand, FieldHolder field)
    {
        JSONObject requestedField;

        requestedField = field.getFieldVal();

        ActionResponse actionResponse = TextFormattingUtils.testOkAndFormat(infoForCommand,
                new ExecutionStatus(),
                true,
                true,
                Optional.of("It is: " + FieldHolder.fieldFromJSonForUser(requestedField)),
                false,//changed to false, but this might be ok being true, (all other except unknownCommand are false.)
                internalState
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

        return TextFormattingUtils.testOkAndFormat(infoForCommand,
                new ExecutionStatus(),
                true,
                true,
                Optional.of(instanceContent.toString()),
                false, //shouldn't fail
                internalState
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

    @Override
    public ActionResponse setFieldWithMissingArg(InfoForCommand infoForCommand, FieldHolder field)
    {
        return failWithMessage(infoForCommand, "I don't know what to set the \"" + field.getFieldName() + "\" field in \"" + field.getParentInstanceName() +
                "\" to. Please repeat and tell me what to set it to (e.g. set " + field.getParentInstanceName() + "'s " + field.getFieldName() + " to something)");
    }


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


    private Optional<GenericInstance> getMostPlausibleInstance(ExecutionStatus executionStatus, Optional<String> optionalInstanceName, Optional<String> fieldName, boolean mutableOnly)
    {
        //find intersection of all instances that have requested instanceName and fieldName
        List<String> conceptOptions;
        if (fieldName.isPresent())
        {
            conceptOptions = conceptContainer.findConceptsForField(executionStatus, fieldName.get(), mutableOnly);
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
            return new ActionResponse("I don't know what I should set it to, please say set "+theField.getParentInstanceName()+"'s " + theField.getFieldName()  + " to something", false, Optional.empty()); //TODO: again, what if learning
        }

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

        return TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                Optional.of(successStr),
                false,//Don't want to teach, since it might keep failing, by parsing again to the same original command
                internalState);

    }

    @Override
    public ActionResponse defineConcept(InfoForCommand infoForCommand, String newConceptName)
    {
        //TODO: remember what was the last concept defined, and add fields to is if no concept is given.
        ExecutionStatus executionStatus = new ExecutionStatus();
        conceptContainer.defineConcept(executionStatus, newConceptName);

        ActionResponse actionResponse = TextFormattingUtils.testOkAndFormat(infoForCommand,
                        executionStatus,
                        true,
                        true,
                        Optional.of("Concept \"" + newConceptName + "\" was defined successfully. Please add fields to it."),
                        false,
                        internalState);
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

        ActionResponse actionResponse = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                Optional.of("Field \"" + fieldName + "\" was added to concept \"" + conceptName + "\"."),
                false,
                internalState);
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

        ActionResponse actionResponse = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                Optional.of("Field \"" + fieldName + "\" was removed from concept \"" + conceptName + "\"."),
                false,
                internalState);
        if (actionResponse.isSuccess())
        {
            //if this field doesn't appear at any other concept
            if (conceptContainer.findConceptsForField(new ExecutionStatus(), fieldName, false).size() == 0)
                commandsToParser.removeField(fieldName);
        }
        return actionResponse;
    }

    @Override
    public ActionResponse createInstanceByConceptName(InfoForCommand infoForCommand, String conceptName)
    {
        if (conceptName.equals(OutgoingEmail.strOutgoingEmailTypeAndName))
        {
            return createNewEmail(infoForCommand, conceptName);
        }
        return failWithMessage(infoForCommand, "creating an instance of \"" + conceptName + "\" requires a name (please repeat command and provide a name)");
    }

    private ActionResponse createNewEmail(InfoForCommand infoForCommand, String conceptName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        outEmailCommandController.createNewEmail(executionStatus);
        List<String> emailFieldNames = outEmailCommandController.changeToRelevantComposedEmailFields(conceptContainer.getFields(conceptName));

        return TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                Optional.of("Composing new email. " + "\"" + conceptName + "\" fields are: " + userFriendlyList(emailFieldNames) + "."),
                false,//true,
                internalState);
    }

    @Override
    public ActionResponse createInstanceByFullNames(InfoForCommand infoForCommand, String conceptName, String newInstanceName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (conceptName.equals(OutgoingEmail.strOutgoingEmailTypeAndName))
        {
            return createNewEmail(infoForCommand, conceptName);
        }
        instanceContainer.addInstance(executionStatus, conceptName, newInstanceName, true);

        ActionResponse actionResponse = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                Optional.of("Instance \"" + newInstanceName + "\" (of concept \"" + conceptName + "\") was created. " + listFieldsOfConcept(conceptName)), //listFieldsOfConcept is safe.
                false,
                internalState);
        if (actionResponse.isSuccess())
        {
            commandsToParser.newInstanceDefined(newInstanceName);
        }
        return actionResponse;
    }


    /**
     * safe call.
     * @param conceptName
     * @return  returns empty string if concept does not exist
     */
    private String listFieldsOfConcept(String conceptName)
    {
        if (conceptContainer.doesConceptExist(conceptName))
            return "\"" + conceptName + "\" fields are: " + userFriendlyList(conceptContainer.getFields(conceptName)) + ".";
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
        ExecutionStatus executionStatus = new ExecutionStatus();
        executionStatus.add(ExecutionStatus.RetStatus.error, "I don't understand");

        return TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                Optional.empty(), //will fail anyway, because added error above.
                true,
                internalState);
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
            commandsToParser.addTrainingEg(commandBeingLearnt, commandsLearnt);
            return new ActionResponse("I now know what to do when you say (for example): \"" + commandBeingLearnt + "\"!", true, Optional.empty());
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
            return new ActionResponse("I'm not sure what to do, if you want to learn \"" + internalState.lastCommandOrLearningCommand + "\", say \"end\" and if you want me to cancel say \"cancel\".", false, Optional.empty());
        }
        return cancel(infoForCommand); //didn't learn anything anyway.
    }


    @Override
    public ActionResponse undefineConcept(InfoForCommand infoForCommand, String conceptName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        if (conceptName.equals(IncomingEmail.incomingEmailType)|| conceptName.equals(ambiguousEmailInstanceName) || conceptName.equals(OutgoingEmail.strOutgoingEmailTypeAndName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "emails concepts are built-in and cannot be undefined");
        }
        if (executionStatus.noError())
        {
            conceptContainer.undefineConcept(executionStatus, conceptName);
            //delete all instances
            instanceContainer.conceptUndefined(conceptName);
        }

        ActionResponse actionResponse = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                Optional.of("concept \"" + conceptName + "\" was undefined and all its instances were deleted."), //TODO: very harsh, should ask if sure before doing this.
                false,
                internalState);
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
        if (instance.getConceptName().equals(IncomingEmail.incomingEmailType)|| conceptName.equals(ambiguousEmailInstanceName))
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "emails in the inbox can't be deleted. Instead move to the next email (say \"next email\")");
        }
        if (executionStatus.noError())
            instanceContainer.deleteInstance(executionStatus, instance);

        ActionResponse actionResponse = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                Optional.of("Instance \"" + instanceName + "\" of concept \"" + conceptName + "\" was deleted."),
                false,
                internalState);
        if (actionResponse.isSuccess())
        {
            //if this field doesn't appear at any other concept
            if (!instanceContainer.getMostPlausibleInstance(new ExecutionStatus(),conceptContainer.getAllConceptNames(),Optional.of(instanceName),false).isPresent())
                commandsToParser.removeInstance(instanceName);
        }
        return actionResponse;
    }


    @Override
    public ActionResponse nextEmailMessage(InfoForCommand infoForCommand)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        inboxCommandController.setToNextEmail(executionStatus);

        return TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                Optional.of("Set to next incoming email successfully."),
                false,
                internalState);
    }

    @Override
    public ActionResponse previousEmailMessage(InfoForCommand infoForCommand)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        inboxCommandController.setToPrevEmail(executionStatus);

        return TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                Optional.of("Set to previous incoming email successfully."),
                false,
                internalState);
    }

    @Override
    public ActionResponse replyTo(InfoForCommand infoForCommand)
    {
        String userSaid =infoForCommand.userSentence.toLowerCase();
        if (userSaid.equals("hi") || userSaid.startsWith("hello"))
            return new ActionResponse("Sorry, but I don't do small-talk. Please give me a command.", true, Optional.empty());
        return new ActionResponse("Don't know what to say", false, Optional.empty());
    }
}
