package testing;

import com.sun.deploy.util.StringUtils;
import instructable.server.ActionResponse;
import instructable.server.IAllUserActions;
import instructable.server.ICommandsToParser;
import instructable.server.TopDMAllActions;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class TestScenario
{
    static boolean testingMode = true;
    static String fileName = "Apr27test.txt";

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

        testHelpers.endTest();

        //teachingToSetRecipientAsContact(allUserActions, testHelpers);

        //learningToForwardAnEmail(allUserActions, testHelpers);
        //emailSomeoneSomeText()

    }

    private static void setFromGet(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        ActionResponse response;

        response = allUserActions.get("what is bob's email?", "bob", "email");
        testHelpers.systemSays(response.sayToUser);
        //testHelpers.systemSays(response.value.get().toJSONString());

        response = allUserActions.createInstance("create a contact jane", "contact", "jane");
        testHelpers.systemSays(response.sayToUser);

        //"take bob's email and set it as jane's email"
        //(set jane email (get bob email))
        response = allUserActions.get("take bob's email", "bob", "email");
        if (response.value.isPresent())
        {
            JSONObject bobsEmail = response.value.get();
            response = allUserActions.set("and set it as jane's email", "jane", "email", bobsEmail);
            testHelpers.systemSays(response.sayToUser);

            response = allUserActions.set("set the recipient to be bob's email", "recipient list", bobsEmail);
        }
        testHelpers.systemSays(response.sayToUser);

        //set from a get

        //get in context

    }

    private static void teachingToSetRecipientAsContact(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        ActionResponse response;

        response = allUserActions.composeEmail("compose an email");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("make bob the recipient", "recipient list", "bob");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.yes("yes");
        testHelpers.systemSays(response.sayToUser);

        response = allUserActions.set("set the recipient to be bob's email", "recipient list", "bob");
        testHelpers.systemSays(response.sayToUser);

    }

    private static void learningToForwardAnEmail(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
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
                    System.out.println("Error!!!!");
                    throw new Exception("Test failed");
                }
                else
                {
                    System.out.println("Success!!!!");
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
