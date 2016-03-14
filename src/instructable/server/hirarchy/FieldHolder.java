package instructable.server.hirarchy;

import instructable.server.backend.ExecutionStatus;
import instructable.server.backend.InstUtils;
import instructable.server.backend.TextFormattingUtils;
import instructable.server.dal.IFieldChanged;
import instructable.server.hirarchy.fieldTypes.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Amos Azaria on 15-Apr-15.
 * <p>
 * This class is like a union of FieldType and List<FieldType>.
 * In future may add a pointer to a different instance.
 * must report any changes to this field to reportsOnChanges (so these changes can be reflected in the DB)
 */
public class FieldHolder
{

    String fieldName;
    PossibleFieldType fieldType;
    boolean isList;
    boolean mutable;
    FieldType field;
    List<FieldType> fieldList;
    private String parentInstanceName;

    IFieldChanged reportsOnChanges; //any change must be reported to the reportsOnChanges


    public FieldHolder(FieldDescription fieldDescription, String parentInstanceName, IFieldChanged reportsOnChanges, Optional<JSONObject> jsonValue)
    {
        this.reportsOnChanges = reportsOnChanges;
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

        if (jsonValue.isPresent())
        {
            ExecutionStatus dummy = new ExecutionStatus();
            setFromJSon(dummy, jsonValue.get(), false, true, true, false); //don't update DB (assuming that this call was initiated by the DB).
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
            case date:
                fieldVal = new DateType();
                break;
        }
        return fieldVal;
    }

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
            appendTo(executionStatus, listToSet.get(i), true, setAlsoImmutable, false);
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
        String retVal = "";
        if (jsonObject.length() == 0 || (!jsonObject.has(fieldListForJson) && !jsonObject.has(fieldForJson)))
            return retVal;
        try
        {
            boolean isFromList = false;
            if (jsonObject.has(isListForJson) && (boolean) jsonObject.get(isListForJson) ||
                    !jsonObject.has(isListForJson) && jsonObject.has(fieldListForJson))
            {
                isFromList = true;
            }
            if (isFromList)
            {
                retVal = "<empty>";
                JSONArray jsonArray = (JSONArray) jsonObject.get(fieldListForJson);
                if (jsonArray.length() >= 1)
                {
                    //if has only one, won't have separation symbol
                    retVal = (jsonArray.get(0)).toString();
                    for (int i = 1; i < jsonArray.length(); i++)
                        retVal = retVal + TextFormattingUtils.uiListSepSymbol + (jsonArray.get(i)).toString();
                }
            }
            else
            {
                retVal = jsonObject.get(fieldForJson).toString();
            }
        } catch (JSONException e)
        {
            e.printStackTrace();
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


        try
        {
            obj.put(fieldTypeForJson, fieldType.toString());
            obj.put(isListForJson, isList);
            if (isList)
            {
                List<String> fieldStrList = fieldList.stream().map(FieldType::asString).collect(Collectors.toList());
                JSONArray jArray = new JSONArray(fieldStrList);
                obj.put(fieldListForJson, jArray);
            }
            else
            {
                obj.put(fieldForJson, field.asString());
            }
        } catch (JSONException e)
        {
            e.printStackTrace();
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
        boolean madeAChange = false;
        try
        {
            if (jsonObject.has(isListForJson) && (boolean) jsonObject.get(isListForJson) ||
                    !jsonObject.has(isListForJson) && jsonObject.has(fieldListForJson))
            {
                mustSetFromList = true;
            }
            if (mustSetFromList)
            {
                List<String> fieldListAsString = InstUtils.convertJArrToStrList((JSONArray) jsonObject.get(fieldListForJson));
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
        } catch (JSONException e)
        {
            e.printStackTrace();
        }

        if (updateDB && madeAChange)
            updateDB();
    }

    private void updateDB()
    {
        reportsOnChanges.fieldChanged(getFieldVal());
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
