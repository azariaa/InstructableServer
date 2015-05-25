package instructable;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.*;

/**
 * Created by Amos Azaria on 20-May-15.
 */
public class Service
{
    static public final int portToUse = 18892;
    static public final String contextSay = "say";
    static public final String userSaysParam = "userSays";
    static public final String userIdParam = "userId";
    //TODO: add userID as a mandatory field (need to clone original parser and create a new TopDMAllActions for evey new user).
    private static final Logger logger = Logger.getLogger(Service.class.getName());
    static private final String loggingFile = "./logging/service.log";

    public static void main(String[] args)
    {
        configLogger();
        running(portToUse);
    }

    private static void configLogger()
    {
        //Handler consoleHandler = null;
        Handler fileHandler  = null;
        //Creating consoleHandler and fileHandler
        //consoleHandler = new ConsoleHandler();
        try
        {
            fileHandler  = new FileHandler(loggingFile);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        // Creating SimpleFormatter
        Formatter simpleFormatter = new SimpleFormatter();
        // Setting formatter to the handler
        fileHandler.setFormatter(simpleFormatter);
        //Assigning handlers to logger object
        //logger.addHandler(consoleHandler); //don't need console handler, it is added automatically
        logger.addHandler(fileHandler);

        //Setting levels to handlers and logger
        //consoleHandler.setLevel(Level.ALL);
        fileHandler.setLevel(Level.ALL);
        logger.setLevel(Level.ALL);

        logger.config("Configuration done.");
    }

    public static void running(int portToListenOn)
    {
        try
        {
            HttpServer server = HttpServer.create(new InetSocketAddress(portToListenOn), 0);
            ServiceDataAndControl serviceDataAndControl = new ServiceDataAndControl(logger);
            HttpContext context = server.createContext("/" + contextSay, new HandlerForHttp(serviceDataAndControl));
            context.getFilters().add(new ParameterFilter());
            server.setExecutor(null); // creates a default executor
            server.start();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "an exception was thrown", ex);
        }
    }

    static class HandlerForHttp implements HttpHandler
    {
        ServiceDataAndControl serviceDataAndControl;

        HandlerForHttp(ServiceDataAndControl serviceDataAndControl)
        {
            this.serviceDataAndControl = serviceDataAndControl;
        }

        public void handle(HttpExchange httpExchange)
        {
            try
            {
                //Map<String, String> parameters = queryToMap(httpExchange.getRequestURI().getQuery());
                Map<String, Object> parameters =
                        (Map<String, Object>) httpExchange.getAttribute(ParameterFilter.parametersStr);
                String systemReply = null;
                if (!parameters.containsKey(userIdParam))
                {
                    logger.warning("no userId");
                    return;
                }
                String userId = parameters.get(userIdParam).toString();
                if (parameters.containsKey(userSaysParam))
                {
                    try
                    {
                        String userSays = parameters.get(userSaysParam).toString();
                        logger.info("UserID:" + userId + ". User says: " + parameters.get(userSaysParam).toString());
                        systemReply = serviceDataAndControl.executeSentenceForUser(userId, userSays);
                    } catch (Exception ex)
                    {
                        logger.log(Level.SEVERE, "an exception was thrown", ex);
                        systemReply = "Sorry, but I got some error...";
                    }
                }
                else
                {
                    logger.warning("UserID:" + userId  + ". User has no " + userSaysParam);
                    systemReply = "Hello, how can I help you?";
                }
                logger.info("UserID:" + userId + ". System reply: " + systemReply);
                httpExchange.sendResponseHeaders(200, systemReply.length());
                OutputStream os = httpExchange.getResponseBody();
                os.write(systemReply.getBytes());
                os.close();
            }
            catch (Exception ex)
            {
                logger.log(Level.SEVERE, "an exception was thrown", ex);
            }
        }
    }


        static public class ParameterFilter extends Filter
        {
            public static final String parametersStr = "parameters";

            @Override
            public String description() {
                return "Parses the requested URI for parameters";
            }

            @Override
            public void doFilter(HttpExchange exchange, Chain chain)
                    throws IOException {
                parseGetParameters(exchange);
                parsePostParameters(exchange);
                chain.doFilter(exchange);
            }

            private void parseGetParameters(HttpExchange exchange)
                    throws UnsupportedEncodingException {

                Map<String, Object> parameters = new HashMap<String, Object>();
                URI requestedUri = exchange.getRequestURI();
                String query = requestedUri.getRawQuery();
                parseQuery(query, parameters);
                exchange.setAttribute(parametersStr, parameters);
            }

            private void parsePostParameters(HttpExchange exchange)
                    throws IOException {

                if ("post".equalsIgnoreCase(exchange.getRequestMethod())) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parameters =
                            (Map<String, Object>)exchange.getAttribute(parametersStr);
                    InputStreamReader isr =
                            new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8.name());
                    BufferedReader br = new BufferedReader(isr);
                    String query = br.readLine();
                    parseQuery(query, parameters);
                }
            }

            @SuppressWarnings("unchecked")
            private void parseQuery(String query, Map<String, Object> parameters)
                    throws UnsupportedEncodingException {

                if (query != null) {
                    String pairs[] = query.split("[&]");

                    for (String pair : pairs) {
                        String param[] = pair.split("[=]");

                        String key = null;
                        String value = null;
                        if (param.length > 0) {
                            key = URLDecoder.decode(param[0],
                                    System.getProperty("file.encoding"));
                        }

                        if (param.length > 1) {
                            value = URLDecoder.decode(param[1],
                                    System.getProperty("file.encoding"));
                        }

                        if (parameters.containsKey(key)) {
                            Object obj = parameters.get(key);
                            if(obj instanceof List<?>) {
                                List<String> values = (List<String>)obj;
                                values.add(value);
                            } else if(obj instanceof String) {
                                List<String> values = new ArrayList<String>();
                                values.add((String)obj);
                                values.add(value);
                                parameters.put(key, values);
                            }
                        } else {
                            parameters.put(key, value);
                        }
                    }
                }
            }

    }
}
