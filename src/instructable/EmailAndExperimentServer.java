package instructable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import instructable.server.hirarchy.EmailMessage;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by Amos Azaria on 28-May-15.
 */
public class EmailAndExperimentServer implements HttpHandler, AgentDataAndControl.ResponseToUserListener
{
    enum TasksToComplete {sendTest,forwardToAll}
    static final int numOfTasks = TasksToComplete.values().length;
    static public final String gameIdParam = "gameId";
    static public final String actionParam = "action";
    static public final String getTasksInfoStr = "getTasksInfo";
    static public final String newGameJoinedStr = "newGameJoined";
    static public final String sendEmailInGameStr = "sendEmailInGame";
    static public final String gameScoreStr = "gameScore";
    static public final String gameTaskStr = "gameTask";
    Logger logger;
    AgentDataAndControl agentDataAndControl;

    final Object mapLock = new Object();
    Map<String,Set<TasksToComplete>> missionsCompletedByUser = new HashMap<>();

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
                Set<TasksToComplete> userTasks = getUserTasks(gameId);
                Integer score = getGameScore(userTasks);
                String currentTask = getCurrentTask(userTasks);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(gameScoreStr,score);
                jsonObject.put(gameTaskStr,currentTask);
                responseToSend = jsonObject.toJSONString();
                break;
            case newGameJoinedStr:
                newGameStarted(gameId);
                break;
            case sendEmailInGameStr:
                if (!parameters.containsKey(EmailMessage.subjectStr) ||
                        !parameters.containsKey(EmailMessage.bodyStr) ||
                        !parameters.containsKey(EmailMessage.copyListStr) ||
                        !parameters.containsKey(EmailMessage.recipientListStr) ||
                        !parameters.containsKey(EmailMessage.senderStr))
                {
                    logger.warning("something is missing in the email, gameId:" + gameId);
                    break;
                }
                sendEmailForUser(gameId,
                        parameters.get(EmailMessage.subjectStr).toString(),
                        parameters.get(EmailMessage.bodyStr).toString(),
                        parameters.get(EmailMessage.copyListStr).toString(),
                        parameters.get(EmailMessage.recipientListStr).toString(),
                        parameters.get(EmailMessage.senderStr).toString()
                );
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

    private String getCurrentTask(Set<TasksToComplete> userTasks)
    {
        if (!userTasks.contains(TasksToComplete.forwardToAll))
            return "Forward to all";
        return "send email to Annie";
    }

    private int getGameScore(Set<TasksToComplete> userTasks)
    {
        return userTasks.size();
        //return (int)userScores.values().stream().filter(x -> x==true).count(); //should work, but need to test it first...
    }

    private Set<TasksToComplete> getUserTasks(String gameId)
    {
        Set<TasksToComplete> userScores = null;
        synchronized(mapLock)
        {
            if (missionsCompletedByUser.containsKey(gameId))
                userScores = missionsCompletedByUser.get(gameId);
        }
        if (userScores == null)
        {
            logger.warning("gameId not in map. user:" + gameId + ". adding it now");
            newGameStarted(gameId);
        }
        return userScores;
    }

    private void newGameStarted(String gameId)
    {
        synchronized(mapLock)
        {
            if (missionsCompletedByUser.containsKey(gameId))
            {
                logger.warning("gameId already in map. user:" + gameId + ".");
                return;
            }
            missionsCompletedByUser.put(gameId, new HashSet<>());
            logger.info("gameId added to map. gameId:" + gameId + ".");
        }

    }

    private void sendEmailForUser(String gameId, String subject, String body, String copyList, String recipientList, String sender)
    {
        Set<TasksToComplete> userTasks = getUserTasks(gameId);
        //TODO: fill out all the experiment!!!!
        boolean completedTaskforwardToAll = true;
        if (completedTaskforwardToAll)
        {
            userTasks.add(TasksToComplete.forwardToAll);
            logger.info("gameId: " + gameId + " completedTaskforwardToAll");
        }
    }

    @Override
    public void responseSentToUser(String gameId, String agentResponse)
    {
        //TODO: check if the user completed one of the training tasks.

    }
}
