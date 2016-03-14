package instructable.server.parser;

import com.google.common.base.Preconditions;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.lisp.FunctionValue;
import instructable.server.backend.ActionResponse;
import instructable.server.backend.IAllUserActions;
import instructable.server.backend.InfoForCommand;
import instructable.server.hirarchy.FieldHolder;
import instructable.server.hirarchy.GenericInstance;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Amos Azaria on 28-Apr-15.
 */
public class LispExecutor
{
    IAllUserActions allUserActions;
    InfoForCommand infoForCommand;
    Optional<ActionResponse> responseOfFailedCall = Optional.empty(); //if fails, sets the responseOfFailedCall to relevant ActionResponse, and stop execution

    public static final String doSeq = "doSeq";
    public static final String stringNoun = "stringNoun";
    public static final String stringValue = "stringValue";

    public LispExecutor(IAllUserActions allUserActions, InfoForCommand infoForCommand)
    {
        this.allUserActions = allUserActions;
        this.infoForCommand = infoForCommand;
    }

    public static List<String> allFunctionNames()
    {
        //get all functions using reflection
        List<String> allFunctionNames = Arrays.asList(IAllUserActions.class.getMethods()).stream().map(Method::getName).collect(Collectors.toList());
        allFunctionNames.add(doSeq);
        allFunctionNames.add(stringNoun);
        allFunctionNames.add(stringValue);
        return allFunctionNames;
    }

    public List<FunctionToExecute> getAllFunctions()
    {
        List<FunctionToExecute> functionToExecutes = new LinkedList<>();

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
                if (currentFunction.equals(stringNoun) || currentFunction.equals(stringValue))
                {
                    String retStr = (String) argumentValues.get(0); //should return a regular string.
                    if (currentFunction.equals(stringNoun)) //remove punctuation from begining and end
                        return retStr.trim().replaceAll("^[;.,]+","").replaceAll("[;.,]+$","");
                    return retStr;
                }

                if (currentFunction.equals(doSeq))
                {
                    // first get all those that succeeded.
                    List<ActionResponse> actionResponseList = argumentValues.stream().filter(obj -> obj instanceof ActionResponse && ((ActionResponse) obj).isSuccess()).map(obj -> (ActionResponse) obj).collect(Collectors.toCollection(LinkedList::new));
                    if (responseOfFailedCall.isPresent())
                        actionResponseList.add(responseOfFailedCall.get());

                    return ActionResponse.createFromList(actionResponseList);
                }

                if (responseOfFailedCall.isPresent())
                    return responseOfFailedCall.get();


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
                    responseOfFailedCall = Optional.of(retVal);
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
