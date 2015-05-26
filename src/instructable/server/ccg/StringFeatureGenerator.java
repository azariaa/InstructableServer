package instructable.server.ccg;

import com.google.common.collect.Maps;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer.StringContext;
import com.jayantkrish.jklol.preprocessing.FeatureGenerator;
import instructable.server.InstUtils;

import java.util.Map;

public class StringFeatureGenerator implements FeatureGenerator<StringContext, String> {
  private static final long serialVersionUID = 1L;

  @Override
  public Map<String, Double> generateFeatures(StringContext context) {

    Map<String, Double> featureValues = Maps.newHashMap();
    featureValues.put("length", (double)getStringLength(context));
    featureValues.put("orgLength", (double)context.getWords().size());
    featureValues.put("baseVerbCount", (double)verbCount(context, true));
    featureValues.put("totVerbCount", (double)verbCount(context, false));
    featureValues.put("endsAtEndOfSentence", (double)endsAtEndOfSentence(context));
    featureValues.put("startsAtBegOfSentence", (context.getSpanStart() == 0 ? 1.0 : 0.0));
    if (getStringLength(context) == 1)
    {
        featureValues.put("length=1", 1.0); //treat length=1 different than all the rest
        String words = getRelevantWords(context);
        featureValues.put("isOnlyEmailAddress", InstUtils.isEmailAddress(words) ? 1.0 : 0.0);
    }
    //featureValues.put("containsAlsoEmailAddress", hasEmailAddress(context.getSpanStart() == 0 ? 1.0 : 0.0));
    //featureValues.put("rootConstituency=" + getRootConstituency(context), 1.0);

    return featureValues;
  }

    private String getRelevantWords(StringContext context)
    {
        StringBuilder words = new StringBuilder();
        for (int idx = context.getSpanEnd(); idx <= context.getSpanEnd(); idx++)
        {
            words.append(context.getWords().get(idx));
        }
        return words.toString();
    }

    private int endsAtEndOfSentence(StringContext context)
    {
        if (context.getWords().size() == context.getSpanEnd() + 1)
            return 1;
        return 0;
    }

    //base verbs (like "set", "send", "forward", etc.) are more likely to be commands
    private int verbCount(StringContext context, boolean onlyBase)
    {
        int baseVerbCounter = 0;
        for (int idx = context.getSpanEnd(); idx <= context.getSpanEnd(); idx++)
        {
            String pos = context.getPos().get(idx);
            if (pos.equals("VB") ||
                    !onlyBase && pos.startsWith("VB")) //could be: VB, VBD, VBG, VBN, VBP, VBZ
                baseVerbCounter++;
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
