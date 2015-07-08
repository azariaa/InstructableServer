package testing;

import instructable.EnvironmentCreatorUtils;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import java.util.List;

import com.jayantkrish.jklol.ccg.CcgBeamSearchInference;
import com.jayantkrish.jklol.ccg.CcgExample;
import com.jayantkrish.jklol.ccg.CcgInference;
import com.jayantkrish.jklol.ccg.CcgParse;
import com.jayantkrish.jklol.ccg.CcgParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionComparator;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionSimplificationException;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionSimplifier;
import com.jayantkrish.jklol.ccg.lambda2.SimplificationComparator;

public class TestDialogues {

  public static void main(String[] args) {
    runTests();
  }
  
  private static void runTests() {
    ParserSettings parserSettings = EnvironmentCreatorUtils.createParser(
        "data/lexiconEntries.txt", "data/lexiconSyn.txt", "data/examples.csv");
    
    String[] tests = new String[] {"data/dialogues/send_email.csv", "data/dialogues/define_contact.csv"};
    
    for (String test : tests) {
      runTest(test, parserSettings);
    }
  }

  private static void runTest(String filename, ParserSettings parserSettings) {
    CcgParser parser = parserSettings.parser;
    ExpressionSimplifier simplifier = CcgUtils.getExpressionSimplifier();
    ExpressionComparator comparator = new SimplificationComparator(simplifier);

    List<CcgExample> testExamples = TestDataDriven.readExamplesFromFile(filename, parserSettings);

    int numCorrect = 0;
    int numParsed = 0;
    for (CcgExample example : testExamples) {
      CcgParse parse = parser.parse(example.getSentence());
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
        
        if (correct == 1) {
          // TODO: amos, how do I appropriately change the parser grammar here?
        }

        numCorrect += correct;
        numParsed++;
      } else {
        System.out.println("NO PARSE");
      }
    }

    double precision = ((double) numCorrect) / numParsed;
    double recall = ((double) numCorrect) / testExamples.size();
    System.out.println("\nPrecision: " + precision);
    System.out.println("Recall: " + recall);
  }
}
