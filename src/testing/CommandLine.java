package testing;

import instructable.server.dal.CreateParserFromFiles;
import instructable.server.CommandsToParser;
import instructable.server.IAllUserActions;
import instructable.server.TopDMAllActions;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import java.util.Optional;
import java.util.Scanner;

/**
 * Created by Amos Azaria on 06-May-15.
 */
public class CommandLine
{
    public static void main(String[] args) throws Exception
    {

        ParserSettings parserSettings = CreateParserFromFiles.createParser(Optional.of("tempUser"));
        TopDMAllActions topDMAllActions = new TopDMAllActions("you@myworkplace.com", "tempUser", new CommandsToParser(parserSettings), (subject, body, copyList, recipientList) -> {}, true, Optional.empty());
        IAllUserActions allUserActions = topDMAllActions;

        CreateParserFromFiles.addInboxEmails(topDMAllActions);

        Scanner scanIn = new Scanner(System.in);
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        //clearConsole();
        System.out.println("S: What would you want me to do?");
        while (true)
        {
            String userSays = scanIn.nextLine();
            if (userSays.equals("exit"))
                break;
            CcgUtils.SayAndExpression response;
            try
            {
                response = parserSettings.parseAndEval(allUserActions, userSays);
                System.out.println("S:" + response.sayToUser + "\n");
            } catch (Exception ex)
            {
                ex.printStackTrace();
                System.out.println("S: error.\n");
            }
        }

    }

}
