package instructable.server.backend;

import java.util.Optional;

/**
 * Created by Amos Azaria on 07-Jul-15.
 */
public interface IGetAwaitingResponse
{
    Optional<ActionResponse> getNSetPendingActionResponse();
}
