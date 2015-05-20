package instructable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import instructable.server.ActionResponse;
import instructable.server.CommandsToParser;
import instructable.server.IAllUserActions;
import instructable.server.TopDMAllActions;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Amos Azaria on 20-May-15.
 */
public class Service
{
    static public final int portToUse = 18892;
    static public final String contextSay = "say";
    static public final String userSaysParam = "userSays";

    public static void main(String[] args)
    {
        ParserSettings parserSettings = EnvironmentCreatorUtils.createParser();
        TopDMAllActions topDMAllActions = new TopDMAllActions(new CommandsToParser(parserSettings));
        IAllUserActions allUserActions = topDMAllActions;
        EnvironmentCreatorUtils.addInboxEmails(topDMAllActions);

        running(portToUse, allUserActions, parserSettings);
    }

    public static void running(int portToListenOn, IAllUserActions allUserActions, ParserSettings parserSettings)
    {
        try
        {
            HttpServer server = HttpServer.create(new InetSocketAddress(portToListenOn), 0);
            server.createContext("/" + contextSay, new MyHandler(allUserActions, parserSettings));
            server.setExecutor(null); // creates a default executor
            server.start();
        }
        // If anything goes wrong, print an error message
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static class MyHandler implements HttpHandler
    {
        IAllUserActions allUserActions;
        ParserSettings parserSettings;

        MyHandler(IAllUserActions allUserActions, ParserSettings parserSettings)
        {
            this.allUserActions = allUserActions;
            this.parserSettings = parserSettings;
        }

        public void handle(HttpExchange httpExchange) throws IOException
        {
            Map<String, String> parameters = queryToMap(httpExchange.getRequestURI().getQuery());
            String systemReply = null;
            if (parameters.containsKey(userSaysParam))
            {
                try
                {
                    ActionResponse response = CcgUtils.ParseAndEval(allUserActions, parserSettings, parameters.get(userSaysParam));
                    systemReply = response.getSayToUser();
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                    systemReply = "Sorry, but I got some error...";
                }
            }
            else
            {
                systemReply = "Hello, how can I help you?";
            }
            httpExchange.sendResponseHeaders(200, systemReply.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(systemReply.getBytes());
            os.close();
        }


        /**
         * returns the url parameters in a map
         *
         * @param query
         * @return map
         */
        public static Map<String, String> queryToMap(String query)
        {
            Map<String, String> result = new HashMap<String, String>();
            for (String param : query.split("&"))
            {
                String pair[] = param.split("=");
                if (pair.length > 1)
                {
                    try
                    {
                        result.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name()));
                    } catch (UnsupportedEncodingException e)
                    {
                        result.put(pair[0], "");
                        e.printStackTrace();
                    }
                }
                else
                {
                    result.put(pair[0], "");
                }
            }
            return result;
        }
    }
}
