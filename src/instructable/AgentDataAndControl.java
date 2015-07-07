package instructable;

import instructable.server.*;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Amos Azaria on 21-May-15.
 */
public class AgentDataAndControl
{

    private class ParserSetAndActions
    {
        ParserSetAndActions(ParserSettings parserSettings, IAllUserActions allUserActions, IGetAwaitingResponse getAwaitingCommand)
        {
            this.parserSettings = parserSettings;
            this.allUserActions = allUserActions;
            this.getAwaitingCommand = getAwaitingCommand;
        }
        ParserSettings parserSettings;
        IAllUserActions allUserActions;
        IGetAwaitingResponse getAwaitingCommand;
    }

    ParserSettings originalParserSettings;
    final private Map<String,ParserSetAndActions> parserSetAndActionsMap;
    protected Logger logger;
    List<ResponseToUserListener> responseToUserListenerList = new LinkedList<>();

    public AgentDataAndControl(Logger logger)
    {
        this.logger = logger;
        parserSetAndActionsMap = new HashMap<>();
        logger.info("Creating environment.");
        originalParserSettings = EnvironmentCreatorUtils.createParser();
        logger.info("Agent Ready!");
    }

    public ParserSetAndActions addNewGame(String gameId, IEmailSender emailSender, IAddInboxEmails addInboxEmails)
    {
        synchronized(parserSetAndActionsMap)
        {
            if (parserSetAndActionsMap.containsKey(gameId))
                return parserSetAndActionsMap.get(gameId);
        }
        ParserSettings parserSettingsCopy = originalParserSettings.clone();
        CommandsToParser commandsToParser = new CommandsToParser(parserSettingsCopy);
        TopDMAllActions topDMAllActions = new TopDMAllActions(commandsToParser, emailSender);
        addInboxEmails.addInboxEmails(topDMAllActions);
        ParserSetAndActions parserSetAndActions =  new ParserSetAndActions(parserSettingsCopy, topDMAllActions, commandsToParser);
        synchronized(parserSetAndActionsMap)
        {
            parserSetAndActionsMap.put(gameId,parserSetAndActions);
        }
        return parserSetAndActions;
    }

    interface ResponseToUserListener
    {
        void responseSentToUser(String gameId, String agentResponse, boolean success);
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
        //setting to null in order to avoid "variable might not have been initialized"
        ParserSetAndActions parserSetAndActions = null;
        synchronized(parserSetAndActionsMap)
        {
                parserSetAndActions = parserSetAndActionsMap.get(gameId);
        }

        CcgUtils.SayAndExpression response = parserSetAndActions.parserSettings.ParseAndEval(parserSetAndActions.allUserActions, userSays);
        logger.info("GameID:" + gameId + ". Lambda expression: " + response.lExpression);
        for (ResponseToUserListener responseToUserListener : responseToUserListenerList)
        {
            responseToUserListener.responseSentToUser(gameId, response.sayToUser, response.success);
        }
        return response.sayToUser;
    }

    public String getPendingResponse(String gameId)
    {
        logger.info("GameID:" + gameId + ". Requested pending response.");
        ParserSetAndActions parserSetAndActions = null;
        synchronized(parserSetAndActionsMap)
        {
            parserSetAndActions = parserSetAndActionsMap.get(gameId);
        }
        Optional<ActionResponse> responseOptional = parserSetAndActions.getAwaitingCommand.getNSetPendingActionResponse();
        if (responseOptional.isPresent())
            return responseOptional.get().getSayToUser();
        return "";
    }

}
