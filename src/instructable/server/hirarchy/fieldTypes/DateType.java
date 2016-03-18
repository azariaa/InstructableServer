package instructable.server.hirarchy.fieldTypes;

import instructable.server.backend.ExecutionStatus;
import instructable.server.backend.InstUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;


/**
 * Created by Amos on 15-Apr-15.
 * <p>
 * fieldTypes can't be added by the users
 */
public class DateType extends FieldType
{

    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /*
    returns true if managed to set
     */
    public void setDouble(ExecutionStatus executionStatus, String val)
    {
        Optional<Date> dateRes = InstUtils.getDate(val);
        if (dateRes.isPresent())
        {
            fieldVal = dateFormat.format(dateRes.get());
            return;
        }

        executionStatus.add(ExecutionStatus.RetStatus.error, "\"" + val + "\" is not a proper date");
    }

    public Optional<Date> getDate()
    {
        Date asDate;
        try
        {
            asDate = dateFormat.parse(fieldVal);
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
        return Optional.of(asDate);
    }

    @Override
    public boolean isEmpty()
    {
        try
        {
            dateFormat.parse(fieldVal);
            return false;
        }
        catch (Exception e)
        {
            return true;
        }
    }

    @Override
    public void appendTo(ExecutionStatus executionStatus, String toAdd, boolean toEnd)
    {
        //can't add to an email address, just replace it.
        //may indicate that the user really means to have a list of emails
        ExecutionStatus settingRetVal = new ExecutionStatus();
        setDouble(settingRetVal, toAdd);
        if (settingRetVal.isOkOrComment())
        {
            executionStatus.add(ExecutionStatus.RetStatus.warning, "a date time cannot be added, and therefore was replaced");
            return;
        }
        else
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "it is not possible to add to an date time; this date time must be replaced with a valid date time");
            return;
        }
    }

    @Override
    public void setFromString(ExecutionStatus executionStatus, String val)
    {
        setDouble(executionStatus, val);
    }
}
