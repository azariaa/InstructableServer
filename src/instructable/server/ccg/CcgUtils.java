package instructable.server.ccg;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jayantkrish.jklol.ccg.*;
import com.jayantkrish.jklol.ccg.SyntacticCategory.Direction;
import com.jayantkrish.jklol.ccg.chart.CcgBeamSearchChart;
import com.jayantkrish.jklol.ccg.cli.AlignmentLexiconInduction;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.*;
import com.jayantkrish.jklol.ccg.lexicon.SpanFeatureAnnotation;
import com.jayantkrish.jklol.ccg.lexicon.StringContext;
import com.jayantkrish.jklol.ccg.lexinduct.*;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.nlpannotation.AnnotatedSentence;
import com.jayantkrish.jklol.preprocessing.DictionaryFeatureVectorGenerator;
import com.jayantkrish.jklol.preprocessing.FeatureVectorGenerator;
import com.jayantkrish.jklol.training.*;
import com.jayantkrish.jklol.util.CsvParser;
import com.jayantkrish.jklol.util.IntegerArrayIterator;
import com.jayantkrish.jklol.util.PairCountAccumulator;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import instructable.server.parser.LispExecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Pattern;


public class CcgUtils
{

    private static Set<String> POS_TAGS_TO_SKIP_IN_LEXICON_INDUCTION = Sets.newHashSet("DT", "IN", "TO", "CC");
    public static final String START_POS_TAG = "START";
    public static final String startSymbol = "start_symbol";
    public static final String STRING_FEATURE_ANNOTATION_NAME = "stringFeatures";

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


