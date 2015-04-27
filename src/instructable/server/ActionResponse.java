package instructable.server;

import org.json.simple.JSONObject;

import java.util.Optional;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class ActionResponse
{
    ActionResponse(String sayToUser, boolean success, Optional<JSONObject> value)
    {
        this.sayToUser = sayToUser;
        this.value = value;
        this.success = success;
    }
    public String sayToUser;
    public boolean success;
    public Optional<JSONObject> value;
}
