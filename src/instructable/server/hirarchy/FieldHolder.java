package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;
import instructable.server.InstUtils;
import instructable.server.TextFormattingUtils;
import instructable.server.hirarchy.fieldTypes.EmailAddress;
import instructable.server.hirarchy.fieldTypes.FieldType;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;
import instructable.server.hirarchy.fieldTypes.StringField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Amos Azaria on 15-Apr-15.
 * <p>
 * This class is like a union of FieldType and List<FieldType>.
 * In future may add a pointer to a different instance.
 */
public class FieldHolder
{

    FieldHolder(FieldDescription fieldDescription, GenericInstance fieldParent)
    {
        this.fieldName = fieldDescription.fieldName;
        this.fieldType = fieldDescription.fieldType;
        this.isList = fieldDescription.isList;
        this.fieldParent = fieldParent;
        this.mutable = fieldDescription.mutable;

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
    boolean mutable;
    FieldType field;
    List<FieldType> fieldList;
    private GenericInstance fieldParent;

    public String getParentInstanceName()
    {
        return fieldParent.getName();
    }


    static final String fieldTypeForJson = "fieldType";
    static final String isListForJson = "isList";
    static final String fieldForJson = "field";
    static final String fieldListForJson = "fieldList";

    public void set(ExecutionStatus executionStatus, String toSet, boolean setAlsoImmutable)
    {
        if (!setAlsoImmutable && !mutable)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "\"" + fieldName + "\" is immutable");
            return;
        }
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


    public void appendTo(ExecutionStatus executionStatus, String toAdd, boolean toEnd, boolean setAlsoImmutable)
    {
        if (!setAlsoImmutable && !mutable)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "\"" + fieldName + "\" is immutable");
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
                fieldList.add(0, toBeAdded);
            return;
        }
        else
        {
            field.appendTo(executionStatus, toAdd, toEnd);
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

    /*
        should be used only for presenting a value to the user.
     */
    static public String fieldFromJSonForUser(JSONObject jsonObject)
    {
        if (jsonObject.isEmpty() || (!jsonObject.containsKey(fieldListForJson) && !jsonObject.containsKey(fieldForJson)))
            return "";
        boolean isFromList = false;
        if (jsonObject.containsKey(isListForJson) && (boolean) jsonObject.get(isListForJson) ||
                !jsonObject.containsKey(isListForJson) && jsonObject.containsKey(fieldListForJson))
            isFromList = true;
        String retVal;
        if (isFromList)
        {
            retVal = "<empty>";
            JSONArray jsonArray = (JSONArray) jsonObject.get(fieldListForJson);
            if (jsonArray.size() >= 1)
            {
                //if has only one, won't have separation symbol
                retVal = (jsonArray.get(0)).toString();
                for (int i = 1; i < jsonArray.size(); i++)
                    retVal = retVal + TextFormattingUtils.uiListSepSymbol + (jsonArray.get(i)).toString();
            }
        }
        else
        {
            retVal = jsonObject.get(fieldForJson).toString();
        }
        return retVal;
    }

    public String fieldValForUser()
    {
        return fieldFromJSonForUser(getFieldVal());
    }

    public JSONObject getFieldVal()
    {
        JSONObject obj = new JSONObject();


        obj.put(fieldTypeForJson, fieldType.toString());
        obj.put(isListForJson, isList);
        if (isList)
        {
            JSONArray jArray = fieldList.stream().map(FieldType::asString).collect(Collectors.toCollection(() -> new JSONArray()));
            obj.put(fieldListForJson, jArray);
        }
        else
        {
            obj.put(fieldForJson, field.asString());
        }
        return obj;
    }

    public void setFromJSon(ExecutionStatus executionStatus, JSONObject jsonObject, boolean addToExisting, boolean appendToEnd, boolean setAlsoImmutable)
    {
        if (!setAlsoImmutable && !mutable)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "\"" + fieldName + "\" is immutable");
            return;
        }
        boolean mustSetFromList = false;
        if (jsonObject.containsKey(isListForJson) && (boolean) jsonObject.get(isListForJson) ||
                !jsonObject.containsKey(isListForJson) && jsonObject.containsKey(fieldListForJson))
            mustSetFromList = true;
        if (mustSetFromList)
        {
            List<String> fieldListAsString = InstUtils.convertJArrToStrList((JSONArray)jsonObject.get(fieldListForJson));
            if (isList)
            {
                if (!addToExisting)
                {
                    fieldList.clear();
                }
                //if need to append to beginning, need first to reverse the array.
                if (!appendToEnd)
                    Collections.reverse(fieldListAsString);
                for (String singleField : fieldListAsString)
                {
                    appendTo(executionStatus, singleField, appendToEnd, setAlsoImmutable);
                }
            }
            else
            {
                //if input is a list (array), and this isn't a list,
                if (fieldListAsString.size() >= 1)
                {
                    if (fieldListAsString.size() > 1)
                    {
                        executionStatus.add(ExecutionStatus.RetStatus.warning, "taking only first item out of " + fieldListAsString.size());
                    }
                    //if it has only one item, take it,
                    if (addToExisting)
                    {
                        appendTo(executionStatus, fieldListAsString.get(0), appendToEnd, setAlsoImmutable);
                    }
                    else
                    {
                        set(executionStatus, fieldListAsString.get(0), setAlsoImmutable);
                    }
                }
                else
                {
                    executionStatus.add(ExecutionStatus.RetStatus.warning, "list is empty");
                }

            }
        }
        else
        {
            if (addToExisting)
            {
                appendTo(executionStatus, (String) jsonObject.get(fieldForJson), appendToEnd, setAlsoImmutable);
            }
            else
            {
                set(executionStatus, (String) jsonObject.get(fieldForJson), setAlsoImmutable);
            }
        }

        return;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    @Override
    public String toString()
    {
        return fieldName;
    }
}