    public static class LexiconInductionRet
    {
        LexiconInductionRet(List<LexiconEntry> lexiconEntries, List<WeightedCcgExample> newExamplesAdded, boolean foundAnyGeneralization)
        {
            this.lexiconEntries = lexiconEntries;
            this.newExamplesAdded = newExamplesAdded;
            this.foundAnyGeneralization = foundAnyGeneralization;
        }
        public List<LexiconEntry> lexiconEntries;
        public List<WeightedCcgExample> newExamplesAdded; //when using "concat" values in this list should replace the original example
        public boolean foundAnyGeneralization;
    }
    /**
     *
     * @param example
     * @param parser
     * @return
     */
    public static LexiconInductionRet induceLexiconEntriesHeuristic(WeightedCcgExample example, CcgParser parser)
    {
        boolean foundAnyGeneralization = false;
        List<WeightedCcgExample> newExamplesAdded = new LinkedList<>();

        ExpressionSimplifier simplifier = getExpressionSimplifier();

        List<String> words = example.getSentence().getWords();
        List<String> pos = example.getSentence().getPosTags();
        Expression2 lf = simplifier.apply(example.getLogicalForm());

        List<Integer> spanStarts = Lists.newArrayList();
        List<Integer> spanEnds = Lists.newArrayList();
        List<List<String>> spanStrings = Lists.newArrayList();
        List<List<Expression2>> spanExpressions = Lists.newArrayList();
        List<List<HeadedSyntacticCategory>> spanSyntacticCategories = Lists.newArrayList();

        CcgBeamSearchChart chart = new CcgBeamSearchChart(example.getSentence(), Integer.MAX_VALUE, 100);
        parser.parseCommon(chart, example.getSentence(), null, null, -1, 2);
        System.out.println(words);
        for (int i = 1; i < words.size(); i++)
        {
            for (int j = words.size() - 1; j >= i; j--)
            {
                List<String> subwords = words.subList(i, j + 1);


                List<Expression2> matchedExpressions = Lists.newArrayList();
                List<HeadedSyntacticCategory> matchedSyntacticCategories = Lists.newArrayList();
                List<CcgParse> parses = chart.decodeBestParsesForSpan(i, j, 100, parser);
                for (CcgParse parse : parses)
                {
                    Expression2 lexLf = simplifier.apply(parse.getLogicalForm());

                    // System.out.println(i + " " + j + " " + subwords + " " + parse.getHeadedSyntacticCategory() + " " + lexLf);

                    if (!parseSkipsWordsOnEnd(parse) && (lf.hasSubexpression(lexLf) || (getExpWithStringConcat(lf, lexLf)).isPresent()))
                    {
                        boolean isDuplicate = false;
                        for (int k = 0; k < matchedExpressions.size(); k++)
                        {
                            if (!isDuplicate)
                            {
                                isDuplicate = (matchedExpressions.get(k).equals(lexLf)
                                        && parse.getHeadedSyntacticCategory().equals(matchedSyntacticCategories.get(k)));
                            }
                        }

                        if (!isDuplicate &&
                                !parse.getHeadedSyntacticCategory().getSyntax().toString().equals("S")) //Amos: not reasonable that someone will want a full sentence as an argument. //Amos: without this there was a problem when a new command (e.g. forward to charlie) extended an older command (e.g. forward).
                        {
                            matchedExpressions.add(lexLf);
                            matchedSyntacticCategories.add(parse.getHeadedSyntacticCategory());
                        }
                    }
                }

                if (matchedExpressions.size() > 0)
                {
                    spanStarts.add(i);
                    spanEnds.add(j);
                    spanStrings.add(subwords);

                    spanExpressions.add(matchedExpressions);
                    spanSyntacticCategories.add(matchedSyntacticCategories);

				  /*
                  boolean containsStringCategory = false;
				  for (HeadedSyntacticCategory cat : matchedSyntacticCategories) {
				    // This check may be brittle.
				    if (cat.getFinalReturnType().toString().startsWith("String")) {
				      containsStringCategory = true;
				    }
				  }
				  
				  // Adding null to the list enables the matched expression
				  // to be skipped in later processing. 
				  if (!containsStringCategory) {
				    matchedExpressions.add(null);
				    matchedSyntacticCategories.add(null);
				  }
				  */

                    i = j;
                    break;
                }
            }
        }

	  /*
	  System.out.println(spanStarts);
	  System.out.println(spanEnds);
	  System.out.println(spanStrings);
	  System.out.println(spanExpressions);
	  System.out.println(spanSyntacticCategories);
	  */

        int[] numEntriesPerSpan = new int[spanStarts.size()];
        for (int i = 0; i < spanStarts.size(); i++)
        {
            numEntriesPerSpan[i] = spanExpressions.get(i).size();
        }

        List<LexiconEntry> newEntries = Lists.newArrayList();
        Iterator<int[]> subsetIter = new IntegerArrayIterator(numEntriesPerSpan, new int[0]);
        while (subsetIter.hasNext())
        {
            boolean[] entryUsed = new boolean[spanStarts.size()];
            Arrays.fill(entryUsed, false);
            boolean atLeastOneUsed = false;

            int[] indexes = subsetIter.next();
            Expression2 substituted = lf;
            Optional<Expression2> noSub = Optional.empty();
            for (int i = 0; i < spanStarts.size(); i++)
            {
                Expression2 var = Expression2.constant("$" + i);
                Expression2 toSubstitute = spanExpressions.get(i).get(indexes[i]);

                if (toSubstitute != null && (substituted.hasSubexpression(toSubstitute) || getExpWithStringConcat(substituted, toSubstitute).isPresent()))
                {
                    if (substituted.hasSubexpression(toSubstitute))
                        substituted = substituted.substitute(toSubstitute, var);
                    else
                    {
                        if (!noSub.isPresent())
                            noSub = Optional.of(lf);
                        noSub = Optional.of(getExpWithStringConcat(noSub.get(), toSubstitute).get());
                        substituted = getExpWithStringConcat(substituted, toSubstitute).get().substitute(toSubstitute, var);
                    }
                    entryUsed[i] = true;
                    atLeastOneUsed = true;
                }
            }
            if (noSub.isPresent())
                newExamplesAdded.add(example.newExampleWithDifferentLf(noSub.get()));

            if (!atLeastOneUsed && spanStarts.size() > 0)
            {
                continue;
            }


            List<Integer> idxOfCandidates = new LinkedList<>();
            //find command candidates
            for (int i = 1; i < words.size(); i++)
            {
                boolean containedInSpan = false;
                for (int j = 0; j < spanStarts.size(); j++)
                {
                    if (i >= spanStarts.get(j) && i <= spanEnds.get(j) && entryUsed[j])
                    {
                        containedInSpan = true;
                        break;
                    }
                }

                if (containedInSpan)
                {
                    continue;
                }

                if (POS_TAGS_TO_SKIP_IN_LEXICON_INDUCTION.contains(pos.get(i)))
                {
                    continue;
                }
                idxOfCandidates.add(i);
            }

            for (int i : idxOfCandidates)
            {
                // System.out.println(i + " " + words.get(i) + "/" + pos.get(i));

                // Figure out the argument order for the syntactic category being created.
                // This finds the right and left hand side arguments by word index in
                // the sentence.
                List<Integer> leftArgEnds = Lists.newArrayList();
                List<Integer> rightArgStarts = Lists.newArrayList();
                for (int j = 0; j < spanStarts.size(); j++)
                {
                    if (entryUsed[j])
                    {
                        if (spanStarts.get(j) > i)
                        {
                            rightArgStarts.add(spanStarts.get(j));
                        }
                        else
                        {
                            leftArgEnds.add(spanEnds.get(j));
                        }
                    }
                }

                // Order the arguments so that application on them works properly.
                Collections.sort(leftArgEnds);
                Collections.sort(rightArgStarts);
                Collections.reverse(rightArgStarts);
                List<Expression2> args = Lists.newArrayList();

                HeadedSyntacticCategory cat = HeadedSyntacticCategory.parseFrom("S{0}");
                List<String> subjects = Lists.newArrayList();
                List<Integer> argumentNumbers = Lists.newArrayList();
                List<Integer> objects = Lists.newArrayList();
                String head = Joiner.on("_").join(words.subList(i, i + 1));
                int numVars = 1;
                int numArgs = 0;
                // Add left arguments.
                for (int j = 0; j < leftArgEnds.size(); j++)
                {
                    HeadedSyntacticCategory argCat = null;
                    int argCatIndex = -1;
                    for (int k = 0; k < spanStarts.size(); k++)
                    {
                        if (spanEnds.get(k) == leftArgEnds.get(j))
                        {
                            argCat = spanSyntacticCategories.get(k).get(indexes[k]);
                            int[] argCatVarNums = argCat.getUniqueVariables();
                            int[] newVarNums = new int[argCatVarNums.length];
                            for (int l = 0; l < argCatVarNums.length; l++)
                            {
                                newVarNums[l] = l + numVars;
                            }
                            numVars += argCatVarNums.length;
                            argCat = argCat.relabelVariables(argCatVarNums, newVarNums);
                            argCatIndex = k;

                            subjects.add(head);
                            argumentNumbers.add(numArgs + 1);
                            objects.add(argCat.getHeadVariable());
                            numArgs++;
                        }
                    }

                    cat = cat.addArgument(argCat, Direction.LEFT, 0);
                    args.add(Expression2.constant("$" + argCatIndex));
                }

                // Add right arguments.
                for (int j = 0; j < rightArgStarts.size(); j++)
                {
                    HeadedSyntacticCategory argCat = null;
                    int argCatIndex = -1;
                    for (int k = 0; k < spanStarts.size(); k++)
                    {
                        if (spanStarts.get(k) == rightArgStarts.get(j))
                        {
                            argCat = spanSyntacticCategories.get(k).get(indexes[k]);
                            int[] argCatVarNums = argCat.getUniqueVariables();
                            int[] newVarNums = new int[argCatVarNums.length];
                            for (int l = 0; l < argCatVarNums.length; l++)
                            {
                                newVarNums[l] = l + numVars;
                            }
                            numVars += argCatVarNums.length;
                            argCat = argCat.relabelVariables(argCatVarNums, newVarNums);
                            argCatIndex = k;

                            subjects.add(head);
                            argumentNumbers.add(numArgs + 1);
                            objects.add(argCat.getHeadVariable());
                            numArgs++;
                        }
                    }

                    cat = cat.addArgument(argCat, Direction.RIGHT, 0);
                    args.add(Expression2.constant("$" + argCatIndex));
                }

                cat = cat.getCanonicalForm();
                Expression2 lambdaExpression = null;
                if (args.size() > 0)
                {
                    args.add(Expression2.constant("lambda"));
                    Collections.reverse(args);
                    args.add(substituted);
                    lambdaExpression = Expression2.nested(args);
                }
                else
                {
                    lambdaExpression = substituted;
                }
                // System.out.println(cat);
                // System.out.println(lambdaExpression);

                List<Set<String>> assignments = Lists.newArrayList();
                //Set<String> freeVars = StaticAnalysis.getFreeVariables(lf); //taken from below
                //String head2 = Iterables.getFirst(freeVars, null); //also this was taken from below
                assignments.add(Sets.newHashSet(head)); //Amos changed, used to be "head"
                for (int j = 1; j < numVars; j++)
                {
                    assignments.add(Collections.<String>emptySet());
                }
                CcgCategory ccgCategory = new CcgCategory(cat, lambdaExpression, subjects, argumentNumbers, objects, assignments);
                newEntries.add(new LexiconEntry(words.subList(i, i + 1), ccgCategory));
                if (numArgs > 0) //if any of the new lexicons receive at least 1 argument (either left or right), we consider that we have found some generalization
                    foundAnyGeneralization = true;
            }
        }

//	  //adding just the whole sentence as a lexicon entry, in case any other parse fails
        //this part was removed, instead we now check before giving a sentence to the parser whether it is identical to a learnt command.
//	  CsvParser csv = LexiconEntry.getCsvParser();
//	  List<String> sentenceWords = example.getSentence().getWords().subList(1, example.getSentence().getWords().size());
//	  List<String> parts = Lists.newArrayList(String.join(" ", sentenceWords), "S{0}", example.getLogicalForm().toString());
//	  newEntries.add(LexiconEntry.parseLexiconEntry(csv.toCsv(parts)));

        return new LexiconInductionRet(newEntries, newExamplesAdded, foundAnyGeneralization);
    }

