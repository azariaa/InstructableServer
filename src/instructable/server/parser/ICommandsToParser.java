package instructable.server.parser;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.backend.ActionResponse;

import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public interface ICommandsToParser
{
    void addTrainingEg(String originalCommand, List<Expression2> commandsLearnt, Optional<ActionResponse> replyWhenDone);

    //needs to add ConceptName to the lexicon and the conceptName to the environment table
    void newConceptDefined(String conceptName);

    //needs to add FieldName to the lexicon and the conceptName to the environment table
    void newFieldDefined(String fieldName);

    //needs to add instanceName to the lexicon and the conceptName to the environment table
    void newInstanceDefined(String instanceName);

    //adds a list of alternatives from new demonstration
    void newDemonstrateAlt(String type, List<String> names, boolean willTrainLater);

    void removeField(String fieldName);

    void removeInstance(String instanceName);

    void undefineConcept(String conceptName);

    void failNextCommand();
}
