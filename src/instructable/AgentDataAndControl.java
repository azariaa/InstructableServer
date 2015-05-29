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

    public String executeSentenceForUser(String gameId, String userSays)
    {
        if (!allUserActionsMap.containsKey(gameId))
        {
            TopDMAllActions topDMAllActions = new TopDMAllActions(new CommandsToParser(parserSettings));
            EnvironmentCreatorUtils.addInboxEmails(topDMAllActions);
            allUserActionsMap.put(gameId, topDMAllActions);
        }

        CcgUtils.SayAndExpression response = CcgUtils.ParseAndEval(allUserActionsMap.get(gameId), parserSettings, userSays);
        logger.info("GameID:" + gameId + ". Lambda expression: " + response.lExpression);
        return response.sayToUser;
    }

}
