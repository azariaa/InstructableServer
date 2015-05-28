package instructable;

import instructable.server.CommandsToParser;
import instructable.server.IAllUserActions;
import instructable.server.TopDMAllActions;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by Amos Azaria on 21-May-15.
 */
public class AgentDataAndControl
{
    private ParserSettings parserSettings;
    private Map<String,IAllUserActions> allUserActionsMap;
    protected Logger logger;

    public AgentDataAndControl(Logger logger)
    {
        this.logger = logger;
        parserSettings = EnvironmentCreatorUtils.createParser();
        allUserActionsMap = new HashMap<>();
    }

    public String executeSentenceForUser(String userId, String userSays)
    {
        if (!allUserActionsMap.containsKey(userId))
        {
            TopDMAllActions topDMAllActions = new TopDMAllActions(new CommandsToParser(parserSettings));
            EnvironmentCreatorUtils.addInboxEmails(topDMAllActions);
            allUserActionsMap.put(userId, topDMAllActions);
        }

        CcgUtils.SayAndExpression response = CcgUtils.ParseAndEval(allUserActionsMap.get(userId), parserSettings, userSays);
        logger.info("UserID:" + userId + ". Lambda expression: " + response.lExpression);
        return response.sayToUser;
    }

}
