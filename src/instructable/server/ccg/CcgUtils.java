package instructable.server.ccg;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jayantkrish.jklol.ccg.*;
import com.jayantkrish.jklol.ccg.SyntacticCategory.Direction;
import com.jayantkrish.jklol.ccg.chart.CcgBeamSearchChart;
import com.jayantkrish.jklol.ccg.cli.AlignmentLexiconInduction;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.*;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer.StringContext;
import com.jayantkrish.jklol.ccg.lexinduct.*;
import com.jayantkrish.jklol.ccg.supertag.ListSupertaggedSentence;
import com.jayantkrish.jklol.ccg.supertag.SupertaggedSentence;
import com.jayantkrish.jklol.models.DiscreteVariable;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.preprocessing.DictionaryFeatureVectorGenerator;
import com.jayantkrish.jklol.preprocessing.FeatureVectorGenerator;
import com.jayantkrish.jklol.training.*;
import com.jayantkrish.jklol.util.CsvParser;
import com.jayantkrish.jklol.util.IntegerArrayIterator;
import com.jayantkrish.jklol.util.PairCountAccumulator;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import instructable.server.IAllUserActions;
import instructable.server.LispExecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;


public class CcgUtils
{

  private static Set<String> POS_TAGS_TO_SKIP_IN_LEXICON_INDUCTION = Sets.newHashSet("DT", "IN", "TO", "CC");
  final public static String START_POS_TAG = "START";
  final static String startSymbol = "start_symbol";

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

	  CcgBeamSearchChart chart = new CcgBeamSearchChart(example.getSentence(), -1, 100);
	  parser.parseCommon(chart, example.getSentence(), null, null, -1, 1);
	  for (int i = 1; i < words.size(); i++) {
		  for (int j = words.size() - 1; j >= i; j--) {
			  List<String> subwords = words.subList(i, j + 1);

			  List<Expression2> matchedExpressions = Lists.newArrayList();
			  List<HeadedSyntacticCategory> matchedSyntacticCategories = Lists.newArrayList();
			  List<CcgParse> parses = chart.decodeBestParsesForSpan(i, j, 100, parser);
			  for (CcgParse parse : parses) {
			    Expression2 lexLf = simplifier.apply(parse.getLogicalForm());
				  if (hasSubexpression(lf, lexLf)) {
					  matchedExpressions.add(lexLf);
					  matchedSyntacticCategories.add(parse.getHeadedSyntacticCategory());
				  }
			  }

			  if (matchedExpressions.size() > 0) {
				  spanStarts.add(i);
				  spanEnds.add(j);
				  spanStrings.add(subwords);
				  spanExpressions.add(matchedExpressions);
				  spanSyntacticCategories.add(matchedSyntacticCategories);

				  i = j + 1;
				  break;
			  }
		  }
	  }

	  System.out.println(spanStarts);
	  System.out.println(spanEnds);
	  System.out.println(spanStrings);
	  System.out.println(spanExpressions);
	  System.out.println(spanSyntacticCategories);

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
		    List<Expression2> args = Lists.newArrayList();

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
		      args.add(Expression2.constant("$" + argCatIndex));
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
		      args.add(Expression2.constant("$" + argCatIndex));
		    }

		    cat = cat.getCanonicalForm();
		    args.add(Expression2.constant("lambda"));
		    Collections.reverse(args);
		    args.add(substituted);
		    Expression2 lambdaExpression = Expression2.nested(args);
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
  private static ExpressionTree expressionToExpressionTree(Expression2 expression)
  {
    ExpressionSimplifier simplifier = getExpressionSimplifier();
    Set<String> constantsToIgnore = Sets.newHashSet();
    Map<String, String> typeReplacements = Maps.newHashMap();

    return ExpressionTree.fromExpression(expression, simplifier, typeReplacements,
        constantsToIgnore, 0, 2);
  }

    public static CcgExample createCcgExample(List<String> tokens, Expression2 expression)
    {
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
        tokens.add(startSymbol);
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
        tokens.add(startSymbol);
        poss.add(START_POS_TAG);
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
