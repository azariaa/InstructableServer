package instructable.server.ccg;

import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.*;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionSimplifier;
import com.jayantkrish.jklol.ccg.lambda2.StaticAnalysis;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer.StringContext;
import com.jayantkrish.jklol.ccg.supertag.ListSupertaggedSentence;
import com.jayantkrish.jklol.ccg.supertag.SupertaggedSentence;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.lisp.LispEval;
import com.jayantkrish.jklol.lisp.SExpression;
import com.jayantkrish.jklol.models.DiscreteVariable;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.preprocessing.DictionaryFeatureVectorGenerator;
import com.jayantkrish.jklol.preprocessing.FeatureGenerator;
import com.jayantkrish.jklol.preprocessing.FeatureVectorGenerator;
import com.jayantkrish.jklol.training.NullLogFunction;
import com.jayantkrish.jklol.util.IndexedList;
import instructable.server.ActionResponse;
import instructable.server.IAllUserActions;
import instructable.server.InfoForCommand;
import instructable.server.LispExecutor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Amos Azaria on 05-May-15.
 */
public class ParserSettings implements Cloneable
{
    private ParserSettings()
    {

    }

    public ParserSettings (List<String> lexiconEntries, List<String> synonyms, String[] unaryRules,
                           FeatureGenerator<StringContext, String> featureGenerator, List<String[]> examplesList)
    {
        final String midRowComment = "//";
        final String fullRowComment = "#";
        env = Environment.empty();
        symbolTable = IndexedList.create();

        // remove all that appears after a "//" or starts with # (parseLexiconEntries only removes lines that start with "#")
        List<String> lexiconWithoutComments = lexiconEntries.stream().filter(e->!e.contains(fullRowComment)).map(e -> (e.contains(midRowComment) ? e.substring(0, e.indexOf(midRowComment)) : e)).collect(Collectors.toList());

        // add synonyms. format: newWord, {meaning1,meaning2,...} (i.e, newWord = meaning1 U meaning2)
        List<String> lexEntriesFromSyn = new LinkedList<>();
        for (String synonym : synonyms)
        {
            if (synonym.trim().length() <=1 || synonym.contains(fullRowComment) || synonym.trim().startsWith(midRowComment)) //comment row
                continue;
            String newWord = synonym.substring(0,synonym.indexOf(","));
            //newWord.replace("\"","");
            String[] meanings = synonym.substring(synonym.indexOf("{")+1,synonym.indexOf("}")).split(",");
            for (String meaning : meanings)
            {
                String meaningWOQuotes = meaning.trim().replace("\"","");
                for (String lexiconEntry : lexiconWithoutComments) //could save time if transferred all entries to a map, but this is anyway done only once
                {
                    String entryWord = lexiconEntry.substring(0,lexiconEntry.indexOf(",")).replace("\"", "");
                    if (entryWord.equals(meaningWOQuotes))
                    {
                        String newLexiconEntry = newWord + lexiconEntry.substring(lexiconEntry.indexOf(","));
                        lexEntriesFromSyn.add(newLexiconEntry);
                    }
                }
            }
        }

        List<String> allLexiconEntries = new LinkedList<>(lexiconWithoutComments);
        allLexiconEntries.addAll(lexEntriesFromSyn);

        List<LexiconEntry> lexicon = LexiconEntry.parseLexiconEntries(allLexiconEntries);

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
            CcgExample example = CcgUtils.createCcgExample(examplesList.get(i)[0], expression, posUsed, true);
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

        ActionResponse response;
        try
        {
            LispEval.EvalResult result = lispEval.eval(sExpression, env);
            response = (ActionResponse) result.getValue();
        }
        catch (ActionResponse actionResponse)
        {
            response = actionResponse;
        }

        return  response;
    }

    public CcgUtils.SayAndExpression ParseAndEval(IAllUserActions allUserActions, String userSays)
    {
        Expression2 expression;
        ActionResponse response;
        expression = parse(userSays);
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

    /**
     * Parses a sentence text using {@code parser} to produce a
     * logical form. The text is represented by a list of tokens
     * (e.g., obtained by splitting the input on spaces). The returned
     * logical form is given in a LISP S-Expression format.
     *
     * @param sentence
     * @return
     */
    private Expression2 parse(String sentence)
    {
        CcgInference inferenceAlg = new CcgExactInference(null, -1L, Integer.MAX_VALUE, 1);
        ExpressionSimplifier simplifier = CcgUtils.getExpressionSimplifier();

        //List<String> pos = Collections.nCopies(tokens.size(), ParametricCcgParser.DEFAULT_POS_TAG);
        List<String> tokens = new LinkedList<>();
        List<String> poss = new LinkedList<>();
        tokens.add(CcgUtils.startSymbol);
        poss.add(CcgUtils.START_POS_TAG);
        CcgUtils.tokenizeAndPOS(sentence, tokens, poss, false, posUsed);
        SupertaggedSentence supertaggedSentence = ListSupertaggedSentence.createWithUnobservedSupertags(tokens, poss);

        //      //if we want to return only sentences and fieldVal in upper level:
        DiscreteVariable dv = parser.getSyntaxVarType();

        //upto here
        CcgParse parse = inferenceAlg.getBestParse(parser, supertaggedSentence, new InstChartCost(), new NullLogFunction());
        //if parse is empty we want to parse to unknownCommand
        Expression2 expression;
        if (parse == null)
            expression = ExpressionParser.expression2().parseSingleExpression("(" + IAllUserActions.unknownCommandStr + ")");
        else
            expression = parse.getLogicalForm();
        return simplifier.apply(expression);
    }

    public void addTrainingEg(String originalCommand, List<Expression2> commandsLearnt)
    {
        Expression2 expressionLearnt = CcgUtils.combineCommands(commandsLearnt);
//        FileWriter out = null;
//        try
//        {
//            out = new FileWriter(tempFileName, true);
//            out.write(originalCommand + "," + expression.toString() + "\n");
//            out.close();
//        } catch (IOException e)
//        {
//            e.printStackTrace();
//        }

        CcgExample example = CcgUtils.createCcgExample(originalCommand, expressionLearnt, posUsed, false);

        List<LexiconEntry> newEntries = CcgUtils.induceLexiconEntriesHeuristic(example, parser);
        System.out.println(newEntries);

        updateParserGrammar(newEntries, Lists.newArrayList());
        ccgExamples.add(example);
        SufficientStatistics newParameters = CcgUtils.train(parserFamily, ccgExamples, 2);

        parser = parserFamily.getModelFromParameters(newParameters);
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
