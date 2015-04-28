package instructable.server;

import com.google.common.base.Preconditions;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.lisp.FunctionValue;

import java.util.List;

/**
 * Created by Amos Azaria on 28-Apr-15.
 */
public class LispExecutor
{
    IAllUserActions allUserActions;
    InfoForCommand infoForCommand;

    public LispExecutor(IAllUserActions allUserActions, InfoForCommand infoForCommand)
    {
        this.allUserActions = allUserActions;
        this.infoForCommand = infoForCommand;
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
            return allUserActions.sendEmail(infoForCommand);
        }
    }

    public class SetFunction implements FunctionValue
    {
        @Override
        public Object apply(List<Object> list, Environment environment)
        {
            Preconditions.checkArgument(list.size() >= 2 && list.size() <= 4);
            if (list.size() == 2)
            {
                if (list.get(1) instanceof ActionResponse)
                    return allUserActions.set(infoForCommand, (String) list.get(0), ((ActionResponse) list.get(1)).value.get());
                return allUserActions.set(infoForCommand, (String) list.get(0), (String) list.get(1));
            }
            else if (list.size() == 3)
            {
                if (list.get(2) instanceof ActionResponse)
                    return allUserActions.set(infoForCommand, (String) list.get(0), (String) list.get(1), ((ActionResponse) list.get(2)).value.get());
                return allUserActions.set(infoForCommand, (String) list.get(0), (String) list.get(1), (String) list.get(2));
            }
            if (list.get(3) instanceof ActionResponse)
                return allUserActions.set(infoForCommand, (String) list.get(0), (String) list.get(1), (String) list.get(2), ((ActionResponse) list.get(3)).value.get());
            return allUserActions.set(infoForCommand, (String) list.get(0), (String) list.get(1), (String) list.get(2), (String) list.get(3));
        }
    }
}
