package instructable.server.ccg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jayantkrish.jklol.ccg.*;
import com.jayantkrish.jklol.ccg.cli.AlignmentLexiconInduction;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.*;
import com.jayantkrish.jklol.ccg.lexinduct.*;
import com.jayantkrish.jklol.ccg.supertag.ListSupertaggedSentence;
import com.jayantkrish.jklol.ccg.supertag.SupertaggedSentence;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.lisp.LispEval;
import com.jayantkrish.jklol.lisp.SExpression;
import com.jayantkrish.jklol.models.DiscreteVariable;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.preprocessing.DictionaryFeatureVectorGenerator;
import com.jayantkrish.jklol.preprocessing.FeatureVectorGenerator;
import com.jayantkrish.jklol.training.*;
import com.jayantkrish.jklol.util.CsvParser;
import com.jayantkrish.jklol.util.IndexedList;
import com.jayantkrish.jklol.util.PairCountAccumulator;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import instructable.server.ActionResponse;
import instructable.server.IAllUserActions;
import instructable.server.InfoForCommand;
import instructable.server.LispExecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;


public class CcgUtils
{

    /**
     * Induces a collection of lexicon entries from a data set of text
     * paired with logical forms.
     *
     * @param ccgExamples
     * @return
     */
    public static List<LexiconEntry> induceLexiconEntriesFromExamples(List<CcgExample> ccgExamples)
    {
        List<AlignmentExample> examples = Lists.newArrayList();
        Set<Expression2> allExpressions = Sets.newHashSet();

        for (CcgExample ccgExample : ccgExamples)
        {
            ExpressionTree tree = expressionToExpressionTree(ccgExample.getLogicalForm());
            examples.add(new AlignmentExample(ccgExample.getSentence().getWords(), tree));

            tree.getAllExpressions(allExpressions);
        }




    FeatureVectorGenerator<Expression2> featureVectorGen = DictionaryFeatureVectorGenerator
        .createFromData(allExpressions, new ExpressionTokenFeatureGenerator(Collections.<String>emptySet()), false);

    // TODO: add an option to the alignment model that allows it to
    // align strings in the expressions to the same string in the
    // input text.
    int ngramLength = 1;
    ParametricCfgAlignmentModel pam = ParametricCfgAlignmentModel.buildAlignmentModelWithNGrams(
          examples, featureVectorGen, ngramLength);
    
    SufficientStatistics smoothing = pam.getNewSufficientStatistics();
    smoothing.increment(0.1);

    SufficientStatistics initial = pam.getNewSufficientStatistics();
    initial.increment(1);

    ExpectationMaximization em = new ExpectationMaximization(10, new NullLogFunction());
    SufficientStatistics trainedParameters = em.train(new CfgAlignmentEmOracle(pam, smoothing),
        initial, examples);

    CfgAlignmentModel model = pam.getModelFromParameters(trainedParameters);
    PairCountAccumulator<List<String>, LexiconEntry> alignments = AlignmentLexiconInduction
        .generateLexiconFromAlignmentModel(model, examples, Maps.<String, String>newHashMap());
    
    List<LexiconEntry> entries = Lists.newArrayList();
    entries.addAll(alignments.getKeyValueMultimap().values());
    
    return entries;
  }

  /**
   * 
   * @param expression
   * @return
   */
  private static ExpressionTree expressionToExpressionTree(Expression2 expression) {
    ExpressionSimplifier simplifier = getExpressionSimplifier();
    Set<String> constantsToIgnore = Sets.newHashSet();
    Map<String, String> typeReplacements = Maps.newHashMap();
    
    return ExpressionTree.fromExpression(expression, simplifier, typeReplacements,
        constantsToIgnore, 0, 2);
  }

    public static void updateParserGrammar(String newLexicon, ParserSettings parserSettings)
    {
        List<String> lexiconAsList = new LinkedList<String>();
        lexiconAsList.add(newLexicon);
        List<LexiconEntry> lexiconEntries = LexiconEntry.parseLexiconEntries(lexiconAsList);
        updateParserGrammar(lexiconEntries, new LinkedList<CcgUnaryRule>(), parserSettings);
    }


