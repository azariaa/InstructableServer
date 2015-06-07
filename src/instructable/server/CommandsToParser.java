package instructable.server;

import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.CcgExample;
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import instructable.server.ccg.CcgUtils;
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
        Expression2 expressionLearnt = CcgUtils.combineCommands(commandsLearnt);
//        FileWriter out = null;
//        try
//        {
//            out = new FileWriter(tempFileName, true);
//            out.write(originalCommand + "," + expression.toString() + "\n");
//            out.close();
//        } catch (IOException e)
//        {
//            e.printStackTrace();
//        }

        CcgExample example = CcgUtils.createCcgExample(originalCommand, expressionLearnt, parserSettings.posUsed);

        List<LexiconEntry> newEntries = CcgUtils.induceLexiconEntriesHeuristic(example, parserSettings.parser);

        parserSettings.updateParserGrammar(newEntries, Lists.newArrayList());
        parserSettings.ccgExamples.add(example);
        SufficientStatistics newParameters = CcgUtils.train(parserSettings.parserFamily, parserSettings.ccgExamples);

        parserSettings.parser = parserSettings.parserFamily.getModelFromParameters(newParameters);
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
}
