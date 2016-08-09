package instructable.server;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.backend.ActionResponse;
import instructable.server.backend.IAllUserActions;
import instructable.server.backend.IGetAwaitingResponse;
import instructable.server.backend.TopDMAllActions;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;
import instructable.server.senseffect.*;
import instructable.server.dal.CreateParserFromFiles;
import instructable.server.dal.EmailPassword;
import instructable.server.parser.CommandsToParser;

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
    final private Map<String, ParserSetAndActions> parserSetAndActionsMap;
    protected Logger logger;
    List<ResponseToUserListener> responseToUserListenerList = new LinkedList<>();
    boolean usePendingResponses = true;

    /**
     * @param logger
     * @param usePendingResponses if false, all executions will return only when done (relevant only for learning new commands)
     */
    public AgentDataAndControl(Logger logger, boolean usePendingResponses)
    {
        this.logger = logger;
        this.usePendingResponses = usePendingResponses;
        parserSetAndActionsMap = new HashMap<>();
        logger.info("Creating environment.");
        originalParserSettings = CreateParserFromFiles.createParser(Optional.empty());//create a general parser
        logger.info("Agent Ready!");
    }

    public ParserSetAndActions addNewUser(String userId, IEmailSender emailSender, Optional<IAddInboxEmails> addInboxEmails, Optional<IEmailFetcher> emailFetcher, boolean replaceOld)
    {
        if (!replaceOld)
        {
            synchronized (parserSetAndActionsMap)
            {
                if (parserSetAndActionsMap.containsKey(userId))
                    return parserSetAndActionsMap.get(userId);
            }
        }
        ParserSetAndActions parserSetAndActions = getParserSetAndActions(userId, emailSender, addInboxEmails, emailFetcher);
        synchronized (parserSetAndActionsMap)
        {
            parserSetAndActionsMap.put(userId, parserSetAndActions);
        }
        return parserSetAndActions;
    }

    private ParserSetAndActions getParserSetAndActions(String userId, IEmailSender emailSender, Optional<IAddInboxEmails> addInboxEmails, Optional<IEmailFetcher> emailFetcher)
    {
        ParserSettings parserSettingsCopy = originalParserSettings.createPSFromGeneralForUser(userId);
        CommandsToParser commandsToParser = new CommandsToParser(parserSettingsCopy);

        //TODO: calendar should be user's calendar.
        TopDMAllActions topDMAllActions = new TopDMAllActions("you@myworkplace.com", userId, commandsToParser, emailSender, usePendingResponses, emailFetcher, Optional.of(new RealCalendar()));
        if (addInboxEmails.isPresent())
            addInboxEmails.get().addInboxEmails(topDMAllActions);
        return new ParserSetAndActions(parserSettingsCopy, topDMAllActions, commandsToParser);
    }

    interface ResponseToUserListener
    {
        void responseSentToUser(String userId, String agentResponse, boolean success);
    }

    public void addResponseToUserListener(ResponseToUserListener responseToUserListener)
    {
        if (!responseToUserListenerList.contains(responseToUserListener))
            responseToUserListenerList.add(responseToUserListener);
        if (responseToUserListenerList.size() > 1)
            logger.warning("shouldn't happen that responseToUserListenerList.size() is: " + responseToUserListenerList.size());
    }

    public Optional<String> executeSentenceForUser(String userId, List<String> userSays)
    {
        return executeSentenceOrGetPending(userId, userSays);
    }

    public Optional<String> getPendingResponse(String userId)
    {
        return executeSentenceOrGetPending(userId, new LinkedList<>());
    }

    /**
     * @param userId
     * @param userSays set to empty list for a pending response. If there is more than one sentence, the parser will choose the one which doesn't parse to unknown command.
     * @return agent's response to user's sentence. Optional.empty, if user wasn't found.
     */
    private Optional<String> executeSentenceOrGetPending(String userId, List<String> userSays)
    {
        boolean getPendingResponse = userSays.isEmpty();
        logger.info("UserID:" + userId + ". " + (getPendingResponse ? "Requested pending response." : "User says: " + userSays.get(0)));
        ParserSetAndActions parserSetAndActions;
        synchronized (parserSetAndActionsMap)
        {
            parserSetAndActions = parserSetAndActionsMap.get(userId);
        }
        if (parserSetAndActions == null)
            return Optional.empty();
        String sayToUser = "";
        boolean success = false;
        if (getPendingResponse)
        {
            Optional<ActionResponse> responseOptional = parserSetAndActions.getAwaitingCommand.getNSetPendingActionResponse();
            if (responseOptional.isPresent())
            {
                sayToUser = responseOptional.get().getSayToUserOrExec();
                success = true;
            }
        }
        else
        {
            CcgUtils.SayAndExpression response = parserSetAndActions.parserSettings.parseAndEval(Optional.of(userId), parserSetAndActions.allUserActions, userSays);
            logger.info("UserID:" + userId + ". Lambda expression: " + response.lExpression);
            sayToUser = response.sayToUser;
            success = response.success;
        }
        for (ResponseToUserListener responseToUserListener : responseToUserListenerList)
        {
            responseToUserListener.responseSentToUser(userId, sayToUser, success);
        }
        return Optional.of(sayToUser);
    }

    public ParserSettings getParserSettingsForTestingOnly(String userId)
    {
        ParserSetAndActions parserSetAndActions;
        synchronized (parserSetAndActionsMap)
        {
            parserSetAndActions = parserSetAndActionsMap.get(userId);
        }
        return parserSetAndActions.parserSettings;
    }

    /**
     * @param userId
     * @param userSays   required, since learning needs the original sentence said by the user
     * @param expression
     */
    public void executeExpressionForTestingOnly(String userId, String userSays, Expression2 expression)
    {
        ParserSetAndActions parserSetAndActions;
        synchronized (parserSetAndActionsMap)
        {
            parserSetAndActions = parserSetAndActionsMap.get(userId);
        }
        parserSetAndActions.parserSettings.evaluate(parserSetAndActions.allUserActions, userSays, expression);
    }

    public String setNewUserUsernamePwd(String userId, String username, String encPassword, String email, String realPwd)
    {
        Optional<RealEmailOperations> emailSender = EmailPassword.getRealEmailOp(username, encPassword, email, realPwd);
        if (!emailSender.isPresent())
            return "Error setting new password";
        addNewUser(userId, emailSender.get(), Optional.empty(), Optional.of((IEmailFetcher) emailSender.get()), true); //replace if old existed
        return "email and password set successfully";
    }

    public String newUserNoPwd(String userId, String username, String encPassword)
    {
        Optional<RealEmailOperations> emailSender = EmailPassword.getRealEmailOp(username, encPassword);
        if (!emailSender.isPresent())
            return "Error getting password. Please set password again.";
        addNewUser(userId, emailSender.get(), Optional.empty(), Optional.of(emailSender.get()), false);
        return "new user added " + RealtimeAgentServer.successContains + ".";
    }

}
