package instructable.server.backend;

import com.google.common.base.Preconditions;
import instructable.server.Consts;
import instructable.server.hirarchy.FieldHolder;
import instructable.server.hirarchy.GenericInstance;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 20-Apr-15.
 * <p>
 * Holds sayToUserOrExec and success. Can also hold a value (Json), an instance and a field. Caller must check type!
 * sayToUserOrExec is only used in upper most call (or the first failure). [Though sometime, the user may expect two responses]
 */
public class ActionResponse //extends RuntimeException
{

    public static ActionResponse createFromList(List<ActionResponse> actionResponseList)
    {
        if (actionResponseList.isEmpty())
            return new ActionResponse("Error: nothing to append", false, Optional.empty());
        Optional<String> learningSentence = Optional.empty();
        //StringBuilder fullResponse = new StringBuilder();
        LinkedList<SayExecOrCmd> allSayToUserOrExec = new LinkedList<>();
        boolean success = true;
        for (ActionResponse actionResponse : actionResponseList)
        {
            allSayToUserOrExec.addAll(actionResponse.sayToUserOrExec);
            if (actionResponse.learningSentence.isPresent()) //always use last one (which may be failure.
                learningSentence = actionResponse.learningSentence;
            if (!actionResponse.isSuccess())
            {
                success = false;
                break;
            }
        }

        return new ActionResponse(allSayToUserOrExec, success, learningSentence);
    }

    public String onlySentenceToUser()
    {
        if (sayToUserOrExec.size() > 0 && sayToUserOrExec.getLast().isCmdExec() == TypeOfCmdForUser.toSay)
            return sayToUserOrExec.getLast().toSay;
        return "found nothing to say";
    }

    public enum ActionResponseType
    {
        simple, value, instance, field
    }

    private ActionResponse(boolean success, Optional<String> learningSentence)
    {
        this.learningSentence = learningSentence;
        this.success = success;
        type = ActionResponseType.simple;
    }


    /**
     *
     * @param sayToUser Don't add "\n" at the end. Will add if needed.
     * @param success
     * @param learningSentence
     */
    public ActionResponse(String sayToUser, boolean success, Optional<String> learningSentence)
    {
        this(success, learningSentence);
        this.sayToUserOrExec.add(new SayExecOrCmd(sayToUser));
    }

    public ActionResponse(JSONObject execForUser, boolean success, Optional<String> learningSentence)
    {
        this(success, learningSentence);
        this.sayToUserOrExec.add(new SayExecOrCmd(execForUser));
    }

    static public ActionResponse reqEmailAndPswd()
    {
        ActionResponse response = new ActionResponse(false, Optional.empty());
        response.sayToUserOrExec.add(new SayExecOrCmd(TypeOfCmdForUser.noEmailPswd));
        return response;
    }

    public ActionResponse(LinkedList<SayExecOrCmd> sayToUserOrExec, boolean success, Optional<String> learningSentence)
    {
        this(success, learningSentence);
        this.sayToUserOrExec = sayToUserOrExec;
    }

    public void addValue(JSONObject value)
    {
        this.value = value;
        type = ActionResponseType.value;
    }

    public void addInstance(GenericInstance instance)
    {
        this.instance = instance;
        type = ActionResponseType.instance;
    }

    public void addField(FieldHolder field)
    {
        this.field = field;
        type = ActionResponseType.field;
    }

    public ActionResponseType getType()
    {
        return type;
    }

    /**
     * merges all sequential executions to a single json execution command. Preserves order of sentences.
     */
    public String getSayToUserOrExec()
    {
        StringBuilder retVal = new StringBuilder();

        Optional<JSONObject> tmpCmdsToExec = Optional.empty();
        for (SayExecOrCmd sayToOrExec : sayToUserOrExec)
        {
            if (sayToOrExec.isCmdExec() == TypeOfCmdForUser.noEmailPswd)
            {
                retVal.append(Consts.getEmailAndPassword).append("\n");
            }
            else if (sayToOrExec.isCmdExec() == TypeOfCmdForUser.toSay)
            {
                if (tmpCmdsToExec.isPresent()) //if we already have some JSON commands, add them and empty it
                {
                    retVal.append(Consts.execCmdPre + tmpCmdsToExec.get().toString());
                    retVal.append("\n");
                    tmpCmdsToExec = Optional.empty();
                }
                retVal.append(sayToOrExec.toSay).append("\n");
            }
            else
            {
                if (!tmpCmdsToExec.isPresent())
                {
                    tmpCmdsToExec = Optional.of(sayToOrExec.toExec);
                }
                else
                {
                    //add sayToOrExec.toExec to tmpCmdsToExec
                    JSONObject source = sayToOrExec.toExec;
                    JSONObject nextBlock = tmpCmdsToExec.get();
                    try //moving to the end of the next blocks
                    {
                        while (nextBlock.has("nextBlock") && nextBlock.get("nextBlock") != null)
                        {
                            nextBlock = nextBlock.getJSONObject("nextBlock");
                        }
                    } catch (JSONException ignored) //it's ok if there is an ex
                    {
                    }
                    try
                    {
                        JSONObject clonedNextBlock = new JSONObject(source.getJSONObject("nextBlock").toString());
                        nextBlock.put("nextBlock", clonedNextBlock);
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

        }
        if (tmpCmdsToExec.isPresent()) //if we have some JSON commands, add them
        {
            retVal.append(Consts.execCmdPre + tmpCmdsToExec.get().toString());
        }
        if (learningSentence.isPresent())
            retVal.append("\n" + learningSentence.get());
        //if we end with JSON commands, don't just add an empty line
        if (learningSentence.isPresent() || !tmpCmdsToExec.isPresent())
            retVal.append("\n");
        return retVal.toString();
    }

    private LinkedList<SayExecOrCmd> sayToUserOrExec = new LinkedList<>(); //holds a sentence to say, or a JSON to execute

    public boolean isSuccess()
    {
        return success;
    }

    private Optional<String> learningSentence;

    public JSONObject getValue()
    {
        Preconditions.checkState(type == ActionResponseType.value);
        return value;
    }

    public GenericInstance getInstance()
    {
        Preconditions.checkState(type == ActionResponseType.instance);
        return instance;
    }

    public FieldHolder getField()
    {
        Preconditions.checkState(type == ActionResponseType.field);
        return field;
    }

    private ActionResponseType type;
    private boolean success;
    private JSONObject value;
    private GenericInstance instance;
    private FieldHolder field;

    public enum TypeOfCmdForUser {toSay, toExecSug, noEmailPswd}
    static class SayExecOrCmd
    {
        public TypeOfCmdForUser isCmdExec()
        {
            return cmdExec;
        }

        TypeOfCmdForUser cmdExec = TypeOfCmdForUser.toSay;
        String toSay = "";
        JSONObject toExec;

        public SayExecOrCmd(String toSay)
        {
            this.toSay = toSay;
            cmdExec = TypeOfCmdForUser.toSay;
        }

        public SayExecOrCmd(JSONObject toExec)
        {
            this.toExec = toExec;
            cmdExec = TypeOfCmdForUser.toExecSug;
        }

        public SayExecOrCmd(TypeOfCmdForUser typeOfCmdForUser)
        {
            cmdExec = typeOfCmdForUser;
        }
    }
}
