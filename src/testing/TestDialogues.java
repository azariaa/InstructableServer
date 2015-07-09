package testing;

import instructable.AgentDataAndControl;
import instructable.ExperimentTaskController;
import instructable.server.ccg.CcgDetokenizer;
import instructable.server.ccg.CcgUtils;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jayantkrish.jklol.ccg.CcgExample;
import com.jayantkrish.jklol.ccg.CcgParse;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionComparator;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionSimplificationException;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionSimplifier;
import com.jayantkrish.jklol.ccg.lambda2.SimplificationComparator;

public class TestDialogues
{

    public static void main(String[] args)
    {
        runTests();
    }

    private static void runTests()
    {
        Logger logger = Logger.getLogger(TestDialogues.class.getName());
        logger.setLevel(Level.SEVERE);
        AgentDataAndControl agentDataAndControl = new AgentDataAndControl(logger,false);

        String[] tests = new String[]{"data/dialogues/send_email.csv", "data/dialogues/define_contact.csv",
            "data/dialogues/forward.csv", "data/dialogues/reply.csv"};

        System.out.println("here");
        
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

        System.out.println(agentDataAndControl.getParserSettingsForTestingOnly(gameId).parser);
        
        int numCorrect = 0;
        int numParsed = 0;
        for (CcgExample example : testExamples) {
            CcgParse parse = agentDataAndControl.getParserSettingsForTestingOnly(gameId).parser.parse(example.getSentence());
            System.out.println("====");
            System.out.println("SENT: " + example.getSentence());
            if (parse != null) {
                int correct = 0;
                Expression2 lf = null;
                Expression2 correctLf = simplifier.apply(example.getLogicalForm());

                try {
                  lf = simplifier.apply(parse.getLogicalForm());
                  correct = comparator.equals(lf, correctLf) ? 1 : 0;
                } catch (ExpressionSimplificationException e) {
                    // Make lf print out as null.
                    lf = Expression2.constant("null");
                }

                System.out.println("PREDICTED: " + lf);
                System.out.println("TRUE:      " + correctLf);
                System.out.println("DEPS: " + parse.getAllDependencies());
                System.out.println("CORRECT: " + correct);

                //Amos: This function needs the original sentence, since that is what will be used for learning
                //Plus, we don't want to limit the test to use the same POS. (I'm saying is that the test files should have the original sentences, not annotated sentences).
                List<String> words = example.getSentence().getWords();
                String sentence = CcgDetokenizer.getDetokenizer().apply(words.subList(1, words.size())).getConstant();
                sentence = sentence.substring(1, sentence.length() - 1);
                agentDataAndControl.executeExpressionForTestingOnly(gameId, sentence, correctLf);

                numCorrect += correct;
                numParsed++;
            }
            else {
                System.out.println("NO PARSE");
            }
        }

        double precision = ((double) numCorrect) / numParsed;
        double recall = ((double) numCorrect) / testExamples.size();
        System.out.println("\nPrecision: " + precision);
        System.out.println("Recall: " + recall);
    }
}