  /**
   * Adds new lexicon entries and unary rules to the grammar of the
   * CCG parser in {@code settings}.
   * 
   * @param lexiconEntries
   * @param unaryRules
   * @param settings
   */
  public static void updateParserGrammar(List<LexiconEntry> lexiconEntries, List<CcgUnaryRule> unaryRules,
      ParserSettings settings) {
    settings.lexicon.addAll(lexiconEntries);
    settings.unaryRules.addAll(unaryRules);

    ParametricCcgParser newFamily = buildParametricCcgParser(settings.lexicon, settings.unaryRules, settings.posUsed);
    SufficientStatistics newParameters = newFamily.getNewSufficientStatistics();
    newParameters.transferParameters(settings.parserParameters);
    settings.parserParameters = newParameters;
    settings.parser = newFamily.getModelFromParameters(newParameters);
  }

  public static CcgExample createCcgExample(List<String> tokens, Expression2 expression) {
      List<String> pos = Collections.nCopies(tokens.size(), ParametricCcgParser.DEFAULT_POS_TAG);
      SupertaggedSentence sentence = ListSupertaggedSentence.createWithUnobservedSupertags(tokens, pos);

      return new CcgExample(sentence, null, null, expression, null);
  }


    /**
     * Creates a family of CCG parsing models given a collection
     * of lexicon entries. The returned {@code ParametricCcgParser} must
     * be trained before it can be used for semantic parsing. See the
     * {@link #train} method.
     * <p>
     * <p>
     * The returned object is an infinite family of CCG parsers, where
     * each CCG parser is defined by a particular parameter vector.
     *
     * @param lexiconEntries
     * @return
     */
    public static ParametricCcgParser buildParametricCcgParser(List<LexiconEntry> lexiconEntries,
                                                               List<CcgUnaryRule> inputUnaryRules,
                                                               Set<String> posSet)
    {
        CcgCategory stringCategory = CcgCategory.parseFrom("String{0},(lambda $0 $0),0 special:string");
        //CcgCategory stringCategory = CcgCategory.parseFrom("S{0},(lambda $0 (unknownCommand),0 unknownCommand");

        List<Set<String>> assignments = Lists.newArrayList();
        assignments.add(Sets.newHashSet(ParametricCcgParser.SKIP_PREDICATE));
        CcgCategory stringSkipCategory = new CcgCategory(ParametricCcgParser.SKIP_CAT,
                ParametricCcgParser.SKIP_LF, Collections.<String>emptyList(),
                Collections.<Integer>emptyList(), Collections.<Integer>emptyList(), assignments);
        CcgFeatureFactory featureFactory = new InstCcgFeatureFactory(Arrays.asList(stringCategory, stringSkipCategory));
        // Read in the lexicon to instantiate the model.

        List<CcgBinaryRule> binaryRules = Lists.newArrayList();
        List<CcgUnaryRule> unaryRules = Lists.newArrayList(inputUnaryRules);
        unaryRules.add(CcgUnaryRule.parseFrom("DUMMY{0} DUMMY{0},(lambda x x)"));

        boolean allowComposition = true;
        boolean skipWords = true;
        boolean normalFormOnly = false;

        return ParametricCcgParser.parseFromLexicon(lexiconEntries, binaryRules, unaryRules,
                featureFactory, posSet, allowComposition, null, skipWords, normalFormOnly);
    }

    /**
     * Trains a CCG parser given a collection of training examples. This
     * method selects a particular CCG parser from the given parametric family
     * (i.e., by selecting a particular parameter vector using the training
     * data).
     *
     * @param parametricCcgParser
     * @param trainingExamples
     * @return
     */
    public static SufficientStatistics train(ParametricCcgParser parametricCcgParser,
                                             List<CcgExample> trainingExamples) {
        ExpressionSimplifier simplifier = getExpressionSimplifier();
        ExpressionComparator comparator = new SimplificationComparator(simplifier);

        int beamSize = 5000;
        CcgInference inferenceAlgorithm = new CcgBeamSearchInference(null, comparator, beamSize,
                -1, Integer.MAX_VALUE, Runtime.getRuntime().availableProcessors(), false);
        GradientOracle<CcgParser, CcgExample> oracle = new CcgPerceptronOracle(parametricCcgParser,
                inferenceAlgorithm, 0.0);

        int numIterations = 10 * trainingExamples.size();
        double l2Regularization = 0.01;
        GradientOptimizer trainer = StochasticGradientTrainer.createWithL2Regularization(numIterations,
                1, 1.0, true, true, l2Regularization, new NullLogFunction());
        SufficientStatistics parameters = trainer.train(oracle, oracle.initializeGradient(),
                trainingExamples);
        //to see the parameters that were actually used:
        //parametricCcgParser.getParameterDescription(parameters)
        return parameters;
    }

