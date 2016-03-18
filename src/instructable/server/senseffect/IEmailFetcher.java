package instructable.server.senseffect;

import instructable.server.hirarchy.EmailInfo;

import java.util.Optional;

/**
 * Created by Amos Azaria on 10-Aug-15.
 */
public interface IEmailFetcher
{
    int getLastEmailIdx();
    Optional<EmailInfo> getEmailInfo(int emailIdx);
}
