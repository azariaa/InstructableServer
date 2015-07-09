package instructable;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;
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
    boolean usePendingResponses = true;

    /**
     *
     * @param logger
     * @param usePendingResponses if false, all executions will return only when done (relevant only for learning new commands)
     */
    public AgentDataAndControl(Logger logger, boolean usePendingResponses)
    {
        this.logger = logger;
        this.usePendingResponses = usePendingResponses;
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
        ParserSetAndActions parserSetAndActions = getParserSetAndActions(emailSender, addInboxEmails);
        synchronized(parserSetAndActionsMap)
        {
            parserSetAndActionsMap.put(gameId,parserSetAndActions);
        }
        return parserSetAndActions;
    }

    private ParserSetAndActions getParserSetAndActions(IEmailSender emailSender, IAddInboxEmails addInboxEmails)
    {
        ParserSettings parserSettingsCopy = originalParserSettings.clone();
        CommandsToParser commandsToParser = new CommandsToParser(parserSettingsCopy);
        TopDMAllActions topDMAllActions = new TopDMAllActions(commandsToParser, emailSender, usePendingResponses);
        addInboxEmails.addInboxEmails(topDMAllActions);
        return new ParserSetAndActions(parserSettingsCopy, topDMAllActions, commandsToParser);
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
        return executeSentenceOrGetPending(gameId, Optional.of(userSays));
    }

    public String getPendingResponse(String gameId)
    {
        return executeSentenceOrGetPending(gameId, Optional.empty());
    }

    /**
     *
     * @param gameId
     * @param userSays set to Optional.empty() for a pending response.
     * @return
     */
    private String executeSentenceOrGetPending(String gameId, Optional<String> userSays)
    {
        boolean getPendingResponse = !userSays.isPresent();
        logger.info("GameID:" + gameId + ". " + (getPendingResponse ? "Requested pending response." :  "User says: " + userSays.get()));
        ParserSetAndActions parserSetAndActions;
        synchronized(parserSetAndActionsMap)
        {
            parserSetAndActions = parserSetAndActionsMap.get(gameId);
        }
        String sayToUser = "";
        boolean success = false;
        if (getPendingResponse)
        {
            Optional<ActionResponse> responseOptional = parserSetAndActions.getAwaitingCommand.getNSetPendingActionResponse();
            if (responseOptional.isPresent())
            {
                sayToUser = responseOptional.get().getSayToUser();
                success = true;
            }
        }
        else
        {
            CcgUtils.SayAndExpression response = parserSetAndActions.parserSettings.parseAndEval(parserSetAndActions.allUserActions, userSays.get());
            logger.info("GameID:" + gameId + ". Lambda expression: " + response.lExpression);
            sayToUser = response.sayToUser;
            success = response.success;
        }
        for (ResponseToUserListener responseToUserListener : responseToUserListenerList)
        {
            responseToUserListener.responseSentToUser(gameId, sayToUser, success);
        }
        return sayToUser;
    }

    public ParserSettings getParserSettingsForTestingOnly(String gameId)
    {
        ParserSetAndActions parserSetAndActions;
        synchronized(parserSetAndActionsMap)
        {
            parserSetAndActions = parserSetAndActionsMap.get(gameId);
        }
        return parserSetAndActions.parserSettings;
    }

    /**
     *
     * @param gameId
     * @param userSays required, since learning needs the original sentence said by the user
     * @param expression
     */
    public void executeExpressionForTestingOnly(String gameId, String userSays, Expression2 expression)
    {
        ParserSetAndActions parserSetAndActions;
        synchronized(parserSetAndActionsMap)
        {
            parserSetAndActions = parserSetAndActionsMap.get(gameId);
        }
        parserSetAndActions.parserSettings.evaluate(parserSetAndActions.allUserActions, userSays, expression);
    }

}
