package instructable.server.ccg;

import instructable.server.ActionResponse;
import instructable.server.IAllUserActions;
import instructable.server.InfoForCommand;
import instructable.server.LispExecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jayantkrish.jklol.ccg.CcgBeamSearchInference;
import com.jayantkrish.jklol.ccg.CcgBinaryRule;
import com.jayantkrish.jklol.ccg.CcgCategory;
import com.jayantkrish.jklol.ccg.CcgExactInference;
import com.jayantkrish.jklol.ccg.CcgExample;
import com.jayantkrish.jklol.ccg.CcgFeatureFactory;
import com.jayantkrish.jklol.ccg.CcgInference;
import com.jayantkrish.jklol.ccg.CcgParse;
import com.jayantkrish.jklol.ccg.CcgParser;
import com.jayantkrish.jklol.ccg.CcgPerceptronOracle;
import com.jayantkrish.jklol.ccg.CcgUnaryRule;
import com.jayantkrish.jklol.ccg.HeadedSyntacticCategory;
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.ccg.ParametricCcgParser;
import com.jayantkrish.jklol.ccg.SyntacticCategory.Direction;
import com.jayantkrish.jklol.ccg.cli.AlignmentLexiconInduction;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionComparator;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionReplacementRule;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionSimplifier;
import com.jayantkrish.jklol.ccg.lambda2.LambdaApplicationReplacementRule;
import com.jayantkrish.jklol.ccg.lambda2.SimplificationComparator;
import com.jayantkrish.jklol.ccg.lambda2.StaticAnalysis;
import com.jayantkrish.jklol.ccg.lambda2.VariableCanonicalizationReplacementRule;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer.StringContext;
import com.jayantkrish.jklol.ccg.lexinduct.AlignmentExample;
import com.jayantkrish.jklol.ccg.lexinduct.CfgAlignmentEmOracle;
import com.jayantkrish.jklol.ccg.lexinduct.CfgAlignmentModel;
import com.jayantkrish.jklol.ccg.lexinduct.ExpressionTokenFeatureGenerator;
import com.jayantkrish.jklol.ccg.lexinduct.ExpressionTree;
import com.jayantkrish.jklol.ccg.lexinduct.ParametricCfgAlignmentModel;
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
import com.jayantkrish.jklol.training.ExpectationMaximization;
import com.jayantkrish.jklol.training.GradientOptimizer;
import com.jayantkrish.jklol.training.GradientOracle;
import com.jayantkrish.jklol.training.NullLogFunction;
import com.jayantkrish.jklol.training.StochasticGradientTrainer;
import com.jayantkrish.jklol.util.CsvParser;
import com.jayantkrish.jklol.util.IndexedList;
import com.jayantkrish.jklol.util.IntegerArrayIterator;
import com.jayantkrish.jklol.util.PairCountAccumulator;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;


public class CcgUtils
{
  
  private static Set<String> POS_TAGS_TO_SKIP_IN_LEXICON_INDUCTION = Sets.newHashSet("DT", "IN", "TO", "CC");
  private static String START_POS_TAG = "START";

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
    
