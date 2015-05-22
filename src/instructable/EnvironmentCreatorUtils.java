package instructable;

import instructable.server.IIncomingEmailControlling;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;
import instructable.server.ccg.StringFeatureGenerator;
import instructable.server.hirarchy.IncomingEmail;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.io.Files;
import com.sun.tools.javac.util.Paths;

/**
 * Created by Amos Azaria on 20-May-15.
 */
public class EnvironmentCreatorUtils
{

    public static ParserSettings createParser()
    {
//        String[] lexiconEntries1 = new String[] {"\"send\",\"S{0}\",\"(sendEmail)\",\"0 sendEmail\"",
//                "\"set\",\"((S{0}/PP_String{2}){0}/Field{1}){0}\",\"(lambda $1 $2 (setFieldFromString $1 $2))\",\"0 setFieldFromString\",\"setFieldFromString 1 1\",\"setFieldFromString 2 2\"",
//                "\"set\",\"((S{0}/PP_Field{2}){0}/String{1}){0}\",\"(lambda $1 $2 (setFieldFromString $2 $1))\",\"0 setFieldFromString\",\"setFieldFromString 1 1\",\"setFieldFromString 2 2\"",
//                "\"of\",\"((Field{0}\\FieldName{2}){0}/InstanceName{1}){0}\",\"(lambda $1 $2 (getProbFieldByInstanceNameAndFieldName $1 $2))\",\"0 getProbFieldByInstanceNameAndFieldName\",\"getProbFieldByInstanceNameAndFieldName 1 1\",\"getProbFieldByInstanceNameAndFieldName 2 2\"",
//                "\"to\",\"(PP_String{0}/String{1}){0}\",\"(lambda $1 $1)\", \"0 to\", \"to 1 1\"",
//                "as,PP_String/String,(lambda $1 $1)",
//                "as,PP_Field/Field,(lambda $1 $1)",
//                "body,FieldName{0},body,0 body",
//                "email,InstanceName{0},outgoing_email,0 outgoing_email",
//                "recipient,FieldName{0},recipient_list,0 recipient_list",
//                "subject,FieldName{0},subject,0 subject",
//                "bob,InstanceName{0},bob,0 bob"
//        };

        List<String> lexiconEntries = null;
        try
        {
            lexiconEntries = Files.readAllLines(Paths.get("data/lexiconEntries.txt"));
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        String[] unaryRules = new String[]{
                "Field{0} FieldVal{0},(lambda x (evalField x))",
                //"FieldVal{0} S{0},(lambda x x)", //TODO: bring this back once it works
                "MutableField{0} FieldVal{0},(lambda x (evalField x))",
                "FieldName{0} Field{0},(lambda x (getProbFieldByFieldName x))",
                "InstanceName{0} Instance{0},(lambda x (getProbInstanceByName x))",
                "FieldName{0} MutableField{0},(lambda x (getProbMutableFieldByFieldName x))",
                "ConceptName{0} Instance{0}/InstanceName{0}, (lambda x (lambda y (getInstance x y)))",
                "String{0} S{0},(lambda x (unknownCommand))" //TODO: remove this, once it's added in: buildParametricCcgParser (so only full sentence will be transferred to S)
        };


//        String[][] examplesArr = new String[][] {{"send email", "(sendEmail)"},
//                {"set the body of bob to bar world", "(setFieldFromString (getProbFieldByInstanceNameAndFieldName outgoing_email body) \"bar world\")"},
//                {"set the recipient list of the email to myself@myjob.com", "(setFieldFromString (getProbFieldByInstanceNameAndFieldName outgoing_email recipient_list) \"myself@myjob.com\")"},
//                {"set the subject of this email to hello world and you too", "(setFieldFromString (getProbFieldByInstanceNameAndFieldName outgoing_email subject) \"hello world and you too\")"},
//                {"set the subject of this email to what", "(setFieldFromString (getProbFieldByInstanceNameAndFieldName outgoing_email subject) \"what\")"},
//                {"set the subject of this email as when", "(setFieldFromString (getProbFieldByInstanceNameAndFieldName outgoing_email subject) \"when\")"},
//                {"add email to contact", "(addFieldToConcept contact \"email\")"},
//                {"set bob's email to baba", "(setFieldFromString (getProbFieldByInstanceNameAndFieldName bob email) \"baba\")"},
//                {"set jane's email to baba", "(setFieldFromString (getProbFieldByInstanceNameAndFieldName jane email) \"baba\")"},
//                {"and set it as jane's email","(setFieldFromPreviousEval (getProbFieldByInstanceNameAndFieldName jane email))"}
//        };

        return CcgUtils.getParserSettings(lexiconEntries, unaryRules, new StringFeatureGenerator(),
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
