package instructable.server.ccg;

import com.google.common.collect.Maps;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer.StringContext;
import com.jayantkrish.jklol.preprocessing.FeatureGenerator;
import instructable.server.InstUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StringFeatureGenerator implements FeatureGenerator<StringContext, String>
{
    private static final long serialVersionUID = 1L;

    @Override
    public Map<String, Double> generateFeatures(StringContext context)
    {

        Map<String, Double> featureValues = Maps.newHashMap();
        featureValues.put("length", (double) getStringLength(context));
        featureValues.put("orgLength", (double) context.getWords().size());
        featureValues.put("baseVerbCount", (double) verbCount(context, true));
        featureValues.put("totVerbCount", (double) verbCount(context, false));
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
