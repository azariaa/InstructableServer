package instructable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Amos Azaria on 28-May-15.
 */
public class EmailAndExperimentServer implements HttpHandler, AgentDataAndControl.ResponseToUserListener
{
    static public final String gameIdParam = "gameId";
    static public final String actionParam = "action";
    static public final String getTasksInfoStr = "getTasksInfo";
    static public final String newGameJoinedStr = "newGameJoined";
    static public final String sayToAgentStr = "sayToAgent";
    static public final String userSaysParam = "userSays";
    static public final String gameScoreStr = "gameScore";
    static public final String gameTaskStr = "gameTask";
    static public final String recentTaskCompletedStr = "recentTaskCompleted";
    Logger logger;
    AgentDataAndControl agentDataAndControl;

    final Object mapLock = new Object();
    Map<String, ExperimentTaskController> missionsCompletedByUser = new HashMap<>();

    public EmailAndExperimentServer(AgentDataAndControl agentDataAndControl)
    {
        this.agentDataAndControl = agentDataAndControl;
        logger = agentDataAndControl.logger;
        agentDataAndControl.addResponseToUserListener(this);
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException
    {
        //TODO: add all tasks!!!
        Map<String, Object> parameters =
                (Map<String, Object>) httpExchange.getAttribute(Service.ParameterFilter.parametersStr);
        if (!parameters.containsKey(gameIdParam))
        {
            logger.warning("no gameId");
            return;
        }
        String gameId = parameters.get(gameIdParam).toString();
        if (!parameters.containsKey(actionParam))
        {
            logger.warning("no action, gameId:" + gameId);
            return;
        }
        String action = parameters.get(actionParam).toString();
        Optional<ExperimentTaskController> opExperimentTaskController = getUserTasks(gameId);
        if (!opExperimentTaskController.isPresent())
        {
            if (!action.equals(newGameJoinedStr))
                logger.warning("gameId not in map, adding now. gameId:" + gameId);
            ExperimentTaskController experimentTaskControl = newGameStarted(gameId);
            agentDataAndControl.addNewGame(gameId,experimentTaskControl,experimentTaskControl);
        }
        ExperimentTaskController experimentTaskController = getUserTasks(gameId).get();
        String responseToSend = "";
        switch (action)
        {
            case getTasksInfoStr:
                Integer score = experimentTaskController.getGameScore();
                String currentTaskText = experimentTaskController.getCurrentTaskText();
                String recentTask = experimentTaskController.getRecentTaskName();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(gameScoreStr,score);
                jsonObject.put(gameTaskStr,currentTaskText);
                jsonObject.put(recentTaskCompletedStr,recentTask);
                responseToSend = jsonObject.toJSONString();
                break;
            case newGameJoinedStr:
                break;
            case sayToAgentStr:
                if (parameters.containsKey(userSaysParam))
                {
                    try
                    {
                        String userSays = parameters.get(userSaysParam).toString();
                        responseToSend = agentDataAndControl.executeSentenceForUser(gameId, userSays);
                    } catch (Exception ex)
                    {
                        agentDataAndControl.logger.log(Level.SEVERE, "an exception was thrown", ex);
                        responseToSend = "Sorry, but I got some error...";
                    }
                    agentDataAndControl.logger.info("GameID:" + gameId + ". System reply: " + responseToSend);
                }
                break;
            default:
                logger.warning("unknown action, gameId:" + gameId + ". action:" + action);
                break;
        }
        //We should always send a response even if it's empty!
        httpExchange.sendResponseHeaders(200, responseToSend.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(responseToSend.getBytes());
        os.close();
    }

    private Optional<ExperimentTaskController> getUserTasks(String gameId)
    {
        Optional<ExperimentTaskController> userScores = Optional.empty();
        synchronized(mapLock)
        {
            if (missionsCompletedByUser.containsKey(gameId))
               userScores = Optional.of(missionsCompletedByUser.get(gameId));
        }
        return userScores;
    }

    private ExperimentTaskController newGameStarted(String gameId)
    {
        synchronized(mapLock)
        {
            if (missionsCompletedByUser.containsKey(gameId))
            {
                logger.warning("gameId already in map. user:" + gameId + ".");
            }
            else
            {
                missionsCompletedByUser.put(gameId, new ExperimentTaskController(logger, gameId));
                logger.info("gameId added to map. gameId:" + gameId + ".");
            }
            return missionsCompletedByUser.get(gameId);
        }

    }



    @Override
    public void responseSentToUser(String gameId, String agentResponse)
    {
        ExperimentTaskController experimentTaskController;
        synchronized(mapLock)
        {
            if (missionsCompletedByUser.containsKey(gameId))
                experimentTaskController = missionsCompletedByUser.get(gameId);
            else
                return;
        }
        experimentTaskController.responseSentToUser(agentResponse);
    }
}