  public static List<LexiconEntry> induceLexiconEntriesHeuristic(CcgExample example, CcgParser parser)  {
	  ExpressionSimplifier simplifier = getExpressionSimplifier();
	  
	  List<String> words = example.getSentence().getWords();
	  List<String> pos = example.getSentence().getPosTags();
	  Expression2 lf = simplifier.apply(example.getLogicalForm());
	  
	  List<Integer> spanStarts = Lists.newArrayList();
	  List<Integer> spanEnds = Lists.newArrayList();
	  List<List<String>> spanStrings = Lists.newArrayList();
	  List<List<Expression2>> spanExpressions = Lists.newArrayList();
	  List<List<HeadedSyntacticCategory>> spanSyntacticCategories = Lists.newArrayList();
	  
	  for (int i = 0; i < words.size(); i++) {
		  for (int j = i; j < words.size(); j++) {
			  List<String> subwords = words.subList(i, j + 1);
			  List<String> subpos = pos.subList(i, j + 1);
			  List<LexiconEntry> entries = parser.getLexiconEntries(subwords, subpos);

			  List<Expression2> matchedExpressions = Lists.newArrayList();
			  List<HeadedSyntacticCategory> matchedSyntacticCategories = Lists.newArrayList();
			  for (LexiconEntry entry : entries) {
				  Expression2 lexLf = simplifier.apply(entry.getCategory().getLogicalForm());
				  if (hasSubexpression(lf, lexLf)) {
					  matchedExpressions.add(lexLf);
					  matchedSyntacticCategories.add(entry.getCategory().getSyntax());
				  }
			  }

			  if (matchedExpressions.size() > 0) {
				  spanStarts.add(i);
				  spanEnds.add(j);
				  spanStrings.add(subwords);
				  spanExpressions.add(matchedExpressions);
				  spanSyntacticCategories.add(matchedSyntacticCategories);
			  }
		  }
	  }
	  
	  // System.out.println(spanStarts);
	  // System.out.println(spanEnds);
	  // System.out.println(spanStrings);
	  // System.out.println(spanExpressions);
	  // System.out.println(spanSyntacticCategories);
	  
	  int[] numEntriesPerSpan = new int[spanStarts.size()];
	  for (int i = 0; i < spanStarts.size(); i++) {
		  numEntriesPerSpan[i] = spanExpressions.get(i).size();
	  }

	  List<LexiconEntry> newEntries = Lists.newArrayList();
	  Iterator<int[]> subsetIter = new IntegerArrayIterator(numEntriesPerSpan, new int[0]);
	  while (subsetIter.hasNext()) {
		  int[] indexes = subsetIter.next();
		  Expression2 substituted = lf;
		  for (int i = 0; i < spanStarts.size(); i++) {
			  Expression2 var = Expression2.constant("$" + i);
			  substituted = replaceSubexpression(substituted, spanExpressions.get(i).get(indexes[i]), var);
		  }
		  
		  for (int i = 1; i < words.size(); i++) {
		    boolean containedInSpan = false;
		    for (int j = 0 ; j < spanStarts.size(); j++) {
		      if (i >= spanStarts.get(j) && i <= spanEnds.get(j)) {
		        containedInSpan = true;
		        break;
		      }
		    }
		    
		    if (containedInSpan) {
		      continue;
		    }
		    
		    if (POS_TAGS_TO_SKIP_IN_LEXICON_INDUCTION.contains(pos.get(i))) {
		      continue;
		    }
		    
		    // System.out.println(words.get(i) + "/" + pos.get(i));
		
		    Expression2 lambdaExpression = substituted;
		    List<Integer> leftArgEnds = Lists.newArrayList();
		    List<Integer> rightArgStarts = Lists.newArrayList();
		    for (int j = 0; j < spanStarts.size(); j++) {
		      if (spanStarts.get(j) > i) {
		        rightArgStarts.add(spanStarts.get(j));
		      } else {
		        leftArgEnds.add(spanEnds.get(j));
		      }
		    }
		    
		    Collections.sort(leftArgEnds);
		    Collections.sort(rightArgStarts);
		    Collections.reverse(rightArgStarts);
		    
		    HeadedSyntacticCategory cat = HeadedSyntacticCategory.parseFrom("S{0}");
		    for (int j = 0; j < leftArgEnds.size(); j++) {
		      HeadedSyntacticCategory argCat = null;
		      int argCatIndex = -1;
		      for (int k = 0; k < spanStarts.size(); k++) {
		        if (spanEnds.get(k) == leftArgEnds.get(j)) {
		          argCat = spanSyntacticCategories.get(k).get(indexes[k]);
		          argCatIndex = k;
		        }
		      }
		      
		      cat = cat.addArgument(argCat, Direction.LEFT, 0);
		      lambdaExpression = Expression2.nested(Expression2.constant("lambda"),
		          Expression2.constant("$" + argCatIndex), lambdaExpression);
		    }
		    
		    for (int j = 0; j < rightArgStarts.size(); j++) {
		      HeadedSyntacticCategory argCat = null;
		      int argCatIndex = -1;
		      for (int k = 0; k < spanStarts.size(); k++) {
		        if (spanStarts.get(k) == rightArgStarts.get(j)) {
		          argCat = spanSyntacticCategories.get(k).get(indexes[k]);
		          argCatIndex = k;
		        }
		      }

		      cat = cat.addArgument(argCat, Direction.RIGHT, 0);
		      lambdaExpression = Expression2.nested(Expression2.constant("lambda"),
		          Expression2.constant("$" + argCatIndex), lambdaExpression);
		    }

		    cat = cat.getCanonicalForm();
		    // System.out.println(cat);
		    // System.out.println(lambdaExpression);
		    
		    List<String> subjects = Lists.newArrayList();
		    List<Integer> argumentNumbers = Lists.newArrayList();
		    List<Integer> objects = Lists.newArrayList();
		    int numVars = cat.getUniqueVariables().length;
		    List<Set<String>> assignments = Lists.newArrayList();
		    for (int j = 0; j < numVars; j++) {
		      assignments.add(Collections.<String>emptySet());
		    }
		    CcgCategory ccgCategory = new CcgCategory(cat, lambdaExpression, subjects, argumentNumbers, objects, assignments);
		    newEntries.add(new LexiconEntry(words.subList(i, i + 1), ccgCategory));
		  }
	  }

	  return newEntries;
  }
  
