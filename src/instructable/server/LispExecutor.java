package instructable.server;

import com.google.common.base.Preconditions;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.lisp.FunctionValue;
import instructable.server.hirarchy.FieldHolder;
import instructable.server.hirarchy.GenericInstance;
import org.json.simple.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Amos Azaria on 28-Apr-15.
 */
public class LispExecutor
{
    IAllUserActions allUserActions;
    InfoForCommand infoForCommand;

    public static final String doSeq = "doSeq";

    public LispExecutor(IAllUserActions allUserActions, InfoForCommand infoForCommand)
    {
        this.allUserActions = allUserActions;
        this.infoForCommand = infoForCommand;
    }

    public static List<String> allFunctionNames()
    {
        List<String> allFunctionNames = Arrays.asList(IAllUserActions.class.getMethods()).stream().map(Method::getName).collect(Collectors.toList());
        allFunctionNames.add(doSeq);
        return allFunctionNames;
    }

    public List<FunctionToExecute> getAllFunctions()
    {
        //get all functions using reflection
        List<FunctionToExecute> functionToExecutes = new LinkedList<>();
        Method[] methods = IAllUserActions.class.getMethods();
        // Arrays.asList(methods).forEach(method -> env.bindName(method.getName(), lispExecutor.getFunction(method.getName()), symbolTable));
        for (String functionName : allFunctionNames())
        {
            functionToExecutes.add(new FunctionToExecute(functionName));
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
                boolean hasAResponse = false;
                //make sure all ActionResponse we got are success, otherwise just propagate them up
                for (Object obj : argumentValues)
                {
                    if (obj instanceof ActionResponse)
                    {
                        if (!((ActionResponse) obj).isSuccess())
                            return obj;
                        else
                            hasAResponse = true;
                    }
                }

                if (currentFunction.equals(doSeq))
                {
                    if (!hasAResponse)
                        return new ActionResponse("Error! called do sequential, but no response found.", false);

                    //need to append all responses in the order of evaluation.

                    List<ActionResponse> actionResponseList = argumentValues.stream().filter(obj -> obj instanceof ActionResponse).map(obj -> (ActionResponse) obj).collect(Collectors.toCollection(() -> new LinkedList<>()));

                    return ActionResponse.createFromList(actionResponseList);
                }

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
                    throw new NoSuchMethodException("method " + currentFunction + " not found");

                //then get the parameter and match.
                Class<?>[] parameters = method.getParameterTypes();
                Preconditions.checkArgument(argumentValues.size() + 1 == parameters.length);
                List<Object> invokeArgs = new LinkedList<>();
                invokeArgs.add(infoForCommand);
                for (int i = 1; i < parameters.length; i++)
                {
                    int idxInArgs = i - 1;
                    if (parameters[i].isAssignableFrom(String.class))
                        invokeArgs.add(argumentValues.get(idxInArgs));
                    if (parameters[i].isAssignableFrom(JSONObject.class))
                        invokeArgs.add(((ActionResponse) argumentValues.get(idxInArgs)).getValue());
                    if (parameters[i].isAssignableFrom(FieldHolder.class))
                        invokeArgs.add(((ActionResponse) argumentValues.get(idxInArgs)).getField());
                    if (parameters[i].isAssignableFrom(GenericInstance.class))
                        invokeArgs.add(((ActionResponse) argumentValues.get(idxInArgs)).getInstance());
                    //Preconditions.checkArgument(arg);
                }
                ActionResponse retVal =  (ActionResponse)method.invoke(allUserActions, invokeArgs.toArray());
                if (!retVal.isSuccess())
                {
                    //Important!!! This runtime exception is actually a returnValue and is later caught in ParserSettings.evaluate.
                    //This is done this way, in order to stop the execution of the rest of the expression (without interfering with LispEval.eval()
                    throw retVal;
                }
                return retVal;
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
            return null;
        }
    }
}
