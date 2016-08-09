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
        LinkedList<SayOrExec> allSayToUserOrExec = new LinkedList<>();
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
        if (sayToUserOrExec.size() > 0 && !sayToUserOrExec.getLast().isCmdExec())
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
        this.sayToUserOrExec.add(new SayOrExec(sayToUser));
    }

    public ActionResponse(JSONObject execForUser, boolean success, Optional<String> learningSentence)
    {
        this(success, learningSentence);
        this.sayToUserOrExec.add(new SayOrExec(execForUser));
    }

    public ActionResponse(LinkedList<SayOrExec> sayToUserOrExec, boolean success, Optional<String> learningSentence)
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
     * includes all sequential executions as a single json execution command. Preserves order of sentences.
     */
    public String getSayToUserOrExec()
    {
        StringBuilder retVal = new StringBuilder();
        boolean isPrevExec = false;
        Optional<JSONObject> tmpCmdsToExec = Optional.empty();
        for (SayOrExec sayToOrExec : sayToUserOrExec)
        {
            if (!sayToOrExec.isCmdExec())
            {
                if (tmpCmdsToExec.isPresent()) //if we have some JSON commands, add them and empty it
                {
                    retVal.append(Consts.execCmdPre + tmpCmdsToExec.get().toString());
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
                    try
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
                        nextBlock.put("nextBlock", source.getJSONObject("nextBlock"));
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            isPrevExec = sayToOrExec.isCmdExec();
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

    private LinkedList<SayOrExec> sayToUserOrExec = new LinkedList<>(); //holds a sentence to say, or a JSON to execute

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

    class SayOrExec
    {
        public boolean isCmdExec()
        {
            return cmdExec;
        }

        boolean cmdExec = false;
        String toSay = "";
        JSONObject toExec;

        public SayOrExec(String toSay)
        {
            this.toSay = toSay;
            cmdExec = false;
        }

        public SayOrExec(JSONObject toExec)
        {
            this.toExec = toExec;
            cmdExec = true;
        }
    }
}
