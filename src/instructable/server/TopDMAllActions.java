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

        private static enum InternalLearningStateMode

        {
            none, pendOnLearning, learning
        }

        private InternalLearningStateMode internalLearningStateMode;
        private boolean pendingOnEmailCreation;
        List<Expression2> expressionsLearnt = new LinkedList<>();
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

    }

    InternalState internalState;
    String currentConcept = ""; //in use when update concept.

    @Override
    public ActionResponse sendEmail(InfoForCommand infoForCommand)
    {
        StringBuilder retSentences = new StringBuilder();
        ExecutionStatus executionStatus = new ExecutionStatus();
        outEmailCommandController.sendEmail(executionStatus);
        ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
        if (executionStatus.isError())
        {
            if (!outEmailCommandController.isAnEmailBeingComposed())
            {
                TextFormattingUtils.noEmailFound(retSentences, internalState);
                return new ActionResponse(retSentences.toString(), false);
            }
        }


        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of("Email sent successfully."),
                false,//true,
                internalState);

        return new ActionResponse(response.toString(), success);
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
            return new ActionResponse("Great! When you say, for example: \"" + lastCommand + "\", what shall I do first?", true);
        }
        else
        {
            return new ActionResponse("I did not understand what you said yes to, please give the full request.", false);
        }
    }

    @Override
    public ActionResponse no(InfoForCommand infoForCommand)
    {
        return new ActionResponse("Ok, I won't do anything.", true);
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
            return new ActionResponse("Ok, I won't learn it.", true);
        }
        return new ActionResponse("There is nothing that I can cancel.", true);
    }

    @Override
    public ActionResponse getInstance(InfoForCommand infoForCommand, String conceptName, String instanceName)
    {
        instanceName = AliasMapping.instanceNameMapping(instanceName);
        instanceName = inboxCommandController.addCounterToEmailMessageIdIfRequired(instanceName);

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
                    TextFormattingUtils.noEmailFound(retSentences, internalState);
                    return new ActionResponse(retSentences.toString(), false);
                }
            }
        }
        StringBuilder response = new StringBuilder();
        if (TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of("Got instance \"" + instanceName + "\" of concept \"" + conceptName + "\"."),
                false,
                internalState))
        {
            return new ActionResponse(response.toString(), true, instance.get());
        }
        return new ActionResponse(response.toString(), false);
    }

    @Override
    public ActionResponse getFieldFromInstance(InfoForCommand infoForCommand, GenericInstance instance, String fieldName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<FieldHolder> field = instance.getField(executionStatus, fieldName);
        StringBuilder response = new StringBuilder();
        if (TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of("Got field \"" + fieldName + "\" from instance \"" + instance.getName() + "\"."),
                false,
                internalState))
        {
            return new ActionResponse(response.toString(), true, field.get());
        }
        return new ActionResponse(response.toString(), false);
    }

    @Override
    public ActionResponse getProbInstanceByName(InfoForCommand infoForCommand, String instanceName)
    {
        instanceName = AliasMapping.instanceNameMapping(instanceName);
        instanceName = inboxCommandController.addCounterToEmailMessageIdIfRequired(instanceName);

        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<GenericInstance> instance = getMostPlausibleInstance(executionStatus, Optional.of(instanceName), Optional.empty(), false);
        StringBuilder response = new StringBuilder();
        if (TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of("Got instance \"" + instanceName + "\"."),
                false,
                internalState))
        {
            return new ActionResponse(response.toString(), true, instance.get());
        }
        return new ActionResponse(response.toString(), false);
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
        Optional<FieldHolder> field = Optional.empty();
        String successStr = "";
        if (instance.isPresent())
        {
            field = instance.get().getField(executionStatus, fieldName);
            if (field.isPresent())
                successStr = "Got field \"" + fieldName + "\" from instance \"" + field.get().getParentInstanceName() + "\".";
        }
        StringBuilder response = new StringBuilder();
        if (TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of(successStr),
                false,
                internalState))
        {
            return new ActionResponse(response.toString(), true, field.get());
        }
        //if failed, but is trying to set to outgoing_email, ask if would like to create new email
        if (mutableOnly && (instanceName.isPresent() && instanceName.get().equals(OutgoingEmail.strOutgoingEmailTypeAndName))
                || conceptContainer.findConceptsForField(new ExecutionStatus(),fieldName,mutableOnly).contains(OutgoingEmail.strOutgoingEmailTypeAndName))
        {
            noEmailFound(response.append("\n"), internalState);
        }
        return new ActionResponse(response.toString(), false);
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
            return new ActionResponse("It is: " + FieldHolder.fieldFromJSonForUser(previousFieldEval.get()), true, previousFieldEval.get());
        return new ActionResponse("There is no previously evaluated field.", false);
    }

    @Override
    public ActionResponse evalField(InfoForCommand infoForCommand, FieldHolder field)
    {
        JSONObject requestedField;

        requestedField = field.getFieldVal();

        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                new ExecutionStatus(),
                true,
                true,
                response,
                Optional.of("It is: " + FieldHolder.fieldFromJSonForUser(requestedField)),
                false,//changed to false, but this might be ok being true, (all other except unknownCommand are false.)
                internalState
        );
        if (success)
            previousFieldEval = Optional.of(requestedField);
        return new ActionResponse(response.toString(), success, requestedField);
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
        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                new ExecutionStatus(),
                true,
                true,
                response,
                Optional.of(instanceContent.toString()),
                false, //shouldn't fail
                internalState
        );
        return new ActionResponse(response.toString(), success);
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
            return new ActionResponse("I don't know what I should set it to, please rephrase.", false);
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

        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of(successStr),
                false,//Don't want to teach, since it might keep failing, by parsing again to the same original command
                internalState);

        return new ActionResponse(response.toString(), success);
    }

    @Override
    public ActionResponse defineConcept(InfoForCommand infoForCommand, String newConceptName)
    {
        //TODO: remember what was the last concept defined, and add fields to is if no concept is given.
        ExecutionStatus executionStatus = new ExecutionStatus();
        conceptContainer.defineConcept(executionStatus, newConceptName);

        StringBuilder response = new StringBuilder();
        boolean success =
                TextFormattingUtils.testOkAndFormat(infoForCommand,
                        executionStatus,
                        true,
                        true,
                        response,
                        Optional.of("Concept \"" + newConceptName + "\" was defined successfully. Please add fields to it."),
                        false,
                        internalState);
        if (success)
        {
            commandsToParser.newConceptDefined(newConceptName);
        }
        return new ActionResponse(response.toString(), success);
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
        FieldDescription fieldDescription = new FieldDescription(fieldName, possibleFieldType, isList, mutable);
        conceptContainer.addFieldToConcept(executionStatus, conceptName, fieldDescription);
        if (executionStatus.noError())
            instanceContainer.fieldAddedToConcept(executionStatus, conceptName, fieldDescription);

        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                response,
                Optional.of("Field \"" + fieldName + "\" was added to concept \"" + conceptName + "\"."),
                false,
                internalState);
        if (success)
        {
            commandsToParser.newFieldDefined(fieldName);
        }
        return new ActionResponse(response.toString(), success);
    }

    @Override
    public ActionResponse createInstanceByConceptName(InfoForCommand infoForCommand, String conceptName)
    {
        if (conceptName.equals(OutgoingEmail.strOutgoingEmailTypeAndName))
        {
            return createNewEmail(infoForCommand, conceptName);
        }
        return new ActionResponse("Creating an instance of \"" + conceptName + "\" requires a name (please repeat command and provide a name).", false);
    }

    private ActionResponse createNewEmail(InfoForCommand infoForCommand, String conceptName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        outEmailCommandController.createNewEmail(executionStatus);
        StringBuilder response = new StringBuilder();
        List<String> emailFieldNames = outEmailCommandController.changeToRelevantComposedEmailFields(conceptContainer.getFields(conceptName));
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of("Composing new email. " + "\"" + conceptName + "\" fields are: " + userFriendlyList(emailFieldNames) + "."),
                false,//true,
                internalState);

        return new ActionResponse(response.toString(), success);
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

        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                response,
                Optional.of("Instance \"" + newInstanceName + "\" (of concept \"" + conceptName + "\") was created. "),
                false,
                internalState);
        if (success)
        {
            commandsToParser.newInstanceDefined(newInstanceName);
            response.append(listFieldsOfConcept(conceptName));
        }
        return new ActionResponse(response.toString(), success);
    }

    private String listFieldsOfConcept(String conceptName)
    {
        return "\"" + conceptName + "\" fields are: " + userFriendlyList(conceptContainer.getFields(conceptName)) + ".";
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

        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                response,
                Optional.empty(), //will fail anyway, because added error above.
                true,
                internalState);
        return new ActionResponse(response.toString(), success);
    }

    /*
        We excecute all sentences as we go. no need to execute them again, just update the parser for next time.
     */
    @Override
    public ActionResponse end(InfoForCommand infoForCommand)
    {
        if (!internalState.isInLearningMode())
        {
            return new ActionResponse("Not sure what you are talking about.", false);
        }
        String commandBeingLearnt = internalState.lastCommandOrLearningCommand;
        List<Expression2> commandsLearnt = internalState.endLearningGetSentences();

        //make sure learnt at least one successful sentence
        if (commandsLearnt.size() > 0)
        {

            commandsToParser.addTrainingEg(commandBeingLearnt, commandsLearnt);
            return new ActionResponse("I now know what to do when you say (for example): \"" + commandBeingLearnt + "\"!", true);
        }
        return new ActionResponse("I'm afraid that I didn't learn anything.", false);

    }


    @Override
    public ActionResponse deleteConcept(InfoForCommand infoForCommand, String conceptName)
    {
        return null;
    }

    @Override
    public ActionResponse deleteInstance(InfoForCommand infoForCommand, String instanceName)
    {
        return null;
    }

    @Override
    public ActionResponse deleteInstance(InfoForCommand infoForCommand, String conceptName, String instanceName)
    {
        return null;
    }


    @Override
    public ActionResponse nextEmailMessage(InfoForCommand infoForCommand)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        inboxCommandController.setToNextEmail(executionStatus);

        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                response,
                Optional.of("Set to next incoming email successfully."),
                false,
                internalState);
        return new ActionResponse(response.toString(), success);
    }

    @Override
    public ActionResponse previousEmailMessage(InfoForCommand infoForCommand)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        inboxCommandController.setToPrevEmail(executionStatus);

        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                response,
                Optional.of("Set to previous incoming email successfully."),
                false,
                internalState);
        return new ActionResponse(response.toString(), success);
    }

    @Override
    public ActionResponse replyTo(InfoForCommand infoForCommand)
    {
        String userSaid =infoForCommand.userSentence.toLowerCase();
        if (userSaid.equals("hi") || userSaid.startsWith("hello"))
            return new ActionResponse("Sorry, but I don't do small-talk. Please give me a command.", true);
        return new ActionResponse("Don't know what to day", false);
    }
}
