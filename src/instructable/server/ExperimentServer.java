package instructable.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Amos Azaria on 28-May-15.
 */
public class ExperimentServer implements HttpHandler, AgentDataAndControl.ResponseToUserListener
{
    static public final String gameIdParam = "gameId";
    static public final String actionParam = RealtimeAgentServer.actionParam;
    static public final String getTasksInfoStr = "getTasksInfo";
    static public final String newGameJoinedStr = "newGameJoined";
    static public final String sayToAgentStr = "sayToAgent";
    static public final String userSaysParam = "userSays";
    static public final String gameScoreStr = "gameScore";
    static public final String gameTaskStr = "gameTask";
    static public final String recentTaskCompletedStr = "recentTaskCompleted";
    static public final String resendRequested = "resendRequested";
    Logger logger;
    AgentDataAndControl agentDataAndControl;

    final Object mapLock = new Object();
    Map<String, ExperimentTaskController> missionsCompletedByUser = new HashMap<>();

    public ExperimentServer(AgentDataAndControl agentDataAndControl)
    {
        this.agentDataAndControl = agentDataAndControl;
        logger = agentDataAndControl.logger;
        agentDataAndControl.addResponseToUserListener(this);
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException
    {
        try
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
                agentDataAndControl.addNewUser(gameId, experimentTaskControl, Optional.of(experimentTaskControl), Optional.empty(), false);
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
                    jsonObject.put(gameScoreStr, score);
                    jsonObject.put(gameTaskStr, currentTaskText);
                    jsonObject.put(recentTaskCompletedStr, recentTask);
                    responseToSend = jsonObject.toString();//.toJSONString();
                    break;
                case newGameJoinedStr:
                    break;
                case sayToAgentStr:
                    try
                    {
                        if (parameters.containsKey(userSaysParam))
                        {
                            String userSays = parameters.get(userSaysParam).toString();
                            Optional<String> res = agentDataAndControl.executeSentenceForUser(gameId, gameId, gameId, new LinkedList<>(Collections.singleton(userSays)));
                            if (res.isPresent())
                                responseToSend = res.get();
                            else
                                responseToSend = RealtimeAgentServer.userNotRegistered;

                        }
                        else if (parameters.containsKey(resendRequested))
                        {
                            Optional<String> res = agentDataAndControl.getPendingResponse(gameId, gameId, gameId);
                            if (res.isPresent())
                                responseToSend = res.get();
                            else
                                responseToSend = RealtimeAgentServer.userNotRegistered;
                        }
                    } catch (Exception ex)
                    {
                        agentDataAndControl.logger.log(Level.SEVERE, "an exception was thrown", ex);
                        responseToSend = "Sorry, but I got some error...";
                    }
                    agentDataAndControl.logger.info("GameID:" + gameId + ". System reply: " + responseToSend);
                    break;
                default:
                    logger.warning("unknown action, gameId:" + gameId + ". action:" + action);
                    break;
            }
            //We should always send a response even if it's empty!
            httpExchange.sendResponseHeaders(200, responseToSend.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(responseToSend.getBytes());
            os.flush();
            os.close();
        } catch (Exception ex)
        {
            logger.log(Level.SEVERE, "an exception was thrown", ex);
        }
    }

    private Optional<ExperimentTaskController> getUserTasks(String gameId)
    {
        Optional<ExperimentTaskController> userScores = Optional.empty();
        synchronized (mapLock)
        {
            if (missionsCompletedByUser.containsKey(gameId))
                userScores = Optional.of(missionsCompletedByUser.get(gameId));
        }
        return userScores;
    }

    private ExperimentTaskController newGameStarted(String gameId)
    {
        synchronized (mapLock)
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
    public void responseSentToUser(String userId, String agentResponse, boolean success)
    {
        if (success)
        {
            ExperimentTaskController experimentTaskController;
            synchronized (mapLock)
            {
                if (missionsCompletedByUser.containsKey(userId))
                    experimentTaskController = missionsCompletedByUser.get(userId);
                else
                    return;
            }
            experimentTaskController.responseSentToUser(agentResponse);
        }
    }
}
