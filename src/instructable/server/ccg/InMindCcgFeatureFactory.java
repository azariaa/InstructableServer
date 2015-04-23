package instructable.server.ccg;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.CcgCategory;
import com.jayantkrish.jklol.ccg.CcgFeatureFactory;
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.ccg.lexicon.ParametricCcgLexicon;
import com.jayantkrish.jklol.ccg.lexicon.ParametricCombiningLexicon;
import com.jayantkrish.jklol.ccg.lexicon.ParametricStringLexicon;
import com.jayantkrish.jklol.ccg.lexicon.ParametricTableLexicon;
import com.jayantkrish.jklol.ccg.lexicon.StringLexicon;
import com.jayantkrish.jklol.models.DiscreteFactor;
import com.jayantkrish.jklol.models.DiscreteVariable;
import com.jayantkrish.jklol.models.TableFactor;
import com.jayantkrish.jklol.models.VariableNumMap;
import com.jayantkrish.jklol.models.loglinear.DenseIndicatorLogLinearFactor;
import com.jayantkrish.jklol.models.loglinear.IndicatorLogLinearFactor;
import com.jayantkrish.jklol.models.parametric.CombiningParametricFactor;
import com.jayantkrish.jklol.models.parametric.ConstantParametricFactor;
import com.jayantkrish.jklol.models.parametric.ParametricFactor;

/**
 * Creates the features for a CCG parser.
 * 
 * @author jayantk
 */
public class InMindCcgFeatureFactory implements CcgFeatureFactory {
  
  private final CcgCategory stringCategory;
  private final List<String> stringCategoryPredicates;
  
  public InMindCcgFeatureFactory(CcgCategory stringCategory) {
    this.stringCategory = Preconditions.checkNotNull(stringCategory);
    this.stringCategoryPredicates = Lists.newArrayList();
    this.stringCategoryPredicates.addAll(stringCategory.getSemanticHeads());
    for (Set<String> assignment : stringCategory.getAssignment()) {
      this.stringCategoryPredicates.addAll(assignment);
    }
  }

  @Override
  public DiscreteVariable getSemanticPredicateVar(List<String> semanticPredicates) {
    semanticPredicates.addAll(this.stringCategoryPredicates);
    return new DiscreteVariable("semanticPredicates", semanticPredicates);
  }

  @Override
  public ParametricFactor getDependencyFeatures(VariableNumMap dependencyHeadVar,
      VariableNumMap headSyntaxVar, VariableNumMap dependencyArgNumVar, VariableNumMap dependencyArgVar,
      VariableNumMap dependencyHeadPosVar, VariableNumMap dependencyArgPosVar) {

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
      VariableNumMap dependencyHeadPosVar, VariableNumMap distanceVar) {

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
      VariableNumMap dependencyHeadPosVar, VariableNumMap wordDistanceVar) {
    return getDistanceFeatures(dependencyHeadVar, headSyntaxVar, dependencyArgNumVar,
        dependencyHeadPosVar, wordDistanceVar);
  }

  @Override
  public ParametricFactor getDependencyPuncDistanceFeatures(VariableNumMap dependencyHeadVar,
      VariableNumMap headSyntaxVar, VariableNumMap dependencyArgNumVar,
      VariableNumMap dependencyHeadPosVar, VariableNumMap puncDistanceVar) {
    // Can't compute the distance in terms of punctuation symbols
    // without POS tags to identify punctuation.
    VariableNumMap allVars = VariableNumMap.unionAll(dependencyHeadVar, headSyntaxVar,
        dependencyArgNumVar, dependencyHeadPosVar, puncDistanceVar);
    return new ConstantParametricFactor(allVars, TableFactor.logUnity(allVars));
  }

