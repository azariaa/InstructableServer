package instructable.server.ccg;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.CcgCategory;
import com.jayantkrish.jklol.ccg.CcgFeatureFactory;
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.ccg.lexicon.ParametricCcgLexicon;
import com.jayantkrish.jklol.ccg.lexicon.ParametricFeaturizedLexiconScorer;
import com.jayantkrish.jklol.ccg.lexicon.ParametricLexiconScorer;
import com.jayantkrish.jklol.ccg.lexicon.ParametricStringLexicon;
import com.jayantkrish.jklol.ccg.lexicon.ParametricSyntaxLexiconScorer;
import com.jayantkrish.jklol.ccg.lexicon.ParametricTableLexicon;
import com.jayantkrish.jklol.ccg.lexicon.ParametricUnknownWordLexicon;
import com.jayantkrish.jklol.ccg.lexicon.StringLexicon;
import com.jayantkrish.jklol.ccg.lexicon.StringLexicon.CategorySpanConfig;
import com.jayantkrish.jklol.models.DiscreteFactor;
import com.jayantkrish.jklol.models.DiscreteVariable;
import com.jayantkrish.jklol.models.TableFactor;
import com.jayantkrish.jklol.models.VariableNumMap;
import com.jayantkrish.jklol.models.loglinear.DenseIndicatorLogLinearFactor;
import com.jayantkrish.jklol.models.loglinear.IndicatorLogLinearFactor;
import com.jayantkrish.jklol.models.loglinear.ParametricLinearClassifierFactor;
import com.jayantkrish.jklol.models.parametric.CombiningParametricFactor;
import com.jayantkrish.jklol.models.parametric.ConstantParametricFactor;
import com.jayantkrish.jklol.models.parametric.ParametricFactor;

/**
 * Creates the features for a CCG parser.
 *
 * @author jayantk
 */
public class InstCcgFeatureFactory implements CcgFeatureFactory
{

  private final List<CcgCategory> stringCategories;
  private final List<CcgCategory> unknownCommandCategories;
  private final List<String> stringCategoryPredicates;
  
  private final String stringFeatureAnnotationName;
  private final DiscreteVariable stringFeatureDictionary;

    public InstCcgFeatureFactory(List<CcgCategory> stringCategories,
        List<CcgCategory> unknownCommandCategories, String stringFeatureAnnotationName,
        DiscreteVariable stringFeatureDictionary)
    {
        this.stringCategories = Preconditions.checkNotNull(stringCategories);
        this.unknownCommandCategories = Preconditions.checkNotNull(unknownCommandCategories);
        this.stringCategoryPredicates = Lists.newArrayList();
        List<CcgCategory> allStringCategories = Lists.newArrayList();
        allStringCategories.addAll(stringCategories);
        allStringCategories.addAll(unknownCommandCategories);
        for (CcgCategory stringCategory : allStringCategories)
        {
            this.stringCategoryPredicates.addAll(stringCategory.getSemanticHeads());
            for (Set<String> assignment : stringCategory.getAssignment())
            {
                this.stringCategoryPredicates.addAll(assignment);
            }
        }

        this.stringFeatureAnnotationName = Preconditions.checkNotNull(stringFeatureAnnotationName);
        this.stringFeatureDictionary = Preconditions.checkNotNull(stringFeatureDictionary);
    }

    @Override
    public DiscreteVariable getSemanticPredicateVar(List<String> semanticPredicates)
    {
        List<String> predicates = new LinkedList<>(semanticPredicates);
        predicates.addAll(this.stringCategoryPredicates);
        return new DiscreteVariable("semanticPredicates", predicates);
    }

