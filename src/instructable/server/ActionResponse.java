package instructable.server;

import com.google.common.base.Preconditions;
import instructable.server.hirarchy.FieldHolder;
import instructable.server.hirarchy.GenericInstance;
import org.json.JSONObject;

import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 20-Apr-15.
 * <p>
 * Holds sayToUser and success. Can also hold a value (Json), an instance and a field. Caller must check type!
 * sayToUser is only used in upper most call (or the first failure). [Though sometime, the user may expect two responses]
 */
public class ActionResponse //extends RuntimeException
{

    public static ActionResponse createFromList(List<ActionResponse> actionResponseList)
    {
        if (actionResponseList.isEmpty())
            return new ActionResponse("Error: nothing to append", false, Optional.empty());
        Optional<String> learningSentence = Optional.empty();
        StringBuilder fullResponse = new StringBuilder();
        boolean success = true;
        for (ActionResponse actionResponse : actionResponseList)
        {
            fullResponse.append(actionResponse.sayToUser);
            fullResponse.append("\n");
            if (actionResponse.learningSentence.isPresent()) //always use last one (which may be failure.
                learningSentence = actionResponse.learningSentence;
            if (!actionResponse.isSuccess())
            {
                success = false;
                break;
            }
        }

        return new ActionResponse(fullResponse.toString().trim(),success,learningSentence);
    }

    public String onlySentenceToUser()
    {
        return sayToUser;
    }

    public enum ActionResponseType
    {
        simple, value, instance, field
    }

    /**
     *
     * @param sayToUser Don't add "\n" at the end. Will add if needed.
     * @param success
     * @param learningSentence
     */
    public ActionResponse(String sayToUser, boolean success, Optional<String> learningSentence)
    {
        this.learningSentence = learningSentence;
        this.sayToUser = sayToUser;
        this.success = success;
        type = ActionResponseType.simple;
    }

    public void addValue(JSONObject value)
    {
        this.value = value;
        type = ActionResponseType.value;
    }

    public void addInstance(GenericInstance instance)
    {
        this.instance = instance;
        type = ActionResponseType.instance;
    }

    public void addField(FieldHolder field)
    {
        this.field = field;
        type = ActionResponseType.field;
    }

    public ActionResponseType getType()
    {
        return type;
    }

    public String getSayToUser()
    {
        String retVal;
        if (learningSentence.isPresent())
            retVal = sayToUser + "\n" + learningSentence.get();
        else
            retVal = sayToUser;
        return retVal + "\n";
    }

    private String sayToUser;

    public boolean isSuccess()
    {
        return success;
    }

    private Optional<String> learningSentence;

    public JSONObject getValue()
    {
        Preconditions.checkState(type == ActionResponseType.value);
        return value;
    }

    public GenericInstance getInstance()
    {
        Preconditions.checkState(type == ActionResponseType.instance);
        return instance;
    }

    public FieldHolder getField()
    {
        Preconditions.checkState(type == ActionResponseType.field);
        return field;
    }

    private ActionResponseType type;
    private boolean success;
    private JSONObject value;
    private GenericInstance instance;
    private FieldHolder field;
}