    /**
     * If lexLf presents a substring of any subexpression of lf, this function will return lf with the relevant string
     * substituted with a concatenation of its beginning and lexLf.
     * E.g. if lf is (doSeq (say "hi") (say (StringValue "good morning"))) and lexLf is (StringValue "morning"), this function will return
     * (doSeq (say "hi" (say (concat (StringValue "good") (StringValue "morning"))
     *
     * Note: LIA can't generalize: good morning -> say good morning, because good morning appears twice and there is no word to capture it. LIA can however generalize: good morning -> have a wonderful morning
     */
    private static Optional<Expression2> getExpWithStringConcat(Expression2 lf, Expression2 lexLf)
    {
        return getExpWithStringConcat(lf, lexLf, lf);
    }

    /**
     *
     * @param originalLf the original logical form. Once the function returns a logical form that is a concatenation of
     *                   lexLf in lf, it will substitute lf in the original logical form with the result.
     */
    private static Optional<Expression2> getExpWithStringConcat(Expression2 lf, Expression2 lexLf, Expression2 originalLf)
    {
        if (lf.getSubexpressions() == null || !lexLf.toString().startsWith("(string"))
            return Optional.empty();
        String lexLfAsStr = lexLf.toString();
        int spaceIdx = lexLfAsStr.indexOf(" \"");
        if (spaceIdx <= 0)
            return Optional.empty();
        // strVal is the actual string of lexLF, e.g. in (stringValue "hello world"), strVal will be hello world (no quotes).
        String strVal = lexLfAsStr.substring(spaceIdx + 2, lexLfAsStr.length() - 2); //removing quotes and ")"
        if (strVal.length() == 0)
            return Optional.empty();
        for (Expression2 expr : lf.getSubexpressions())
        {
            //Currently concatenating two strings reserves the type.
            //While it may make sense that after concatenating a stringValue with a stringNoun we could get a stringValue,
            //I'm trying to avoid this. It might be better if we could just reduce the weights for that to happen and not
            //totally disallow this.
            Pattern p = Pattern.compile("\\b"+strVal+"\\b");
            if ((expr.toString().startsWith("(stringNoun") && lexLf.toString().startsWith("(stringNoun") ||
                    expr.toString().startsWith("(stringValue") && lexLf.toString().startsWith("(stringValue")) &&
                    p.matcher(expr.toString()).find())
            {
                //insert string from lexLF into expr (lf's subexpression) using "concat"
                int strValLocInLf = expr.toString().indexOf(strVal);
                int strValLocEnd = strValLocInLf + strVal.length();
                boolean atStart = expr.toString().indexOf(" \"") + 2 == strValLocInLf;
                boolean atEnd = strValLocEnd + 2 == expr.toString().length();
                if (atStart && atEnd)
                {
                    //no need to concatenate! (regular procedure should find this)
                    return Optional.empty();//bug, used to be: Optional.of(expr);
                }
                String withLexLfStr;
                if (atEnd)
                    withLexLfStr = "(concat " + expr.toString().substring(0, strValLocInLf - 1) + "\") " + lexLfAsStr + ")";
                else if (atStart)
                    withLexLfStr = "(concat " + lexLfAsStr + " " + expr.toString().substring(0, expr.toString().indexOf(" \"") + 2) + expr.toString().substring(strValLocEnd + 1) + ")";
                else
                    withLexLfStr = "(concat (concat " + expr.toString().substring(0, strValLocInLf - 1) + "\") " + lexLfAsStr + ") " + expr.toString().substring(0, expr.toString().indexOf(" \"") + 2) + expr.toString().substring(strValLocEnd + 1) + " )";

                return Optional.of(originalLf.substitute(expr, ExpressionParser.expression2().parseSingleExpression(withLexLfStr)));
            }
            else
            {
                Optional<Expression2> recCall = getExpWithStringConcat(expr, lexLf, originalLf);
                if (recCall.isPresent())
                    return recCall;
            }
        }
        return Optional.empty();
    }

