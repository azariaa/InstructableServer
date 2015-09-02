package instructable.server;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.ccg.ParserSettings;

import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 13-May-15.
 */
public class CommandsToParser implements ICommandsToParser, IGetAwaitingResponse
{
    static final String tempFileName = "learntCommands.csv";
    ParserSettings parserSettings;
    Optional<ActionResponse> pendingActionResponse = Optional.empty();
    boolean isCurrentlyLearning = false;
    private final Object lockWhileLearning = new Object();
    public CommandsToParser(ParserSettings parserSettings)
    {
        this.parserSettings = parserSettings;
    }

    @Override
    public void addTrainingEg(String originalCommand, List<Expression2> commandsLearnt, Optional<ActionResponse> actionResponseWhenDone)
    {
        synchronized (lockWhileLearning)
        {
            parserSettings.addTrainingEg(originalCommand, commandsLearnt);
            pendingActionResponse = actionResponseWhenDone;
        }
    }

    @Override
    public Optional<ActionResponse> getNSetPendingActionResponse()
    {
        Optional<ActionResponse> retVal = Optional.empty();
        synchronized (lockWhileLearning)
        {
            if (pendingActionResponse.isPresent())
            {
                retVal = pendingActionResponse;
                pendingActionResponse = Optional.empty();
            }
        }
        return retVal;
    }

    private enum ConceptFieldInstance {Concept,Field,Instance}

    @Override
    public void newConceptDefined(String conceptName)
    {
        newDefined(ConceptFieldInstance.Concept, conceptName);
    }

    private void newDefined(ConceptFieldInstance conceptFieldInstance, String actualName)
    {
        String what = conceptFieldInstance.toString();
        parserSettings.updateParserGrammar("\""+actualName + "\",\"" + what + "Name{0}\",\"" + actualName.replace(" ", "_") + "\",0 \"" + actualName.replace(" ", "_")+"\"");
        parserSettings.env.bindName(actualName.replace(" ", "_"), actualName.replace("_", " "), parserSettings.symbolTable);
        //parserSettings.retrain(2);
    }

    private void toRemove(ConceptFieldInstance conceptFieldInstance, String actualName)
    {
        String what = conceptFieldInstance.toString();
        parserSettings.removeFromParserGrammar("\""+actualName + "\",\"" + what + "Name{0}\",\"" + actualName.replace(" ", "_")+"\"");
        parserSettings.env.unbindName(actualName.replace(" ", "_"), parserSettings.symbolTable);
        //parserSettings.retrain(2);
    }

    @Override
    public void newFieldDefined(String fieldName)
    {
        newDefined(ConceptFieldInstance.Field, fieldName);
    }

    @Override
    public void newInstanceDefined(String instanceName)
    {
        newDefined(ConceptFieldInstance.Instance, instanceName);
    }

    @Override
    public void removeField(String fieldName)
    {
        toRemove(ConceptFieldInstance.Field, fieldName);
    }

    @Override
    public void removeInstance(String instanceName)
    {
        toRemove(ConceptFieldInstance.Instance, instanceName);
    }

    @Override
    public void undefineConcept(String conceptName)
    {
        toRemove(ConceptFieldInstance.Concept, conceptName);
    }

    @Override
    public void failNextCommand()
    {
        this.parserSettings.failNextCommand();
    }
}
