package instructable.server.dal;

import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;
import instructable.server.hirarchy.EmailInfo;
import instructable.server.senseffect.IIncomingEmailControlling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;


/**
 * Created by Amos Azaria on 20-May-15.
 */
public class CreateParserFromFiles
{
  
  public static ParserSettings createParser(Optional<String> userId) {
    return createParser(userId, "data/lexiconEntries.txt", "data/lexiconSyn.txt", "data/examples.csv");
  }

    public static ParserSettings createParser(Optional<String> userId, String lexicon, String lexiconSyn, String trainingExamples)
    {

        List<String> lexiconEntries = null;
        List<String> synonyms = null;
        try
        {
            lexiconEntries = Files.readAllLines(Paths.get(lexicon));
            synonyms = Files.readAllLines(Paths.get(lexiconSyn));
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
                "ConceptName{0} ExactInstance{0}/InstanceName{0}, (lambda x (lambda y (getInstance x y)))",
                "InstanceName{0} MutableField{0}/FieldName{0}, (lambda x y (getProbMutableFieldByInstanceNameAndFieldName x y))",
                "InstanceName{0} Field{0}/FieldName{0}, (lambda x (lambda y (getProbFieldByInstanceNameAndFieldName x y)))",
                "EmailAmbiguous{0} InstanceName{0}, (lambda x x)", //only if the parser doesn't manage to identify which kind of email the user means, will this be used.
                "EmailAmbiguous{0} Instance{0},(lambda x (getProbInstanceByName x))",
                "ExactInstance{0} Instance{0},(lambda x x)" //exact instance is also an instance (that can be read).
                //"PP_StringV{1} (S{0}\\(S{0}/PP_StringV{1}){0}){0}, (lambda x x)", //these two are for handling sentences like: "set body to hello and subject to see you"
                //"MutableField{1} ((S{0}/PP_StringV{2}){0}\\(S{0}/PP_StringV{2}){0}/MutableField{1}){0}){0}, (lambda x y x y)"
        };


        ParserKnowledgeSeeder parserKnowledgeSeeder = new ParserKnowledgeSeeder(userId, lexiconEntries, synonyms, unaryRules, CcgUtils.loadExamples(Paths.get(trainingExamples)));

        return new ParserSettings(parserKnowledgeSeeder);
    }



    public static void addInboxEmails(IIncomingEmailControlling incomingEmailControlling)
    {
        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo("bob7@myjob.com",
                "department party",
                Arrays.asList(new String[]{"you@myjob.com"}),
                new LinkedList<String>(),
                "We will have our department party next Wednesday at 4:00pm. Please forward this email to your spouse."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailInfo("dan@myjob.com",
                "another email",
                Arrays.asList(new String[]{"you@myjob.com"}),
                new LinkedList<String>(),
                "sending another email."
        ));
    }


    private CreateParserFromFiles()
    {
        //static class
    }
}