    @Override
    public ParametricFactor getDependencyFeatures(VariableNumMap dependencyHeadVar,
                                                  VariableNumMap headSyntaxVar, VariableNumMap dependencyArgNumVar, VariableNumMap dependencyArgVar,
                                                  VariableNumMap dependencyHeadPosVar, VariableNumMap dependencyArgPosVar)
    {

        ParametricFactor wordWordFactor = new DenseIndicatorLogLinearFactor(
                VariableNumMap.unionAll(dependencyHeadVar, headSyntaxVar, dependencyArgNumVar, dependencyArgVar), true);
        VariableNumMap wordPosVars = VariableNumMap.unionAll(dependencyHeadVar, headSyntaxVar, dependencyArgNumVar, dependencyArgPosVar);
        VariableNumMap posWordVars = VariableNumMap.unionAll(headSyntaxVar, dependencyArgNumVar, dependencyArgVar, dependencyHeadPosVar);
        VariableNumMap posPosVars = VariableNumMap.unionAll(headSyntaxVar, dependencyArgNumVar, dependencyHeadPosVar, dependencyArgPosVar);

        ParametricFactor wordPosFactor, posWordFactor, posPosFactor;
        wordPosFactor = new ConstantParametricFactor(wordPosVars, TableFactor.logUnity(wordPosVars));
        posWordFactor = new ConstantParametricFactor(posWordVars, TableFactor.logUnity(posWordVars));
        posPosFactor = new ConstantParametricFactor(posPosVars, TableFactor.logUnity(posPosVars));

        VariableNumMap allVars = VariableNumMap.unionAll(dependencyHeadVar, headSyntaxVar,
                dependencyArgNumVar, dependencyArgVar, dependencyHeadPosVar, dependencyArgPosVar);
        return new CombiningParametricFactor(allVars, Arrays.asList("word-word", "word-pos", "pos-word", "pos-pos"),
                Arrays.asList(wordWordFactor, wordPosFactor, posWordFactor, posPosFactor), true);
    }

    private ParametricFactor getDistanceFeatures(VariableNumMap dependencyHeadVar,
                                                 VariableNumMap headSyntaxVar, VariableNumMap dependencyArgNumVar,
                                                 VariableNumMap dependencyHeadPosVar, VariableNumMap distanceVar)
    {

        ParametricFactor wordDistanceFactor = new DenseIndicatorLogLinearFactor(VariableNumMap.unionAll(
                dependencyHeadVar, headSyntaxVar, dependencyArgNumVar, distanceVar), true);
        VariableNumMap posDistanceVars = VariableNumMap.unionAll(
                headSyntaxVar, dependencyArgNumVar, dependencyHeadPosVar, distanceVar);
        ParametricFactor posDistanceFactor;
        posDistanceFactor = new ConstantParametricFactor(posDistanceVars, TableFactor.logUnity(posDistanceVars));

        VariableNumMap allVars = VariableNumMap.unionAll(dependencyHeadVar, headSyntaxVar,
                dependencyArgNumVar, dependencyHeadPosVar, distanceVar);
        return new CombiningParametricFactor(allVars, Arrays.asList("distance", "pos-backoff-distance"),
                Arrays.asList(wordDistanceFactor, posDistanceFactor), true);
    }

    @Override
    public ParametricFactor getDependencyWordDistanceFeatures(VariableNumMap dependencyHeadVar,
                                                              VariableNumMap headSyntaxVar, VariableNumMap dependencyArgNumVar,
                                                              VariableNumMap dependencyHeadPosVar, VariableNumMap wordDistanceVar)
    {
        return getDistanceFeatures(dependencyHeadVar, headSyntaxVar, dependencyArgNumVar,
                dependencyHeadPosVar, wordDistanceVar);
    }

    @Override
    public ParametricFactor getDependencyPuncDistanceFeatures(VariableNumMap dependencyHeadVar,
                                                              VariableNumMap headSyntaxVar, VariableNumMap dependencyArgNumVar,
                                                              VariableNumMap dependencyHeadPosVar, VariableNumMap puncDistanceVar)
    {
        // Can't compute the distance in terms of punctuation symbols
        // without POS tags to identify punctuation.
        VariableNumMap allVars = VariableNumMap.unionAll(dependencyHeadVar, headSyntaxVar,
                dependencyArgNumVar, dependencyHeadPosVar, puncDistanceVar);
        return new ConstantParametricFactor(allVars, TableFactor.logUnity(allVars));
    }

