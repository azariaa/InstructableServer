package instructable.server;

import instructable.server.hirarchy.OutgoingEmail;

/**
 * Created by Amos Azaria on 30-Apr-15.
 * //TODO: Should be later stored in DB or at least in a file (and should be able to update)
 */
public class AliasMapping
{
    public static String instanceNameMapping(String instanceName)
    {
        if (instanceName == "this email" || instanceName == "the email")
            return OutgoingEmail.strOutgoingEmailTypeAndName;

        return instanceName;
    }
}
