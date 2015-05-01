package instructable.server;

import instructable.server.hirarchy.FieldHolder;
import instructable.server.hirarchy.GenericInstance;
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

    //the following functions will usually be used only internally
    ActionResponse getInstance(InfoForCommand infoForCommand, String conceptName, String instanceName);

    ActionResponse getFieldFromInstance(InfoForCommand infoForCommand, GenericInstance instance, String fieldName);

    //this function may either be used as the most upper level function, or result may be used later for a set, or just if the user asks for information
    //TODO: add later: ActionResponse getFieldFromPreviousInstance(InfoForCommand infoForCommand, String fieldName);

    ActionResponse getProbInstanceByName(InfoForCommand infoForCommand, String instanceName);

    ActionResponse getProbFieldByInstanceNameAndFieldName(InfoForCommand infoForCommand, String instanceName, String fieldName);

    ActionResponse getProbFieldByFieldName(InfoForCommand infoForCommand, String fieldName);

    //this function may either be used as the most upper level function, or result may be used later for a set, or just if the user asks for information
    ActionResponse evalField(InfoForCommand infoForCommand, FieldHolder field); //from FieldHolder to Json (from field to fieldVal)

    //the following functions may only be the most upper level function, results reach user
    ActionResponse readInstance(InfoForCommand infoForCommand, GenericInstance instance); //read email etc.

    ActionResponse setFieldFromString(InfoForCommand infoForCommand, FieldHolder field, String val);

    ActionResponse setFieldFromFieldVal(InfoForCommand infoForCommand, FieldHolder field, JSONObject jsonVal);

    ActionResponse setFieldFromPreviousEval(InfoForCommand infoForCommand, FieldHolder field);

    ActionResponse addToFieldFromString(InfoForCommand infoForCommand, FieldHolder field, String val);

    ActionResponse addToFieldFromFieldVal(InfoForCommand infoForCommand, FieldHolder field, JSONObject jsonVal);

    ActionResponse addToFieldFromPreviousEval(InfoForCommand infoForCommand, FieldHolder field);

    ActionResponse addToFieldAtStartFromString(InfoForCommand infoForCommand, FieldHolder field, String val);

    ActionResponse addToFieldAtStartFromFieldVal(InfoForCommand infoForCommand, FieldHolder field, JSONObject jsonVal);

    ActionResponse addToFieldAtStartFromPreviousEval(InfoForCommand infoForCommand, FieldHolder field);


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

    ActionResponse endLearning(InfoForCommand infoForCommand); // e.g. "that's it"


    ActionResponse deleteConcept(InfoForCommand infoForCommand, String conceptName);
    ActionResponse deleteInstance(InfoForCommand infoForCommand, String instanceName);
    ActionResponse deleteInstance(InfoForCommand infoForCommand, String conceptName, String instanceName);

    ActionResponse nextEmailMessage(InfoForCommand infoForCommand);
    ActionResponse previousEmailMessage(InfoForCommand infoForCommand);


    //future work: ActionResponse undoLastAction(InfoForCommand infoForCommand);

}
