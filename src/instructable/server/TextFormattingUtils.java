package instructable.server;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 22-Apr-15.
 */
public class TextFormattingUtils
{
    final public static String uiListSepSymbol = ";";

    /*
            separates with commas and adds "and" instead of last comma.
         */
    public static String userFriendlyList(List<String> givenList)
    {
        if (givenList == null || givenList.size() == 0)
            return "none";
        String appendedFields = StringUtils.join(givenList, ", ");
        String toReplace = ",";  //replace last "," with and.
        String replacement = " and";
        int idx = appendedFields.lastIndexOf(",");
        if (idx != -1)
        {
            appendedFields = appendedFields.substring(0, idx)
                    + replacement
                    + appendedFields.substring(idx + toReplace.length(), appendedFields.length());
        }
        return appendedFields;
    }

    /*
        internalState can be null if askToTeachIfFails is false
        returns success.
     */
    public static ActionResponse testOkAndFormat(InfoForCommand infoForCommand,
                                          ExecutionStatus executionStatus,
                                          boolean failWithWarningToo,
                                          boolean ignoreComments,
                                          Optional<String> optionalSuccessSentence,
                                          boolean askToTeachIfFails,
                                          TopDMAllActions.InternalState internalState)
    {
        StringBuilder response = new StringBuilder();
        Optional<String> learningSentence = Optional.empty();
        boolean isInLearningPhase = internalState.isInLearningMode();
        ExecutionStatus.RetStatus retStatus = executionStatus.getStatus();
        boolean success = retStatus == ExecutionStatus.RetStatus.ok || retStatus == ExecutionStatus.RetStatus.comment ||
                (retStatus == ExecutionStatus.RetStatus.warning && !failWithWarningToo);

        internalState.userGaveCommand(infoForCommand, success); //this also clears pending on learning or  email creation

        if (retStatus == ExecutionStatus.RetStatus.error || retStatus == ExecutionStatus.RetStatus.warning ||
                (retStatus == ExecutionStatus.RetStatus.comment && !ignoreComments))
        {
            ExecutionStatus.StatusAndMessage statusAndMessage = executionStatus.getStatusAndMessage();
            if (statusAndMessage.message.isPresent())
            {
                if (success)
                {
                    response.append("I see that " + statusAndMessage.message.get() + ". ");
                }
                else
                {
                    response.append("Sorry, but " + statusAndMessage.message.get() + ".");

                    if (isInLearningPhase)
                    {
                        learningSentence = Optional.of("What should I do instead (when executing: \"" + internalState.lastCommandOrLearningCommand + "\")?");
                    }
                }
            }
            else if (executionStatus.isError())
            {
                response.append("There was some kind of error.");
            }
        }

        if (!success && askToTeachIfFails && !isInLearningPhase)
        {
            response.append("\nWould you like to teach me what to do in this case (either say yes or simply ignore this question)?");
            internalState.pendOnLearning();
        }

        if (success && optionalSuccessSentence.isPresent())
        {
            response.append(optionalSuccessSentence.get());
            if (isInLearningPhase)
                learningSentence = Optional.of("What shall I do next (when executing: \"" + internalState.lastCommandOrLearningCommand + "\")?");
        }

        if (learningSentence.isPresent())
        {
            if (internalState.shouldFailLearning())
                learningSentence = Optional.of("I didn't learn anything. If you want to teach me what to do when you say \"" + internalState.lastCommandOrLearningCommand + "\", say it again, and answer \"yes\" when I ask if you want to teach me.");
            else if (internalState.isLearningForTooLong() || internalState.userHavingTrouble())
            {
                learningSentence = Optional.of(learningSentence.get() + "\nI noticed that you are teaching me a command for a while now, it's ok with me and you may continue, but if you want me to end and learn this new command, say \"end\". If you want me to cancel this command say \"cancel\".");
            }
        }

        return new ActionResponse(response.toString(),success,learningSentence);
    }


    static public ActionResponse noEmailFound(TopDMAllActions.InternalState internalState)
    {
        return noEmail(internalState, new StringBuilder());
    }

    static public ActionResponse noEmailFound(TopDMAllActions.InternalState internalState, ActionResponse actionResponse)
    {
        return noEmail(internalState, new StringBuilder(actionResponse.onlySentenceToUser()));
    }

    static private ActionResponse noEmail(TopDMAllActions.InternalState internalState, StringBuilder response)
    {
        response.append("I see that there is no email being composed.\n");
        response.append("Do you want to compose a new email?\n");
        internalState.pendOnEmailCreation();
        return new ActionResponse(response.toString(),false, Optional.empty());
    }

}
