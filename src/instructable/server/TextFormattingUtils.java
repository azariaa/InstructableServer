package instructable.server;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 22-Apr-15.
 */
public class TextFormattingUtils
{
    final public static String uiListSepSymbol = ";";

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


    static public ActionResponse noEmailFound(TopDMAllActions.InternalState internalState)
    {
        return noEmail(internalState, new StringBuilder());
    }

    static public ActionResponse noEmailFound(TopDMAllActions.InternalState internalState, ActionResponse actionResponse)
    {
        return noEmail(internalState, new StringBuilder(actionResponse.onlySentenceToUser()));
    }

    static private ActionResponse noEmail(TopDMAllActions.InternalState internalState, StringBuilder response)
    {
        response.append("I see that there is no email being composed.\n");
        response.append("Do you want to compose a new email?\n");
        internalState.pendOnEmailCreation();
        return new ActionResponse(response.toString(),false, Optional.empty());
    }

}
