package instructable.server;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;

/**
 * Created by Amos Azaria on 28-Apr-15.
 */
public class InfoForCommand
{
    public InfoForCommand(String userSentence, Expression2 expression)
    {
        this.userSentence = userSentence;
        this.expression = expression;
    }

    String userSentence;
    Expression2 expression;
}
