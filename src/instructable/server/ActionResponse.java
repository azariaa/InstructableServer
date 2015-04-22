package instructable.server;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class ActionResponse
{
    ActionResponse(String sayToUser, String value)
    {
        this.sayToUser = sayToUser;
        this.value = value;
    }
    public String value;
    public String sayToUser;
}
