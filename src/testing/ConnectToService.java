package testing;

import instructable.server.RealtimeAgentServer;
import instructable.server.ExperimentServer;
import instructable.server.Service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Created by Amos Azaria on 20-May-15.
 */
public class ConnectToService
{
    static private final boolean useRealtimeAgent = true; //determines whether to provide actual user credentials for sending real emails.
    static private final String userId = "a992";
    static private final String USER_AGENT = "Mozilla/5.0";
    static private final String httpIp = "http://34.193.23.122";//"http://localhost";

    public static void main(String[] args) throws Exception
    {
        initializeGameId();
        runRequestsInLoop();
    }

    private static void initializeGameId() throws Exception
    {
        String url = httpIp + ":" + Service.portToUse + "/" + (useRealtimeAgent? Service.contextRealtimeAgent : Service.contextEmailAndExperiment);// + "?" + Service.userSaysParam + "=" +URLEncoder.encode(userSays, StandardCharsets.UTF_8.name());

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //using post
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        String parameters =  (useRealtimeAgent? RealtimeAgentServer.userIdParam : ExperimentServer.gameIdParam) + "=" + userId;
        parameters += "&" + (useRealtimeAgent? RealtimeAgentServer.actionParam : ExperimentServer.actionParam) + "=" + (useRealtimeAgent? RealtimeAgentServer.actionSetEmailAndPswd : ExperimentServer.newGameJoinedStr);
        if (useRealtimeAgent)
        {
            parameters += "&" + RealtimeAgentServer.username + "=" + "234jhf84kj";
            parameters += "&" + RealtimeAgentServer.encPwd + "=" + "fsk2iue73";
            parameters += "&" + RealtimeAgentServer.email + "=" + UserCredentials.email;
            parameters += "&" + RealtimeAgentServer.realPwd + "=" + UserCredentials.password;
        }

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(parameters);
        wr.flush();
        wr.close();
        int responseCode = con.getResponseCode();
        if (responseCode != 200)
        {
            System.out.println("S: error. (response code is: "+responseCode + ")");
        }
    }

    private static void runRequestsInLoop()
    {
        Scanner scanIn = new Scanner(System.in);
        while (true)
        {
            try
            {
                String userSays = scanIn.nextLine();
                if (userSays.equals("exit"))
                    break;

                String say = getSayToUser(userSays);

                //print result
                System.out.println(say);
                if (say.contains(RealtimeAgentServer.whenContainedSendAnotherRequest))
                {
                    say = getSayToUser(null); //ask for resend
                    System.out.println(say);
                }

            } catch (Exception ex)
            {
                ex.printStackTrace();
                System.out.println("S: error.\n");
            }
        }
    }

    /**
     *
     * @param userSays set to null if server asked for resend
     * @return
     * @throws IOException
     */
    private static String getSayToUser(String userSays) throws IOException
    {
        String url = httpIp + ":"+ Service.portToUse + "/" + Service.contextRealtimeAgent;// + "?" + Service.userSaysParam + "=" +URLEncoder.encode(userSays, StandardCharsets.UTF_8.name());

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //using post
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        String parameters =  RealtimeAgentServer.userIdParam + "=" + userId;
        if (userSays != null)
        {
            parameters += "&" + RealtimeAgentServer.actionParam + "=" + RealtimeAgentServer.actionUserSays;
            parameters += "&" + RealtimeAgentServer.userSaysParam + "=" + URLEncoder.encode(userSays, StandardCharsets.UTF_8.name());
        }
        else
        {
            parameters += "&" + RealtimeAgentServer.actionParam + "=" + RealtimeAgentServer.actionResendRequested;
        }

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(parameters);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        if (responseCode != 200)
        {
            System.out.println("S: error. (response code is: "+responseCode + ")");
        }
        //System.out.println("\nSending 'POST' request to URL : " + url);
        //System.out.println("Post parameters : " + parameters);
        //System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        response.append("S: ");

        while ((inputLine = in.readLine()) != null)
        {
            response.append(inputLine + "\n");
        }
        in.close();

        return response.toString();
    }
}
