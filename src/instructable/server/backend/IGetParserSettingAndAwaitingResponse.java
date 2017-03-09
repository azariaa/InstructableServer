package instructable.server.backend;

import instructable.server.ccg.ParserSettings;

import java.util.Optional;

/**
 * Created by Amos Azaria on 07-Jul-15.
 */
public interface IGetParserSettingAndAwaitingResponse
{
    Optional<ActionResponse> getNSetPendingActionResponse();
    ParserSettings getParserSettings();
}
