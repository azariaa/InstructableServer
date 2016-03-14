package instructable.server.hirarchy;

import instructable.server.backend.ExecutionStatus;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
public class CalendarEvent
{
    public static final String strCalendarEventTypeAndName = "calendar event";
    public static final String titleStr = "title";
    public static final String descriptionStr = "description";
    public static final String dateTimeStr = "date and time";
    public static final String participantsListStr = "participant list";

    protected GenericInstance instance;

    public static List<FieldDescription> getFieldDescriptions(boolean mutable)
    {

        FieldDescription[] fieldDescriptions = new FieldDescription[]
                {
                        new FieldDescription(titleStr, PossibleFieldType.singleLineString, false, mutable),
                        new FieldDescription(descriptionStr, PossibleFieldType.multiLineString, false, mutable),
                        new FieldDescription(dateTimeStr, PossibleFieldType.date, false, mutable), //sender is never mutable
                        new FieldDescription(participantsListStr, PossibleFieldType.emailAddress, true, mutable)//,
                        //new FieldDescription(copyListStr, PossibleFieldType.emailAddress, true, mutable)
                };
        return  Arrays.asList(fieldDescriptions);
    }


    public CalendarEvent(InstanceContainer instanceContainer, String eventId, boolean isMutable)
    {
        instanceContainer.addInstance(new ExecutionStatus(), strCalendarEventTypeAndName, eventId, isMutable);
        instance = instanceContainer.getInstance(new ExecutionStatus(), strCalendarEventTypeAndName, eventId).get(); //not checking, since just created.
    }

    public CalendarEvent(GenericInstance instance)
    {
        this.instance = instance;
    }


    private String getFieldWithoutChecking(String fieldName)
    {
        return instance.getField(new ExecutionStatus(), fieldName).get().fieldValForUser();
    }

    public GenericInstance getInstance()
    {
        return instance;
    }

    public boolean hasDate()
    {
        return !instance.fieldIsEmpty(dateTimeStr);
    }
}
