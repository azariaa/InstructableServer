package instructable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
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
        String responseToSend = "";
        switch (action)
        {
            case getTasksInfoStr:
                ExperimentTaskController experimentTaskController = getUserTasks(gameId);
                Integer score = experimentTaskController.getGameScore();
                String currentTask = experimentTaskController.getCurrentTaskText();
                String recentTask = experimentTaskController.getRecentTaskName();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(gameScoreStr,score);
                jsonObject.put(gameTaskStr,currentTask);
                jsonObject.put(recentTaskCompletedStr,recentTask);
                responseToSend = jsonObject.toJSONString();
                break;
            case newGameJoinedStr:
                ExperimentTaskController experimentTaskControl = newGameStarted(gameId);
                agentDataAndControl.addNewGame(gameId,experimentTaskControl,experimentTaskControl);
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

    private ExperimentTaskController getUserTasks(String gameId)
    {
        ExperimentTaskController userScores = null;
        synchronized(mapLock)
        {
               userScores = missionsCompletedByUser.get(gameId);
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
