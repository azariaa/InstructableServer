package instructable;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

/**
 * Created by Amos Azaria on 20-May-15.
 */
public class Service
{
    static public final int portToUse = 18892;
    static public final String contextSayToAgent = "say";
    static public final String contextEmailAndExperiment = "emailAndExperiment";
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
            fileHandler  = new FileHandler(loggingFile,true);
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
            ExecutorService executor = Executors.newFixedThreadPool(20);
            HttpServer server = HttpServer.create(new InetSocketAddress(portToListenOn), 0);
            AgentDataAndControl agentDataAndControl = new AgentDataAndControl(logger,true);
            HttpContext agentContext = server.createContext("/" + contextSayToAgent, new AgentServer(agentDataAndControl));
            agentContext.getFilters().add(new ParameterFilter());
            HttpContext emailAndExperimentContext = server.createContext("/" + contextEmailAndExperiment, new EmailAndExperimentServer(agentDataAndControl));
            emailAndExperimentContext.getFilters().add(new ParameterFilter());
            server.setExecutor(executor);
            server.start();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "an exception was thrown", ex);
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
