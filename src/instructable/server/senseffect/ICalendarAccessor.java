package instructable.server.senseffect;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Created by Amos Azaria on 14-Mar-16.
 */
public interface ICalendarAccessor
{
    class EventFields
    {
        public String title;
        public String description;
        public Date time;
        public Double durationInMinutes;
        public List<String> participants;
        public String eventId;

        public EventFields(String eventId, String title, String description, Date time, double durationInMinutes, List<String> participants)
        {
            this.title = title;
            this.description = description;
            this.time = time;
            this.durationInMinutes = durationInMinutes;
            this.participants = participants;
            this.eventId = eventId;
        }
    }
    void SaveEvent(EventFields eventFields);
    Optional<EventFields> getNextPrevCalendarEvent(boolean next, Date startingFrom, Double currEventDuration);
}