    @Override
    public ParametricFactor getDependencyVerbDistanceFeatures(VariableNumMap dependencyHeadVar,
                                                              VariableNumMap headSyntaxVar, VariableNumMap dependencyArgNumVar,
                                                              VariableNumMap dependencyHeadPosVar, VariableNumMap verbDistanceVar)
    {
        // Can't compute the distance in terms of verbs without
        // POS tags to identify verbs.
        VariableNumMap allVars = VariableNumMap.unionAll(dependencyHeadVar, headSyntaxVar,
                dependencyArgNumVar, dependencyHeadPosVar, verbDistanceVar);
        return new ConstantParametricFactor(allVars, TableFactor.logUnity(allVars));
    }

    @Override
    public List<ParametricCcgLexicon> getLexiconFeatures(VariableNumMap terminalWordVar,
                                                   VariableNumMap ccgCategoryVar, VariableNumMap terminalPosVar, VariableNumMap terminalSyntaxVar,
                                                   DiscreteFactor lexiconIndicatorFactor, Collection<LexiconEntry> lexiconEntries,
                                                   DiscreteFactor unknownWordLexiconIndicatorFactor, Collection<LexiconEntry> unknownWordLexiconEntries)
    {

        // Features for mapping words to ccg categories (which include both
        // syntax and semantics).
        ParametricFactor terminalParametricFactor = new IndicatorLogLinearFactor(
                terminalWordVar.union(ccgCategoryVar), lexiconIndicatorFactor);

        // This lexicon contains the lexicon entries given to the method.
        List<ParametricCcgLexicon> lexicons = Lists.newArrayList();
        ParametricCcgLexicon tableLexicon = new ParametricTableLexicon(terminalWordVar,
                ccgCategoryVar, terminalParametricFactor);
        lexicons.add(tableLexicon);
        
        ParametricFactor unknownTerminalFamily = new IndicatorLogLinearFactor(
            terminalPosVar.union(ccgCategoryVar), unknownWordLexiconIndicatorFactor);
        ParametricCcgLexicon unknownLexicon = new ParametricUnknownWordLexicon(terminalWordVar,
            terminalPosVar, ccgCategoryVar, unknownTerminalFamily);
        lexicons.add(unknownLexicon);
        
        List<CcgCategory> allStringLexiconCategories = Lists.newArrayList();
        List<CategorySpanConfig> config = Lists.newArrayList();
        allStringLexiconCategories.addAll(stringCategories);
        config.addAll(Collections.nCopies(stringCategories.size(), CategorySpanConfig.ALL_SPANS));
        allStringLexiconCategories.addAll(unknownCommandCategories);
        config.addAll(Collections.nCopies(unknownCommandCategories.size(), CategorySpanConfig.WHOLE_SENTENCE));

        // Add a lexicon that instantiates strings in the parse.
        StringLexicon stringLexicon = new StringLexicon(terminalWordVar, allStringLexiconCategories, config, CcgDetokenizer.getDetokenizer());
        ParametricCcgLexicon parametricStringLexicon = new ParametricStringLexicon(stringLexicon);
        lexicons.add(parametricStringLexicon);

        return lexicons;
    }

    
    @Override
    public List<ParametricLexiconScorer> getLexiconScorers(VariableNumMap terminalWordVar,
      VariableNumMap ccgCategoryVar, VariableNumMap terminalPosVar,
      VariableNumMap terminalSyntaxVar) {
      List<ParametricLexiconScorer> scorers = Lists.newArrayList();
      // Backoff features mapping words to syntactic categories (ignoring 
      // semantics). These features aren't very useful for semantic parsing. 
      VariableNumMap vars = terminalWordVar.union(terminalSyntaxVar); 
      ParametricFactor terminalSyntaxFamily = new ConstantParametricFactor(vars,
          TableFactor.logUnity(vars));

      VariableNumMap terminalPosVars = VariableNumMap.unionAll(terminalPosVar, terminalSyntaxVar);
      ParametricFactor terminalPosFamily = new DenseIndicatorLogLinearFactor(terminalPosVars, true);

      scorers.add(new ParametricSyntaxLexiconScorer(terminalWordVar, terminalPosVar,
          terminalSyntaxVar, terminalPosFamily, terminalSyntaxFamily));

      VariableNumMap featureVar = VariableNumMap.singleton(terminalSyntaxVar.getOnlyVariableNum() - 1,
          "ccgLexiconFeatures", stringFeatureDictionary);
      ParametricLinearClassifierFactor featureFamily = new ParametricLinearClassifierFactor(
          featureVar, terminalSyntaxVar, VariableNumMap.EMPTY,
          stringFeatureDictionary, null, false);
      
      scorers.add(new ParametricFeaturizedLexiconScorer(stringFeatureAnnotationName,
          terminalSyntaxVar, featureVar, featureFamily));

      return scorers;
    }

