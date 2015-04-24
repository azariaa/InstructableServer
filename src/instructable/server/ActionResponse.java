package instructable.server;

import org.json.simple.JSONObject;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class ActionResponse
{
    ActionResponse(String sayToUser, JSONObject value)
    {
        this.sayToUser = sayToUser;
        this.value = value;
    }
    public JSONObject value;
    public String sayToUser;
}
