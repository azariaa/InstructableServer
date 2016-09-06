package instructable.server.parser;

import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.backend.ActionResponse;
import instructable.server.backend.IGetAwaitingResponse;
import instructable.server.ccg.ParserSettings;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Amos Azaria on 13-May-15.
 * Every update to the parser also updates DB
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

    private enum ConceptFieldInstance {Concept,Field,Instance}//,Script}

    @Override
    public void newConceptDefined(String conceptName)
    {
        newDefined(ConceptFieldInstance.Concept, conceptName);
    }

    private void newDefined(ConceptFieldInstance conceptFieldInstance, String actualName)
    {
        newDefined(conceptFieldInstance.toString() + "Name", actualName);

    }

    private void newDefined(String what, String actualName)
    {
        parserSettings.updateParserGrammar("\""+actualName + "\",\"" + what + "{0}\",\"" + actualName.replace(" ", "_") + "\",0 \"" + actualName.replace(" ", "_")+"\"");
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
    public void newDemonstrateAlt(String type, List<String> names, boolean willTrainLater)
    {
        List<String> lexEntriesToAdd = names.stream().map(actualName-> "\""+actualName + "\",\"" + type + "{0}\",\"" + actualName.replace(" ", "_") + "\",0 \"" + actualName.replace(" ", "_")+"\"").collect(Collectors.toList());
        List<LexiconEntry> lexiconEntries = LexiconEntry.parseLexiconEntries(lexEntriesToAdd);
        parserSettings.updateParserGrammar(lexiconEntries, willTrainLater);
        for (String actualName : names)
            parserSettings.env.bindName(actualName.replace(" ", "_"), actualName.replace("_", " "), parserSettings.symbolTable);
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
