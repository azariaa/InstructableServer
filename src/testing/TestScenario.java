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
        testHelpers.newSection("buildRequiredDB");

        ActionResponse response;
        String userSays;
        IAllUserActions allUserActions = topDMAllActions;
        IIncomingEmailControlling incomingEmailControlling = topDMAllActions;

        userSays = "create a contact my spouse";
        testHelpers.userSays(userSays);
        response = allUserActions.createInstance(userSays, "contact", "my spouse");
        testHelpers.systemSays(response.sayToUser);

        userSays = "set its email to my.spouse@gmail.com";
        testHelpers.userSays(userSays);
        response = allUserActions.set(userSays, "email", "my.spouse@gmail.com");
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
        testHelpers.newSection("setFromGet");

        ActionResponse response;
        String userSays;

        userSays = "what is bob's email?";
        testHelpers.userSays(userSays);
        response = allUserActions.get(userSays, "bob", "email");
        testHelpers.systemSays(response.sayToUser);
        //testHelpers.systemSays(response.value.get().toJSONString());

        userSays = "create a contact jane";
        testHelpers.userSays(userSays);
        response = allUserActions.createInstance(userSays, "contact", "jane");
        testHelpers.systemSays(response.sayToUser);


        userSays = "take bob's email";
        testHelpers.userSays(userSays);
        response = allUserActions.get(userSays, "bob", "email");
        testHelpers.systemSays(response.sayToUser);

        userSays = "and set it as jane's email";
        testHelpers.userSays(userSays);
        response = allUserActions.setFromPreviousGet(userSays, "jane", "email");
        testHelpers.systemSays(response.sayToUser);


        testHelpers.userSays("take bob's email and set it as jane's email");
        //parser should translate to:
        //(set jane email (get bob email))
        response = allUserActions.get("take bob's email", "bob", "email");
        if (response.value.isPresent())
        {
            response = allUserActions.set("and set it as jane's email", "jane", "email", response.value.get());
        }
        testHelpers.systemSays(response.sayToUser);

        testHelpers.userSays("set the recipient to be bob's email");
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
        testHelpers.newSection("teachingToSetRecipientAsContact");

        ActionResponse response;
        String userSays;

        userSays = "compose an email";
        testHelpers.userSays(userSays);
        response = allUserActions.composeEmail(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "set jane's email to be jane@gmail.com";
        testHelpers.userSays(userSays);
        response = allUserActions.set(userSays, "jane", "email", "jane@gmail.com");
        testHelpers.systemSays(response.sayToUser);

        userSays = "make bob the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.set(userSays, "recipient list", "bob");
        testHelpers.systemSays(response.sayToUser);

        userSays = "yes";
        testHelpers.userSays(userSays);
        response = allUserActions.yes(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "take bob's email";
        testHelpers.userSays(userSays);
        response = allUserActions.get(userSays, "bob", "email");
        testHelpers.systemSays(response.sayToUser);

        userSays = "and set it as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.setFromPreviousGet(userSays, "recipient list");
        testHelpers.systemSays(response.sayToUser);

        userSays = "that's it";
        testHelpers.userSays(userSays);
        response = allUserActions.endTeaching(userSays);
        testHelpers.systemSays(response.sayToUser);

        testHelpers.userSays("make jane the recipient");
        //should now translate to:
        {
            response = allUserActions.get("take jane's email", "jane", "email");
            if (response.success)
                response = allUserActions.setFromPreviousGet("and set it as the recipient", "recipient list");
            testHelpers.systemSays(response.sayToUser);
        }

    }

    private static void learningToForwardAnEmail(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.newSection("learningToForwardAnEmail");

        ActionResponse response;
        String userSays;

        userSays = "forward this email to my spouse";
        testHelpers.userSays(userSays);
        response = allUserActions.unknownCommand(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "sure";
        testHelpers.userSays(userSays);
        response = allUserActions.yes(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "first create a new email";
        testHelpers.userSays(userSays);
        response = allUserActions.composeEmail(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "then copy the subject from the incoming email to the outgoing email";
        testHelpers.userSays(userSays);
        response = allUserActions.unknownCommand(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "take the subject from the incoming email";
        testHelpers.userSays(userSays);
        response = allUserActions.get(userSays, "inbox", "subject");
        testHelpers.systemSays(response.sayToUser);

        userSays = "and set it as the outgoing email's subject";
        testHelpers.userSays(userSays);
        response = allUserActions.setFromPreviousGet(userSays, "outgoing email", "subject");
        testHelpers.systemSays(response.sayToUser);

        userSays = "take the body from the incoming email";
        testHelpers.userSays(userSays);
        response = allUserActions.get(userSays, "inbox", "body");
        testHelpers.systemSays(response.sayToUser);

        userSays = "and set it as the body";
        testHelpers.userSays(userSays);
        response = allUserActions.setFromPreviousGet(userSays, "outgoing email", "body"); //TODO: should understand since incoming email should not be mutable, or maybe leave for parser
        testHelpers.systemSays(response.sayToUser);

        userSays = "send the email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(userSays);
        testHelpers.systemSays(response.sayToUser);

        testHelpers.userSays("oh, yeah, set the recipient as my spouse");
        // should translate to:
        {
            response = allUserActions.get("take my spouse's email", "my spouse", "email");
            if (response.success)
                response = allUserActions.setFromPreviousGet("and set it as the recipient", "recipient list");
            testHelpers.systemSays(response.sayToUser);
        }

        userSays = "now send the email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "that's it, you're done!";
        testHelpers.userSays(userSays);
        response = allUserActions.endTeaching(userSays);
        testHelpers.systemSays(response.sayToUser);

    }

    private static void sendingBasicEmail(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.systemSays("Let's start by sending a dummy email to your-self, set the subject to hello and the body to test.");
        ActionResponse response;
        String userSays;

        userSays = "send an email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "yes";
        testHelpers.userSays(userSays);
        response = allUserActions.yes(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "set the subject of this email to hello";
        testHelpers.userSays(userSays);
        response = allUserActions.set(userSays, "outgoing email", "subject", "hello");
        testHelpers.systemSays(response.sayToUser);

        userSays = "put test in body";
        testHelpers.userSays(userSays);
        response = allUserActions.set(userSays, "body", "test");
        testHelpers.systemSays(response.sayToUser);

        userSays = "send the email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "set myself as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.set(userSays, "recipient list", "myself");
        //how should we know that recipient is recipient list? Leave it for the parser?
        testHelpers.systemSays(response.sayToUser);

        userSays = "set myself@myjob.com as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.set(userSays, "recipient list", "myself@myjob.com");
        //should be able to learn something from this!!!
        testHelpers.systemSays(response.sayToUser);

        userSays = "send";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "send";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(userSays);
        testHelpers.systemSays(response.sayToUser);
    }


    private static void definingContact(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.newSection("definingContact");

        ActionResponse response;
        String userSays;

        userSays = "compose a new email";
        testHelpers.userSays(userSays);
        response = allUserActions.composeEmail(userSays);
        testHelpers.systemSays(response.sayToUser);

        userSays = "set my spouse as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.set(userSays, "recipient list", "my spouse");
        testHelpers.systemSays(response.sayToUser);

        userSays = "I want to teach you what a contact is";
        testHelpers.userSays(userSays);
        response = allUserActions.defineConcept(userSays, "contact");
        testHelpers.systemSays(response.sayToUser);

        userSays = "Define contact!";
        testHelpers.userSays(userSays);
        response = allUserActions.defineConcept(userSays, "contact");
        testHelpers.systemSays(response.sayToUser);

        userSays = "add email as a field in contact";
        testHelpers.userSays(userSays);
        response = allUserActions.addFieldToConcept(userSays, "contact", "email");
        testHelpers.systemSays(response.sayToUser);

        userSays = "create a contact, call it bob";
        testHelpers.userSays(userSays);
        response = allUserActions.createInstance(userSays, "contact", "bob");
        testHelpers.systemSays(response.sayToUser);

        userSays = "set bob's email to baba";
        testHelpers.userSays(userSays);
        response = allUserActions.set(userSays, "bob", "email", "baba");
        testHelpers.systemSays(response.sayToUser);

        userSays = "set bob's email to bob@gmail.com";
        testHelpers.userSays(userSays);
        response = allUserActions.set(userSays, "bob", "email", "bob@gmail.com");
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

        public void userSays(String str)
        {
            String[] sentences = str.split("\n");
            for (String sentence : sentences)
            {
                System.out.println("U: " + sentence);
            }
            allSystemReplies.append(str + "\n");
        }

        public void newSection(String sectionName)
        {
            String outputStr = "---------------- now in " + sectionName + " --------------------\n";
            System.out.println(outputStr);
            allSystemReplies.append(outputStr + "\n");
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