    private static boolean parseSkipsWordsOnEnd(CcgParse parse)
    {
        if (parse.isTerminal())
        {
            return false;
        }
        else
        {
            CcgParse left = parse.getLeft();
            CcgParse right = parse.getRight();

            return left.getHeadedSyntacticCategory().equals(ParametricCcgParser.SKIP_CAT) ||
                    right.getHeadedSyntacticCategory().equals(ParametricCcgParser.SKIP_CAT) ||
                    parseSkipsWordsOnEnd(right) || parseSkipsWordsOnEnd(left);
        }
    }

    /**
     * @param expression
     * @return
     */
    private static ExpressionTree expressionToExpressionTree(Expression2 expression)
    {
        ExpressionSimplifier simplifier = getExpressionSimplifier();
        Set<String> constantsToIgnore = Sets.newHashSet();
        Map<String, String> typeReplacements = Maps.newHashMap();

        return ExpressionTree.fromExpression(expression, simplifier, typeReplacements,
                constantsToIgnore, 0, 2, 2);
    }

    public static WeightedCcgExample createCcgExample(List<String> tokens, Expression2 expression)
    {
        List<String> pos = Collections.nCopies(tokens.size(), ParametricCcgParser.DEFAULT_POS_TAG);
        AnnotatedSentence sentence = new AnnotatedSentence(tokens, pos);

        return new WeightedCcgExample(sentence, expression, 1.0);
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
        //CcgCategory stringNCategory = CcgCategory.parseFrom("StringN{0},(lambda $1 (stringNoun $1)),0 stringNoun,stringNoun 1 1");
        CcgCategory stringNCategory = CcgCategory.parseFrom("StringN{0},(lambda $1 (stringNoun $1)),0 stringNoun");
        //CcgCategory stringVCategory = CcgCategory.parseFrom("StringV{0},(lambda $1 (stringValue $1)),0 stringValue,stringValue 1 1");
        CcgCategory stringVCategory = CcgCategory.parseFrom("StringV{0},(lambda $1 (stringValue $1)),0 stringValue");
        CcgCategory unknownCommandCategory = CcgCategory.parseFrom("UnknownCommand{0},(lambda $0 (unknownCommand)),0 unknownCommand");
        List<LexiconEntry> unknownWordLexiconEntries = Lists.newArrayList();

        CcgFeatureFactory featureFactory = new InstCcgFeatureFactory(Arrays.asList(stringNCategory, stringVCategory),
                Arrays.asList(unknownCommandCategory), STRING_FEATURE_ANNOTATION_NAME, featureVectorGenerator.getFeatureDictionary());
        // Read in the lexicon to instantiate the model.

        List<CcgBinaryRule> binaryRules = Lists.newArrayList();
        List<CcgUnaryRule> unaryRules = Lists.newArrayList(inputUnaryRules);
        unaryRules.add(CcgUnaryRule.parseFrom("DUMMY{0} DUMMY{0},(lambda x x)"));

        boolean allowComposition = true;
        boolean skipWords = true;
        boolean normalFormOnly = false;

        return ParametricCcgParser.parseFromLexicon(Lists.newArrayList(lexiconEntries),
                unknownWordLexiconEntries, binaryRules, unaryRules, featureFactory, posSet,
                allowComposition, null, skipWords, normalFormOnly);
    }

