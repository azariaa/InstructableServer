package testing;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.ActionResponse;
import instructable.server.IAllUserActions;
import instructable.server.ICommandsToParser;
import instructable.server.TopDMAllActions;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import java.util.List;
import java.util.Scanner;

/**
 * Created by Amos Azaria on 06-May-15.
 */
public class CommandLine
{
    public static void main(String[] args) throws Exception
    {
        IAllUserActions allUserActions = new TopDMAllActions(new ICommandsToParser()
        {
            @Override
            public void addTrainingEg(String originalCommand, List<Expression2> commandsLearnt)
            {

            }

            @Override
            public void newConceptDefined(String conceptName)
            {

            }

            @Override
            public void newFieldDefined(String fieldName)
            {

            }

            @Override
            public void newInstanceDefined(String instanceName)
            {

            }
        });

        ParserSettings parserSettings = TestWithParser.createParser();


        Scanner scanIn = new Scanner(System.in);
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        //clearConsole();
        System.out.println("S: What would you want me to do?");
        while (true)
        {
            String userSays = scanIn.nextLine();
            if (userSays.equals("exit"))
                break;
            ActionResponse response;
            try
            {
                response = CcgUtils.ParseAndEval(allUserActions, parserSettings, userSays);
                System.out.println("S:" + response.getSayToUser() + "\n");
            } catch (Exception ex)
            {
                System.out.println("S: error.\n");
            }
        }

    }
}
