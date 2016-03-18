package instructable.server.hirarchy.fieldTypes;

import instructable.server.backend.ExecutionStatus;


/**
 * Created by Amos on 15-Apr-15.
 * <p>
 * fieldTypes can't be added by the users
 */
public class TypeDouble extends FieldType
{

    @Override
    public boolean isEmpty()
    {
        return fieldVal == null;
    }

    @Override
    public void appendTo(ExecutionStatus executionStatus, String toAdd, boolean toEnd)
    {
        try
        {
            Double numToAdd = Double.parseDouble(toAdd);
            Double originalNum = 0.0;
            if (fieldVal != null)
            {
                try
                {
                    originalNum = Double.parseDouble(fieldVal);
                }
                catch (Exception ignored)
                {
                }
            }
            fieldVal = (toAdd + originalNum).toString();
        }
        catch (Exception ex)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "\"" + toAdd + "\" is not a proper number");
        }
    }

    @Override
    public void setFromString(ExecutionStatus executionStatus, String val)
    {
        try
        {
            Double temp = Double.parseDouble(val);
            fieldVal = val;
        }
        catch (Exception ex)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "\"" + val + "\" is not a proper number");
        }
    }

    public double getDouble()
    {
        double retVal = 0.0;
        if (fieldVal != null)
        {
            try
            {
                retVal = Double.parseDouble(fieldVal);
            }
            catch (Exception ignored)
            {
            }
        }
        return retVal;
    }
}