    /**
     * Trains a CCG parser given a collection of training examples. This
     * method selects a particular CCG parser from the given parametric family
     * (i.e., by selecting a particular parameter vector using the training
     * data).
     *
     * @param parametricCcgParser
     * @param trainingExamples
     * @param numPasses
     * @return
     */
    public static SufficientStatistics train(ParametricCcgParser parametricCcgParser,
                                             List<WeightedCcgExample> trainingExamples,
                                             int numPasses, SufficientStatistics initialParameters)
    {
        ExpressionSimplifier simplifier = getExpressionSimplifier();
        ExpressionComparator comparator = new SimplificationComparator(simplifier);

        int beamSize = 300;
        CcgBeamSearchInference inferenceAlgorithm = new CcgBeamSearchInference(null, comparator, beamSize,
                -1, Integer.MAX_VALUE, Runtime.getRuntime().availableProcessors(), false);
        GradientOracle<CcgParser, WeightedCcgExample> oracle = new WeightedCcgPerceptronOracle(
                parametricCcgParser, inferenceAlgorithm, comparator);
//        GradientOracle<CcgParser, CcgExample> oracle = new CcgLoglikelihoodOracle(parametricCcgParser, new ExpressionComparator()
//        {
//            @Override
//            public boolean equals(Expression2 a, Expression2 b)
//            {
//                return false;
//            }
//        }, 10);

        boolean averagedParameters = true;
        if (initialParameters == null)
        {
            initialParameters = oracle.initializeGradient();
            averagedParameters = false;
        }

        int numIterations = numPasses * trainingExamples.size();
        double l2Regularization = 0.01;
        GradientOptimizer trainer = StochasticGradientTrainer.createWithL2Regularization(numIterations,
                1, 1.0, true, averagedParameters, l2Regularization, new NullLogFunction());
        SufficientStatistics parameters = trainer.train(oracle, initialParameters,
                trainingExamples);
        //Print out the parameters that were learned:
        // System.out.println(parametricCcgParser.getParameterDescription(parameters));
        return parameters;
    }

