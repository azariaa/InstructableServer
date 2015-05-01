package instructable.server;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 22-Apr-15.
 */
public class TextFormattingUtils
{
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
    public static boolean testOkAndFormat(InfoForCommand infoForCommand,
                  ExecutionStatus executionStatus,
                  boolean failWithWarningToo,
                  boolean ignoreComments,
                  StringBuilder response,
                  Optional<String> optionalSuccessSentence,
                  boolean askToTeachIfFails,
                  TopDMAllActions.InternalState internalState)
    {
        boolean isInLearningPhase = internalState.isInLearningMode();
        ExecutionStatus.RetStatus retStatus = executionStatus.getStatus();
        boolean success = retStatus == ExecutionStatus.RetStatus.ok || retStatus == ExecutionStatus.RetStatus.comment ||
                (retStatus == ExecutionStatus.RetStatus.warning && !failWithWarningToo);

        internalState.userGaveCommand(infoForCommand, success);

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
                        response.append("\nWhat should I do instead (when executing: \""+ internalState.lastCommandOrLearningCommand + "\")?");
                    }
                }
            } else if (executionStatus.isError())
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
                response.append("\nWhat shall I do next (when executing: \""+ internalState.lastCommandOrLearningCommand + "\")?");
        }


        return success;
    }


    static public void noEmailFound(StringBuilder response, TopDMAllActions.InternalState internalState)
    {
        response.append("I see that there is no email being composed.\n");
        response.append("Do you want to compose a new email?\n");
        internalState.pendOnEmailCreation();
    }

}
