package instructable.server;

import com.google.common.base.Preconditions;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.lisp.FunctionValue;

import java.util.List;

/**
 * Created by Amos Azaria on 28-Apr-15.
 */
public class LispToCommand
{
    IAllUserActions allUserActions;
    String userSays;

    public LispToCommand(IAllUserActions allUserActions, String userSays)
    {
        this.allUserActions = allUserActions;
        this.userSays = userSays;
    }

    public SendEmailFunction getSendEmailFunction() {
        return new SendEmailFunction();
    }

    public SetFunction getSetFunction()
    {
        return new SetFunction();
    }

    public class SendEmailFunction implements FunctionValue
    {
        @Override
        public Object apply(List<Object> argumentValues, Environment env) {
            Preconditions.checkArgument(argumentValues.size() == 0);
            return allUserActions.sendEmail(userSays);
        }
    }

    public class SetFunction implements FunctionValue
    {
        @Override
        public Object apply(List<Object> list, Environment environment)
        {
            Preconditions.checkArgument(list.size() == 3);
            //if (list.get(2) instanceof  ActionResponse)
                //return allUserActions.set(userSays, (String)list.get(0), (String)list.get(1), (ActionResponse)list.get(2));
            return allUserActions.set(userSays, (String)list.get(0), (String)list.get(1), (String)list.get(2));
        }
    }
}
