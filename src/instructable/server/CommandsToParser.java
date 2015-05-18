package instructable.server;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import java.io.FileWriter;
import java.io.IOException;
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
        Expression2 expression = CcgUtils.combineCommands(commandsLearnt);
        FileWriter out = null;
        try
        {
            out = new FileWriter(tempFileName, true);
            out.write(originalCommand + "," + expression.toString() + "\n");
            out.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void newConceptDefined(String conceptName)
    {
        //maybe convert to one function.
        CcgUtils.updateParserGrammar(conceptName + ",ConceptName{0}," + conceptName, parserSettings);
        parserSettings.env.bindName(conceptName, conceptName.replace("_", " "), parserSettings.symbolTable);
    }

    @Override
    public void newFieldDefined(String fieldName)
    {
        CcgUtils.updateParserGrammar(fieldName + ",FieldName{0}," + fieldName, parserSettings);
        parserSettings.env.bindName(fieldName, fieldName.replace("_", " "), parserSettings.symbolTable);
    }

    @Override
    public void newInstanceDefined(String instanceName)
    {
        CcgUtils.updateParserGrammar(instanceName + ",InstanceName{0}," + instanceName, parserSettings);
        parserSettings.env.bindName(instanceName, instanceName.replace("_", " "), parserSettings.symbolTable);
    }
}