  // These are really hacky ways of solving this problem...
  public static boolean hasSubexpression(Expression2 expression, Expression2 subexpression) {
	  String expressionString = expression.toString();
	  String subexpressionString = subexpression.toString();

	  return expressionString.contains(subexpressionString);
  }
  
  public static Expression2 replaceSubexpression(Expression2 expression, Expression2 subexpression, Expression2 replacement) {
	  String expressionString = expression.toString();
	  String subexpressionString = subexpression.toString();
	  String replacementString = replacement.toString();
	  
	  return ExpressionParser.expression2().parseSingleExpression(expressionString.replace(
			  subexpressionString, replacementString));
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

    ParametricCcgParser newFamily = buildParametricCcgParser(settings.lexicon, settings.unaryRules,
        settings.posUsed, settings.featureVectorGenerator);
    SufficientStatistics newParameters = newFamily.getNewSufficientStatistics();
    newParameters.transferParameters(settings.parserParameters);
    settings.parserParameters = newParameters;
    settings.parserFamily = newFamily; 
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
                                                               Set<String> posSet,
                                                               FeatureVectorGenerator<StringContext> featureVectorGenerator)
    {
        CcgCategory stringCategory = CcgCategory.parseFrom("String{0},(lambda $0 $0),0 special:string");
        CcgCategory unknownCommandCategory = CcgCategory.parseFrom("S{0},(lambda $0 (unknownCommand)),0 unknownCommand");
        List<LexiconEntry> unknownWordLexiconEntries = Lists.newArrayList();

        CcgFeatureFactory featureFactory = new InstCcgFeatureFactory(Arrays.asList(stringCategory),
            Arrays.asList(unknownCommandCategory), featureVectorGenerator);
        // Read in the lexicon to instantiate the model.

        List<CcgBinaryRule> binaryRules = Lists.newArrayList();
        List<CcgUnaryRule> unaryRules = Lists.newArrayList(inputUnaryRules);
        unaryRules.add(CcgUnaryRule.parseFrom("DUMMY{0} DUMMY{0},(lambda x x)"));

        boolean allowComposition = true;
        boolean skipWords = true;
        boolean normalFormOnly = false;

        return ParametricCcgParser.parseFromLexicon(lexiconEntries, unknownWordLexiconEntries,
            binaryRules, unaryRules, featureFactory, posSet, allowComposition, null, skipWords,
            normalFormOnly);
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

        // TODO undo this.
        // int numIterations = 10 * trainingExamples.size();
        int numIterations = 3 * trainingExamples.size();
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
        tokens.add("start_symbol");
        poss.add(START_POS_TAG);
        tokenizeAndPOS(sentence, tokens, poss, true, usedPOS);
        SupertaggedSentence supertaggedSentence = ListSupertaggedSentence.createWithUnobservedSupertags(tokens, poss);

        return new CcgExample(supertaggedSentence, null, null, expression, null);
    }