    public static WeightedCcgExample createCcgExample(String sentence, Expression2 expression,
                                                      Set<String> usedPOS, boolean addNewPOS, FeatureVectorGenerator<StringContext> stringFeatureGenerator)
    {
        //List<String> pos = Collections.nCopies(tokens.size(), ParametricCcgParser.DEFAULT_POS_TAG);
        List<String> tokens = new LinkedList<>();
        List<String> poss = new LinkedList<>();
        tokens.add(startSymbol);
        poss.add(START_POS_TAG);
        tokenizeAndPOS(sentence, tokens, poss, addNewPOS, usedPOS);
        AnnotatedSentence supertaggedSentence = new AnnotatedSentence(tokens, poss);

        if (stringFeatureGenerator != null)
        {
            SpanFeatureAnnotation annotation = SpanFeatureAnnotation.annotate(supertaggedSentence, stringFeatureGenerator);
            supertaggedSentence = supertaggedSentence.addAnnotation(STRING_FEATURE_ANNOTATION_NAME, annotation);
        }

        return new WeightedCcgExample(supertaggedSentence, expression, 1.0);
    }

    public static List<WeightedCcgExample> featurizeExamples(List<WeightedCcgExample> examples,
                                                             FeatureVectorGenerator<StringContext> stringFeatureGenerator)
    {
        List<WeightedCcgExample> newExamples = Lists.newArrayList();
        for (WeightedCcgExample example : examples)
        {
            AnnotatedSentence sent = example.getSentence();
            SpanFeatureAnnotation annotation = SpanFeatureAnnotation.annotate(sent, stringFeatureGenerator);
            AnnotatedSentence newSent = sent.addAnnotation(STRING_FEATURE_ANNOTATION_NAME, annotation);

            newExamples.add(new WeightedCcgExample(newSent, example.getLogicalForm(), example.getWeight()));
        }
        return newExamples;
    }

