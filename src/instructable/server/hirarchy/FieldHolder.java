package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;
import instructable.server.hirarchy.fieldTypes.EmailAddress;
import instructable.server.hirarchy.fieldTypes.FieldType;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;
import instructable.server.hirarchy.fieldTypes.StringField;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Amos Azaria on 15-Apr-15.
 *
 * This class is like a union of FieldType and List<FieldType>.
 */
public class FieldHolder
{

    FieldHolder(FieldDescription fieldDescription)
    {
        this.fieldName = fieldDescription.fieldName;
        this.fieldType = fieldDescription.fieldType;
        this.isList = fieldDescription.isList;

        if (isList)
        {
            fieldList = new LinkedList<FieldType>();
        }
        else
        {
            field = createNewField(fieldType);
        }

    }

    private static FieldType createNewField(PossibleFieldType fieldType)
    {
        FieldType fieldVal = null;
        switch (fieldType)
        {
            case emailAddress:
                fieldVal = new EmailAddress();
                break;
            case multiLineString:
                fieldVal = new StringField(true);
                break;
            case singleLineString:
                fieldVal = new StringField(false);
                break;
        }
        return fieldVal;
    }

    String fieldName;
    PossibleFieldType fieldType;
    boolean isList;
    FieldType field;
    List<FieldType> fieldList;

    public void set(ExecutionStatus executionStatus, String toSet)
    {
        FieldType toSetField = createNewField(fieldType);
        toSetField.setFromString(executionStatus, toSet);
        if (executionStatus.isError())
            return;
        if (isList)
        {
            fieldList.clear();
            fieldList.add(toSetField);
        }
        else
        {
            field = toSetField;
        }
        return;
    }

    public void appendToEnd(ExecutionStatus executionStatus, String toAdd)
    {
        field.appendTo(executionStatus, toAdd, true);
    }

    public void addToBeginning(ExecutionStatus executionStatus, String toAdd)
    {
        field.appendTo(executionStatus, toAdd, false);
    }

    private void appendTo(ExecutionStatus executionStatus, String toAdd, boolean toEnd)
    {
        if (toAdd == null)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "the toAdd field cannot be null");
            return;
        }

        if (isList)
        {
            FieldType toBeAdded = createNewField(fieldType);
            toBeAdded.setFromString(executionStatus, toAdd);
            if (executionStatus.isError())
                return;

            if (toEnd)
                fieldList.add(toBeAdded);
            else
                fieldList.add(0,toBeAdded);
            return;
        }
        else
        {
            field.appendTo(executionStatus, toAdd,toEnd);
            return;
        }
    }

    public boolean isEmpty()
    {
        if (isList)
        {
            return fieldList.isEmpty();
        }
        else
        {
            return field.isEmpty();
        }
    }
}
