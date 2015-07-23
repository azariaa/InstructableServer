package instructable.server.dal;

import org.json.JSONObject;

/**
 * Created by Amos Azaria on 23-Jul-15.
 */
public interface IFieldChanged
{
    void fieldChanged(JSONObject fieldVal);
}
