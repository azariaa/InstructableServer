package instructable.server.ccg;

import instructable.server.utils.InstUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.jayantkrish.jklol.ccg.lexicon.StringContext;
import com.jayantkrish.jklol.preprocessing.FeatureGenerator;

public class StringFeatureGenerator implements FeatureGenerator<StringContext, String>
{
    private static final long serialVersionUID = 1L;
    
    private static final int MAX_LENGTH = 3;
    private static final int MAX_POS_LENGTH = 1;

    @Override
    public Map<String, Double> generateFeatures(StringContext context)
    {
        Map<String, Double> featureValues = Maps.newHashMap();
        featureValues.put(histogramFeatureValue("length", getStringLength(context), MAX_LENGTH), 1.0);

        featureValues.put(histogramFeatureValue("orgLength", context.getWords().size(), MAX_LENGTH), 1.0);

        int[] semiPosCount = semiPosCount(context);
        for (PosForFeatures posForFeatures : PosForFeatures.values()) {
          String baseFeatureName = "tot" + posForFeatures.toString();
          featureValues.put(histogramFeatureValue(baseFeatureName, semiPosCount[posForFeatures.ordinal()], MAX_POS_LENGTH), 1.0);
        }
        featureValues.put("atStart"+getPosForFeatures(context.getPos().get(context.getSpanStart())).toString(), 1.0);
        featureValues.put("atEnd"+getPosForFeatures(context.getPos().get(context.getSpanEnd())).toString(), 1.0);
        featureValues.put("endsAtEndOfSentence", (double) endsAtEndOfSentence(context));
        featureValues.put("startsAtBegOfSentence", (context.getSpanStart() == 0 ? 1.0 : 0.0));
        List<String> tokens = getRelevantTokens(context);
        InstUtils.EmailAddressRelation emailAddressRelation = InstUtils.getEmailAddressRelation(tokens);
        if (emailAddressRelation == InstUtils.EmailAddressRelation.isOneEmail ||
                emailAddressRelation == InstUtils.EmailAddressRelation.listOfEmails)
        {
            //encouraging a list of emails to be together (as it is also considered as "only emails" and also listOfEmails.
            featureValues.put("only emails", 1.0);
            featureValues.put("numOfEmailWords",(double)tokens.size()); //the more emails (and good "and"s) the better
        }
        if (emailAddressRelation != InstUtils.EmailAddressRelation.isOneEmail)
        {
            featureValues.put(emailAddressRelation.toString(), 1.0);
        }

        if (getStringLength(context) == 1 || emailAddressRelation == InstUtils.EmailAddressRelation.listOfEmails)
        {
            featureValues.put("oneUnit", 1.0); //treat length=1 different than all the rest
        }

        return featureValues;
    }
    
    private String histogramFeatureValue(String baseFeatureName, int value, int maxValue) {
      String featureName = null;
      if (value <= maxValue) {
        featureName = baseFeatureName + "=" + value;
      } else {
        featureName = baseFeatureName + ">" + maxValue;
      }
      return featureName.intern();
    }

    private List<String> getRelevantTokens(StringContext context)
    {
        List<String> tokens = new LinkedList<>();
        for (int idx = context.getSpanStart(); idx <= context.getSpanEnd(); idx++)
        {
            tokens.add(context.getWords().get(idx));
        }
        return tokens;
    }

    private int endsAtEndOfSentence(StringContext context)
    {
        if (context.getWords().size() == context.getSpanEnd() + 1)
            return 1;
        return 0;
    }

    enum PosForFeatures
    {
        baseVerb, //VB base verbs (like "set", "send", "forward", etc.) are more likely to be commands
        otherVerbs, //could be: VBD, VBG, VBN, VBP, VBZ
        nouns, //NN, NNS
        prpNprps,
        adverbs, //RB,RBR,RBS
        dt,
        different
    }

    private PosForFeatures getPosForFeatures(String pos)
    {
        if (pos.equals("VB"))
            return PosForFeatures.baseVerb;
        else if (pos.startsWith("VB"))
            return PosForFeatures.otherVerbs;
        else if (pos.startsWith("NN"))
            return PosForFeatures.nouns;
        else if (pos.startsWith("PRP"))
            return PosForFeatures.prpNprps;
        else if (pos.startsWith("RB"))
            return PosForFeatures.adverbs;
        return PosForFeatures.different;
    }

    private int[] semiPosCount(StringContext context)
    {
        int[] semiPosCounter = new int[PosForFeatures.values().length];
        for (int idx = context.getSpanStart(); idx <= context.getSpanEnd(); idx++)
        {
            String pos = context.getPos().get(idx);
            semiPosCounter[getPosForFeatures(pos).ordinal()]++;
        }
        return semiPosCounter;
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
