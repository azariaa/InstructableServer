package instructable.server;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;

import java.util.List;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public interface ICommandsToParser
{
    void addTrainingEg(String originalCommand, List<Expression2> commandsLearnt);

    //needs to add ConceptName to the lexicon and the conceptName to the environment table
    void newConceptDefined(String conceptName);

    //needs to add FieldName to the lexicon and the conceptName to the environment table
    void newFieldDefined(String fieldName);

    //needs to add instanceName to the lexicon and the conceptName to the environment table
    void newInstanceDefined(String instanceName);
}
