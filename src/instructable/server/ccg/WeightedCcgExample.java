package instructable.server.ccg;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.CcgExample;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.nlpannotation.AnnotatedSentence;

public class WeightedCcgExample {

  private final AnnotatedSentence sentence;
  private final Expression2 logicalForm;

  private final double weight;
  
  public WeightedCcgExample(AnnotatedSentence sentence, Expression2 logicalForm, double weight) {
    this.sentence = Preconditions.checkNotNull(sentence);
    this.logicalForm = Preconditions.checkNotNull(logicalForm);

    this.weight = weight;
  }
  
  public static List<CcgExample> toCcgExamples(List<WeightedCcgExample> examples) {
    List<CcgExample> ccgExamples = Lists.newArrayList();
    for (WeightedCcgExample e : examples) {
      ccgExamples.add(e.toCcgExample());
    }
    return ccgExamples;
  }

  public AnnotatedSentence getSentence() {
    return sentence;
  }

  public Expression2 getLogicalForm() {
    return logicalForm;
  }
  
  public CcgExample toCcgExample() {
    return new CcgExample(sentence, null, null, logicalForm, null);
  }

  public double getWeight() {
    return weight;
  }
}
