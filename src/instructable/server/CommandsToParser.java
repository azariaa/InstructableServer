package instructable.server;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.ccg.ParserSettings;

import java.util.List;

/**
 * Created by Amos Azaria on 13-May-15.
 */
public class CommandsToParser implements ICommandsToParser
{
    static final String tempFileName = "learntCommands.csv";
    ParserSettings parserSettings;
    public CommandsToParser(ParserSettings parserSettings)
    {
        this.parserSettings = parserSettings;
    }
    @Override
    public void addTrainingEg(String originalCommand, List<Expression2> commandsLearnt)
    {
        parserSettings.addTrainingEg(originalCommand, commandsLearnt);
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
        parserSettings.updateParserGrammar(actualName + "," + what + "Name{0}," + actualName.replace(" ", "_"));
        parserSettings.env.bindName(actualName.replace(" ", "_"), actualName.replace("_", " "), parserSettings.symbolTable);
        //parserSettings.retrain(2);
    }

    private void toRemove(ConceptFieldInstance conceptFieldInstance, String actualName)
    {
        String what = conceptFieldInstance.toString();
        parserSettings.removeFromParserGrammar(actualName + "," + what + "Name{0}," + actualName.replace(" ", "_"));
        //parserSettings.env.unbindName() //doesn't have this function, but it doesn't really matter
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
}
