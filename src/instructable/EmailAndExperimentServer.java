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
    static public final String userIdParam = "userId";
    static public final String actionParam = "action";
    static public final String getUserScoreStr = "getUserScore";
    static public final String newUserJoinedStr = "newUserJoined";
    static public final String sendEmailForUserStr = "sendEmailForUser";
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
        if (!parameters.containsKey(userIdParam))
        {
            logger.warning("no userId");
            return;
        }
        String userId = parameters.get(userIdParam).toString();
        if (!parameters.containsKey(actionParam))
        {
            logger.warning("no action, userId:" + userId);
            return;
        }
        String action = parameters.get(userIdParam).toString();
        if (action.equals(getUserScoreStr))
        {
            Integer score = getUserScore(userId);
            String systemReply = score.toString();
            httpExchange.sendResponseHeaders(200, systemReply.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(systemReply.getBytes());
            os.close();
        }
        else if (action.equals(newUserJoinedStr))
        {
            newUserJoined(userId);
        }
        else if (action.equals(sendEmailForUserStr))
        {
            if (!parameters.containsKey(EmailMessage.subjectStr) ||
                    !parameters.containsKey(EmailMessage.bodyStr) ||
                    !parameters.containsKey(EmailMessage.copyListStr) ||
                    !parameters.containsKey(EmailMessage.recipientListStr) ||
                    !parameters.containsKey(EmailMessage.senderStr))
            {
                logger.warning("something is missing in the email, userId:" + userId);
                return;
            }
            sendEmailForUser(userId,
                    parameters.get(EmailMessage.subjectStr).toString(),
                    parameters.get(EmailMessage.bodyStr).toString(),
                    parameters.get(EmailMessage.copyListStr).toString(),
                    parameters.get(EmailMessage.recipientListStr).toString(),
                    parameters.get(EmailMessage.senderStr).toString()
            );
        }
        else
        {
            logger.warning("unknown action, userId:" + userId + ". action:" + action);
            return;
        }
    }

    private int getUserScore(String userId)
    {
        Set<TasksToComplete> userTasks = getUserTasks(userId);

        return userTasks.size();
        //return (int)userScores.values().stream().filter(x -> x==true).count(); //should work, but need to test it first...
    }

    private Set<TasksToComplete> getUserTasks(String userId)
    {
        Set<TasksToComplete> userScores = null;
        synchronized(mapLock)
        {
            if (missionsCompletedByUser.containsKey(userId))
                userScores = missionsCompletedByUser.get(userId);
        }
        if (userScores == null)
        {
            logger.warning("userId not in map. user:" + userId + ". adding it now");
            newUserJoined(userId);
        }
        return userScores;
    }

    private void newUserJoined(String userId)
    {
        synchronized(mapLock)
        {
            if (missionsCompletedByUser.containsKey(userId))
            {
                logger.warning("userId already in map. user:" + userId + ".");
                return;
            }
            logger.info("userId added to map. user:" + userId + ".");
            missionsCompletedByUser.put(userId, new HashSet<>());
        }

    }

    private void sendEmailForUser(String userId, String subject, String body, String copyList, String recipientList, String sender)
    {
        Set<TasksToComplete> userTasks = getUserTasks(userId);
        //TODO: fill out all the experiment!!!!
        boolean completedTaskforwardToAll = true;
        if (completedTaskforwardToAll)
            userTasks.add(TasksToComplete.forwardToAll);
    }
}
