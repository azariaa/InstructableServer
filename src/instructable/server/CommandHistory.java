package instructable.server;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.Callable;

/**
 * Created by Amos Azaria on 03-Aug-15.
 */
public class CommandHistory
{
    Stack<FullUserCommand> commandStack;
    boolean recording;

    private static class FullUserCommand
    {

        Stack<SingleExecution> partsOfUserCommand;
        int commandId;


        private static class SingleExecution
        {
            Callable<ActionResponse> callableForUndo;

            public SingleExecution(Callable<ActionResponse> callableForSingleExecutable)
            {
                callableForUndo = callableForSingleExecutable;
            }

            ActionResponse undo() throws Exception
            {
                return callableForUndo.call();
            }
        }

        FullUserCommand(int commandId)
        {
            this.commandId = commandId;
            partsOfUserCommand = new Stack<>();
        }

        public int getCommandID()
        {
            return commandId;
        }

        /**
         *
         * @return appended result of full user command. empty if no command was undone.
         */
        Optional<ActionResponse> undo()
        {
            List<ActionResponse> actionResponseList = new LinkedList<>();
            while (!partsOfUserCommand.isEmpty())
            {
                try
                {
                    ActionResponse response = partsOfUserCommand.pop().undo();
                    actionResponseList.add(response);
                    if (!response.isSuccess())
                        break;
                } catch (Exception e)
                {
                    break;
                }
            }
            if (actionResponseList.isEmpty())
                return Optional.empty();

            return Optional.of(ActionResponse.createFromList(actionResponseList));
        }

        void push(Callable<ActionResponse> callableForSingleExecutable)
        {
            partsOfUserCommand.push(new SingleExecution(callableForSingleExecutable));
        }
    }


    CommandHistory()
    {
        commandStack = new Stack<>();
    }


    public void startRecording()
    {
        recording = true;
    }

    public boolean isExecutingAnUndoNow()
    {
        return !recording;
    }

    public Optional<ActionResponse> undo()
    {
        if (commandStack.isEmpty())
            return Optional.empty();

        boolean oldRec = recording;
        recording = false;
        Optional<ActionResponse> res = commandStack.pop().undo();
        recording = oldRec;
        return res;
    }

    //should be called only on success
    public void push(InfoForCommand infoForCommand, Callable<ActionResponse> callableForUndo)
    {
        if (recording)
        {
            int commandId = infoForCommand.hashCode();
            //TODO: check if belongs to last command or if should start and push a new command
            if (commandStack.isEmpty() || commandStack.peek().getCommandID() != commandId)
            {
                commandStack.push(new FullUserCommand(commandId));
            }
            commandStack.peek().push(callableForUndo);
        }
    }
}
