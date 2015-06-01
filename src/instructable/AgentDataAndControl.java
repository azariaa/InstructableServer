package instructable;

import instructable.server.CommandsToParser;
import instructable.server.IAllUserActions;
import instructable.server.TopDMAllActions;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by Amos Azaria on 21-May-15.
 */
public class AgentDataAndControl
{
    private class ParserSetAndActions
    {
        ParserSetAndActions(ParserSettings parserSettings, IAllUserActions allUserActions)
        {
            this.parserSettings = parserSettings;
            this.allUserActions = allUserActions;
        }
        ParserSettings parserSettings;
        IAllUserActions allUserActions;
    }

    ParserSettings originalParserSettings;
    final private Map<String,ParserSetAndActions> parserSetAndActionsMap;
    protected Logger logger;
    List<ResponseToUserListener> responseToUserListenerList = new LinkedList<>();

    public AgentDataAndControl(Logger logger)
    {
        this.logger = logger;
        parserSetAndActionsMap = new HashMap<>();
        originalParserSettings = EnvironmentCreatorUtils.createParser();
        logger.info("Agent Ready!");
    }

    public ParserSetAndActions addNewGame(String gameId)
    {
        ParserSettings parserSettingsCopy = originalParserSettings.clone();
        TopDMAllActions topDMAllActions = new TopDMAllActions(new CommandsToParser(parserSettingsCopy));
        EnvironmentCreatorUtils.addInboxEmails(topDMAllActions);
        ParserSetAndActions parserSetAndActions =  new ParserSetAndActions(parserSettingsCopy, topDMAllActions);
        synchronized(parserSetAndActionsMap)
        {
            parserSetAndActionsMap.put(gameId,parserSetAndActions);
        }
        return parserSetAndActions;
    }

    interface ResponseToUserListener
    {
        void responseSentToUser(String gameId, String agentResponse);
    }

    public void addResponseToUserListener(ResponseToUserListener responseToUserListener)
    {
        if (!responseToUserListenerList.contains(responseToUserListener))
            responseToUserListenerList.add(responseToUserListener);
        if (responseToUserListenerList.size() > 1)
            logger.warning("shouldn't happen that responseToUserListenerList.size() is: " + responseToUserListenerList.size());
    }

    public String executeSentenceForUser(String gameId, String userSays)
    {
        logger.info("GameID:" + gameId + ". User says: " + userSays);
        boolean needToAddToMap = false;
        //setting to null in order to avoid "variable might not have been initialized"
        ParserSetAndActions parserSetAndActions = null;
        synchronized(parserSetAndActionsMap)
        {
            if (parserSetAndActionsMap.containsKey(gameId))
            {
                parserSetAndActions = parserSetAndActionsMap.get(gameId);
            }
            else
            {
                logger.warning("GameId not found in map, adding now. GameId: " + gameId);
                needToAddToMap = true;
            }
        }
        if (needToAddToMap)
        {
            parserSetAndActions = addNewGame(gameId);
        }

        CcgUtils.SayAndExpression response = CcgUtils.ParseAndEval(parserSetAndActions.allUserActions, parserSetAndActions.parserSettings, userSays);
        logger.info("GameID:" + gameId + ". Lambda expression: " + response.lExpression);
        for (ResponseToUserListener responseToUserListener : responseToUserListenerList)
        {
            responseToUserListener.responseSentToUser(gameId, response.sayToUser);
        }
        return response.sayToUser;
    }

}
