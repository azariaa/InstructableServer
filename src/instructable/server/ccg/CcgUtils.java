package instructable.server.ccg;

import instructable.server.ActionResponse;
import instructable.server.IAllUserActions;
import instructable.server.InfoForCommand;
import instructable.server.LispExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.ccg.ParametricCcgParser;
import com.jayantkrish.jklol.ccg.cli.AlignmentLexiconInduction;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionComparator;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionReplacementRule;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionSimplifier;
import com.jayantkrish.jklol.ccg.lambda2.LambdaApplicationReplacementRule;
import com.jayantkrish.jklol.ccg.lambda2.SimplificationComparator;
import com.jayantkrish.jklol.ccg.lambda2.VariableCanonicalizationReplacementRule;
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
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.preprocessing.DictionaryFeatureVectorGenerator;
import com.jayantkrish.jklol.preprocessing.FeatureVectorGenerator;
import com.jayantkrish.jklol.training.ExpectationMaximization;
import com.jayantkrish.jklol.training.GradientOptimizer;
import com.jayantkrish.jklol.training.GradientOracle;
import com.jayantkrish.jklol.training.NullLogFunction;
import com.jayantkrish.jklol.training.StochasticGradientTrainer;
import com.jayantkrish.jklol.util.IndexedList;
import com.jayantkrish.jklol.util.PairCountAccumulator;

public class CcgUtils {
  
  /**
   * Induces a collection of lexicon entries from a data set of text
   * paired with logical forms.
   *  
   * @param ccgExamples
   * @return
   */
  public static List<LexiconEntry> induceLexiconEntriesFromExamples(List<CcgExample> ccgExamples) {
    List<AlignmentExample> examples = Lists.newArrayList();
    Set<Expression2> allExpressions = Sets.newHashSet();

    for (CcgExample ccgExample : ccgExamples) {
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

  /**
   * Creates a family of CCG parsing models given a collection
   * of lexicon entries. The returned {@code ParametricCcgParser} must
   * be trained before it can be used for semantic parsing. See the
   * {@link #train} method.
   * 
   * <p>
   * The returned object is an infinite family of CCG parsers, where
   * each CCG parser is defined by a particular parameter vector.
   * 
   * @param lexiconEntries
   * @return
   */
  public static ParametricCcgParser buildParametricCcgParser(List<LexiconEntry> lexiconEntries) {
    CcgCategory stringCategory = CcgCategory.parseFrom("N{0},(lambda $0 $0),0 special:string");
    
    List<Set<String>> assignments = Lists.newArrayList();
    assignments.add(Sets.newHashSet(ParametricCcgParser.SKIP_PREDICATE));
    CcgCategory stringSkipCategory = new CcgCategory(ParametricCcgParser.SKIP_CAT,
        ParametricCcgParser.SKIP_LF, Collections.<String>emptyList(),
        Collections.<Integer>emptyList(), Collections.<Integer>emptyList(), assignments); 
    CcgFeatureFactory featureFactory = new InMindCcgFeatureFactory(Arrays.asList(stringCategory, stringSkipCategory));
    // Read in the lexicon to instantiate the model.

    List<CcgBinaryRule> binaryRules = Lists.newArrayList();
    List<CcgUnaryRule> unaryRules = Lists.newArrayList();
    unaryRules.add(CcgUnaryRule.parseFrom("DUMMY{0} DUMMY{0},(lambda x x)"));

    boolean allowComposition = true;
    boolean skipWords = true;
    boolean normalFormOnly = false;

    return ParametricCcgParser.parseFromLexicon(lexiconEntries, binaryRules, unaryRules,
        featureFactory, null, allowComposition, null, skipWords, normalFormOnly);
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
  public static CcgParser train(ParametricCcgParser parametricCcgParser,
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
    return parametricCcgParser.getModelFromParameters(parameters);
  }

  public static CcgExample createCcgExample(List<String> tokens, Expression2 expression) {
      List<String> pos = Collections.nCopies(tokens.size(), ParametricCcgParser.DEFAULT_POS_TAG);
      SupertaggedSentence sentence = ListSupertaggedSentence.createWithUnobservedSupertags(tokens, pos);

      return new CcgExample(sentence, null, null, expression, null);
  }

    public static List<String> tokenize(String sentence) {
        return Arrays.asList(sentence.split(" "));
    }

  /**
   * Parses a tokenized text using {@code parser} to produce a
   * logical form. The text is represented by a list of tokens
   * (e.g., obtained by splitting the input on spaces). The returned
   * logical form is given in a LISP S-Expression format.
   * 
   * @param parser
   * @param tokens
   * @return
   */
  public static Expression2 parse(CcgParser parser, List<String> tokens) {
    CcgInference inferenceAlg = new CcgExactInference(null, -1L, Integer.MAX_VALUE, 1);
    ExpressionSimplifier simplifier = getExpressionSimplifier();

    List<String> pos = Collections.nCopies(tokens.size(), ParametricCcgParser.DEFAULT_POS_TAG);
    SupertaggedSentence sentence = ListSupertaggedSentence.createWithUnobservedSupertags(tokens, pos);
    CcgParse parse = inferenceAlg.getBestParse(parser, sentence, null, new NullLogFunction());

    return simplifier.apply(parse.getLogicalForm());
  }

  public static ExpressionSimplifier getExpressionSimplifier() {
    return new ExpressionSimplifier(Arrays.
        <ExpressionReplacementRule>asList(new LambdaApplicationReplacementRule(),
            new VariableCanonicalizationReplacementRule()));
  }

    public static ActionResponse evaluate(IAllUserActions allUserActions, String userSays, Expression2 expression) {

        LispExecutor lispExecutor = new LispExecutor(allUserActions, new InfoForCommand(userSays, expression));

        Environment env = Environment.empty();
        IndexedList<String> symbolTable = IndexedList.create();
        env.bindName("sendEmail", lispExecutor.getSendEmailFunction(), symbolTable);
        env.bindName("set", lispExecutor.getSetFunction(), symbolTable);
        env.bindName("body", "body", symbolTable);

        LispEval eval = new LispEval(symbolTable);
        SExpression sExpression = ExpressionParser.sExpression(symbolTable).parseSingleExpression(expression.toString());
        LispEval.EvalResult result = eval.eval(sExpression, env);

        return (ActionResponse) result.getValue();
    }

  private CcgUtils() {
    // Prevent instantiation.
  }
}
