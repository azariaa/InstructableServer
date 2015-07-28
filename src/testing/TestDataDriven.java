package testing;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.CcgBeamSearchInference;
import com.jayantkrish.jklol.ccg.CcgExample;
import com.jayantkrish.jklol.ccg.CcgInference;
import com.jayantkrish.jklol.ccg.CcgParser;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionComparator;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionSimplifier;
import com.jayantkrish.jklol.ccg.lambda2.SimplificationComparator;
import com.jayantkrish.jklol.ccg.util.SemanticParserUtils;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;
import instructable.server.dal.CreateParserFromFiles;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TestDataDriven {

  public static void main(String[] args) {
    runTest();
  }
  
  public static List<CcgExample> readExamplesFromFile(String filename, ParserSettings parserSettings) {
    List<String[]> exampleStrings = CcgUtils.loadExamples(Paths.get(filename));
    
    List<CcgExample> examples = Lists.newArrayList();
    for (String[] exampleString : exampleStrings) {
      Preconditions.checkArgument(exampleString.length == 2, "Error parsing csv %s",
          Arrays.toString(exampleString));
      
      Expression2 expression = ExpressionParser.expression2().parseSingleExpression(exampleString[1]);
      CcgExample example = CcgUtils.createCcgExample(exampleString[0], expression, parserSettings.posUsed,
          false, parserSettings.featureVectorGenerator);
      examples.add(example);
    }

    return examples;
  }
  
  private static void runTest() {
    ParserSettings parserSettings = CreateParserFromFiles.createParser(Optional.of("tempUser"),
            "data/lexiconEntries.txt", "data/lexiconSyn.txt", "data/examples.csv");

    /*
    System.out.println("Adding training example");
    List<Expression2> commands = Lists.newArrayList();
    commands.add(ExpressionParser.expression2().parseSingleExpression("(newcommand)"));
    parserSettings.addTrainingEg("newcommand", commands);
    */

    CcgParser parser = parserSettings.parser;
    ExpressionSimplifier simplifier = CcgUtils.getExpressionSimplifier();
    ExpressionComparator comparator = new SimplificationComparator(simplifier);
    CcgInference inferenceAlgorithm = new CcgBeamSearchInference(null, comparator, 100,
                -1, Integer.MAX_VALUE, Runtime.getRuntime().availableProcessors(), false);

    List<CcgExample> trainingExamples = readExamplesFromFile("data/examples.csv", parserSettings);
    List<CcgExample> testExamples = readExamplesFromFile("data/test.csv", parserSettings);
    
    System.out.println("training error");
    SemanticParserUtils.testSemanticParser(trainingExamples, parser,
        inferenceAlgorithm, simplifier, comparator);
    System.out.println("test error");
    SemanticParserUtils.testSemanticParser(testExamples, parser,
        inferenceAlgorithm, simplifier, comparator);
  }


  
  
}
