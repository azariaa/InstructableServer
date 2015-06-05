package testing;

import instructable.EnvironmentCreatorUtils;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import java.nio.file.Paths;
import java.util.List;

import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.CcgExample;
import com.jayantkrish.jklol.ccg.CcgParse;
import com.jayantkrish.jklol.ccg.CcgParser;
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionSimplifier;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;

public class TestLexiconInduction {
    public static void main(String[] args) throws Exception
    {
        runTest();
    }

    private static void runTest() throws Exception
    {
    	ParserSettings parserSettings = EnvironmentCreatorUtils.createParser();
    	CcgParser parser = parserSettings.parser;
    	List<String[]> exampleStrings = CcgUtils.loadExamples(Paths.get("learntCommands.csv"));
    	List<CcgExample> examples = Lists.newArrayList();
    	for (String[] exampleString : exampleStrings) {
    		Expression2 expression = ExpressionParser.expression2().parseSingleExpression(exampleString[1]);
            CcgExample example = CcgUtils.createCcgExample(exampleString[0], expression, parserSettings.posUsed);
            examples.add(example);
    	}

    	List<LexiconEntry> newEntries = Lists.newArrayList();
    	for (CcgExample example : examples) {
    		System.out.println(example.getSentence().getWords());
    		System.out.println(example.getLogicalForm());
    		newEntries.addAll(CcgUtils.induceLexiconEntriesHeuristic(example, parser));
    	}
    	
    	CcgUtils.updateParserGrammar(newEntries, Lists.newArrayList(), parserSettings);
    	SufficientStatistics newParameters = CcgUtils.train(parserSettings.parserFamily, examples);
    	
    	CcgParser newParser = parserSettings.parserFamily.getModelFromParameters(newParameters);
    	for (LexiconEntry entry : newParser.getLexiconEntries("set", "VBN")) {
    	  System.out.println(entry.getWords() + " " + entry.getCategory().getLogicalForm());
    	}
    	
    	ExpressionSimplifier simplifier = CcgUtils.getExpressionSimplifier();
    	for (CcgExample example : examples) {
    	  List<CcgParse> parses = newParser.beamSearch(example.getSentence(), 10);
    	  
    	  System.out.println(example.getSentence().getWords());
    	  System.out.println(example.getLogicalForm());
    	  for (CcgParse parse : parses) {
    	    Expression2 predictedLf = simplifier.apply(parse.getLogicalForm());
    	    System.out.println("   " + predictedLf);
    	  }
    	  System.out.println(parses.size());
    	}
    }
}
