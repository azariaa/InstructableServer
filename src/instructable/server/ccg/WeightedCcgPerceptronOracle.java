package instructable.server.ccg;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.CcgBeamSearchInference;
import com.jayantkrish.jklol.ccg.CcgParse;
import com.jayantkrish.jklol.ccg.CcgParser;
import com.jayantkrish.jklol.ccg.ParametricCcgParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionComparator;
import com.jayantkrish.jklol.inference.MarginalCalculator.ZeroProbabilityError;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.training.GradientOracle;
import com.jayantkrish.jklol.training.LogFunction;

public class WeightedCcgPerceptronOracle implements GradientOracle<CcgParser, WeightedCcgExample> {

  private final ParametricCcgParser family;
  private final CcgBeamSearchInference inferenceAlgorithm;
  private final ExpressionComparator comparator;

  public WeightedCcgPerceptronOracle(ParametricCcgParser family,
      CcgBeamSearchInference inferenceAlgorithm, ExpressionComparator comparator) {
    this.family = Preconditions.checkNotNull(family);
    this.inferenceAlgorithm = Preconditions.checkNotNull(inferenceAlgorithm);
    this.comparator = Preconditions.checkNotNull(comparator);
  }

  @Override
  public SufficientStatistics initializeGradient() {
    return family.getNewSufficientStatistics();
  }

  @Override
  public CcgParser instantiateModel(SufficientStatistics parameters) {
    return family.getModelFromParameters(parameters);
  }

  @Override
  public double accumulateGradient(SufficientStatistics gradient,
      SufficientStatistics currentParameters, CcgParser instantiatedParser,
      WeightedCcgExample example, LogFunction log) {
    // Gradient is the features of the correct CCG parse minus the
    // features of the best predicted parse.

    // Calculate the best predicted parse, i.e., the highest weight parse
    // without conditioning on the true parse.
    log.startTimer("update_gradient/unconditional_max_marginal");
    List<CcgParse> parses = inferenceAlgorithm.beamSearch(instantiatedParser, example.getSentence(), null, log);

    if (parses.size() == 0) {
      // System.out.println("Search error (Predicted): " + example.getSentence());
      log.stopTimer("update_gradient/unconditional_max_marginal");
      throw new ZeroProbabilityError();
    }
    CcgParse bestPredictedParse = parses.get(0);
    log.stopTimer("update_gradient/unconditional_max_marginal");

    // Calculate the best conditional parse, i.e., the highest weight parse
    // with the correct syntactic tree and set of semantic dependencies.
    log.startTimer("update_gradient/conditional_max_marginal");
    List<CcgParse> correctParses = filterParsesByLogicalForm(example.getLogicalForm(), comparator,
        parses, example.getWeight() > 0);
    if (correctParses.size() == 0) {
      // Search error: couldn't find any correct parses.
      System.out.println("Search error (Correct): " + example.getSentence() + " " + example.getLogicalForm());
      System.out.println("predicted: " + bestPredictedParse.getLogicalForm());
      // System.out.println("Expected tree: " + example.getSyntacticParse());
      // System.out.println("Search error cause: " + conditionalChartFilter.analyzeParseFailure());
      log.stopTimer("update_gradient/conditional_max_marginal");
      throw new ZeroProbabilityError();
    }
    CcgParse bestCorrectParse = correctParses.get(0);
    log.stopTimer("update_gradient/conditional_max_marginal");

    // System.out.println("best predicted: " + bestPredictedParse + " " + bestPredictedParse.getSubtreeProbability());
    // System.out.println("best correct:   " + bestCorrectParse + " " + bestCorrectParse.getSubtreeProbability());

    log.startTimer("update_gradient/increment_gradient");
    // Subtract the predicted feature counts.
    family.incrementSufficientStatistics(gradient, currentParameters,
        example.getSentence(), bestPredictedParse, -1.0 * Math.abs(example.getWeight()));
    // Add the feature counts of best correct parse.
    family.incrementSufficientStatistics(gradient, currentParameters,
        example.getSentence(), bestCorrectParse, 1.0 * Math.abs(example.getWeight()));
    log.stopTimer("update_gradient/increment_gradient");

    // Return the amount by which the predicted parse's score exceeds the
    // true parse. (Negate this value, because this is a maximization problem)
    return Math.min(0.0, Math.log(bestCorrectParse.getSubtreeProbability())
        - Math.log(bestPredictedParse.getSubtreeProbability()));
  }

  public static List<CcgParse> filterParsesByLogicalForm(Expression2 observedLogicalForm,
      ExpressionComparator comparator, Iterable<CcgParse> parses, boolean returnCorrect) {
    List<CcgParse> correctParses = Lists.newArrayList();
    for (CcgParse parse : parses) {
      Expression2 predictedLogicalForm = parse.getLogicalForm();

      if (predictedLogicalForm != null &&
          (comparator.equals(predictedLogicalForm, observedLogicalForm) == returnCorrect)) {
        correctParses.add(parse);
      } 
    }
    return correctParses;
  }
}
