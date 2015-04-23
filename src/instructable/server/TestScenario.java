package instructable.server;

import com.sun.deploy.util.StringUtils;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class TestScenario
{
    static boolean testingMode = true;

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

        TestHelpers testHelpers = new TestHelpers(testingMode, "Apr23test.txt");
        sendingBasicEmail(allUserActions, testHelpers);

        definingContact(allUserActions, testHelpers);

        testHelpers.endTest();
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
            allSystemReplies.append(str + "\n");
        }

        public void endTest() throws Exception
        {
            if (testingMode)
            {
                String shouldBe = StringUtils.join(Files.readAllLines(Paths.get(fileName)),"\n");
                if (!shouldBe.equals(allSystemReplies.toString()))
                {
                    System.out.println("Error!!!!");
                    //can add StringUtils.difference of Google diff, but requires an extra jar.
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
