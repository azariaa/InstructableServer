package testing;

import com.jayantkrish.jklol.ccg.CcgExample;
import com.jayantkrish.jklol.ccg.CcgParse;
import com.jayantkrish.jklol.ccg.lambda2.*;
import instructable.AgentDataAndControl;
import instructable.EnvironmentCreatorUtils;
import instructable.ExperimentTaskController;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestDialogues
{

    public static void main(String[] args)
    {
        runTests();
    }

    private static void runTests()
    {
        ParserSettings parserSettings = EnvironmentCreatorUtils.createParser(
                "data/lexiconEntries.txt", "data/lexiconSyn.txt", "data/examples.csv");

        Logger logger = Logger.getLogger(TestDialogues.class.getName());
        logger.setLevel(Level.SEVERE);
        AgentDataAndControl agentDataAndControl = new AgentDataAndControl(logger,false);

        String[] tests = new String[]{"data/dialogues/send_email.csv", "data/dialogues/define_contact.csv"};

        Integer gameId = 0;
        for (String test : tests)
        {
            gameId++;
            runTest(test, gameId.toString(), agentDataAndControl, logger);
        }
    }

    private static void runTest(String filename, String gameId, AgentDataAndControl agentDataAndControl, Logger logger)
    {
        ExperimentTaskController experimentTaskController = new ExperimentTaskController(logger,gameId);
        agentDataAndControl.addNewGame(gameId, experimentTaskController, experimentTaskController);

        ExpressionSimplifier simplifier = CcgUtils.getExpressionSimplifier();
        ExpressionComparator comparator = new SimplificationComparator(simplifier);

        List<CcgExample> testExamples = TestDataDriven.readExamplesFromFile(filename, agentDataAndControl.getParserSettingsForTestingOnly(gameId));

        int numCorrect = 0;
        int numParsed = 0;
        for (CcgExample example : testExamples)
        {
            CcgParse parse = agentDataAndControl.getParserSettingsForTestingOnly(gameId).parser.parse(example.getSentence());
            System.out.println("====");
            System.out.println("SENT: " + example.getSentence());
            if (parse != null)
            {
                int correct = 0;
                Expression2 lf = null;
                Expression2 correctLf = simplifier.apply(example.getLogicalForm());

                try
                {
                    lf = simplifier.apply(parse.getLogicalForm());
                    correct = comparator.equals(lf, correctLf) ? 1 : 0;
                } catch (ExpressionSimplificationException e)
                {
                    // Make lf print out as null.
                    lf = Expression2.constant("null");
                }

                System.out.println("PREDICTED: " + lf);
                System.out.println("TRUE:      " + correctLf);
                System.out.println("DEPS: " + parse.getAllDependencies());
                System.out.println("CORRECT: " + correct);

//                if (correct == 1)
//                {
//                    // TODO: amos, how do I appropriately change the parser grammar here?
//                }
                //Amos: This function needs the original sentence, since that is what will be used for learning
                //Plus, we don't want to limit the test to use the same POS. (I'm saying is that the test files should have the original sentences, not annotated sentences).
                agentDataAndControl.executeExpressionForTestingOnly(gameId, example.getSentence().toString(), correctLf);

                numCorrect += correct;
                numParsed++;
            }
            else
            {
                System.out.println("NO PARSE");
            }
        }

        double precision = ((double) numCorrect) / numParsed;
        double recall = ((double) numCorrect) / testExamples.size();
        System.out.println("\nPrecision: " + precision);
        System.out.println("Recall: " + recall);
    }
}
