package testing;

import instructable.server.backend.IAllUserActions;
import instructable.server.backend.TopDMAllActions;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;
import instructable.server.dal.CreateParserFromFiles;
import instructable.server.dal.DBUtils;
import instructable.server.parser.CommandsToParser;

import java.util.Date;
import java.util.Optional;
import java.util.Scanner;
/**
 * Created by Amos Azaria on 06-May-15.
 */
public class CommandLine
{
    public static void main(String[] args) throws Exception
    {

        String userId = "tempUser";
        DBUtils.clearUserData(userId);
        ParserSettings parserSettings = CreateParserFromFiles.createParser(Optional.of(userId));
        TopDMAllActions topDMAllActions = new TopDMAllActions("you@myworkplace.com", "tempUser", new CommandsToParser(parserSettings, Optional.empty()), (executionStatus, subject, body, copyList, recipientList) -> {}, true, Optional.empty(), Optional.empty());
        IAllUserActions allUserActions = topDMAllActions;

        CreateParserFromFiles.addInboxEmails(topDMAllActions);

        Scanner scanIn = new Scanner(System.in);
        System.out.println("\nS: What would you want me to do?");
        //clearConsole();
        while (true)
        {
            String userSays = scanIn.nextLine();
            if (userSays.equals("exit"))
                break;
            CcgUtils.SayAndExpression response;
            try
            {
                response = parserSettings.parseAndEval(allUserActions, userSays, Optional.of(new Date()));
                System.out.println("S:" + response.sayToUser + "\n");
            } catch (Exception ex)
            {
                ex.printStackTrace();
                System.out.println("S: error.\n");
            }
        }

    }

}
