package instructable.server.hirarchy.fieldTypes;

import instructable.server.ExecutionStatus;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class StringField extends FieldType
{

    public StringField(boolean isMultiline)
    {
        multiline = isMultiline;
    }

    boolean multiline = false;


    public boolean isMultiline()
    {
        return  multiline;
    }

    public String getString()
    {
        return  fieldVal;
    }

    public void setString(String val)
    {
        fieldVal = val;
    }

    @Override
    public boolean isEmpty()
    {
        return ((String)fieldVal).trim().isEmpty();
    }

    @Override
    public void appendTo(ExecutionStatus executionStatus, String toAdd, boolean toEnd)
    {
        String strBetween = " ";
        if (multiline)
            strBetween = "\n";
        if (toEnd)
        {
            if (fieldVal.isEmpty() || toAdd.isEmpty() || Character.isWhitespace(toAdd.charAt(0)) || Character.isWhitespace(fieldVal.charAt(fieldVal.length() - 1)))
                fieldVal = fieldVal + toAdd;
            else
                fieldVal = fieldVal + strBetween + toAdd;
        }
        else
        {
            if (fieldVal.isEmpty() || toAdd.isEmpty() || Character.isWhitespace(toAdd.charAt(toAdd.length() - 1)) || Character.isWhitespace(fieldVal.charAt(0)))
                fieldVal = toAdd + fieldVal;
            else
                fieldVal = toAdd + strBetween + fieldVal;
        }
    }

    @Override
    public void setFromString(ExecutionStatus executionStatus, String val)
    {
        setString(val);
    }
}
