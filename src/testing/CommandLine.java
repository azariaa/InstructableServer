package testing;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import instructable.server.backend.IAllUserActions;
import instructable.server.backend.TopDMAllActions;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;
import instructable.server.dal.CreateParserFromFiles;
import instructable.server.dal.DBUtils;
import instructable.server.parser.CommandsToParser;

import java.util.Date;
import java.util.List;
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
        TopDMAllActions topDMAllActions = new TopDMAllActions("you@myworkplace.com", "tempUser", new CommandsToParser(parserSettings), (subject, body, copyList, recipientList) -> {}, true, Optional.empty());
        IAllUserActions allUserActions = topDMAllActions;

        CreateParserFromFiles.addInboxEmails(topDMAllActions);

        Scanner scanIn = new Scanner(System.in);
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        //clearConsole();
        System.out.println("S: What would you want me to do?");
        String[] exampleDates = {
                "2015-10-10",
                "2015/10/10",
                "2015-10-30 15:30"
        };

        Parser parser = new Parser();
        for (String dateString : exampleDates) {
            List<DateGroup> dates = parser.parse(dateString);
            Date date = dates.get(0).getDates().get(0);
            System.out.println(date);
        }
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
