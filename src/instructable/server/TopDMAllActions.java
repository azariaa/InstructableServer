package instructable.server;

import instructable.server.hirarchy.*;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;

import java.util.List;

import static instructable.server.StaticUtils.userFriendlyList;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class TopDMAllActions implements IAllUserActions
{
    OutEmailCommandController dMContextAndExecution;
    ConceptContainer conceptContainer;
    InstanceContainer instanceContainer;
    ICommandsToParser commandsToParser;

    public TopDMAllActions(ICommandsToParser commandsToParser)
    {
        conceptContainer = new ConceptContainer();
        instanceContainer = new InstanceContainer(conceptContainer);
        dMContextAndExecution = new OutEmailCommandController("myemail@gmail.com", conceptContainer, instanceContainer);
        this.commandsToParser = commandsToParser;
    }

    private enum InternalState
    {
        none, pendingOnEmailCreation
    }

    InternalState internalState = InternalState.none;
    String currentConcept = null; //in use when update concept.

    @Override
    public ActionResponse sendEmail(String usersText)
    {
        checkInternalState();
        StringBuilder retSentences = new StringBuilder();
        ExecutionStatus executionStatus = new ExecutionStatus();
        dMContextAndExecution.sendEmail(executionStatus);
        ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
        if (executionStatus.isError())
        {
            if (testNoEmailBeingComposed(retSentences, statusAndMessage))
                return new ActionResponse(retSentences.toString(), null);
        }

        if (statusAndMessage.message != null)
        {
            retSentences.append("I see that " + statusAndMessage.message + ".\n");
        }
        if (executionStatus.noError())
        {
            retSentences.append("Email sent successfully.\n");
        }
        return new ActionResponse(retSentences.toString(), null);
    }

    private boolean testNoEmailBeingComposed(StringBuilder retSentences, ExecutionStatus.StatusAndMessage statusAndMessage)
    {
        if (statusAndMessage.message != null)
        {
            if (statusAndMessage.message.startsWith("there are no instances of") || statusAndMessage.message.startsWith("there is no email") || statusAndMessage.message.startsWith("there is no"))//TODO: bad bad bad!
            {
                retSentences.append("I see that there is no email being composed.\n");
                retSentences.append("Do you want to compose a new email?\n");
                internalState = InternalState.pendingOnEmailCreation;
                return true;
            }
        }
        return false;
    }

    private void checkInternalState()
    {
        if (internalState == InternalState.pendingOnEmailCreation)
            internalState = InternalState.none;
    }

    @Override
    public ActionResponse composeEmail(String usersText)
    {
        checkInternalState();
        StringBuilder retSentences = new StringBuilder();
        ExecutionStatus executionStatus = new ExecutionStatus();
        dMContextAndExecution.createNewEmail(executionStatus);

        if (executionStatus.isError())
        {
            ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
            retSentences.append("Error. I see that " + statusAndMessage.message + ".\n");
        } else
        {
            retSentences.append("Composing new email. ");
            String conceptName = OutgoingEmail.strOutgoingEmailTypeAndName;
            List<String> emailFieldNames = dMContextAndExecution.changeToRelevantComposedEmailFields(conceptContainer.getFields(conceptName));
            retSentences.append("\"" + conceptName + "\"fields are: " + userFriendlyList(emailFieldNames) + ".");
        }
        return new ActionResponse(retSentences.toString(), null);
    }


    public ActionResponse yes(String usersText)
    {
        if (internalState == InternalState.pendingOnEmailCreation)
        {
            return composeEmail(usersText);
        } else
        {
            return new ActionResponse("I did not understand what you said yes to, please give the full request.", null);
        }
    }

    @Override
    public ActionResponse no(String usersText)
    {
        internalState = InternalState.none;
        return new ActionResponse("Ok, I won't do anything.", null);
    }

    @Override
    public ActionResponse cancel(String usersText)
    {
        internalState = InternalState.none;
        return new ActionResponse("Ok, I won't do anything.", null);
    }

    @Override
    public ActionResponse set(String usersText, String fieldName, String val)
    {
        List<String> conceptOptions = conceptContainer.findConceptsForField(fieldName);
        if (conceptOptions.isEmpty())
        {
            return new ActionResponse("I am not familiar with any concept with a field \"" + fieldName + "\". Please define it first, or use a different field.", null);
        }

        ExecutionStatus executionStatus = new ExecutionStatus();
        GenericConcept theInstance = instanceContainer.getMostPlausibleInstance(executionStatus, conceptOptions);

        ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
        if (statusAndMessage.retStatus == ExecutionStatus.RetStatus.error)
        {
            return new ActionResponse("I see that " + statusAndMessage.message + ".", null);
        }

        return set(fieldName, val, theInstance);
    }

    @Override
    public ActionResponse set(String usersText, String instanceName, String fieldName, String val)
    {
        //find intersection of all instances that have requested instanceName and fieldName
        List<String> conceptOptions = conceptContainer.findConceptsForField(fieldName);
        if (conceptOptions.isEmpty())
        {
            return new ActionResponse("I am not familiar with any concept with a field \"" + fieldName + "\". Please define it first, or use a different field.", null);
        }

        ExecutionStatus executionStatus = new ExecutionStatus();
        GenericConcept theInstance = instanceContainer.getMostPlausibleInstance(executionStatus, conceptOptions, instanceName);

        ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
        if (statusAndMessage.retStatus == ExecutionStatus.RetStatus.error)
        {
            return new ActionResponse("I see that " + statusAndMessage.message + ".", null);
        }

        return set(fieldName, val, theInstance);
    }

    @Override
    public ActionResponse set(String usersText, String conceptName, String instanceName, String fieldName, String val)
    {
        if (!conceptContainer.doesConceptExist(conceptName))
        {
            return new ActionResponse("I am not familiar with the concept: \"" + conceptName + "\". Please define it first, or use a different concept.", null);
        }
        if (!conceptContainer.doesFieldExistInConcept(conceptName, fieldName))
        {
            return new ActionResponse("The concept: \"" + conceptName + "\", does not have a field \"" + fieldName + "\". Please define it first, or use a different field.", null);
        }
        GenericConcept theInstance;
        ExecutionStatus executionStatus = new ExecutionStatus();
        if ((conceptName.equals("email") || conceptName.equals("outgoing email")) &&
                (instanceName.equals("outgoing email") || instanceName.equals("composed email") || instanceName.equals("email")))
        //TODO: these rules should be done in a smarter way (maybe rely on the parser).
        {
            //only outgoing email can be manipulated
            theInstance = dMContextAndExecution.getEmailBeingComposed(executionStatus);
            ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
            StringBuilder retSentences = new StringBuilder();
            if (testNoEmailBeingComposed(retSentences, statusAndMessage))
                return new ActionResponse(retSentences.toString(), null);
        } else
        {
            theInstance = instanceContainer.getInstance(executionStatus, conceptName, instanceName);
        }
        ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
        if (statusAndMessage.retStatus == ExecutionStatus.RetStatus.error)
        {
            return new ActionResponse("I see that " + statusAndMessage.message + ".", null);
        }
        return set(fieldName, val, theInstance);
    }

    private ActionResponse set(String fieldName, String val, GenericConcept theInstance)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        theInstance.setField(executionStatus, fieldName, val);
        ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
        if (statusAndMessage.retStatus == ExecutionStatus.RetStatus.error)
        {
            return new ActionResponse("I see that " + statusAndMessage.message + ".", null);
        } else
        {
            return new ActionResponse("The \"" + fieldName + "\" field in \"" + theInstance.getName() + "\" was set to: \"" + val + "\".", null);
        }
    }

    @Override
    public ActionResponse add(String usersText, String fieldName, String val)
    {
        return null;
    }

    @Override
    public ActionResponse addToBeginning(String usersText, String fieldName, String val)
    {
        return null;
    }

    @Override
    public ActionResponse defineConcept(String usersText, String conceptName)
    {
        //TODO: remember what was the last concept defined, and add fields to is if no concept is given.
        ExecutionStatus executionStatus = new ExecutionStatus();
        conceptContainer.defineConcept(executionStatus, conceptName);

        StringBuilder response = new StringBuilder();
        if (StaticUtils.testOkAndFormat(executionStatus, true, true, response))
        {
            commandsToParser.newConceptDefined(conceptName);
            response.append("Concept \"" + conceptName + "\" was created successfully. Please define its fields.");
        }
        return new ActionResponse(response.toString(), null);
    }

    @Override
    public ActionResponse addFieldToConcept(String usersText, String fieldName)
    {
        //TODO: use timestamp (just like with the instances) to resolve ambiguity
        return null;
    }

    @Override
    public ActionResponse addFieldToConcept(String usersText, String conceptName, String fieldName)
    {
        //TODO: need to infer the field type in a smarter way...
        PossibleFieldType possibleFieldType = PossibleFieldType.multiLineString;
        boolean isList = false;
        if (fieldName.contains("email"))
            possibleFieldType = PossibleFieldType.emailAddress;
        if (conceptName.endsWith("list") || conceptName.endsWith("s")) //TODO: should use plural vs. singular not just use s!
            isList = true;

        return addFieldToConcept(usersText,conceptName,fieldName, possibleFieldType,isList);
    }

    @Override
    public ActionResponse addFieldToConcept(String usersText, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        conceptContainer.addFieldToConcept(executionStatus, conceptName, new FieldDescription(fieldName, possibleFieldType, isList));

        StringBuilder response = new StringBuilder();
        if (StaticUtils.testOkAndFormat(executionStatus, true, true, response))
        {
            commandsToParser.newConceptDefined(conceptName);
            response.append("Field \"" + fieldName + "\" was added to concept \"" + conceptName + "\".");
        }
        return new ActionResponse(response.toString(), null);
    }

    @Override
    public ActionResponse createInstance(String usersText, String conceptName, String instanceName)
    {
        ExecutionStatus executionStatus = new ExecutionStatus();
        instanceContainer.addInstance(executionStatus, conceptName, instanceName);

        StringBuilder response = new StringBuilder();
        if (StaticUtils.testOkAndFormat(executionStatus, true, true, response))
        {
            commandsToParser.newConceptDefined(conceptName);
            response.append("Instance \"" + instanceName + "\" (of concept \"" + conceptName + "\") was created. ");
            response.append(listFieldsOfConcept(conceptName));
        }
        return new ActionResponse(response.toString(), null);
    }

    private String listFieldsOfConcept(String conceptName)
    {
        return "\""+conceptName+"\" fields are: " + userFriendlyList(conceptContainer.getFields(conceptName)) + ".";
    }

    @Override
    public ActionResponse createInstance(String usersText, String instanceName)
    {
        return null;
    }

    @Override
    public ActionResponse setFieldTypeKnownConcept(String usersText, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList)
    {
        return null;
    }

    @Override
    public ActionResponse setFieldType(String usersText, String fieldName, PossibleFieldType possibleFieldType, boolean isList)
    {
        return null;
    }

    @Override
    public ActionResponse unknownCommand(String usersText)
    {
        return null;
    }

    @Override
    public ActionResponse endTeaching(String usersText)
    {
        return null;
    }

    @Override
    public ActionResponse get(String usersText, String fieldName)
    {
        return null;
    }

    @Override
    public ActionResponse get(String usersText, String instanceName, String fieldName)
    {
        return null;
    }

    @Override
    public ActionResponse get(String usersText, String conceptName, String instanceName, String fieldName)
    {
        return null;
    }

    @Override
    public ActionResponse deleteConcept(String usersText, String conceptName)
    {
        return null;
    }

    @Override
    public ActionResponse deleteInstance(String usersText, String instanceName)
    {
        return null;
    }

    @Override
    public ActionResponse deleteInstance(String usersText, String conceptName, String instanceName)
    {
        return null;
    }

    @Override
    public ActionResponse readCurrentEmail(String usersText)
    {
        return null;
    }

    @Override
    public ActionResponse nextEmailMessage(String usersText)
    {
        return null;
    }

    @Override
    public ActionResponse previousEmailMessage(String usersText)
    {
        return null;
    }
}