    static MaxentTagger maxentTagger = new MaxentTagger("resources/english-left3words-distsim.tagger");

    /*
    returns outTokens in lowercase!
    if training, any POS used are added to allowedOrUsedPOS.
    if train == false, uses only POS from allowedOrUsedPOS
     */
    public static void tokenizeAndPOS(String sentence, List<String> outTokens, List<String> outPOSs, boolean train, Set<String> allowedOrUsedPOS)
    {

        final List<String> excludeTokens = Arrays.asList(new String[]{",", ".", "!", "(", ")", "!", "?","\"",":",";"}); //need to exclude this since they won't be available when using speech.

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
            String token = tokenAndPOS.substring(0, idx).toLowerCase(); //converts all tokens to lowercase!
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
        //if parse is empty we want to parse to unknownCommand
        Expression2 expression;
        if (parse == null)
            expression = ExpressionParser.expression2().parseSingleExpression("(" + IAllUserActions.unknownCommandStr + ")");
        else
            expression = parse.getLogicalForm();
        return simplifier.apply(expression);
    }

    public static ExpressionSimplifier getExpressionSimplifier()
    {
        return new ExpressionSimplifier(Arrays.
                <ExpressionReplacementRule>asList(new LambdaApplicationReplacementRule(),
                        new VariableCanonicalizationReplacementRule()));
    }


    public static ParserSettings getParserSettings(List<String> lexiconEntries, String[] unaryRules,
        FeatureGenerator<StringContext, String> featureGenerator, List<String[]> examplesList)
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
        posUsed.add(START_POS_TAG);
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
        
        List<StringContext> allContexts = FeaturizedLexiconScorer.getContextsFromExamples(ccgExamples);
        FeatureVectorGenerator<StringContext> featureVectorGenerator = DictionaryFeatureVectorGenerator
            .createFromData(allContexts, featureGenerator, true);

        parserSettings.posUsed = posUsed;
        ParametricCcgParser family = CcgUtils.buildParametricCcgParser(lexicon, unaryRulesList,
            posUsed, featureVectorGenerator);
        parserSettings.lexicon = lexicon;
        parserSettings.unaryRules = unaryRulesList;
        parserSettings.featureVectorGenerator = featureVectorGenerator; 
        parserSettings.parserParameters = CcgUtils.train(family, ccgExamples);
        parserSettings.parser = family.getModelFromParameters(parserSettings.parserParameters);
        parserSettings.parserFamily = family;
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
        System.out.println(sExpression.toString());
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

    public static class SayAndExpression
    {
        public SayAndExpression(String sayToUser, String lExpression)
        {
            this.sayToUser = sayToUser;
            this.lExpression = lExpression;
        }
        public String sayToUser;
        public String lExpression;
    }

    public static SayAndExpression ParseAndEval(IAllUserActions allUserActions, ParserSettings parserSettings, String userSays)
    {
        Expression2 expression;
        ActionResponse response;
        expression = CcgUtils.parse(parserSettings.parser, userSays, parserSettings.posUsed);
        //System.out.println("debug:" + expression.toString());
        response = CcgUtils.evaluate(allUserActions, userSays, expression, parserSettings);
        return new SayAndExpression(response.getSayToUser(), expression.toString());
    }


    private CcgUtils()
    {
        // Prevent instantiation.
    }

    public static Expression2 combineCommands(List<Expression2> toCombine)
    {
        Preconditions.checkState(toCombine.size() > 0);
        Expression2 retExpression = toCombine.get(0);
        for (int i = 1; i < toCombine.size(); i++)
        {
            //TODO: should be a better way to combine Expressions
            retExpression = ExpressionParser.expression2().parseSingleExpression("(" + LispExecutor.doSeq + retExpression.toString() + " " + toCombine.get(i).toString() + ")");
        }

        return retExpression;
    }
}
