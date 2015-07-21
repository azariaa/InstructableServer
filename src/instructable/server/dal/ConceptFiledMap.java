package instructable.server.dal;

import instructable.server.hirarchy.FieldDescription;

import java.util.*;

/**
 * Created by Amos Azaria on 21-Jul-15.
 *  Has a map holding all concepts and the list of their fields
 *  Gets all information from DB upon construction (can be changed to work directly with DB, so it can become stateless, if has too many users, or runs on multiple servers)
 *  Does not perform any checking, operates on objects and DB as requested.
 */
public class ConceptFiledMap
{
    Map<String, List<FieldDescription>> conceptFieldMap;
    String userId;

    public ConceptFiledMap(String userId)
    {
        this.userId = userId;
        fillMap();
    }

    private void fillMap()
    {
        conceptFieldMap = new HashMap<>();
        //TODO: connect to DB and fill map!
    }


    public boolean hasConcept(String concept)
    {
        return conceptFieldMap.containsKey(concept);
    }

    public List<FieldDescription> getAllFieldDescriptions(String concept)
    {
        return conceptFieldMap.get(concept);
    }

    public Set<String> allConcepts()
    {
        return conceptFieldMap.keySet();
    }

    public void newConcept(String conceptName)
    {
        //TODO: add to db!!!
        conceptFieldMap.put(conceptName, new LinkedList<>());
    }

    public void removeConcept(String conceptName)
    {
        //TODO: remove from db!!!
        conceptFieldMap.remove(conceptName);
    }
}
