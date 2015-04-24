package instructable.server.hirarchy.fieldTypes;

import instructable.server.ExecutionStatus;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
abstract public class FieldType
{
    String fieldVal = "";
    public abstract boolean isEmpty();
    public abstract void appendTo(ExecutionStatus executionStatus, String toAdd, boolean toEnd);
    public abstract void setFromString(ExecutionStatus executionStatus, String val);

    public String asString()
    {
        return fieldVal;
    }
    //Object fieldVal
}
