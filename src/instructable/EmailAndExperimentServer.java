package instructable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import instructable.server.hirarchy.EmailMessage;

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
public class EmailAndExperimentServer implements HttpHandler
{
    enum TasksToComplete {sendTest,forwardToAll}
    static final int numOfTasks = TasksToComplete.values().length;
    static public final String gameIdParam = "gameId";
    static public final String actionParam = "action";
    static public final String getTasksCompletedStr = "getTasksCompleted";
    static public final String newGameJoinedStr = "newGameJoined";
    static public final String sendEmailInGameStr = "sendEmailInGame";
    Logger logger;

    Object mapLock = new Object();
    Map<String,Set<TasksToComplete>> missionsCompletedByUser = new HashMap<>();

    public EmailAndExperimentServer(Logger logger)
    {
        this.logger = logger;
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
        String action = parameters.get(gameIdParam).toString();
        if (action.equals(getTasksCompletedStr))
        {
            Integer score = getGameScore(gameId);
            String systemReply = score.toString();
            httpExchange.sendResponseHeaders(200, systemReply.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(systemReply.getBytes());
            os.close();
        }
        else if (action.equals(newGameJoinedStr))
        {
            newGameStarted(gameId);
        }
        else if (action.equals(sendEmailInGameStr))
        {
            if (!parameters.containsKey(EmailMessage.subjectStr) ||
                    !parameters.containsKey(EmailMessage.bodyStr) ||
                    !parameters.containsKey(EmailMessage.copyListStr) ||
                    !parameters.containsKey(EmailMessage.recipientListStr) ||
                    !parameters.containsKey(EmailMessage.senderStr))
            {
                logger.warning("something is missing in the email, gameId:" + gameId);
                return;
            }
            sendEmailForUser(gameId,
                    parameters.get(EmailMessage.subjectStr).toString(),
                    parameters.get(EmailMessage.bodyStr).toString(),
                    parameters.get(EmailMessage.copyListStr).toString(),
                    parameters.get(EmailMessage.recipientListStr).toString(),
                    parameters.get(EmailMessage.senderStr).toString()
            );
        }
        else
        {
            logger.warning("unknown action, gameId:" + gameId + ". action:" + action);
            return;
        }
    }

    private int getGameScore(String gameId)
    {
        Set<TasksToComplete> userTasks = getUserTasks(gameId);

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
            logger.info("gameId added to map. gameId:" + gameId + ".");
            missionsCompletedByUser.put(gameId, new HashSet<>());
        }

    }

    private void sendEmailForUser(String gameId, String subject, String body, String copyList, String recipientList, String sender)
    {
        Set<TasksToComplete> userTasks = getUserTasks(gameId);
        //TODO: fill out all the experiment!!!!
        boolean completedTaskforwardToAll = true;
        if (completedTaskforwardToAll)
            userTasks.add(TasksToComplete.forwardToAll);
    }
}
