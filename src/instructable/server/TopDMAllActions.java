package instructable.server;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.hirarchy.*;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;
import org.json.simple.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static instructable.server.TextFormattingUtils.userFriendlyList;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class TopDMAllActions implements IAllUserActions, IIncomingEmailControlling
{
    OutEmailCommandController dMContextAndExecution;
    ConceptContainer conceptContainer;
    InstanceContainer instanceContainer;
    ICommandsToParser commandsToParser;
    int currentIncomingEmailIdx = 0;
    static final String emailMessageNameStart = "inbox";

    Optional<JSONObject> previousFieldEval = Optional.empty();

    public TopDMAllActions(ICommandsToParser commandsToParser)
    {
        conceptContainer = new ConceptContainer();
        instanceContainer = new InstanceContainer(conceptContainer);
        dMContextAndExecution = new OutEmailCommandController("myemail@gmail.com", conceptContainer, instanceContainer);
        this.commandsToParser = commandsToParser;
        internalState = new InternalState();
        //TODO: should really have a class controlling all incoming email
        conceptContainer.defineConcept(new ExecutionStatus(), EmailMessage.emailMessageType, EmailMessage.fieldDescriptions);
    }

    @Override
    public void addEmailMessageToInbox(EmailMessage emailMessage)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        int numOfMessages = instanceContainer.getAllInstances(EmailMessage.emailMessageType).size();
        emailMessage.setName(emailMessageNameStart + numOfMessages);
        instanceContainer.addInstance(executionStatus, emailMessage);
    }

    //TODO: may want to allow to pend on function delegates.
    /*
    this class is in-charge of tracking the user's sentences especially for learning.
     */
    public static class InternalState
    {

        private static enum InternalStateMode

        {
            none, pendingOnEmailCreation, pendOnLearning, learning
        }

        private InternalStateMode internalStateMode;
        List<Expression2> expressionsLearnt = new LinkedList<>();
        String lastCommandOrLearningCommand = "";

        public boolean isPendingOnEmailCreation()
        {
            return internalStateMode == InternalStateMode.pendingOnEmailCreation;
        }

        public boolean isPendingOnLearning()
        {
            return internalStateMode == InternalStateMode.pendOnLearning;
        }
        public boolean isInLearningMode()
        {
            return internalStateMode == InternalStateMode.learning;
        }

        public void pendOnEmailCreation()
        {
            internalStateMode = InternalStateMode.pendingOnEmailCreation;
        }

        public void pendOnLearning()
        {
            internalStateMode = InternalStateMode.pendOnLearning;
        }

        public void reset()
        {
            internalStateMode = InternalStateMode.none;
        }

        public String startLearning()
        {
            internalStateMode = InternalStateMode.learning;
            return lastCommandOrLearningCommand;
        }

        public void userGaveCommand(InfoForCommand infoForCommand, boolean success)
        {
            if (internalStateMode == InternalStateMode.learning)
            {
                if (success)
                    expressionsLearnt.add(infoForCommand.expression);
            }
            else
            {
                lastCommandOrLearningCommand = infoForCommand.userSentence;
                internalStateMode = InternalStateMode.none;
            }
        }

        public List<Expression2> endLearningGetSentences()
        {
            internalStateMode = InternalStateMode.none;
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
        dMContextAndExecution.sendEmail(executionStatus);
        ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
        if (executionStatus.isError())
        {
            if (testNoEmailBeingComposed(retSentences, statusAndMessage))
                return new ActionResponse(retSentences.toString(), false);
        }


        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of("Email sent successfully."),
                true,
                internalState);

        return new ActionResponse(response.toString(), success);
    }

    private boolean testNoEmailBeingComposed(StringBuilder retSentences, ExecutionStatus.StatusAndMessage statusAndMessage)
    {
        if (statusAndMessage.message.isPresent())
        {
            if (statusAndMessage.message.get().startsWith("there are no instances of") || statusAndMessage.message.get().startsWith("there is no email") || statusAndMessage.message.get().startsWith("there is no"))//TODO: bad bad bad!
            {
                TextFormattingUtils.noEmailFound(retSentences, internalState);
                return true;
            }
        }
        return false;
    }


    public ActionResponse yes(InfoForCommand infoForCommand)
    {
        if (internalState.isPendingOnEmailCreation())
        {
            //composeEmail(infoForCommand);
            return createInstanceByConceptName(infoForCommand, OutgoingEmail.strOutgoingEmailTypeAndName);
        } else if (internalState.isPendingOnLearning())
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
        internalState.reset();
        return new ActionResponse("Ok, I'll stop.", true);
    }

    @Override
    public ActionResponse getInstance(InfoForCommand infoForCommand, String conceptName, String instanceName)
    {

        instanceName = AliasMapping.instanceNameMapping(instanceName);
        instanceName = addCounterToEmailMessageIdIfRequired(instanceName);

        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<GenericInstance> instance = instanceContainer.getInstance(executionStatus, conceptName, instanceName);
        StringBuilder response = new StringBuilder();
        if(TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of("Got instance \"" + instanceName + "\" of concept \"" + conceptName +"\"."),
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
        if(TextFormattingUtils.testOkAndFormat(infoForCommand,
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
        instanceName = addCounterToEmailMessageIdIfRequired(instanceName);

        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<GenericInstance> instance = getMostPlausibleInstance(executionStatus, Optional.of(instanceName), Optional.empty());
        StringBuilder response = new StringBuilder();
        if(TextFormattingUtils.testOkAndFormat(infoForCommand,
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
        instanceName = AliasMapping.instanceNameMapping(instanceName);
        instanceName = addCounterToEmailMessageIdIfRequired(instanceName);

        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<GenericInstance> instance = getMostPlausibleInstance(executionStatus, Optional.of(instanceName), Optional.of(fieldName));
        Optional<FieldHolder> field = Optional.empty();
        if (instance.isPresent())
        {
             field = instance.get().getField(executionStatus, fieldName);
        }
        StringBuilder response = new StringBuilder();
        if(TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of("Got field \"" + fieldName + "\" from instance \"" + instanceName + "\"."),
                false,
                internalState))
        {
            return new ActionResponse(response.toString(), true, field.get());
        }
        return new ActionResponse(response.toString(), false);
    }

    @Override
    public ActionResponse getProbFieldByFieldName(InfoForCommand infoForCommand, String fieldName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<GenericInstance> instance = getMostPlausibleInstance(executionStatus, Optional.empty(), Optional.of(fieldName));
        Optional<FieldHolder> field = Optional.empty();
        String successStr = "";
        if (instance.isPresent())
        {
            field = instance.get().getField(executionStatus, fieldName);
            if (field.isPresent())
                successStr = "Got field \"" + fieldName + "\" from instance \"" + field.get().getParentInstanceName() + "\".";
        }
        StringBuilder response = new StringBuilder();
        if(TextFormattingUtils.testOkAndFormat(infoForCommand,
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
        return new ActionResponse(response.toString(), false);
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
                true,
                internalState
        );
        if (success)
            previousFieldEval = Optional.of(requestedField);
        return new ActionResponse(response.toString(), success, requestedField);
    }

    @Override
    public ActionResponse readInstance(InfoForCommand infoForCommand, GenericInstance instance)
    {
        //TODO:
        return null;
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
    public ActionResponse setFieldFromPreviousEval(InfoForCommand infoForCommand, FieldHolder field)
    {
        return setAndAdd(infoForCommand, field, Optional.empty(), previousFieldEval, false, true);
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
    public ActionResponse addToFieldFromPreviousEval(InfoForCommand infoForCommand, FieldHolder field)
    {
        return setAndAdd(infoForCommand, field, Optional.empty(), previousFieldEval, true, true);
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

    @Override
    public ActionResponse addToFieldAtStartFromPreviousEval(InfoForCommand infoForCommand, FieldHolder field)
    {
        return setAndAdd(infoForCommand, field, Optional.empty(), previousFieldEval, true, false);
    }


    private Optional<GenericInstance> getMostPlausibleInstance(ExecutionStatus executionStatus, Optional<String> optionalInstanceName, Optional<String> fieldName)
    {
        //find intersection of all instances that have requested instanceName and fieldName
        List<String> conceptOptions;
        if (fieldName.isPresent())
        {
            conceptOptions = conceptContainer.findConceptsForField(executionStatus, fieldName.get());
        }
        else
        {
            conceptOptions = conceptContainer.getAllConceptNames();
        }

        return instanceContainer.getMostPlausibleInstance(executionStatus, conceptOptions, optionalInstanceName);
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
            theField.setFromJSon(executionStatus, jsonVal.get(), addToExisting, appendToEnd);
        }
        else
        {
            if (addToExisting)
                theField.appendTo(executionStatus, val.get(), appendToEnd);
            else
                theField.set(executionStatus, val.get());
        }

        String valForOutput;
        if (jsonVal.isPresent())
            valForOutput = FieldHolder.fieldFromJSonForUser(jsonVal.get());
        else
            valForOutput = val.get();

        String successStr = "The \"" + theField.getFieldName() + "\" field in \"" + theField.getParentInstanceName() + "\" was set to: \"" + valForOutput + "\".";
        if (addToExisting)
            successStr = "\"" + valForOutput + "\" was added to the \"" + theField.getFieldName() + "\" field in \"" +  theField.getParentInstanceName() + "\".";
        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of(successStr),
                true,
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
                Optional.of("Concept \"" + newConceptName + "\" was created successfully. Please define its fields."),
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
        if (conceptName.endsWith("list") || conceptName.endsWith("s")) //TODO: should use plural vs. singular not just use s!
            isList = true;

        return addFieldToConceptWithType(infoForCommand,conceptName,fieldName, possibleFieldType,isList);
    }

    @Override
    public ActionResponse addFieldToConceptWithType(InfoForCommand infoForCommand, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        conceptContainer.addFieldToConcept(executionStatus, conceptName, new FieldDescription(fieldName, possibleFieldType, isList));

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
            ExecutionStatus executionStatus = new ExecutionStatus();
            dMContextAndExecution.createNewEmail(executionStatus);
            StringBuilder response = new StringBuilder();
            List<String> emailFieldNames = dMContextAndExecution.changeToRelevantComposedEmailFields(conceptContainer.getFields(conceptName));
            boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                    executionStatus,
                    false,
                    true,
                    response,
                    Optional.of("Composing new email. " + "\"" + conceptName + "\" fields are: " + userFriendlyList(emailFieldNames) + "."),
                    true,
                    internalState);

            return new ActionResponse(response.toString(), success);
        }
        return new ActionResponse("Creating an instance of \"" + conceptName + "\" requires a name (please repeat command and provide a name).", false);
    }

    @Override
    public ActionResponse createInstanceByFullNames(InfoForCommand infoForCommand, String conceptName, String newInstanceName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        instanceContainer.addInstance(executionStatus, conceptName, newInstanceName);

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
        return "\""+conceptName+"\" fields are: " + userFriendlyList(conceptContainer.getFields(conceptName)) + ".";
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
    public ActionResponse endLearning(InfoForCommand infoForCommand)
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
            return new ActionResponse("I now know what to do when you say (for example): \"" + commandBeingLearnt +"\"!", true);
        }
        return new ActionResponse("I'm afraid that I didn't learn anything.", false);

    }


    private String addCounterToEmailMessageIdIfRequired(String instanceName)
    {
        if (instanceName.equals(emailMessageNameStart))
            return emailMessageNameStart + currentIncomingEmailIdx;
        return instanceName;
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
        //if (instanceContainer.hasInstance(currentIncomingEmailIdx)
        //TODO: check if has next email, if so advance counter, otherwise return error.
        return null;
    }

    @Override
    public ActionResponse previousEmailMessage(InfoForCommand infoForCommand)
    {
        //TODO: check if has previous email, if so reduce counter, otherwise return error.
        return null;
    }
}