    public static CcgExample createCcgExample(String sentence, Expression2 expression, Set<String> usedPOS)
    {
        //List<String> pos = Collections.nCopies(tokens.size(), ParametricCcgParser.DEFAULT_POS_TAG);
        List<String> tokens = new LinkedList<>();
        List<String> poss = new LinkedList<>();
        tokenizeAndPOS(sentence, tokens, poss, true, usedPOS);
        SupertaggedSentence supertaggedSentence = ListSupertaggedSentence.createWithUnobservedSupertags(tokens, poss);

        return new CcgExample(supertaggedSentence, null, null, expression, null);
    }


    static MaxentTagger maxentTagger = new MaxentTagger("resources/english-left3words-distsim.tagger");

    /*
    if training, any POS used are added to allowedOrUsedPOS.
    if train == false, uses only POS from allowedOrUsedPOS
     */
    public static void tokenizeAndPOS(String sentence, List<String> outTokens, List<String> outPOSs, boolean train, Set<String> allowedOrUsedPOS)
    {

        final List<String> excludeTokens = Arrays.asList(new String[]{",", ".", "!", "(", ")", "!", "?"});

        //List<String> tokens = new LinkedList<>();
        //PTBTokenizer ptbTokenizer = PTBTokenizer.newPTBTokenizer(new StringReader(sentence));
        String[] tokensAndPOS = maxentTagger.tagString(sentence).split(" ");
        //while(ptbTokenizer.hasNext())
        for (String tokenAndPOS : tokensAndPOS)
        {
            //remove punctuation (but not apostrophes!)
            //TODO: what to do with uppercase? Ignoring for now.
            //String token = ptbTokenizer.next().toString().toLowerCase();

            int idx = tokenAndPOS.lastIndexOf('_');//can't use split because might have '_' in the token
            String token = tokenAndPOS.substring(0, idx);
            String POS = tokenAndPOS.substring(idx + 1);
            if (!excludeTokens.contains(token))
            {
                outTokens.add(token);
                //POSs.add(POS);
                if (train)
                {
                    allowedOrUsedPOS.add(POS);
                    outPOSs.add(POS);
                }
                else
                {
                    if (allowedOrUsedPOS.contains(POS))
                        outPOSs.add(POS);
                    else
                        outPOSs.add(ParametricCcgParser.DEFAULT_POS_TAG);
                }
            }
        }
    }

    /**
     * Parses a sentence text using {@code parser} to produce a
     * logical form. The text is represented by a list of tokens
     * (e.g., obtained by splitting the input on spaces). The returned
     * logical form is given in a LISP S-Expression format.
     *
     * @param parser
     * @param sentence
     * @return
     */
    public static Expression2 parse(CcgParser parser, String sentence, Set<String> posAllowed)
    {
        CcgInference inferenceAlg = new CcgExactInference(null, -1L, Integer.MAX_VALUE, 1);
        ExpressionSimplifier simplifier = getExpressionSimplifier();

        //List<String> pos = Collections.nCopies(tokens.size(), ParametricCcgParser.DEFAULT_POS_TAG);
        List<String> tokens = new LinkedList<>();
        List<String> poss = new LinkedList<>();
        tokenizeAndPOS(sentence, tokens, poss, false, posAllowed);
        SupertaggedSentence supertaggedSentence = ListSupertaggedSentence.createWithUnobservedSupertags(tokens, poss);

        //      //if we want to return only sentences and fieldVal in upper level:
        DiscreteVariable dv = parser.getSyntaxVarType();

        //upto here
        CcgParse parse = inferenceAlg.getBestParse(parser, supertaggedSentence, new InstChartCost(), new NullLogFunction());
        //TODO: if parse is empty we may want to call unknownCommand, but if there is no parse, this might be a problem.
        //TODO: we may want just to return an error.
        return simplifier.apply(parse.getLogicalForm());
    }

