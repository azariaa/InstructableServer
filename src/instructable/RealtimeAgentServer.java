package instructable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import instructable.server.IAllUserActions;

import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Created by Amos Azaria on 28-May-15.
 * Not in use in (MTurk) experiment, for real use.
 */
public class RealtimeAgentServer implements HttpHandler
{
    AgentDataAndControl agentDataAndControl;
    static public final String actionParam = "action";
    static public final String actionUserSays = "actionUserSays";
    static public final String actionResendRequested = "actionResendRequested";
    static public final String actionNewRealUser = "actionNewRealUser";
    static public final String userSaysParam = "userSays";
    static public final String userIdParam = "userId";
    static public final String whenContainedSendAnotherRequest = IAllUserActions.resendNewRequest; //TODO: not elegant that this is hard coded
    static public final String username = "username";
    static public final String encPwd = "encPwd";
    static public final String email = "email";
    static public final String realPwd = "realPwd";
    static public final String successContains = "successfully"; //TODO: again hard coded
    static public final String multiAltSentenceConcat = "^";
    static public final String multiAltSentenceConcatForRegEx = "\\^";
    static public final String userNotRegistered = "user not registered";

    RealtimeAgentServer(AgentDataAndControl agentDataAndControl)
    {
        this.agentDataAndControl = agentDataAndControl;
    }

    public void handle(HttpExchange httpExchange)
    {
        try
        {
            //Map<String, String> parameters = queryToMap(httpExchange.getRequestURI().getQuery());
            Map<String, Object> parameters =
                    (Map<String, Object>) httpExchange.getAttribute(Service.ParameterFilter.parametersStr);

            if (!parameters.containsKey(actionParam))
            {
                agentDataAndControl.logger.warning("no actionParam");
                return;
            }
            String actionType = parameters.get(actionParam).toString();
            if (!parameters.containsKey(userIdParam))
            {
                agentDataAndControl.logger.warning("no userId");
                return;
            }
            String userId = parameters.get(userIdParam).toString();

            String systemReply;
            try
            {
                switch (actionType)
                {
                    case actionResendRequested:
                    {
                        Optional<String> res = agentDataAndControl.getPendingResponse(userId);
                        if (res.isPresent())
                            systemReply = res.get();
                        else
                            systemReply = userNotRegistered;
                        break;
                    }
                    case actionNewRealUser:
                        if (!(parameters.containsKey(username) && parameters.containsKey(encPwd)))
                        {
                            systemReply = "Error, missing arguments";
                        }
                        else if (parameters.containsKey(email) && parameters.containsKey(realPwd))
                        {
                            systemReply = agentDataAndControl.setNewUserUsernamePwd(userId, parameters.get(username).toString(), parameters.get(encPwd).toString(),
                                    parameters.get(email).toString(), parameters.get(realPwd).toString());
                        }
                        else
                        {
                            systemReply = agentDataAndControl.newUserNoPwd(userId, parameters.get(username).toString(), parameters.get(encPwd).toString());
                        }

                        break;
                    case actionUserSays:
                        if (parameters.containsKey(userSaysParam))
                        {
                            String userSays = parameters.get(userSaysParam).toString();
                            //if doesn't contain multiAltSentenceConcat(^) will have only a single entry
                            List<String> userSentences = Arrays.asList(userSays.split(multiAltSentenceConcatForRegEx)).stream().filter(s->!s.isEmpty()).collect(Collectors.toList());
                            Optional<String> res = agentDataAndControl.executeSentenceForUser(userId, userSentences);
                            if (res.isPresent())
                                systemReply = res.get();
                            else
                                systemReply = userNotRegistered;
                        }
                        else
                        {
                            agentDataAndControl.logger.warning("UserID:" + userId + ". User has no " + userSaysParam);
                            systemReply = "Hello, how can I help you?";
                        }
                        break;
                    default:
                        agentDataAndControl.logger.warning("UserID:" + userId + ". unknown action. action: " + actionType);
                        systemReply = "Error, no action provided.";
                        break;
                }
            } catch (Exception ex)
            {
                agentDataAndControl.logger.log(Level.SEVERE, "an exception was thrown", ex);
                systemReply = "Sorry, but I got some error...";
            }
            agentDataAndControl.logger.info("GameID:" + userId + ". System reply: " + systemReply);
            httpExchange.sendResponseHeaders(200, systemReply.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(systemReply.getBytes());
            os.flush();
            os.close();
        } catch (Exception ex)
        {
            agentDataAndControl.logger.log(Level.SEVERE, "an exception was thrown", ex);
        }
    }
}
