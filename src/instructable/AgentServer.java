package instructable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created by Amos Azaria on 28-May-15.
 * Not in use in (MTurk) experiment, for real use.
 */
public class AgentServer implements HttpHandler
{
    AgentDataAndControl agentDataAndControl;
    static public final String userSaysParam = "userSays";
    static public final String gameIdParam = "gameId";

    AgentServer(AgentDataAndControl agentDataAndControl)
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
            String systemReply = null;
            if (!parameters.containsKey(gameIdParam))
            {
                agentDataAndControl.logger.warning("no gameId");
                return;
            }
            String gameId = parameters.get(gameIdParam).toString();
            if (parameters.containsKey(userSaysParam))
            {
                try
                {
                    String userSays = parameters.get(userSaysParam).toString();
                    systemReply = agentDataAndControl.executeSentenceForUser(gameId, userSays);
                } catch (Exception ex)
                {
                    agentDataAndControl.logger.log(Level.SEVERE, "an exception was thrown", ex);
                    systemReply = "Sorry, but I got some error...";
                }
            }
            else
            {
                agentDataAndControl.logger.warning("GameID:" + gameId + ". User has no " + userSaysParam);
                systemReply = "Hello, how can I help you?";
            }
            agentDataAndControl.logger.info("GameID:" + gameId + ". System reply: " + systemReply);
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
