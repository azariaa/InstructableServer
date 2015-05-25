package instructable.server.ccg;

import com.google.common.collect.Maps;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer.StringContext;
import com.jayantkrish.jklol.preprocessing.FeatureGenerator;

import java.util.Map;

public class StringFeatureGenerator implements FeatureGenerator<StringContext, String> {
  private static final long serialVersionUID = 1L;

  @Override
  public Map<String, Double> generateFeatures(StringContext context) {

    Map<String, Double> featureValues = Maps.newHashMap();
    featureValues.put("length=" + getStringLength(context), 1.0);
    //featureValues.put("rootContingency=" + getRootContingency(context), 1.0);

    return featureValues;
  }

    private int getStringLength(StringContext context)
    {
        return 1 + (context.getSpanEnd() - context.getSpanStart());
    }

    //private String getRootContingency(StringContext context)
    //{
     //   return 1 + (context.getSpanEnd() - context.getSpanStart());
    //}
}
