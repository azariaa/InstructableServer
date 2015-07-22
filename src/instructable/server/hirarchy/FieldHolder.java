package instructable.server.hirarchy;

import instructable.server.ExecutionStatus;
import instructable.server.InstUtils;
import instructable.server.TextFormattingUtils;
import instructable.server.dal.SingleInstance;
import instructable.server.hirarchy.fieldTypes.EmailAddress;
import instructable.server.hirarchy.fieldTypes.FieldType;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;
import instructable.server.hirarchy.fieldTypes.StringField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Amos Azaria on 15-Apr-15.
 * <p>
 * This class is like a union of FieldType and List<FieldType>.
 * In future may add a pointer to a different instance.
 * must report any changes to this field to singleInstance (so these changes can be reflected in the DB)
 */
public class FieldHolder
{
    SingleInstance singleInstance; //any change must be reported to the singleInstance

    public FieldHolder(FieldDescription fieldDescription, String parentInstanceName, SingleInstance reportsOnChanges)
    {
        singleInstance = reportsOnChanges;
        this.fieldName = fieldDescription.fieldName;
        this.fieldType = fieldDescription.fieldType;
        this.isList = fieldDescription.isList;
        this.parentInstanceName = parentInstanceName;
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

    public static FieldHolder createFromDBdata(FieldDescription fieldDescription, String parentInstanceName, SingleInstance reportsOnChanges, JSONObject jsonObject)
    {
        FieldHolder fieldCreated = new FieldHolder(fieldDescription,parentInstanceName,reportsOnChanges);
        ExecutionStatus dummy = new ExecutionStatus();
        fieldCreated.setFromJSon(dummy, jsonObject, false, true, true, false); //don't update DB (assuming that this call was initiated by the DB).
        return fieldCreated;
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
    private String parentInstanceName;

    public String getParentInstanceName()
    {
        return parentInstanceName;
    }


    static final String fieldTypeForJson = "fieldType";
    static final String isListForJson = "isList";
    static final String fieldForJson = "field";
    static final String fieldListForJson = "fieldList";

    public void set(ExecutionStatus executionStatus, String toSet, boolean setAlsoImmutable)
    {
        set(executionStatus, toSet, setAlsoImmutable, true);
    }

    private void set(ExecutionStatus executionStatus, String toSet, boolean setAlsoImmutable, boolean updateDB)
    {
        if (!setAlsoImmutable && !mutable)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "\"" + fieldName + "\" is immutable");
            return;
        }


        //special case for treating list of email addresses (I wish this could have been done in a more generic way)
        if (fieldType == PossibleFieldType.emailAddress && isList)
        {
            List<String> simplyTokenized = Arrays.asList(toSet.split(" "));
            if (simplyTokenized.size() > 1)
            {
                if (InstUtils.getEmailAddressRelation(simplyTokenized) == InstUtils.EmailAddressRelation.listOfEmails)
                {
                    setToList(executionStatus, InstUtils.getEmailsFromEmailList(simplyTokenized), setAlsoImmutable);
                    if (updateDB)
                        updateDB();
                    return;
                }
            }
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
        if (updateDB)
            updateDB();
    }

    /**
     * Caller needs to updateDB (if necessary).
     */
    private void setToList(ExecutionStatus executionStatus, List<String> listToSet, boolean setAlsoImmutable)
    {
        if (listToSet.size() <= 0)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "the list is empty");
        }
        set(executionStatus, listToSet.get(0), setAlsoImmutable, false);
        for (int i = 1; i < listToSet.size(); i++)
            appendTo(executionStatus,listToSet.get(i),true,setAlsoImmutable, false);
    }

    public void appendTo(ExecutionStatus executionStatus, String toAdd, boolean toEnd, boolean setAlsoImmutable)
    {
        appendTo(executionStatus, toAdd, toEnd, setAlsoImmutable, true);
    }

    private void appendTo(ExecutionStatus executionStatus, String toAdd, boolean toEnd, boolean setAlsoImmutable, boolean updateDB)
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
        }
        else
        {
            field.appendTo(executionStatus, toAdd, toEnd);
        }
        if (updateDB)
            updateDB();
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

    //TODO: remove setAlsoImmutable?
    public void setFromJSon(ExecutionStatus executionStatus, JSONObject jsonObject, boolean addToExisting, boolean appendToEnd, boolean setAlsoImmutable)
    {
        setFromJSon(executionStatus, jsonObject, addToExisting, appendToEnd, setAlsoImmutable, true);
    }

    private void setFromJSon(ExecutionStatus executionStatus, JSONObject jsonObject, boolean addToExisting, boolean appendToEnd, boolean setAlsoImmutable, boolean updateDB)
    {
        if (!setAlsoImmutable && !mutable)
        {
            executionStatus.add(ExecutionStatus.RetStatus.error, "\"" + fieldName + "\" is immutable");
            return;
        }
        boolean mustSetFromList = false;
        if (jsonObject.containsKey(isListForJson) && (boolean) jsonObject.get(isListForJson) ||
                !jsonObject.containsKey(isListForJson) && jsonObject.containsKey(fieldListForJson))
        {
            mustSetFromList = true;
        }
        boolean madeAChange = false;
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
                    appendTo(executionStatus, singleField, appendToEnd, setAlsoImmutable, false); //don't update DB now, will update at end if required.
                    madeAChange = true;
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
                        appendTo(executionStatus, fieldListAsString.get(0), appendToEnd, setAlsoImmutable, false); //don't update DB now, will update at end if required.
                        madeAChange = true;
                    }
                    else
                    {
                        set(executionStatus, fieldListAsString.get(0), setAlsoImmutable, false); //don't update DB now, will update at end if required.
                        madeAChange = true;
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
                appendTo(executionStatus, (String) jsonObject.get(fieldForJson), appendToEnd, setAlsoImmutable, false); //don't update DB now, will update at end if required.
                madeAChange = true;
            }
            else
            {
                set(executionStatus, (String) jsonObject.get(fieldForJson), setAlsoImmutable, false); //don't update DB now, will update at end if required.
                madeAChange = true;
            }
        }

        if (updateDB && madeAChange)
            updateDB();
    }

    private void updateDB()
    {
        singleInstance.fieldChanged(fieldName, getFieldVal());
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
