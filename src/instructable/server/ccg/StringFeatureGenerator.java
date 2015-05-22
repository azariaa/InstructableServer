package instructable.server.ccg;

import java.util.Map;

import com.google.common.collect.Maps;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer.StringContext;
import com.jayantkrish.jklol.preprocessing.FeatureGenerator;

public class StringFeatureGenerator implements FeatureGenerator<StringContext, String> {
  private static final long serialVersionUID = 1L;

  @Override
  public Map<String, Double> generateFeatures(StringContext context) {
    int stringLength = 1 + (context.getSpanEnd() - context.getSpanStart());
    
    Map<String, Double> featureValues = Maps.newHashMap();
    featureValues.put("length=" + stringLength, 1.0);

    return featureValues;
  }
}
