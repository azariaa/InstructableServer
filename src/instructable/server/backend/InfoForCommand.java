package instructable.server.backend;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;

import java.util.Date;
import java.util.Optional;

/**
 * Created by Amos Azaria on 28-Apr-15.
 */
public class InfoForCommand
{
    public InfoForCommand(String userSentence, Expression2 expression, Optional<Date> currentTimeOnUsersPhone)
    {
        this.userSentence = userSentence;
        this.expression = expression;
        this.currentTimeOnUsersPhone = currentTimeOnUsersPhone;
    }

    String userSentence;
    Expression2 expression;
    Optional<Date> currentTimeOnUsersPhone;
}
