package instructable.server;

import instructable.server.hirarchy.fieldTypes.PossibleFieldType;
import org.json.simple.JSONObject;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public interface IAllUserActions
{
    ActionResponse sendEmail(InfoForCommand infoForCommand);

    ActionResponse composeEmail(InfoForCommand infoForCommand);

    ActionResponse yes(InfoForCommand infoForCommand);

    ActionResponse no(InfoForCommand infoForCommand);

    ActionResponse cancel(InfoForCommand infoForCommand);

    ActionResponse set(InfoForCommand infoForCommand, String fieldName, String val);

    ActionResponse set(InfoForCommand infoForCommand, String instanceName, String fieldName, String val);

    ActionResponse set(InfoForCommand infoForCommand, String conceptName, String instanceName, String fieldName, String val);

    ActionResponse set(InfoForCommand infoForCommand, String fieldName, JSONObject jsonVal);

    ActionResponse set(InfoForCommand infoForCommand, String instanceName, String fieldName, JSONObject jsonVal);

    ActionResponse set(InfoForCommand infoForCommand, String conceptName, String instanceName, String fieldName, JSONObject jsonVal);

    ActionResponse setFromPreviousGet(InfoForCommand infoForCommand, String fieldName);

    ActionResponse setFromPreviousGet(InfoForCommand infoForCommand, String instanceName, String fieldName);

    ActionResponse setFromPreviousGet(InfoForCommand infoForCommand, String conceptName, String instanceName, String fieldName);

    ActionResponse add(InfoForCommand infoForCommand, String fieldName, String val, boolean appendToEnd);

    ActionResponse add(InfoForCommand infoForCommand, String instanceName, String fieldName, String val, boolean appendToEnd);

    ActionResponse add(InfoForCommand infoForCommand, String conceptName, String instanceName, String fieldName, String val, boolean appendToEnd);

    ActionResponse add(InfoForCommand infoForCommand, String fieldName, JSONObject jsonVal, boolean appendToEnd);

    ActionResponse add(InfoForCommand infoForCommand, String instanceName, String fieldName, JSONObject jsonVal, boolean appendToEnd);

    ActionResponse add(InfoForCommand infoForCommand, String conceptName, String instanceName, String fieldName, JSONObject jsonVal, boolean appendToEnd);

    ActionResponse addFromPreviousGet(InfoForCommand infoForCommand, String fieldName, boolean appendToEnd);

    ActionResponse addFromPreviousGet(InfoForCommand infoForCommand, String instanceName, String fieldName, boolean appendToEnd);

    ActionResponse addFromPreviousGet(InfoForCommand infoForCommand, String conceptName, String instanceName, String fieldName, boolean appendToEnd);

    ActionResponse defineConcept(InfoForCommand infoForCommand, String conceptName);

    ActionResponse addFieldToConcept(InfoForCommand infoForCommand, String fieldName);

    //I will try to learn field type and isList from the fieldName?
    ActionResponse addFieldToConcept(InfoForCommand infoForCommand, String conceptName, String fieldName);

    ActionResponse addFieldToConcept(InfoForCommand infoForCommand, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList);

    ActionResponse createInstance(InfoForCommand infoForCommand, String conceptName, String instanceName);

    ActionResponse createInstance(InfoForCommand infoForCommand, String instanceName);

    ActionResponse setFieldTypeKnownConcept(InfoForCommand infoForCommand, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList);

    ActionResponse setFieldType(InfoForCommand infoForCommand, String fieldName, PossibleFieldType possibleFieldType, boolean isList);

    ActionResponse unknownCommand(InfoForCommand infoForCommand);

    ActionResponse endTeaching(InfoForCommand infoForCommand); // e.g. "that's it"

    //the get functions return an actual value in the ActionResponse.value that can be further used.
    ActionResponse get(InfoForCommand infoForCommand, String fieldName);

    ActionResponse get(InfoForCommand infoForCommand, String instanceName, String fieldName);

    ActionResponse get(InfoForCommand infoForCommand, String conceptName, String instanceName, String fieldName);

    //read current email etc.
    ActionResponse getFullInstance(InfoForCommand infoForCommand, String instanceName);
    ActionResponse getFullInstance(InfoForCommand infoForCommand, String conceptName, String instanceName);

    ActionResponse deleteConcept(InfoForCommand infoForCommand, String conceptName);
    ActionResponse deleteInstance(InfoForCommand infoForCommand, String instanceName);
    ActionResponse deleteInstance(InfoForCommand infoForCommand, String conceptName, String instanceName);

    ActionResponse nextEmailMessage(InfoForCommand infoForCommand);
    ActionResponse previousEmailMessage(InfoForCommand infoForCommand);


    //future work: ActionResponse undoLastAction(InfoForCommand infoForCommand);

}