    public static List<LexiconEntry> induceLexiconHeadsAndDependencies(List<LexiconEntry> lexicon)
    {
        // Try and fill in the dependencies and semantic heads of these entries
        List<LexiconEntry> newLexicon = Lists.newArrayList();
        for (LexiconEntry entry : lexicon)
        {
            CcgCategory ccgCategory = entry.getCategory();
            Expression2 lf = ccgCategory.getLogicalForm();

            // TODO: the head of each category right now is the word itself,
            // not its semantics. This parameterization does not share dependency
            // parameters among different lexical ways to refer to the same function.
            Set<String> freeVars = StaticAnalysis.getFreeVariables(lf);
            String head = Iterables.getFirst(freeVars, null);
            // String head = Joiner.on("_").join(entry.getWords());
            // System.out.println(entry);
            if (head != null)
            {
                HeadedSyntacticCategory syntax = ccgCategory.getSyntax();
                List<String> subjects = Lists.newArrayList();
                List<Integer> argumentNumbers = Lists.newArrayList();
                List<Integer> objects = Lists.newArrayList();

                for (int varNum : syntax.getUniqueVariables())
                {
                    if (varNum == 0)
                    {
                        continue;
                    }

                    subjects.add(head);
                    argumentNumbers.add(varNum);
                    objects.add(varNum);
                }

                List<Set<String>> assignments = Lists.newArrayList();
                for (int i = 0; i < syntax.getUniqueVariables().length; i++)
                {
                    assignments.add(Sets.newHashSet());
                }
                assignments.get(0).add(head);

                CcgCategory newCategory = new CcgCategory(ccgCategory.getSyntax(), lf,
                        subjects, argumentNumbers, objects, assignments);
                // System.out.println("   " + newCategory);

                newLexicon.add(new LexiconEntry(entry.getWords(), newCategory));
            }
            else
            {
                // Can't induce the heads or dependencies, so use the
                // original entry.
                newLexicon.add(entry);
            }
        }
        return newLexicon;
    }

    static MaxentTagger maxentTagger;

    static
    {
        try
        {
            maxentTagger = new MaxentTagger("resources/english-left3words-distsim.tagger");
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    /**
     * @param sentence         sentence to tokenize and POS
     * @param outTokens        in lowercase.
     * @param outPOSs          POS
     * @param addNewPOS        if addNewPOS, any POS used are added to allowedOrUsedPOS.
     *                         if addNewPOS == false, uses only POS from allowedOrUsedPOS
     * @param allowedOrUsedPOS either an object which all POS will be added to (if addNewPOS) or a set of allowed POS (if !addNewPOS)
     */
    public static void tokenizeAndPOS(String sentence, List<String> outTokens, List<String> outPOSs, boolean addNewPOS, Set<String> allowedOrUsedPOS)
    {
        final String slash = "/"; //is replaced with the first word.
        final List<String> excludeTokens = Arrays.asList("(", ")", "\"", ";", slash, "\\", "``", "''"); //"!", "?", //user can also use text, in future may want to support. old: need to exclude this since they won't be available when using speech.
        //do not exclude "." and ",", used as a conditional "and" to concatenate sentences
        //do not exclude ":", may be used as "is"

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
            if (token.equals("e-mail")) //TODO: create a file with synonyms
                token = "email";
            String POS = tokenAndPOS.substring(idx + 1);
            if (!excludeTokens.contains(token))
            {
                if (token.contains(slash))
                    token = token.substring(0, token.indexOf(slash));
                outTokens.add(token);
                //POSs.add(POS);
                if (addNewPOS)
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
        public SayAndExpression(String sayToUser, String lExpression, boolean success)
        {
            this.sayToUser = sayToUser;
            this.lExpression = lExpression;
            this.success = success;
        }

        public String sayToUser;
        public String lExpression;
        public boolean success;
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
            retExpression = ExpressionParser.expression2().parseSingleExpression("(" + LispExecutor.doSeq + " " + retExpression.toString() + " " + toCombine.get(i).toString() + ")");
        }

        return retExpression;
    }
}
