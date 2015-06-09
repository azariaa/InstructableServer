package instructable.server.ccg;

import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.*;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.ccg.lambda2.StaticAnalysis;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer.StringContext;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.lisp.LispEval;
import com.jayantkrish.jklol.lisp.SExpression;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.preprocessing.DictionaryFeatureVectorGenerator;
import com.jayantkrish.jklol.preprocessing.FeatureGenerator;
import com.jayantkrish.jklol.preprocessing.FeatureVectorGenerator;
import com.jayantkrish.jklol.util.IndexedList;
import instructable.server.ActionResponse;
import instructable.server.IAllUserActions;
import instructable.server.InfoForCommand;
import instructable.server.LispExecutor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by Amos Azaria on 05-May-15.
 */
public class ParserSettings implements Cloneable
{

    private ParserSettings()
    {

    }

    public ParserSettings (List<String> lexiconEntries, String[] unaryRules,
                                                   FeatureGenerator<StringContext, String> featureGenerator, List<String[]> examplesList)
    {
        env = Environment.empty();
        symbolTable = IndexedList.create();

        List<LexiconEntry> lexicon = LexiconEntry.parseLexiconEntries(lexiconEntries);

        List<CcgUnaryRule> unaryRulesList = Lists.newArrayList();
        for (String unaryRule : unaryRules)
        {
            unaryRulesList.add(CcgUnaryRule.parseFrom(unaryRule));
        }

        Set<String> posUsed = new HashSet<>();
        posUsed.add(ParametricCcgParser.DEFAULT_POS_TAG);
        posUsed.add(CcgUtils.START_POS_TAG);
        ccgExamples = Lists.newArrayList();
        for (int i = 0; i < examplesList.size(); i++)
        {
            Expression2 expression = ExpressionParser.expression2().parseSingleExpression(examplesList.get(i)[1]);
            CcgExample example = CcgUtils.createCcgExample(examplesList.get(i)[0], expression, posUsed);
            ccgExamples.add(example);
            List<String> allFunctionNames = LispExecutor.allFunctionNames();
            Set<String> freeSet = StaticAnalysis.getFreeVariables(example.getLogicalForm());
            for (String free : freeSet)
            {
                //add all that's not a function name and not a string (starts and ends with ")
                if (!(free.startsWith("\"") && free.endsWith("\"")) && !allFunctionNames.contains(free))
                    env.bindName(free, free.replace("_", " "), symbolTable);
            }
            //StaticAnalysis.inferType() //TODO: Jayant will add this functionality
        }

        List<StringContext> allContexts = FeaturizedLexiconScorer.getContextsFromExamples(ccgExamples);
        FeatureVectorGenerator<StringContext> featureVectorGenerator = DictionaryFeatureVectorGenerator
                .createFromData(allContexts, featureGenerator, true);

        this.posUsed = posUsed;
        ParametricCcgParser family = CcgUtils.buildParametricCcgParser(lexicon, unaryRulesList,
                posUsed, featureVectorGenerator);
        this.lexicon = Lists.newArrayList(lexicon);
        this.unaryRules = Lists.newArrayList(unaryRulesList);
        this.featureVectorGenerator = featureVectorGenerator;
        this.parserParameters = CcgUtils.train(family, ccgExamples, 10);
        this.parser = family.getModelFromParameters(this.parserParameters);
        this.parserFamily = family;
    }

