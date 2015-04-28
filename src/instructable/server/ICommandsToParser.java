package instructable.server;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;

import java.util.List;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public interface ICommandsToParser
{
    void addTrainingEg(String originalCommand, List<Expression2> commandsLearnt);

    void newConceptDefined(String conceptName);

    void newFieldDefined(String fieldName);

    void newInstanceDefined(String instanceName);
}
