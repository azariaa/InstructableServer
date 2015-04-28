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

    Optional<JSONObject> previousGet = Optional.empty();

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
                return new ActionResponse(retSentences.toString(), false, Optional.empty());
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

        return new ActionResponse(response.toString(), success, Optional.empty());
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

    @Override
    public ActionResponse composeEmail(InfoForCommand infoForCommand)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        dMContextAndExecution.createNewEmail(executionStatus);


        StringBuilder response = new StringBuilder();
        String conceptName = OutgoingEmail.strOutgoingEmailTypeAndName;
        List<String> emailFieldNames = dMContextAndExecution.changeToRelevantComposedEmailFields(conceptContainer.getFields(conceptName));
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of("Composing new email. " + "\"" + conceptName + "\" fields are: " + userFriendlyList(emailFieldNames) + "."),
                true,
                internalState);

        return new ActionResponse(response.toString(), success, Optional.empty());
    }


    public ActionResponse yes(InfoForCommand infoForCommand)
    {
        if (internalState.isPendingOnEmailCreation())
        {
            return composeEmail(infoForCommand);
        } else if (internalState.isPendingOnLearning())
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
        internalState.reset();
        return new ActionResponse("Ok, I'll stop.", true, Optional.empty());
    }

    @Override
    public ActionResponse set(InfoForCommand infoForCommand, String fieldName, String val)
    {
        return set(infoForCommand, Optional.empty(), Optional.empty(), fieldName, Optional.of(val), Optional.empty());
    }

    @Override
    public ActionResponse set(InfoForCommand infoForCommand, String instanceName, String fieldName, String val)
    {
        return set(infoForCommand, Optional.empty(), Optional.of(instanceName), fieldName, Optional.of(val), Optional.empty());
    }

    private Optional<GenericConcept> getMostPlausibleInstance(ExecutionStatus executionStatus, Optional<String> optionalInstanceName, String fieldName)
    {
        //find intersection of all instances that have requested instanceName and fieldName
        List<String> conceptOptions = conceptContainer.findConceptsForField(executionStatus, fieldName);
        if (executionStatus.isError())
        {
            return Optional.empty();
        }

        return instanceContainer.getMostPlausibleInstance(executionStatus, conceptOptions, optionalInstanceName);
    }

    /*
        must either have val or jsonVal
     */
    private ActionResponse set(InfoForCommand infoForCommand, Optional<String> conceptName, Optional<String> instanceName, String fieldName, Optional<String> val, Optional<JSONObject> jsonVal)
    {
        if (!val.isPresent() && !jsonVal.isPresent())
        {
            return new ActionResponse("I don't know what I should set it to, please rephrase.", false, Optional.empty());
        }
        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<GenericConcept> theInstance;
        if (conceptName.isPresent()) //must also have instanceName
        {
            if (!conceptContainer.doesConceptExist(conceptName.get()))
            {
                return new ActionResponse("I am not familiar with the concept: \"" + conceptName.get() + "\". Please define it first, or use a different concept.", false, Optional.empty());
            }
            if (!conceptContainer.doesFieldExistInConcept(conceptName.get(), fieldName))
            {
                return new ActionResponse("The concept: \"" + conceptName + "\", does not have a field \"" + fieldName + "\". Please define it first, or use a different field.", false, Optional.empty());
            }

            StringBuilder response = new StringBuilder();
            if ((conceptName.equals("email") || conceptName.equals("outgoing email")) &&
                    (instanceName.equals("outgoing email") || instanceName.equals("composed email") || instanceName.equals("email")))
            //TODO: these rules should be done in a smarter way (maybe rely on the parser).
            {
                //only outgoing email can be manipulated
                Optional<OutgoingEmail> emailInstance = dMContextAndExecution.getEmailBeingComposed(executionStatus);
                if (emailInstance.isPresent())
                {
                    theInstance = Optional.of(emailInstance.get());
                }
                else
                {
                    TextFormattingUtils.noEmailFound(response, internalState);
                    return new ActionResponse(response.toString(), false, Optional.empty());
                }
            } else
            {
                theInstance = instanceContainer.getInstance(executionStatus, conceptName.get(), instanceName.get());
            }

        }
        else
        {
            theInstance = getMostPlausibleInstance(executionStatus, instanceName, fieldName);
        }

        StringBuilder response = new StringBuilder();

        if (TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.empty(),
                false,
                internalState
        ))
        {
              return set(infoForCommand, fieldName, theInstance.get(), val, jsonVal);

        }

        return  new ActionResponse(response.toString(), false, Optional.empty());

    }

    @Override
    public ActionResponse set(InfoForCommand infoForCommand, String conceptName, String instanceName, String fieldName, String val)
    {
        return set(infoForCommand, Optional.of(conceptName), Optional.of(instanceName), fieldName, Optional.of(val),Optional.empty());
    }

    @Override
    public ActionResponse set(InfoForCommand infoForCommand, String fieldName, JSONObject jsonVal)
    {
        return set(infoForCommand, Optional.empty(), Optional.empty(), fieldName, Optional.empty(), Optional.of(jsonVal));
    }

    @Override
    public ActionResponse set(InfoForCommand infoForCommand, String instanceName, String fieldName, JSONObject jsonVal)
    {
        return set(infoForCommand, Optional.empty(), Optional.of(instanceName), fieldName, Optional.empty(), Optional.of(jsonVal));
    }

    @Override
    public ActionResponse set(InfoForCommand infoForCommand, String conceptName, String instanceName, String fieldName, JSONObject jsonVal)
    {
        return set(infoForCommand, Optional.of(conceptName), Optional.of(instanceName), fieldName, Optional.empty(), Optional.of(jsonVal));
    }

    @Override
    public ActionResponse setFromPreviousGet(InfoForCommand infoForCommand, String fieldName)
    {
        return set(infoForCommand, Optional.empty(), Optional.empty(), fieldName, Optional.empty(), previousGet);
    }

    @Override
    public ActionResponse setFromPreviousGet(InfoForCommand infoForCommand, String instanceName, String fieldName)
    {
        return set(infoForCommand, Optional.empty(), Optional.of(instanceName), fieldName, Optional.empty(), previousGet);
    }

    @Override
    public ActionResponse setFromPreviousGet(InfoForCommand infoForCommand, String conceptName, String instanceName, String fieldName)
    {
        return set(infoForCommand, Optional.of(conceptName), Optional.of(instanceName), fieldName, Optional.empty(), previousGet);
    }

    /*
     must either have val or jsonVal
     */
    private ActionResponse set(InfoForCommand infoForCommand, String fieldName, GenericConcept theInstance, Optional<String> val, Optional<JSONObject> jsonVal)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        theInstance.setField(executionStatus, fieldName, val, jsonVal);

        String valForOutput;
        if (jsonVal.isPresent())
            valForOutput = FieldHolder.fieldFromJSonForUser(jsonVal.get());
        else
            valForOutput = val.get();

        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                false,
                true,
                response,
                Optional.of("The \"" + fieldName + "\" field in \"" + theInstance.getName() + "\" was set to: \"" + valForOutput + "\"."),
                true,
                internalState);

        return new ActionResponse(response.toString(), success, Optional.empty());
    }

    @Override
    public ActionResponse add(InfoForCommand infoForCommand, String fieldName, String val)
    {
        return null;
    }

    @Override
    public ActionResponse addToBeginning(InfoForCommand infoForCommand, String fieldName, String val)
    {
        return null;
    }

    @Override
    public ActionResponse defineConcept(InfoForCommand infoForCommand, String conceptName)
    {
        //TODO: remember what was the last concept defined, and add fields to is if no concept is given.
        ExecutionStatus executionStatus = new ExecutionStatus();
        conceptContainer.defineConcept(executionStatus, conceptName);

        StringBuilder response = new StringBuilder();
        boolean success =
        TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                response,
                Optional.of("Concept \"" + conceptName + "\" was created successfully. Please define its fields."),
                false,
                internalState);
        if (success)
        {
            commandsToParser.newConceptDefined(conceptName);
        }
        return new ActionResponse(response.toString(), success, Optional.empty());
    }

    @Override
    public ActionResponse addFieldToConcept(InfoForCommand infoForCommand, String fieldName)
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

        return addFieldToConcept(infoForCommand,conceptName,fieldName, possibleFieldType,isList);
    }

    @Override
    public ActionResponse addFieldToConcept(InfoForCommand infoForCommand, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList)
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
            commandsToParser.newConceptDefined(conceptName);
        }
        return new ActionResponse(response.toString(), success, Optional.empty());
    }

    @Override
    public ActionResponse createInstance(InfoForCommand infoForCommand, String conceptName, String instanceName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        instanceContainer.addInstance(executionStatus, conceptName, instanceName);

        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                response,
                Optional.of("Instance \"" + instanceName + "\" (of concept \"" + conceptName + "\") was created. "),
                false,
                internalState);
        if (success)
        {
            commandsToParser.newConceptDefined(conceptName);
            response.append(listFieldsOfConcept(conceptName));
        }
        return new ActionResponse(response.toString(), success, Optional.empty());
    }

    private String listFieldsOfConcept(String conceptName)
    {
        return "\""+conceptName+"\" fields are: " + userFriendlyList(conceptContainer.getFields(conceptName)) + ".";
    }

    @Override
    public ActionResponse createInstance(InfoForCommand infoForCommand, String instanceName)
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
        return new ActionResponse(response.toString(), success, Optional.empty());
    }

    /*
        We excecute all sentences as we go. no need to execute them again, just update the parser for next time.
     */
    @Override
    public ActionResponse endTeaching(InfoForCommand infoForCommand)
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
            return new ActionResponse("I now know what to do when you say (for example): \"" + commandBeingLearnt +"\"!", true, Optional.empty());
        }
        return new ActionResponse("I'm afraid that I didn't learn anything.", false, Optional.empty());

    }

    @Override
    public ActionResponse get(InfoForCommand infoForCommand, String fieldName)
    {
        //get field of most recently touched instance with the relevant fieldName
        return internalGet(infoForCommand, Optional.empty(), Optional.empty(), fieldName);
    }

    @Override
    public ActionResponse get(InfoForCommand infoForCommand, String instanceName, String fieldName)
    {
        return internalGet(infoForCommand, Optional.empty(), Optional.of(instanceName), fieldName);
    }

    @Override
    public ActionResponse get(InfoForCommand infoForCommand, String conceptName, String instanceName, String fieldName)
    {
        return internalGet(infoForCommand, Optional.of(conceptName), Optional.of(instanceName), fieldName);
    }

    @Override
    public ActionResponse getFullInstance(InfoForCommand infoForCommand, String instanceName)
    {
        return null;
    }

    @Override
    public ActionResponse getFullInstance(InfoForCommand infoForCommand, String conceptName, String instanceName)
    {
        return null;
    }

    private ActionResponse internalGet(InfoForCommand infoForCommand,  Optional<String> conceptName, Optional<String> instanceName, String fieldName)
    {
        instanceName = modifiedInstanceNameIfNeeded(instanceName);

        ExecutionStatus executionStatus = new ExecutionStatus();
        Optional<GenericConcept> instance;
        if (conceptName.isPresent() && instanceName.isPresent())
            instance = instanceContainer.getInstance(executionStatus, conceptName.get(), instanceName.get());
        else
        {
            instance = getMostPlausibleInstance(executionStatus, instanceName, fieldName);
        }

        JSONObject requestedField = new JSONObject();
        if (instance.isPresent())// identical to: executionStatus.noError()
        {

            requestedField = instance.get().getField(executionStatus, fieldName);
        }

        StringBuilder response = new StringBuilder();
        boolean success = TextFormattingUtils.testOkAndFormat(infoForCommand,
                executionStatus,
                true,
                true,
                response,
                Optional.of("It is: " + FieldHolder.fieldFromJSonForUser(requestedField)),
                true,
                internalState
        );
        if (success)
            previousGet = Optional.of(requestedField);
        return new ActionResponse(response.toString(), success, Optional.of(requestedField));
    }

    private Optional<String> modifiedInstanceNameIfNeeded(Optional<String> instanceName)
    {
        if (instanceName.isPresent())
        {
            if (instanceName.get().equals(emailMessageNameStart))
                return Optional.of(emailMessageNameStart + currentIncomingEmailIdx);
        }
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
