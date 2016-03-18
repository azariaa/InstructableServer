package instructable.server.senseffect;

import java.util.Date;
import java.util.List;

/**
 * Created by Amos Azaria on 14-Mar-16.
 */
public interface ICalendarAccessor
{
    void SaveEvent(String title, String description, Date time, double durationInMinutes, List<String> participants);
}
