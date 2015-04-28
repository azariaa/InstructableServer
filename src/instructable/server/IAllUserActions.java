package instructable.server;

import instructable.server.hirarchy.fieldTypes.PossibleFieldType;
import org.json.simple.JSONObject;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public interface IAllUserActions
{
    ActionResponse sendEmail(String usersText);

    ActionResponse composeEmail(String usersText);

    ActionResponse yes(String usersText);

    ActionResponse no(String usersText);

    ActionResponse cancel(String usersText);

    ActionResponse set(String usersText, String fieldName, String val);

    ActionResponse set(String usersText, String instanceName, String fieldName, String val);

    ActionResponse set(String usersText, String conceptName, String instanceName, String fieldName, String val);

    ActionResponse set(String usersText, String fieldName, JSONObject jsonVal);

    ActionResponse set(String usersText, String instanceName, String fieldName, JSONObject jsonVal);

    ActionResponse set(String usersText, String conceptName, String instanceName, String fieldName, JSONObject jsonVal);

    ActionResponse setFromPreviousGet(String usersText, String fieldName);

    ActionResponse setFromPreviousGet(String usersText, String instanceName, String fieldName);

    ActionResponse setFromPreviousGet(String usersText, String conceptName, String instanceName, String fieldName);

    ActionResponse add(String usersText, String fieldName, String val);

    ActionResponse addToBeginning(String usersText, String fieldName, String val);

    ActionResponse defineConcept(String usersText, String conceptName);

    ActionResponse addFieldToConcept(String usersText, String fieldName);

    //I will try to learn field type and isList from the fieldName?
    ActionResponse addFieldToConcept(String usersText, String conceptName, String fieldName);

    ActionResponse addFieldToConcept(String usersText, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList);

    ActionResponse createInstance(String usersText, String conceptName, String instanceName);

    ActionResponse createInstance(String usersText, String instanceName);

    ActionResponse setFieldTypeKnownConcept(String usersText, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList);

    ActionResponse setFieldType(String usersText, String fieldName, PossibleFieldType possibleFieldType, boolean isList);

    ActionResponse unknownCommand(String usersText);

    ActionResponse endTeaching(String usersText); // e.g. "that's it"

    //the get functions return an actual value in the ActionResponse.value that can be further used.
    ActionResponse get(String usersText, String fieldName);

    ActionResponse get(String usersText, String instanceName, String fieldName);

    ActionResponse get(String usersText, String conceptName, String instanceName, String fieldName);

    //read current email etc.
    ActionResponse getFullInstance(String usersText, String instanceName);
    ActionResponse getFullInstance(String usersText, String conceptName, String instanceName);

    ActionResponse deleteConcept(String usersText, String conceptName);
    ActionResponse deleteInstance(String usersText, String instanceName);
    ActionResponse deleteInstance(String usersText, String conceptName, String instanceName);

    ActionResponse nextEmailMessage(String usersText);
    ActionResponse previousEmailMessage(String usersText);


    //future work: ActionResponse undoLastAction(String usersText);

}