  @Override
  public ParametricFactor getDependencyVerbDistanceFeatures(VariableNumMap dependencyHeadVar,
      VariableNumMap headSyntaxVar, VariableNumMap dependencyArgNumVar,
      VariableNumMap dependencyHeadPosVar, VariableNumMap verbDistanceVar) {
    // Can't compute the distance in terms of verbs without
    // POS tags to identify verbs.
    VariableNumMap allVars = VariableNumMap.unionAll(dependencyHeadVar, headSyntaxVar,
        dependencyArgNumVar, dependencyHeadPosVar, verbDistanceVar);
    return new ConstantParametricFactor(allVars, TableFactor.logUnity(allVars));
  }

  @Override
  public ParametricCcgLexicon getLexiconFeatures(VariableNumMap terminalWordVar,
      VariableNumMap ccgCategoryVar, VariableNumMap terminalPosVar, VariableNumMap terminalSyntaxVar,
      DiscreteFactor lexiconIndicatorFactor, Collection<LexiconEntry> lexiconEntries) {

      // Features for mapping words to ccg categories (which include both 
      // syntax and semantics). 
      ParametricFactor terminalParametricFactor = new IndicatorLogLinearFactor(
          terminalWordVar.union(ccgCategoryVar), lexiconIndicatorFactor);

      // Backoff features mapping words to syntactic categories (ignoring 
      // semantics). These features aren't very useful for semantic parsing. 
      VariableNumMap vars = terminalWordVar.union(terminalSyntaxVar); 
      ParametricFactor terminalSyntaxFactor = new ConstantParametricFactor(vars,
          TableFactor.logUnity(vars));
     
      // Backoff distribution over parts-of-speech and syntactic 
      // categories.
      VariableNumMap terminalPosVars = VariableNumMap.unionAll(terminalPosVar, terminalSyntaxVar);
      ParametricFactor terminalPosParametricFactor;
      terminalPosParametricFactor = new ConstantParametricFactor(terminalPosVars, TableFactor.logUnity(terminalPosVars));

      // This lexicon contains the lexicon entries given to the method.
      ParametricCcgLexicon tableLexicon = new ParametricTableLexicon(terminalWordVar,
          ccgCategoryVar, terminalParametricFactor, terminalPosVar, terminalSyntaxVar,
          terminalPosParametricFactor, terminalSyntaxFactor);

      // Add a lexicon that instantiates strings in the parse.
      StringLexicon stringLexicon = new StringLexicon(terminalWordVar, Arrays.asList(stringCategory));
      ParametricCcgLexicon parametricStringLexicon = new ParametricStringLexicon(stringLexicon);

      return new ParametricCombiningLexicon(terminalWordVar, Arrays.asList("lexicon", "stringLexicon"),
          Arrays.asList(tableLexicon, parametricStringLexicon));
  }

  @Override
  public ParametricFactor getBinaryRuleFeatures(VariableNumMap leftSyntaxVar,
      VariableNumMap rightSyntaxVar, VariableNumMap parentSyntaxVar, DiscreteFactor binaryRuleDistribution) {
    VariableNumMap allVars = VariableNumMap.unionAll(leftSyntaxVar, rightSyntaxVar, parentSyntaxVar);
    return new DenseIndicatorLogLinearFactor(allVars, true);
  }

  @Override
  public ParametricFactor getUnaryRuleFeatures(VariableNumMap unaryRuleSyntaxVar,
      VariableNumMap unaryRuleVar, DiscreteFactor unaryRuleDistribution) {
    VariableNumMap allVars = VariableNumMap.unionAll(unaryRuleSyntaxVar, unaryRuleVar);
    return new IndicatorLogLinearFactor(allVars, unaryRuleDistribution);
  }

  @Override
  public ParametricFactor getHeadedBinaryRuleFeatures(VariableNumMap leftSyntaxVar,
      VariableNumMap rightSyntaxVar, VariableNumMap parentSyntaxVar,
      VariableNumMap headedBinaryRulePredicateVar, VariableNumMap headedBinaryRulePosVar) {
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
      VariableNumMap rootPosVar) {
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
