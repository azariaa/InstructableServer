package testing;

import com.sun.deploy.util.StringUtils;
import instructable.server.*;
import instructable.server.hirarchy.EmailMessage;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class TestScenario
{
    static boolean testingMode = true;
    static String fileName = "Apr28test.txt";

    public static void main(String[] args) throws Exception
    {
        IAllUserActions allUserActions = new TopDMAllActions(new ICommandsToParser()
        {
            @Override
            public void addTrainingEg(String originalCommand, List<String> replaceWith)
            {

            }

            @Override
            public void newConceptDefined(String conceptName)
            {

            }

            @Override
            public void newFieldDefined(String fieldName)
            {

            }

            @Override
            public void newInstanceDefined(String instanceName)
            {

            }
        });

        TestHelpers testHelpers = new TestHelpers(testingMode, fileName);
        sendingBasicEmail(allUserActions, testHelpers);
        definingContact(allUserActions, testHelpers);

        setFromGet(allUserActions, testHelpers);

        teachingToSetRecipientAsContact(allUserActions, testHelpers);

        buildRequiredDB((TopDMAllActions)allUserActions, testHelpers);

        learningToForwardAnEmail(allUserActions, testHelpers);

        testHelpers.endTest();
        //emailSomeoneSomeText()

    }

    private static void buildRequiredDB(TopDMAllActions topDMAllActions, TestHelpers testHelpers)
    {
        testHelpers.systemSays("now in buildRequiredDB");

        ActionResponse response;
        IAllUserActions allUserActions = topDMAllActions;
        IIncomingEmailControlling incomingEmailControlling = topDMAllActions;

        response = allUserActions.createInstance("create a contact my spouse", "contact", "my spouse");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("set its email to my.spouse@gmail.com", "email", "my.spouse@gmail.com");
        testHelpers.systemSays(response.sayToUser);

        incomingEmailControlling.addEmailMessageToInbox(new EmailMessage("bob7@myjob.com",
                "department party",
                Arrays.asList(new String[] {"you@myjob.com"}),
                new LinkedList<String>(),
                "We will have our department party next Wednesday at 4:00pm. Please forward this email to your spouse."
        ));

        incomingEmailControlling.addEmailMessageToInbox(new EmailMessage("dan@myjob.com",
                "another email",
                Arrays.asList(new String[] {"you@myjob.com"}),
                new LinkedList<String>(),
                "sending another email."
        ));
    }

    private static void setFromGet(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.systemSays("now in setFromGet");

        ActionResponse response;

        response = allUserActions.get("what is bob's email?", "bob", "email");
        testHelpers.systemSays(response.sayToUser);
        //testHelpers.systemSays(response.value.get().toJSONString());

        response = allUserActions.createInstance("create a contact jane", "contact", "jane");
        testHelpers.systemSays(response.sayToUser);


        response = allUserActions.get("take bob's email", "bob", "email");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.setFromPreviousGet("and set it as jane's email", "jane", "email");
        testHelpers.systemSays(response.sayToUser);


        //"take bob's email and set it as jane's email"
        //parser should translate to:
        //(set jane email (get bob email))
        response = allUserActions.get("take bob's email", "bob", "email");
        if (response.value.isPresent())
        {
            response = allUserActions.set("and set it as jane's email", "jane", "email", response.value.get());
        }
        testHelpers.systemSays(response.sayToUser);

        //"set the recipient to be bob's email"
        //parser should translate to:
        //(set recipient_list (get bob email))
        response = allUserActions.get("take bob's email", "bob", "email");
        if (response.value.isPresent())
        {
            response = allUserActions.set("set the recipient to be bob's email", "recipient list", response.value.get());
        }

        testHelpers.systemSays(response.sayToUser);
    }

    private static void teachingToSetRecipientAsContact(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.systemSays("now in teachingToSetRecipientAsContact");

        ActionResponse response;

        response = allUserActions.composeEmail("compose an email");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("set jane's email to be jane@gmail.com", "jane", "email", "jane@gmail.com");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("make bob the recipient", "recipient list", "bob");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.yes("yes");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.get("take bob's email", "bob", "email");
        testHelpers.systemSays(response.sayToUser);
        response = allUserActions.setFromPreviousGet("and set it as the recipient", "recipient list");
        testHelpers.systemSays(response.sayToUser);
        response = allUserActions.endTeaching("that's it");
        testHelpers.systemSays(response.sayToUser);

        //"make jane the recipient" should now translate to:
        {
            response = allUserActions.get("take jane's email", "jane", "email");
            if (response.success)
                response = allUserActions.setFromPreviousGet("and set it as the recipient", "recipient list");
            testHelpers.systemSays(response.sayToUser);
        }

    }

    private static void learningToForwardAnEmail(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.systemSays("now in learningToForwardAnEmail");

        ActionResponse response;

        response = allUserActions.unknownCommand("forward this email to my spouse");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.yes("sure");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.composeEmail("first create a new email");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.unknownCommand("then copy the subject from the incoming email to the outgoing email");
        testHelpers.systemSays(response.sayToUser);


        response = allUserActions.get("take the subject from the incoming email", "inbox", "subject");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.setFromPreviousGet("and set it as the outgoing email's subject", "outgoing email", "subject");
        testHelpers.systemSays(response.sayToUser);


        response = allUserActions.get("take the body from the incoming email", "inbox", "body");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.setFromPreviousGet("and set it as the body", "outgoing email", "body"); //TODO: should understand since incoming email should not be mutable, or maybe leave for parser
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.sendEmail("send the email");
        testHelpers.systemSays(response.sayToUser);

        //"oh, yeah, set the recipient as my spouse"
        // should translate to:
        {
            response = allUserActions.get("take my spouse's email", "my spouse", "email");
            if (response.success)
                response = allUserActions.setFromPreviousGet("and set it as the recipient", "recipient list");
            testHelpers.systemSays(response.sayToUser);
        }

        response = allUserActions.sendEmail("now send the email");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.endTeaching("that's it, you're done!");
        testHelpers.systemSays(response.sayToUser);

    }

    private static void sendingBasicEmail(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.systemSays("Let's start by sending a dummy email to your-self, set the subject to hello and the body to test.");
        ActionResponse response;

        response = allUserActions.sendEmail("send an email");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.yes("yes");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("set the subject of this email to hello", "outgoing email", "subject", "hello");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("put test in body", "body", "test");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.sendEmail("send the email");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("set myself as the recipient", "recipient list", "myself");
        //how should we know that recipient is recipient list? Leave it for the parser?
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("set myself@myjob.com as the recipient", "recipient list", "myself@myjob.com");
        //should be able to learn something from this!!!
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.sendEmail("send");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.sendEmail("send");
        testHelpers.systemSays(response.sayToUser);
    }


    private static void definingContact(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.systemSays("now in definingContact");

        ActionResponse response;

        response = allUserActions.composeEmail("compose a new email");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("set my spouse as the recipient", "recipient list", "my spouse");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.defineConcept("I want to teach you what a contact is", "contact");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.defineConcept("Define contact!", "contact");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.addFieldToConcept("add email as a field in contact", "contact", "email");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.createInstance("create a contact, call it bob", "contact", "bob");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("set bob's email to baba", "bob", "email", "baba");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("set bob's email to bob@gmail.com", "bob", "email", "bob@gmail.com");
        testHelpers.systemSays(response.sayToUser);

    }

    static class TestHelpers
    {
        boolean testingMode;
        StringBuilder allSystemReplies;
        String fileName;
        public TestHelpers(boolean testingMode, String fileName)
        {
            this.testingMode = testingMode;
            allSystemReplies = new StringBuilder();
            this.fileName = fileName;
        }

        public void systemSays(String str)
        {
            String[] sentences = str.split("\n");
            for (String sentence : sentences)
            {
                System.out.println("S: " + sentence);
            }
            System.out.println();
            allSystemReplies.append(str + "\n\n");
        }

        public void endTest() throws Exception
        {
            if (testingMode)
            {
                String shouldBe = StringUtils.join(Files.readAllLines(Paths.get(fileName)),"\n");
                if (!shouldBe.equals(allSystemReplies.toString()))
                {
                    String failFileName = new SimpleDateFormat("yyyyMMddhhmm'fail.txt'").format(new Date());
                    PrintWriter out = new PrintWriter(failFileName);
                    out.println(allSystemReplies.toString());
                    out.close();
                    System.out.println("Error!!!!\nTest failed!!!\n");
                    throw new Exception("Test failed");
                }
                else
                {
                    System.out.println("Success!!!!\n");
                }
            }
            else
            {
                PrintWriter out = new PrintWriter(fileName);
                out.println(allSystemReplies.toString());
                out.close();
            }
        }
    }
}
