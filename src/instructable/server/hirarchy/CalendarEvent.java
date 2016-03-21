package instructable.server.hirarchy;

import instructable.server.backend.ExecutionStatus;
import instructable.server.hirarchy.fieldTypes.DateType;
import instructable.server.hirarchy.fieldTypes.PossibleFieldType;
import instructable.server.hirarchy.fieldTypes.TypeDouble;
import instructable.server.senseffect.ICalendarAccessor;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 15-Apr-15.
 */
public class CalendarEvent
{
    public static final String strCalendarEventTypeAndName = "calendar event";

    public static final String titleStr = "subject";//"title";
    public static final String descriptionStr = "description";
    public static final String dateTimeStr = "date and time";
    public static final String durationStr = "duration";
    public static final String participantsListStr = "participant list";
    //could add location

    public static final Double defaultDuration = 30.0;

    protected GenericInstance instance;

    public static List<FieldDescription> getFieldDescriptions(boolean mutable)
    {

        FieldDescription[] fieldDescriptions = new FieldDescription[]
                {
                        new FieldDescription(titleStr, PossibleFieldType.singleLineString, false, mutable),
                        new FieldDescription(descriptionStr, PossibleFieldType.multiLineString, false, mutable),
                        new FieldDescription(dateTimeStr, PossibleFieldType.date, false, mutable),
                        new FieldDescription(participantsListStr, PossibleFieldType.emailAddress, true, mutable),
                        new FieldDescription(durationStr, PossibleFieldType.typeDouble, false, mutable)
                        //new FieldDescription(copyListStr, PossibleFieldType.emailAddress, true, mutable)
                };
        return  Arrays.asList(fieldDescriptions);
    }


    public CalendarEvent(InstanceContainer instanceContainer, String eventId, boolean isMutable)
    {
        instanceContainer.addInstance(new ExecutionStatus(), strCalendarEventTypeAndName, eventId, isMutable);
        instance = instanceContainer.getInstance(new ExecutionStatus(), strCalendarEventTypeAndName, eventId).get(); //not checking, since just created.
        instance.setField(new ExecutionStatus(), durationStr, Optional.of(defaultDuration.toString()), Optional.empty(), false, false, false);
    }

    public CalendarEvent(InstanceContainer instanceContainer, ICalendarAccessor.EventFields eventFields, boolean isMutable)
    {
        this(instanceContainer, eventFields.eventId, isMutable);
        simpleSet(titleStr, eventFields.title);
        simpleSet(descriptionStr, eventFields.description);
        simpleSet(dateTimeStr, DateType.dateFormat.format(eventFields.time));
        simpleSet(durationStr, eventFields.durationInMinutes.toString());
    }

    private void simpleSet(String field, String val)
    {
        if (field != null && val != null)
            instance.setField(new ExecutionStatus(), field, Optional.of(val), Optional.empty(), false, false, true);
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

    public String getTitle()
    {
        return getFieldWithoutChecking(titleStr);
    }

    public String getDescription()
    {
        return getFieldWithoutChecking(descriptionStr);
    }

    public Optional<Date> getDate()
    {
        return ((DateType)(instance.getField(new ExecutionStatus(), dateTimeStr).get().field)).getDate();
    }

    public double getDuration()
    {
        return ((TypeDouble)(instance.getField(new ExecutionStatus(), durationStr).get().field)).getDouble();
    }

    public List<String> getParticipants()
    {
        return null; //TODO:!!!!
    }
}
