//package instructable.server.senseffect;
//
//import com.google.api.client.auth.oauth2.Credential;
//import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
//import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
//import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
//import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.http.HttpTransport;
//import com.google.api.client.json.JsonFactory;
//import com.google.api.client.json.jackson2.JacksonFactory;
//import com.google.api.client.util.DateTime;
//import com.google.api.client.util.store.FileDataStoreFactory;
//import com.google.api.services.calendar.Calendar;
//import com.google.api.services.calendar.CalendarScopes;
//import com.google.api.services.calendar.model.Event;
//import com.google.api.services.calendar.model.EventDateTime;
//import com.google.api.services.calendar.model.Events;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.*;
//
///**
// * Created by Amos Azaria on 15-Jul-15.
// */
//public class RealCalendar implements ICalendarAccessor
//{
//
//    /**
//     * Application name.
//     */
//    private static final String APPLICATION_NAME = "Inmind Calendar";
//
//    /**
//     * Directory to store user credentials for this application.
//     */
//    private static final java.io.File DATA_STORE_DIR = new java.io.File(
//            System.getProperty("user.home"), ".credentials/ENCinmind1");
//
//    /**
//     * Global instance of the {@link FileDataStoreFactory}.
//     */
//    private static FileDataStoreFactory DATA_STORE_FACTORY;
//
//    /**
//     * Global instance of the JSON factory.
//     */
//    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
//
//    /**
//     * Global instance of the HTTP transport.
//     */
//    private static HttpTransport HTTP_TRANSPORT;
//
//    /**
//     * Global instance of the scopes required by this quickstart.
//     */
//    private static final List<String> SCOPES = Arrays.asList(CalendarScopes.CALENDAR);
//
//    static
//    {
//        try
//        {
//            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
//        } catch (Throwable t)
//        {
//            t.printStackTrace();
//            System.exit(1);
//        }
//    }
//
//    Calendar calService;
//    public RealCalendar()
//    {
//        try
//        {
//            calService = getCalendarService();
//        } catch (IOException e)
//        {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Creates an authorized Credential object.
//     *
//     * @return an authorized Credential object.
//     * @throws IOException
//     */
//    public static Credential authorize() throws IOException
//    {
//        // Load client secrets.
//        //InputStream in = GCalendar.class.getResourceAsStream("client_secret.json");
//
//        InputStream in = new FileInputStream("resources/client_secret.json");
//
//        GoogleClientSecrets clientSecrets =
//                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
//
//        // Build flow and trigger user authorization request.
//        GoogleAuthorizationCodeFlow flow =
//                new GoogleAuthorizationCodeFlow.Builder(
//                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
//                        .setDataStoreFactory(DATA_STORE_FACTORY)
//                        .setAccessType("offline")
//                        .build();
//        Credential credential = new AuthorizationCodeInstalledApp(
//                flow, new LocalServerReceiver()).authorize("user");
//        System.out.println(
//                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
//        return credential;
//    }
//
//    /**
//     * Build and return an authorized Calendar client service.
//     *
//     * @return an authorized Calendar client service
//     * @throws IOException
//     */
//    public static com.google.api.services.calendar.Calendar
//    getCalendarService() throws IOException
//    {
//        Credential credential = authorize();
//        return new com.google.api.services.calendar.Calendar.Builder(
//                HTTP_TRANSPORT, JSON_FACTORY, credential)
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//    }
//
//    public boolean amIFree(Date start, int durationInMinutes)
//    {
//        try
//        {
//
//            long span = durationInMinutes*60*1000; //30 minutes
//            Date endTime = new Date(start.getTime() +  span);
//
//            Events events = calService.events().list("primary")
//                    .setMaxResults(10)
//                    .setTimeMin(new DateTime(start))
//                    .setTimeMax(new DateTime(endTime))
//                    .setOrderBy("startTime")
//                    .setSingleEvents(true)
//                    .execute();
//
//            return (events.getItems().size() == 0);
//
//        } catch (Exception ex)
//        {
//            ex.printStackTrace();
//        }
//        return false;
//    }
//
//    public Optional<Date> nextFreeTime(Date start, int durationInMinutes)
//    {
//        try
//        {
//
//            long span = durationInMinutes*60*1000; //30 minutes
//            Date endTime = new Date(start.getTime() +  span);
//
//            Events events = calService.events().list("primary")
//                    .setMaxResults(10)
//                    .setTimeMin(new DateTime(start))
//                    .setOrderBy("startTime")
//                    .setSingleEvents(true)
//                    .execute();
//
//            List<Event> items = events.getItems();
//            DateTime prevEventEnd = new DateTime(start);
//            for (Event event : items)
//            {
//                DateTime eventStart = event.getStart().getDateTime();
//                if (eventStart.getValue() >= prevEventEnd.getValue() + span)
//                    return Optional.of(new Date(prevEventEnd.getValue()));
//                prevEventEnd = event.getEnd().getDateTime();
//            }
//
//        } catch (Exception ex)
//        {
//            ex.printStackTrace();
//        }
//        return Optional.empty();
//    }
//
//
//    @Override
//    public void SaveEvent(EventFields eventFields)//String title, String description, Date time, double durationInMinutes, List<String> participants)
//    {
//        try
//        {
//            Event eventToAdd = new Event();
//            if (eventFields.title != null)
//                eventToAdd.setSummary(eventFields.title);
//            if (eventFields.description != null)
//                eventToAdd.setDescription(eventFields.description);
//            EventDateTime startTime = new EventDateTime().setDateTime(new DateTime(eventFields.time.getTime()));
//            EventDateTime endTime = new EventDateTime().setDateTime(new DateTime(eventFields.time.getTime() + (long)(eventFields.durationInMinutes*60*1000)));
//            eventToAdd.setStart(startTime);
//            eventToAdd.setEnd(endTime);
//
//            calService.events().insert("primary",eventToAdd).execute();
//        }
//        catch (Exception ex)
//        {
//            ex.printStackTrace();
//        }
//    }
//
//    @Override
//    public Optional<EventFields> getNextPrevCalendarEvent(boolean next, Date startingFrom, Double currEventDuration)
//    {
//        DateTime startingAsDateTime = new DateTime(new Date(startingFrom.getTime() + (next ? (int)(currEventDuration*60*1000) : 0)));//, TimeZone.getDefault());
//        Events events = null;
//        if (next)
//        {
//            try
//            {
//                events = calService.events().list("primary")
//                        .setMaxResults(1)
//                        .setTimeMin(startingAsDateTime)
//                        .setOrderBy("startTime")
//                        .setSingleEvents(true)
//                        .execute();
//            } catch (IOException e)
//            {
//                return Optional.empty();
//            }
//            List<Event> items = events.getItems();
//            if (items.size() == 0)
//            {
//                return Optional.empty();
//            }
//            return Optional.of(getEventFieldsFromGoogleEvent(items.get(0)));
//        }
//        else
//        {
//            int daysBack = 1;
//            while (daysBack <= 100)
//            {
//                try
//                {
//                    //can't sort descending, so checking latest from 1, 10 and 100 days back.
//                    events = calService.events().list("primary")
//                            //.setMaxResults(1)
//                            .setTimeMax(startingAsDateTime)
//                            .setTimeMin(new DateTime(startingFrom.getTime() - daysBack * 24 * 60 * 60 * 1000))
//                            .setOrderBy("startTime")
//                            //.setOrderBy("d")
//                            .setSingleEvents(true)
//                            .execute();
//                } catch (IOException e)
//                {
//                    return Optional.empty();
//                }
//                if (events != null && events.getItems().size() > 0)
//                {
//                    break;
//                }
//                daysBack *= 10;
//            }
//            if (events == null)
//                return Optional.empty();
//
//            List<Event> items = events.getItems();
//            if (items.size() == 0)
//            {
//                return Optional.empty();
//            }
//            return Optional.of(getEventFieldsFromGoogleEvent(items.get(items.size()-1)));
//        }
//    }
//
//    EventFields getEventFieldsFromGoogleEvent(Event event)
//    {
//        Date eventTime = new Date(event.getStart().getDateTime().getValue());
//        String eventId = "event";//event.getId()
//        if (event.getSummary() != null && event.getSummary().length() > 4)
//        {
//            eventId = event.getSummary().split(" ")[0];
//            if (eventId.length() > 10)
//                eventId = eventId.substring(0,10);
//        }
//        eventId = eventId + ((eventTime.getTime()/1000/10) % 10000000); //adding eventTime so will work also with recurrent
//        return new EventFields(
//                eventId,
//                event.getSummary(),
//                event.getDescription(),
//                eventTime,
//                (event.getEnd().getDateTime().getValue() - event.getStart().getDateTime().getValue())/(1000*60),
//                new LinkedList<String>()
//        );
//    }
//
//    public void printUpcomingEvents() throws IOException
//    {
//
//        // List the next 10 events from the primary calendar.
//        DateTime now = new DateTime(System.currentTimeMillis());
//        Events events = calService.events().list("primary")
//                .setMaxResults(10)
//                .setTimeMin(now)
//                .setOrderBy("startTime")
//                .setSingleEvents(true)
//                .execute();
//        List<Event> items = events.getItems();
//        if (items.size() == 0)
//        {
//            System.out.println("No upcoming events found.");
//        }
//        else
//        {
//            System.out.println("Upcoming events");
//            for (Event event : items)
//            {
//                DateTime start = event.getStart().getDateTime();
//                if (start == null)
//                {
//                    start = event.getStart().getDateTime();
//                }
//                System.out.printf("%s (%s)\n", event.getSummary(), start);
//            }
//        }
//    }
//
//}
//
