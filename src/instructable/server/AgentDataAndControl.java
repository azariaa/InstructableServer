package instructable.server;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.backend.*;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;
import instructable.server.dal.CreateParserFromFiles;
import instructable.server.dal.EmailPassword;
import instructable.server.parser.CommandsToParser;
import instructable.server.senseffect.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Amos Azaria on 21-May-15.
 */
public class AgentDataAndControl
{


    private class ParserSetAndActions
    {
        ParserSetAndActions(IAllUserActions allUserActions, IGetParserSettingAndAwaitingResponse getPSAndAwaiting)
        {
            this.allUserActions = allUserActions;
            this.getPSAndAwaiting = getPSAndAwaiting;
        }

        ParserSettings getParserSettings()
        {
            return getPSAndAwaiting.getParserSettings();
        }

        IAllUserActions allUserActions;
        IGetParserSettingAndAwaitingResponse getPSAndAwaiting;
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

    public ParserSetAndActions addNewUser(String userId, IEmailSender emailSender, Optional<IAddInboxEmails> addInboxEmails, Optional<IEmailFetcher> emailFetcher, boolean replaceOld, boolean connectToDB)
    {
        if (!replaceOld)
        {
            synchronized (parserSetAndActionsMap)
            {
                if (parserSetAndActionsMap.containsKey(userId))
                    return parserSetAndActionsMap.get(userId);
            }
        }
        ParserSetAndActions parserSetAndActions = getParserSetAndActions(userId, emailSender, addInboxEmails, emailFetcher, connectToDB);
        synchronized (parserSetAndActionsMap)
        {
            parserSetAndActionsMap.put(userId, parserSetAndActions);
        }
        return parserSetAndActions;
    }

    private ParserSetAndActions getParserSetAndActions(String userId, IEmailSender emailSender, Optional<IAddInboxEmails> addInboxEmails, Optional<IEmailFetcher> emailFetcher, boolean connectToDB)
    {
        ParserSettings parserSettingsCopy = originalParserSettings.createPSFromGeneralForUser(userId);
        CommandsToParser commandsToParser = new CommandsToParser(parserSettingsCopy, Optional.of(() -> originalParserSettings.createPSFromGeneralForUser(userId)));

        //TODO: why is this: you@youremail.com
        TopDMAllActions topDMAllActions = new TopDMAllActions("you@youremail.com", userId, commandsToParser, emailSender, usePendingResponses, emailFetcher, Optional.empty(), connectToDB);
        if (addInboxEmails.isPresent())
            addInboxEmails.get().addInboxEmails(topDMAllActions);
        return new ParserSetAndActions(topDMAllActions, commandsToParser);
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

    public Optional<String> executeSentenceForUser(String userId, String username, String encPassword, List<String> userSays, Date userTime)
    {
        return executeSentenceOrGetPending(userId, username, encPassword, userSays, Optional.of(userTime));
    }

    public Optional<String> getPendingResponse(String userId, String username, String encPassword)
    {
        return executeSentenceOrGetPending(userId, username, encPassword, new LinkedList<>(), Optional.empty());
    }

    /**
     * @param userId
     * @param userSays set to empty list for a pending response. If there is more than one sentence, the parser will choose the one which doesn't parse to unknown command.
     * @return agent's response to user's sentence. Optional.empty, if user wasn't found.
     */
    private Optional<String> executeSentenceOrGetPending(String userId, String username, String encPassword, List<String> userSays, Optional<Date> userTime)
    {
        boolean getPendingResponse = userSays.isEmpty();
        logger.info("UserID:" + userId + ". " + (getPendingResponse ? "Requested pending response." : "User says: " + userSays.get(0)));
        ParserSetAndActions parserSetAndActions;
        synchronized (parserSetAndActionsMap)
        {
            parserSetAndActions = parserSetAndActionsMap.get(userId);
        }
        if (parserSetAndActions == null)
            parserSetAndActions = addUserFromDBIfHas(userId, username, encPassword);
        String sayToUser = "";
        boolean success = false;
        if (getPendingResponse)
        {
            Optional<ActionResponse> responseOptional = parserSetAndActions.getPSAndAwaiting.getNSetPendingActionResponse();
            if (responseOptional.isPresent())
            {
                sayToUser = responseOptional.get().getSayToUserOrExec();
                success = true;
            }
        }
        else
        {
            CcgUtils.SayAndExpression response = parserSetAndActions.getParserSettings().parseAndEval(Optional.of(userId), parserSetAndActions.allUserActions, userSays, userTime);
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
        return parserSetAndActions.getParserSettings();
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
        parserSetAndActions.getParserSettings().evaluate(parserSetAndActions.allUserActions, userSays, expression, Optional.empty());
    }

    public String setEmailAndPswd(String userId, String username, String encPassword, String email, String realPwd)
    {
        Optional<RealEmailOperations> emailSender = EmailPassword.getRealEmailOp(username, encPassword, email, realPwd);
        if (!emailSender.isPresent())
            return "Error setting new password";
        addNewUser(userId, emailSender.get(), Optional.empty(), Optional.of(emailSender.get()), true, true); //replace if old existed
        return "email and password set successfully. Make sure to turn on access for less secure apps at: https://www.google.com/settings/security/lesssecureapps. You can say reset email and password if you want to modify them";

    }

    public String newUserNoPwd(String userId, String username, String encPassword)
    {
        addUserFromDBIfHas(userId, username, encPassword);

        return "new user added succefully";
    }

    private ParserSetAndActions addUserFromDBIfHas(String userId, String username, String encPassword)
    {
        Optional<RealEmailOperations> realEmailOps = EmailPassword.getRealEmailOp(username, encPassword);
        //if (!emailSender.isPresent())
        //return "Error getting password. Please set password again.";
        return addNewUser(userId,
                realEmailOps.isPresent() ? realEmailOps.get() : new EmptyEmailOperations(),
                Optional.empty(),
                Optional.of(realEmailOps.isPresent() ? realEmailOps.get() : new EmptyEmailOperations()), false, true);
    }

}