    public static ExpressionSimplifier getExpressionSimplifier()
    {
        return new ExpressionSimplifier(Arrays.
                <ExpressionReplacementRule>asList(new LambdaApplicationReplacementRule(),
                        new VariableCanonicalizationReplacementRule()));
    }


    public static ParserSettings getParserSettings(List<String> lexiconEntries, String[] unaryRules, List<String[]> examplesList)
    {
        ParserSettings parserSettings = new ParserSettings();
        parserSettings.env = Environment.empty();
        parserSettings.symbolTable = IndexedList.create();


        List<LexiconEntry> lexicon = LexiconEntry.parseLexiconEntries(lexiconEntries);


        List<CcgUnaryRule> unaryRulesList = Lists.newArrayList();
        for (String unaryRule : unaryRules)
        {
            unaryRulesList.add(CcgUnaryRule.parseFrom(unaryRule));
        }

        Set<String> posUsed = new HashSet<>();
        posUsed.add(ParametricCcgParser.DEFAULT_POS_TAG);
        List<CcgExample> ccgExamples = Lists.newArrayList();
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
                    parserSettings.env.bindName(free, free.replace("_", " "), parserSettings.symbolTable);
            }
            //StaticAnalysis.inferType() //TODO: Jayant will add this functionality
        }

        parserSettings.posUsed = posUsed;
        ParametricCcgParser family = CcgUtils.buildParametricCcgParser(lexicon, unaryRulesList, posUsed);
        parserSettings.lexicon = lexicon;
        parserSettings.unaryRules = unaryRulesList;
        parserSettings.parserParameters = CcgUtils.train(family, ccgExamples);
        parserSettings.parser = family.getModelFromParameters(parserSettings.parserParameters);
        return parserSettings;
    }

    public static ActionResponse evaluate(IAllUserActions allUserActions, String userSays, Expression2 expression, ParserSettings parserSettings)
    {

        LispExecutor lispExecutor = new LispExecutor(allUserActions, new InfoForCommand(userSays, expression));

        //env.bindName("sendEmail", lispExecutor.getFunction("sendEmail"), symbolTable);
        //env.bindName("setFieldFromString", lispExecutor.getFunction("setFieldFromString"), symbolTable);
        //env.bindName("getProbFieldByInstanceNameAndFieldName", lispExecutor.getFunction("getProbFieldByInstanceNameAndFieldName"), symbolTable);

        List<LispExecutor.FunctionToExecute> functionToExecutes = lispExecutor.getAllFunctions();
        //this is ok, since we use the same function name it should override any old definition
        functionToExecutes.forEach(function -> parserSettings.env.bindName(function.getFunctionName(), function, parserSettings.symbolTable));

        //change in evaluation that every unknown name will change to it as a string (changing "_" with " ")

        //env.bindName("body", "body", symbolTable);
        //env.bindName("outgoing_email", "outgoing email", symbolTable);
        //env.bindName("recipient_list", "recipient list", symbolTable);
        //env.bindName("subject", "subject", symbolTable);
        //env.bindName("bob", "bob", symbolTable);

        LispEval eval = new LispEval(parserSettings.symbolTable);
        SExpression sExpression = ExpressionParser.sExpression(parserSettings.symbolTable).parseSingleExpression(expression.toString());
        LispEval.EvalResult result = eval.eval(sExpression, parserSettings.env);

        return (ActionResponse) result.getValue();
    }


    public static List<String[]> loadExamples(java.nio.file.Path filePath)
    {
        final String cvsSplitBy = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
        List<String[]> retVal = new LinkedList<>();
        try
        {

            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filePath.toString())));

            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                line = line.replace("\"\"", "\\\"");
                retVal.add(CsvParser.defaultParser().parseLine(line));
            }
            //use reflection, check all types of input functions and then add all other variables as their types.
            //should use learner
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return retVal;
    }


    public static ActionResponse ParseAndEval(IAllUserActions allUserActions, ParserSettings parserSettings, String userSays)
    {
        Expression2 expression;
        ActionResponse response;
        expression = CcgUtils.parse(parserSettings.parser, userSays, parserSettings.posUsed);
        System.out.println("debug:" + expression.toString());
        response = CcgUtils.evaluate(allUserActions, userSays, expression, parserSettings);
        return response;
    }


    private CcgUtils()
    {
        // Prevent instantiation.
    }
}