    public ActionResponse evaluate(IAllUserActions allUserActions, String userSays, Expression2 expression)
    {

        LispExecutor lispExecutor = new LispExecutor(allUserActions, new InfoForCommand(userSays, expression));

        //env.bindName("sendEmail", lispExecutor.getFunction("sendEmail"), symbolTable);
        //env.bindName("setFieldFromString", lispExecutor.getFunction("setFieldFromString"), symbolTable);
        //env.bindName("getProbFieldByInstanceNameAndFieldName", lispExecutor.getFunction("getProbFieldByInstanceNameAndFieldName"), symbolTable);

        List<LispExecutor.FunctionToExecute> functionToExecutes = lispExecutor.getAllFunctions();
        //this is ok, since we use the same function name it should override any old definition
        functionToExecutes.forEach(function -> env.bindName(function.getFunctionName(), function, symbolTable));

        //change in evaluation that every unknown name will change to it as a string (changing "_" with " ")

        //env.bindName("body", "body", symbolTable);
        //env.bindName("outgoing_email", "outgoing email", symbolTable);
        //env.bindName("recipient_list", "recipient list", symbolTable);
        //env.bindName("subject", "subject", symbolTable);
        //env.bindName("bob", "bob", symbolTable);

        LispEval lispEval = new LispEval(symbolTable);
        SExpression sExpression = ExpressionParser.sExpression(symbolTable).parseSingleExpression(expression.toString());
        System.out.println(sExpression.toString());
        LispEval.EvalResult result = lispEval.eval(sExpression, env);

        return (ActionResponse) result.getValue();
    }

    public CcgUtils.SayAndExpression ParseAndEval(IAllUserActions allUserActions, String userSays)
    {
        Expression2 expression;
        ActionResponse response;
        expression = CcgUtils.parse(parser, userSays, posUsed);
        //System.out.println("debug:" + expression.toString());
        response = this.evaluate(allUserActions, userSays, expression);
        return new CcgUtils.SayAndExpression(response.getSayToUser(), expression.toString(), response.isSuccess());
    }

    /**
   * Adds new lexicon entries and unary rules to the grammar of the
   * CCG parser in {@code settings}.
   *
   * @param lexiconEntries
   * @param unaryRules
     */
  public void updateParserGrammar(List<LexiconEntry> lexiconEntries, List<CcgUnaryRule> unaryRules) {
    lexicon.addAll(lexiconEntries);
    this.unaryRules.addAll(unaryRules);

    ParametricCcgParser newFamily = CcgUtils.buildParametricCcgParser(lexicon, this.unaryRules,
            posUsed, featureVectorGenerator);
    SufficientStatistics newParameters = newFamily.getNewSufficientStatistics();
    newParameters.transferParameters(parserParameters);
    parserParameters = newParameters;
    parserFamily = newFamily;
    parser = newFamily.getModelFromParameters(newParameters);
  }

    public void updateParserGrammar(String newLexicon)
    {
      List<String> lexiconAsList = new LinkedList<>();
      lexiconAsList.add(newLexicon);
      List<LexiconEntry> lexiconEntries = LexiconEntry.parseLexiconEntries(lexiconAsList);
      this.updateParserGrammar(lexiconEntries, new LinkedList<>());
    }

    @Override
    public ParserSettings clone()
    {
        ParserSettings parserSettings = new ParserSettings();
        parserSettings.ccgExamples = new LinkedList<>(ccgExamples);
        parserSettings.lexicon = new LinkedList<>(lexicon); //(LinkedList<LexiconEntry>)lexicon.clone();
        parserSettings.unaryRules = new LinkedList<>(unaryRules);
        parserSettings.featureVectorGenerator = featureVectorGenerator;
        parserSettings.parser = parser;
        parserSettings.parserFamily = parserFamily;
        parserSettings.parserParameters = parserParameters.duplicate();
        parserSettings.env = env;
        parserSettings.symbolTable = new IndexedList<String>(symbolTable);
        parserSettings.posUsed = posUsed;
        return parserSettings;
    }

    public List<CcgExample> ccgExamples;
    public List<LexiconEntry> lexicon;
    public List<CcgUnaryRule> unaryRules;
    public FeatureVectorGenerator<StringContext> featureVectorGenerator;
    public CcgParser parser;
    public ParametricCcgParser parserFamily;
    public SufficientStatistics parserParameters;

    public Environment env;
    public IndexedList<String> symbolTable;
    public Set<String> posUsed;
}
