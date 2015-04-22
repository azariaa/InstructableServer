package instructable.server;

import com.sun.deploy.util.StringUtils;

import java.util.List;

/**
 * Created by Amos Azaria on 22-Apr-15.
 */
public class StaticUtils
{
    /*
        separates with commas and adds "and" instead of last comma.
     */
    public static String userFriendlyList(List<String> givenList)
    {
        if (givenList == null || givenList.size() == 0)
            return "none";
        String appendedFields = StringUtils.join(givenList, ", ");
        String toReplace = ",";  //replace last "," with and.
        String replacement = " and";
        int idx = appendedFields.lastIndexOf(",");
        if (idx != -1)
        {
            appendedFields = appendedFields.substring(0, idx)
                    + replacement
                    + appendedFields.substring(idx + toReplace.length(), appendedFields.length());
        }
        return appendedFields;
    }

}