    @Override
    public ParametricFactor getBinaryRuleFeatures(VariableNumMap leftSyntaxVar,
                                                  VariableNumMap rightSyntaxVar, VariableNumMap parentSyntaxVar, DiscreteFactor binaryRuleDistribution)
    {
        VariableNumMap allVars = VariableNumMap.unionAll(leftSyntaxVar, rightSyntaxVar, parentSyntaxVar);
        return new DenseIndicatorLogLinearFactor(allVars, true);
    }

    @Override
    public ParametricFactor getUnaryRuleFeatures(VariableNumMap unaryRuleSyntaxVar,
                                                 VariableNumMap unaryRuleVar, DiscreteFactor unaryRuleDistribution)
    {
        VariableNumMap allVars = VariableNumMap.unionAll(unaryRuleSyntaxVar, unaryRuleVar);
        return new IndicatorLogLinearFactor(allVars, unaryRuleDistribution);
    }

    @Override
    public ParametricFactor getHeadedBinaryRuleFeatures(VariableNumMap leftSyntaxVar,
                                                        VariableNumMap rightSyntaxVar, VariableNumMap parentSyntaxVar,
                                                        VariableNumMap headedBinaryRulePredicateVar, VariableNumMap headedBinaryRulePosVar)
    {
        ParametricFactor wordFactor = new DenseIndicatorLogLinearFactor(VariableNumMap.unionAll(
                leftSyntaxVar, rightSyntaxVar, parentSyntaxVar, headedBinaryRulePredicateVar), true);

        VariableNumMap posVars = VariableNumMap.unionAll(leftSyntaxVar, rightSyntaxVar,
                parentSyntaxVar, headedBinaryRulePosVar);
        ParametricFactor posFactor;
        posFactor = new ConstantParametricFactor(posVars, TableFactor.logUnity(posVars));

        VariableNumMap allVars = VariableNumMap.unionAll(leftSyntaxVar, rightSyntaxVar, parentSyntaxVar,
                headedBinaryRulePredicateVar, headedBinaryRulePosVar);

        return new CombiningParametricFactor(allVars, Arrays.asList("word-binary-rule",
                "pos-binary-rule"), Arrays.asList(wordFactor, posFactor), true);
    }

    @Override
    public ParametricFactor getHeadedRootFeatures(VariableNumMap rootSyntaxVar, VariableNumMap rootPredicateVar,
                                                  VariableNumMap rootPosVar)
    {
        ParametricFactor wordFactor = new DenseIndicatorLogLinearFactor(VariableNumMap.unionAll(
                rootSyntaxVar, rootPredicateVar), true);

        VariableNumMap posVars = VariableNumMap.unionAll(rootSyntaxVar, rootPosVar);
        ParametricFactor posFactor;
        posFactor = new ConstantParametricFactor(posVars, TableFactor.logUnity(posVars));

        VariableNumMap allVars = VariableNumMap.unionAll(rootSyntaxVar, rootPredicateVar, rootPosVar);
        return new CombiningParametricFactor(allVars, Arrays.asList("root-word",
                "root-pos"), Arrays.asList(wordFactor, posFactor), true);
    }
}
