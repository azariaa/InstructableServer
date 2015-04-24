package instructable.server.hirarchy.fieldTypes;

import instructable.server.ExecutionStatus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * Created by Amos on 15-Apr-15.
 *
 * fieldTypes can't be added by the users
 */
public class EmailAddress extends FieldType
{
    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private boolean testAddress(String addr)
    {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(addr);
        return matcher.matches();
    }

    /*
    returns true if manged to set
     */
    public void setAddress(ExecutionStatus executionStatus, String val)
    {
        if (testAddress(val))
        {
            fieldVal = val;
            return;
        }

        //tries to fix email address, especially useful for speech input.
        val = val.replace(" at ", " @ ");
        val = val.replace(" dot ", " . ");
        val = val.trim().replace(" ", "");
        if (testAddress(val))
        {
            fieldVal = val;
            executionStatus.add(ExecutionStatus.RetStatus.comment, "minor email fixes were performed");
            return;
        }

        executionStatus.add(ExecutionStatus.RetStatus.error, "\""+ val + "\" is not an email address");
        return;
    }

    public String getAddress()
    {
        return fieldVal;
    }

    @Override
    public boolean isEmpty()
    {
        return fieldVal == null;
    }

    @Override
    public void appendTo(ExecutionStatus executionStatus, String toAdd, boolean toEnd)
    {
       //can't add to an email address, just replace it.
        //may indicate that the user really means to have a list of emails
        ExecutionStatus settingRetVal = new ExecutionStatus();
        setAddress(settingRetVal, toAdd);
        if (settingRetVal.isOkOrComment())
        {
            executionStatus.add(ExecutionStatus.RetStatus.warning, "an email address cannot be added, and therefore was replaced");
            return;
        }
        else
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "it is not possible to add to an existing email address; this email address must be replaced with a valid email address");
            return;
        }
    }

    @Override
    public void setFromString(ExecutionStatus executionStatus, String val)
    {
        setAddress(executionStatus, val);
    }
}
