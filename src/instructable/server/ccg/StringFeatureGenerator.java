package instructable.server.ccg;

import com.google.common.collect.Maps;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer.StringContext;
import com.jayantkrish.jklol.preprocessing.FeatureGenerator;

import java.util.List;
import java.util.Map;

public class StringFeatureGenerator implements FeatureGenerator<StringContext, String> {
  private static final long serialVersionUID = 1L;

  @Override
  public Map<String, Double> generateFeatures(StringContext context) {

    Map<String, Double> featureValues = Maps.newHashMap();
    featureValues.put("length=" + getStringLength(context), 1.0);
    featureValues.put("orgLength=" + context.getWords().size(), 1.0);
    featureValues.put("baseVerbCount=" + verbCount(context, true), 1.0);
    featureValues.put("totVerbCount=" + verbCount(context, false), 1.0);
    featureValues.put("endsAtEndOfSentence=" + endsAtEndOfSentence(context), 1.0);
    featureValues.put("startsAtBegOfSentence=" + (context.getSpanStart() == 0 ? 1 : 0), 1.0);
    //featureValues.put("rootConstituency=" + getRootConstituency(context), 1.0);

    return featureValues;
  }

    private int endsAtEndOfSentence(StringContext context)
    {
        if (context.getWords().size() == context.getSpanEnd())
            return 1;
        return 0;
    }

    //base verbs (like "set", "send", "forward", etc.) are more likely to be commands
    private int verbCount(StringContext context, boolean onlyBase)
    {
        int baseVerbCounter = 0;
        for (int idx = context.getSpanEnd(); idx <= context.getSpanEnd(); idx++)
        {
            List<String> poss = context.getPos();
            if (poss != null) //TODO: Jayant why is POS sometimes null?
            {
                String pos = poss.get(idx);
                if (pos.equals("VB") ||
                        !onlyBase && pos.startsWith("VB")) //could be: VB, VBD, VBG, VBN, VBP, VBZ
                    baseVerbCounter++;
            }
        }
        return baseVerbCounter;
    }

    private int getStringLength(StringContext context)
    {
        return 1 + (context.getSpanEnd() - context.getSpanStart());
    }

    //private String getRootConstituency(StringContext context)
    //{
     //   return null;
    //}
}
