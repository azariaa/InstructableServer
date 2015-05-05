package instructable.server;

import com.google.common.base.Preconditions;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.lisp.FunctionValue;
import instructable.server.hirarchy.FieldHolder;
import instructable.server.hirarchy.GenericInstance;
import org.json.simple.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
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

    public List<FunctionToExecute> getAllFunctions()
    {
        //get all functions using reflection
        List<FunctionToExecute> functionToExecutes = new LinkedList<>();
        Method[] methods = IAllUserActions.class.getMethods();
        // Arrays.asList(methods).forEach(method -> env.bindName(method.getName(), lispExecutor.getFunction(method.getName()), symbolTable));
        for (Method method : methods)
        {
            functionToExecutes.add(new FunctionToExecute(method.getName()));
        }
        return functionToExecutes;
    }
    //public AllFunction getFunction(String name) {
    //    return new AllFunction(name);
    //}


    public class FunctionToExecute implements FunctionValue
    {
        String currentFunction;

        public String getFunctionName()
        {
            return currentFunction;
        }
        public FunctionToExecute(String currentFunction)
        {
            this.currentFunction = currentFunction;
        }
        @Override
        public Object apply(List<Object> argumentValues, Environment environment)
        {
            try
            {
                //first get function by name (no overloading so it is easy)
                //we can't use IAllUserActions.class.getMethod(currentFunction), because we don't know the parameters
                Method method = null;
                Method[] methods = IAllUserActions.class.getMethods();
                for (Method mOption : methods)
                {
                    if (mOption.getName().equals(currentFunction))
                    {
                        method = mOption;
                        break;
                    }
                }
                if (method == null)
                    throw new NoSuchMethodException("method "+currentFunction+" not found");

                //then get the parameter and match.
                Class<?>[] parameters = method.getParameterTypes();
                Preconditions.checkArgument(argumentValues.size()+1 == parameters.length);
                List<Object> invokeArgs = new LinkedList<>();
                invokeArgs.add(infoForCommand);
                for (int i = 1; i < parameters.length; i++)
                {
                    int idxInArgs = i-1;
                    if (parameters[i].isAssignableFrom(String.class))
                        invokeArgs.add(argumentValues.get(idxInArgs));
                    if (parameters[i].isAssignableFrom(JSONObject.class))
                        invokeArgs.add(((ActionResponse)argumentValues.get(idxInArgs)).getValue());
                    if (parameters[i].isAssignableFrom(FieldHolder.class))
                        invokeArgs.add(((ActionResponse)argumentValues.get(idxInArgs)).getField());
                    if (parameters[i].isAssignableFrom(GenericInstance.class))
                        invokeArgs.add(((ActionResponse)argumentValues.get(idxInArgs)).getInstance());
                    //Preconditions.checkArgument(arg);
                }
                return method.invoke(allUserActions, invokeArgs.toArray());
            } catch (NoSuchMethodException e)
            {
                e.printStackTrace();
            } catch (InvocationTargetException e)
            {
                e.printStackTrace();
            } catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
//
//            switch(currentFunction)
//            {
//                //do all this with reflection.
//
//
//                IAllUserActions.class.getMethod(currentFunction);
//                case "sendEmail":
//                    Preconditions.checkArgument(argumentValues.size() == 0);
//                    return allUserActions.sendEmail(infoForCommand);
//                case "composeEmail":
//                    return allUserActions.composeEmail(infoForCommand);
//                case "yes":
//                    return allUserActions.yes(infoForCommand);
//                case "no":
//                    return allUserActions.no(infoForCommand);
//                //case "cancel":
//                    //return allUserActions.cancel(infoForCommand);
//
//                case "getProbFieldByInstanceNameAndFieldName":
//                    return allUserActions.getProbFieldByInstanceNameAndFieldName(infoForCommand,(String)argumentValues.get(0),(String)argumentValues.get(1));
//                case "getProbFieldByFieldName":
//                    return allUserActions.getProbFieldByFieldName(infoForCommand, (String)argumentValues.get(0));
//                case "evalField":
//                    return allUserActions.evalField(infoForCommand, ((ActionResponse)argumentValues.get(0)).getField());
//                case "setFieldFromString":
//                    return allUserActions.setFieldFromString(infoForCommand,((ActionResponse)argumentValues.get(0)).getField(),(String)argumentValues.get(1));
//
//
//
//            }
            return null;
        }
    }
}
