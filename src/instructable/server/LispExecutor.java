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

    public AllFunction getFunction(String name) {
        return new AllFunction(name);
    }


    public class AllFunction implements FunctionValue
    {
        String currentFunction;
        public AllFunction(String currentFunction)
        {
            this.currentFunction = currentFunction;
        }
        @Override
        public Object apply(List<Object> argumentValues, Environment environment)
        {
            switch(currentFunction)
            {
                case "set":
//                    Preconditions.checkArgument(argumentValues.size() >= 2 && argumentValues.size() <= 4);
//                    if (argumentValues.size() == 2)
//                    {
//                        if (argumentValues.get(1) instanceof ActionResponse)
//                            return allUserActions.set(infoForCommand, (String) argumentValues.get(0), ((ActionResponse) argumentValues.get(1)).value.get());
//                        return allUserActions.set(infoForCommand, (String) argumentValues.get(0), (String) argumentValues.get(1));
//                    } else if (argumentValues.size() == 3)
//                    {
//                        if (argumentValues.get(2) instanceof ActionResponse)
//                            return allUserActions.set(infoForCommand, (String) argumentValues.get(0), (String) argumentValues.get(1), ((ActionResponse) argumentValues.get(2)).value.get());
//                        return allUserActions.set(infoForCommand, (String) argumentValues.get(0), (String) argumentValues.get(1), (String) argumentValues.get(2));
//                    }
//                    if (argumentValues.get(3) instanceof ActionResponse)
//                        return allUserActions.set(infoForCommand, (String) argumentValues.get(0), (String) argumentValues.get(1), (String) argumentValues.get(2), ((ActionResponse) argumentValues.get(3)).value.get());
//                    return allUserActions.set(infoForCommand, (String) argumentValues.get(0), (String) argumentValues.get(1), (String) argumentValues.get(2), (String) argumentValues.get(3));
                case "sendEmail":
                    Preconditions.checkArgument(argumentValues.size() == 0);
                    return allUserActions.sendEmail(infoForCommand);
            }
            return new Object();
        }
    }
}
