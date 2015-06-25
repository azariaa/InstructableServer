package instructable;

import instructable.server.IIncomingEmailControlling;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;
import instructable.server.ccg.StringFeatureGenerator;
import instructable.server.hirarchy.IncomingEmail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by Amos Azaria on 20-May-15.
 */
public class EnvironmentCreatorUtils
{

    public static ParserSettings createParser()
    {

        List<String> lexiconEntries = null;
        List<String> synonyms = null;
        try
        {
            lexiconEntries = Files.readAllLines(Paths.get("data/lexiconEntries.txt"));
            synonyms = Files.readAllLines(Paths.get("data/lexiconSyn.txt"));
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        String[] unaryRules = new String[]{
                "Field{0} FieldVal{0},(lambda x (evalField x))",
                "FieldVal{0} S{0},(lambda x x)",
                "FieldName{0} Field{0},(lambda x (getProbFieldByFieldName x))",
                "FieldName{0} FieldVal{0},(lambda x (evalField (getProbFieldByFieldName x)))", //this one just combines the two above (and below).
                //"MutableField{0} FieldVal{0},(lambda x (evalField x))", //no need to evaluate a mutable field, if needs mutable, why will it try to evaluate?
                "FieldName{0} MutableField{0},(lambda x (getProbMutableFieldByFieldName x))",
                "Field{0} S{0},(lambda x (evalField x))",
                "MutableField{0} S{0},(lambda x (evalField x))",
                "InstanceName{0} Instance{0},(lambda x (getProbInstanceByName x))",
                "ConceptName{0} Instance{0}/InstanceName{0}, (lambda x (lambda y (getInstance x y)))",
                "InstanceName{0} MutableField{0}/FieldName{0}, (lambda x y (getProbMutableFieldByInstanceNameAndFieldName x y))",
                "InstanceName{0} Field{0}/FieldName{0}, (lambda x (lambda y (getProbFieldByInstanceNameAndFieldName x y)))"
        };


        return new ParserSettings(lexiconEntries, synonyms, unaryRules, new StringFeatureGenerator(),
            CcgUtils.loadExamples(Paths.get("data/examples.csv")));
    }



    public static void addInboxEmails(IIncomingEmailControlling incomingEmailControlling)
    {
        incomingEmailControlling.addEmailMessageToInbox(new IncomingEmail("bob7@myjob.com",
                "department party",
                Arrays.asList(new String[]{"you@myjob.com"}),
                new LinkedList<String>(),
                "We will have our department party next Wednesday at 4:00pm. Please forward this email to your spouse."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new IncomingEmail("dan@myjob.com",
                "another email",
                Arrays.asList(new String[]{"you@myjob.com"}),
                new LinkedList<String>(),
                "sending another email."
        ));
    }


    private EnvironmentCreatorUtils()
    {
        //static class
    }
}
