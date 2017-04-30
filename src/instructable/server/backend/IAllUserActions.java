package instructable.server.backend;

import instructable.server.hirarchy.FieldHolder;
import instructable.server.hirarchy.GenericInstance;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;
import org.json.JSONObject;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public interface IAllUserActions
{
    String unknownCommandStr = "unknownCommand";
    String cancelStr = "cancel";
    String resendNewRequest = "this takes several seconds"; //TODO: not elegant that this is hard coded

    ActionResponse sendEmail(InfoForCommand infoForCommand);

    ActionResponse send(InfoForCommand infoForCommand, String instanceName);

//    ActionResponse reset(InfoForCommand infoForCommand, String instanceName);

    ActionResponse resetEmailAndPassword(InfoForCommand infoForCommand);

    ActionResponse saveCalendarEvent(InfoForCommand infoForCommand);

    ActionResponse save(InfoForCommand infoForCommand, String instanceName);

    //ActionResponse composeEmail(InfoForCommand infoForCommand);

    ActionResponse yes(InfoForCommand infoForCommand);

    ActionResponse no(InfoForCommand infoForCommand);

    ActionResponse cancel(InfoForCommand infoForCommand);

    ActionResponse getInstance(InfoForCommand infoForCommand, String conceptName, String instanceName);

    ActionResponse getFieldFromInstance(InfoForCommand infoForCommand, GenericInstance instance, String fieldName);

    //this function may either be used as the most upper level function, or result may be used later for a set, or just if the user asks for information
    //TODO: add later: ActionResponse getFieldFromPreviousInstance(InfoForCommand infoForCommand, String fieldName);

    ActionResponse getProbInstanceByName(InfoForCommand infoForCommand, String instanceName);

    ActionResponse getProbInstance(InfoForCommand infoForCommand);

    ActionResponse getProbFieldByInstanceNameAndFieldName(InfoForCommand infoForCommand, String instanceName, String fieldName);

    ActionResponse getProbFieldByFieldName(InfoForCommand infoForCommand, String fieldName);

    ActionResponse getProbMutableFieldByInstanceNameAndFieldName(InfoForCommand infoForCommand, String instanceName, String fieldName);

    ActionResponse getProbMutableFieldByFieldName(InfoForCommand infoForCommand, String fieldName);

    ActionResponse getProbFieldVal(InfoForCommand infoForCommand);

    //this function may either be used as the most upper level function, or result may be used later for a set, or just if the user asks for information
    ActionResponse evalField(InfoForCommand infoForCommand, FieldHolder field); //from FieldHolder to Json (from field to fieldVal)

    //the following functions may only be the most upper level function, results reach user
    ActionResponse readInstance(InfoForCommand infoForCommand, GenericInstance instance); //read email etc.

    ActionResponse setFieldFromString(InfoForCommand infoForCommand, FieldHolder field, String val);

    ActionResponse setFieldFromFieldVal(InfoForCommand infoForCommand, FieldHolder field, JSONObject jsonVal);

    ActionResponse setFieldWithMissingArg(InfoForCommand infoForCommand, FieldHolder field);

    ActionResponse setWhatFromString(InfoForCommand infoForCommand, String val);

    ActionResponse setWhatFromField(InfoForCommand infoForCommand, FieldHolder field);

    //ActionResponse setFieldFromPreviousEval(InfoForCommand infoForCommand, FieldHolder field);

    ActionResponse addToFieldFromString(InfoForCommand infoForCommand, FieldHolder field, String val);

    ActionResponse addToFieldFromFieldVal(InfoForCommand infoForCommand, FieldHolder field, JSONObject jsonVal);

    //ActionResponse addToFieldFromPreviousEval(InfoForCommand infoForCommand, FieldHolder field);

    ActionResponse addToFieldAtStartFromString(InfoForCommand infoForCommand, FieldHolder field, String val);

    ActionResponse addToFieldAtStartFromFieldVal(InfoForCommand infoForCommand, FieldHolder field, JSONObject jsonVal);

    ActionResponse addToWhat(InfoForCommand infoForCommand, String toAdd);

    //ActionResponse addToFieldAtStartFromPreviousEval(InfoForCommand infoForCommand, FieldHolder field);


    ActionResponse defineConcept(InfoForCommand infoForCommand, String newConceptName);

    //not sure this is needed, it is resonable to require the conceptName that is getting a new field
    ActionResponse addFieldToProbConcept(InfoForCommand infoForCommand, String newFieldName);

    //I will try to learn field type and isList from the fieldName?
    ActionResponse addFieldToConcept(InfoForCommand infoForCommand, String conceptName, String fieldName);

    ActionResponse addFieldToConceptWithType(InfoForCommand infoForCommand, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList, boolean mutable);

    ActionResponse removeFieldFromConcept(InfoForCommand infoForCommand, String conceptName, String fieldName);

    ActionResponse createInstanceByConceptName(InfoForCommand infoForCommand, String conceptName);

    ActionResponse createInstanceByFullNames(InfoForCommand infoForCommand, String conceptName, String newInstanceName);

    //doesn't sound like a good idea (though could use last used concept, or just give an error.
    ActionResponse createInstanceByInstanceName(InfoForCommand infoForCommand, String newInstanceName);

    ActionResponse setFieldTypeKnownConcept(InfoForCommand infoForCommand, String conceptName, String fieldName, PossibleFieldType possibleFieldType, boolean isList);

    ActionResponse setFieldType(InfoForCommand infoForCommand, String fieldName, PossibleFieldType possibleFieldType, boolean isList);

    ActionResponse unknownCommand(InfoForCommand infoForCommand);

    ActionResponse teachNewCommand(InfoForCommand infoForCommand);

    ActionResponse end(InfoForCommand infoForCommand); // mostly for end learning e.g. "that's it"

    ActionResponse endOrCancel(InfoForCommand infoForCommand); //user said: "stop", not sure if means "end" or "cancel"

    ActionResponse undefineConcept(InfoForCommand infoForCommand, String conceptName);

    ActionResponse deleteInstance(InfoForCommand infoForCommand, GenericInstance instance);

    ActionResponse next(InfoForCommand infoForCommand, String instanceName);

    ActionResponse previous(InfoForCommand infoForCommand, String instanceName);

    ActionResponse newest(InfoForCommand infoForCommand, String instanceName);

    ActionResponse say(InfoForCommand infoForCommand, String toSay);

    ActionResponse undo(InfoForCommand infoForCommand);
    ActionResponse removeLastLearnedCommand(InfoForCommand infoForCommand); //same as undo, but only if in learning mode
    //deleteLearnedCommand ??? how to do this?


//    ActionResponse sugExecFromJSON(InfoForCommand infoForCommand, String jsonBlock);
    ActionResponse sugExecClick(InfoForCommand infoForCommand, String filterText);
    ActionResponse sugExec(InfoForCommand infoForCommand, String actionType, String filterText, String fromLocation, String actionPrm);
    ActionResponse demonstrate(InfoForCommand infoForCommand);
    ActionResponse userHasDemonstrated(InfoForCommand infoForCommand, JSONObject json);

    ActionResponse forgetAllLearned(InfoForCommand infoForCommand);

    ActionResponse searchYoutube(InfoForCommand infoForCommand, String searchTerm);

    ActionResponse getNewsGuardian(InfoForCommand infoForCommand, String searchTerm);

    ActionResponse getAnswerToFactoid(InfoForCommand infoForCommand, String searchTerm);

    ActionResponse spell(InfoForCommand infoForCommand, String word);

    ActionResponse timeFuncs(InfoForCommand infoForCommand, String type, String arg1, String arg2, String arg3);

    ActionResponse plusAction(InfoForCommand infoForCommand, String arg1, String arg2);


    //future work: ActionResponse undoLastAction(InfoForCommand infoForCommand);

}
