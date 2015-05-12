package instructable.server;

import com.google.common.base.Preconditions;
import instructable.server.hirarchy.FieldHolder;
import instructable.server.hirarchy.GenericInstance;
import org.json.simple.JSONObject;

/**
 * Created by Amos Azaria on 20-Apr-15.
 * <p>
 * Holds sayToUser and success. Can also hold a value (Json), an instance and a field. Caller must check type!
 * sayToUser is only used in upper most call (or the first failure). [Though sometime, the user may expect two responses]
 */
public class ActionResponse
{
    public enum ActionResponseType
    {
        simple, value, instance, field
    }

    ActionResponse(String sayToUser, boolean success)
    {
        this.sayToUser = sayToUser;
        this.success = success;
        type = ActionResponseType.simple;
    }

    ActionResponse(String sayToUser, boolean success, JSONObject value)
    {
        this(sayToUser, success);
        this.value = value;
        type = ActionResponseType.value;
    }

    ActionResponse(String sayToUser, boolean success, GenericInstance instance)
    {
        this(sayToUser, success);
        this.instance = instance;
        type = ActionResponseType.instance;
    }

    ActionResponse(String sayToUser, boolean success, FieldHolder field)
    {
        this(sayToUser, success);
        this.field = field;
        type = ActionResponseType.field;
    }

    public ActionResponseType getType()
    {
        return type;
    }

    public String getSayToUser()
    {
        return sayToUser;
    }

    private String sayToUser;

    public boolean isSuccess()
    {
        return success;
    }

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
