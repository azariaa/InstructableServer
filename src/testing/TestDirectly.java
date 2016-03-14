package testing;

import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.backend.*;
import instructable.server.ccg.ParserSettings;
import instructable.server.controllers.IEmailSender;
import instructable.server.hirarchy.EmailInfo;
import instructable.server.parser.ICommandsToParser;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class TestDirectly
{
    static boolean testingMode = true;
    static String fileName = "June18test.txt";

    public static void main(String[] args) throws Exception
    {
        runTest();
    }

    private static void runTest() throws Exception
    {
        IAllUserActions allUserActions = new TopDMAllActions("you@myworkplace.com", "test", new ICommandsToParser()
        {
            @Override
            public void addTrainingEg(String originalCommand, List<Expression2> commandsLearnt, Optional<ActionResponse> replyWhenDone)
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

            @Override
            public void removeField(String fieldName)
            {

            }

            @Override
            public void removeInstance(String instanceName)
            {

            }

            @Override
            public void undefineConcept(String conceptName)
            {

            }

            @Override
            public void failNextCommand()
            {

            }
        },
                new IEmailSender()
                {
                    @Override
                    public void sendEmail(String subject, String body, String copyList, String recipientList)
                    {
                        //do nothing
                    }
                }, false, Optional.empty());

        TestHelpers testHelpers = new TestHelpers(testingMode, fileName);

        ParserSettings parserSettings = null;

        sendingBasicEmail(allUserActions, testHelpers, parserSettings);

        definingContact(allUserActions, testHelpers, parserSettings);

        setFromGet(allUserActions, testHelpers, parserSettings);

        teachingToSetRecipientAsContact(allUserActions, testHelpers, parserSettings);

        buildRequiredDB((TopDMAllActions) allUserActions, testHelpers, parserSettings);

        learningToForwardAnEmail(allUserActions, testHelpers, parserSettings);

        smallUpdates(allUserActions,testHelpers,parserSettings);

        testHelpers.endTest();
        //emailSomeoneSomeText()
    }


    private static void sendingBasicEmail(IAllUserActions allUserActions, TestHelpers testHelpers, ParserSettings parserSettings)
    {
        testHelpers.systemSays("Let's start by sending a dummy email to your-self, set the subject to hello and the body to test.");
        ActionResponse response;
        String userSays;

        userSays = "send an email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "yes";
        testHelpers.userSays(userSays);
        response = allUserActions.yes(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set the subject of this email to hello";
        // (setFieldFromString (getProbFieldByInstanceNameAndFieldName outgoing_email subject) "hello")
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "outgoing email", "subject");
        if (response.isSuccess())
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "hello");
        testHelpers.systemSays(response.getSayToUser());


        //System.exit(0);

        userSays = "put test in body";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbMutableFieldByFieldName(new InfoForCommand(userSays, null), "body");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "test");
        }
        testHelpers.systemSays(response.getSayToUser());

        userSays = "send the email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set myself as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbMutableFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "myself");
        }
        //how should we know that recipient is recipient list? Leave it for the parser?
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set myself@myjob.com as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbMutableFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "myself@myjob.com");
        }
        //could learn something from this?!
        testHelpers.systemSays(response.getSayToUser());

        userSays = "send";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "send";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());
    }

    private static void definingContact(IAllUserActions allUserActions, TestHelpers testHelpers, ParserSettings parserSettings)
    {
        testHelpers.newSection("definingContact");

        ActionResponse response;
        String userSays;

        userSays = "compose a new email";
        testHelpers.userSays(userSays);
        response = allUserActions.createInstanceByConceptName(new InfoForCommand(userSays, null), "outgoing email");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set my spouse as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbMutableFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "my spouse");
        }
        testHelpers.systemSays(response.getSayToUser());

        //TODO: didn't do this one
        userSays = "I want to teach you what a contact is";
        testHelpers.userSays(userSays);
        response = allUserActions.defineConcept(new InfoForCommand(userSays, null), "contact");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "Define contact!";
        testHelpers.userSays(userSays);
        response = allUserActions.defineConcept(new InfoForCommand(userSays, null), "contact");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "add email as a field in contact";
        testHelpers.userSays(userSays);
        //TODO: left this for the next
        response = allUserActions.addFieldToConcept(new InfoForCommand(userSays, null), "contact", "email");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "create a contact, call it bob";
        testHelpers.userSays(userSays);
        //TODO: left this for the next
        response = allUserActions.createInstanceByFullNames(new InfoForCommand(userSays, null), "contact", "bob");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set bob's email to baba";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "bob", "email");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "baba");
        }
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set bob's email to bob@gmail.com";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "bob", "email");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "bob@gmail.com");
        }
        testHelpers.systemSays(response.getSayToUser());

    }


    private static void setFromGet(IAllUserActions allUserActions, TestHelpers testHelpers, ParserSettings parserSettings)
    {
        testHelpers.newSection("setFromGet");

        ActionResponse response;
        String userSays;

        userSays = "what is bob's email?";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "bob", "email");
        if (response.isSuccess())
        {
            //the parser should no not to return a field to the user but first evaluate it
            response = allUserActions.evalField(new InfoForCommand(userSays, null), response.getField());
        }
        testHelpers.systemSays(response.getSayToUser());
        //testHelpers.systemSays(response.value.get().toJSONString());

        userSays = "create a contact jane";
        testHelpers.userSays(userSays);
        response = allUserActions.createInstanceByFullNames(new InfoForCommand(userSays, null), "contact", "jane");
        testHelpers.systemSays(response.getSayToUser());


        userSays = "take bob's email";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "bob", "email");
        if (response.isSuccess())
        {
            //maybe this should be done automatically every time.
            response = allUserActions.evalField(new InfoForCommand(userSays, null), response.getField());
        }
        testHelpers.systemSays(response.getSayToUser());

        userSays = "and set it as jane's email";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "jane", "email");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromFieldVal(new InfoForCommand(userSays, null), response.getField(), allUserActions.getProbFieldVal(new InfoForCommand(userSays, null)).getValue());
        }
        testHelpers.systemSays(response.getSayToUser());


        userSays = "take bob's email and set it as jane's email";
        testHelpers.userSays(userSays);
        //parser should translate to:
        //(set (get jane email) (eval (get bob email)))
        ActionResponse janeEmailField = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "jane", "email");
        if (janeEmailField.isSuccess())
        {
            ActionResponse bobEmailField = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "bob", "email");

            if (bobEmailField.isSuccess())
            {
                ActionResponse bobEmailFieldVal = allUserActions.evalField(new InfoForCommand(userSays, null), bobEmailField.getField());

                if (bobEmailFieldVal.isSuccess())
                {
                    response = allUserActions.setFieldFromFieldVal(new InfoForCommand(userSays, null), janeEmailField.getField(), bobEmailFieldVal.getValue());
                }
            }
        }
        //TODO: think what to do with set with bob's email behind'
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set the recipient to be bob's email";
        testHelpers.userSays(userSays);
        //parser should translate to:
        //(set (get recipient_list) (eval (get bob email)))
        ActionResponse recipientField = allUserActions.getProbMutableFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (recipientField.isSuccess())
        {
            ActionResponse bobEmailField = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "bob", "email");

            if (bobEmailField.isSuccess())
            {
                ActionResponse bobEmailFieldVal = allUserActions.evalField(new InfoForCommand(userSays, null), bobEmailField.getField());

                if (bobEmailFieldVal.isSuccess())
                {
                    response = allUserActions.setFieldFromFieldVal(new InfoForCommand(userSays, null), recipientField.getField(), bobEmailFieldVal.getValue());
                }
            }
        }
        testHelpers.systemSays(response.getSayToUser());


        //simple add test
        userSays = "add nana@gmail.com to the recipient list";
        testHelpers.userSays(userSays);
        //parser should translate to:
        //(add (get recipient_list) "nana@gmail.com")
        recipientField = allUserActions.getProbMutableFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (recipientField.isSuccess())
        {
            response = allUserActions.addToFieldFromString(new InfoForCommand(userSays, null), recipientField.getField(), "nana@gmail.com");
        }

        testHelpers.systemSays(response.getSayToUser());
    }


    private static void teachingToSetRecipientAsContact(IAllUserActions allUserActions, TestHelpers testHelpers, ParserSettings parserSettings)
    {
        testHelpers.newSection("teachingToSetRecipientAsContact");

        ActionResponse response;
        String userSays;

        userSays = "compose an email";
        testHelpers.userSays(userSays);
        response = allUserActions.createInstanceByConceptName(new InfoForCommand(userSays, null), "outgoing email");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set jane's email to be jane@gmail.com";
        //(set (get jane email) jane@gmail.com)
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "jane", "email");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "jane@gmail.com");
        }
        testHelpers.systemSays(response.getSayToUser());

        userSays = "make bob the recipient";
        testHelpers.userSays(userSays);
        //response = allUserActions.set(new InfoForCommand(userSays,null), "recipient list", "bob");
        response = allUserActions.unknownCommand(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "yes";
        testHelpers.userSays(userSays);
        response = allUserActions.yes(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "take bob's email";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "bob", "email");
        if (response.isSuccess())
        {
            //maybe this should be done automatically every time.
            response = allUserActions.evalField(new InfoForCommand(userSays, null), response.getField());
        }
        testHelpers.systemSays(response.getSayToUser());

        userSays = "and set it as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbMutableFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (response.isSuccess())
        {
            //but then it will fail here...
            response = allUserActions.setFieldFromFieldVal(new InfoForCommand(userSays, null), response.getField(), allUserActions.getProbFieldVal(new InfoForCommand(userSays, null)).getValue());
        }
        //response = CcgUtils.parseAndEval(allUserActions, parserSettings, userSays);
        testHelpers.systemSays(response.getSayToUser());

        userSays = "that's it";
        testHelpers.userSays(userSays);
        response = allUserActions.end(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "make jane the recipient";
        testHelpers.userSays(userSays);
        //should now translate to:
        {
            response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "jane", "email");
            if (response.isSuccess())
            {
                //maybe this should be done automatically every time.
                ActionResponse fieldEval = allUserActions.evalField(new InfoForCommand(userSays, null), response.getField());

                if (fieldEval.isSuccess())
                {
                    response = allUserActions.getProbMutableFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
                    if (response.isSuccess())
                    {
                        //but then it will fail here...
                        response = allUserActions.setFieldFromFieldVal(new InfoForCommand(userSays, null), response.getField(), fieldEval.getValue());
                    }
                }
            }
        }
        testHelpers.systemSays(response.getSayToUser());

    }


    private static void buildRequiredDB(TopDMAllActions topDMAllActions, TestHelpers testHelpers, ParserSettings parserSettings)
    {
        testHelpers.newSection("buildRequiredDB");

        ActionResponse response;
        String userSays;
        IAllUserActions allUserActions = topDMAllActions;
        IIncomingEmailControlling incomingEmailControlling = topDMAllActions;

        userSays = "create a contact my spouse";
        testHelpers.userSays(userSays);
        response = allUserActions.createInstanceByFullNames(new InfoForCommand(userSays, null), "contact", "my spouse");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set its email to my.spouse@gmail.com";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays, null), "email");
        if (response.isSuccess())
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "my.spouse@gmail.com");
        testHelpers.systemSays(response.getSayToUser());

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


    private static void learningToForwardAnEmail(IAllUserActions allUserActions, TestHelpers testHelpers, ParserSettings parserSettings)
    {
        testHelpers.newSection("learningToForwardAnEmail");

        ActionResponse response;
        String userSays;

        userSays = "forward this email to my spouse";
        testHelpers.userSays(userSays);
        response = allUserActions.unknownCommand(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "sure";
        testHelpers.userSays(userSays);
        response = allUserActions.yes(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "first create a new email";
        testHelpers.userSays(userSays);
        response = allUserActions.createInstanceByConceptName(new InfoForCommand(userSays, null), "outgoing email");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "then copy the subject from the incoming email to the outgoing email";
        testHelpers.userSays(userSays);
        response = allUserActions.unknownCommand(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "take the subject from the incoming email";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "inbox", "subject");
        if (response.isSuccess())
            response = allUserActions.evalField(new InfoForCommand(userSays, null), response.getField());
        testHelpers.systemSays(response.getSayToUser());

        userSays = "and set it as the outgoing email's subject";
        testHelpers.userSays(userSays);
        ActionResponse fieldResponse = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "outgoing email", "subject");
        if (fieldResponse.isSuccess())
            response = allUserActions.setFieldFromFieldVal(new InfoForCommand(userSays, null), fieldResponse.getField(), response.getValue());
        testHelpers.systemSays(response.getSayToUser());

        userSays = "take the body from the incoming email";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "inbox", "body");
        if (response.isSuccess())
            response = allUserActions.evalField(new InfoForCommand(userSays, null), response.getField());
        testHelpers.systemSays(response.getSayToUser());

        userSays = "and set it as the body";
        testHelpers.userSays(userSays);
        //response = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays, null), "body"); //TODO: should understand since incoming email should not be mutable, or maybe leave for parser
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "outgoing email", "body");
        if (response.isSuccess())
            response = allUserActions.setFieldFromFieldVal(new InfoForCommand(userSays, null), response.getField(), allUserActions.getProbFieldVal(new InfoForCommand(userSays, null)).getValue());
        testHelpers.systemSays(response.getSayToUser());

        userSays = "send the email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "oh, yeah, set the recipient as my spouse";
        testHelpers.userSays(userSays);
        // should translate to: (set (get recipient_list) (eval (get my_spouse email)))
        {
            ActionResponse recipientField = allUserActions.getProbMutableFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
            if (recipientField.isSuccess())
            {
                ActionResponse spouseEmailField = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "my spouse", "email");

                if (spouseEmailField.isSuccess())
                {
                    ActionResponse spouseEmailFieldVal = allUserActions.evalField(new InfoForCommand(userSays, null), spouseEmailField.getField());

                    if (spouseEmailFieldVal.isSuccess())
                    {
                        response = allUserActions.setFieldFromFieldVal(new InfoForCommand(userSays, null), recipientField.getField(), spouseEmailFieldVal.getValue());
                    }
                }
            }
            testHelpers.systemSays(response.getSayToUser());
        }

        userSays = "now send the email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "that's it, you're done!";
        testHelpers.userSays(userSays);
        response = allUserActions.end(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

    }


    private static void smallUpdates(IAllUserActions allUserActions, TestHelpers testHelpers, ParserSettings parserSettings)
    {
        testHelpers.newSection("smallUpdates");
        String userSays;
        ActionResponse response;

        userSays = "next message";
        testHelpers.userSays(userSays);
        response = allUserActions.next(new InfoForCommand(userSays, null), "email");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "read incoming email";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbInstanceByName(new InfoForCommand(userSays, null), "inbox");
        if (response.isSuccess())
            response = allUserActions.readInstance(new InfoForCommand(userSays, null), response.getInstance());
        testHelpers.systemSays(response.getSayToUser());

        userSays = "add tasks to contact";
        testHelpers.userSays(userSays);
        response = allUserActions.addFieldToConcept(new InfoForCommand(userSays, null), "contact", "tasks");
        testHelpers.systemSays(response.getSayToUser());

        //tasks is plural, so will be assigned a list
        userSays = "set bob's tasks to dealing with complaints";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbMutableFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "bob", "tasks");
        if (response.isSuccess())
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "dealing with complaints");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "add helping new workers to bob's tasks";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbMutableFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "bob", "tasks");
        if (response.isSuccess())
            response = allUserActions.addToFieldFromString(new InfoForCommand(userSays, null), response.getField(), "helping new workers");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "what are bob's tasks";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "bob", "tasks");
        if (response.isSuccess())
            response = allUserActions.evalField(new InfoForCommand(userSays, null), response.getField());
        testHelpers.systemSays(response.getSayToUser());

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
            StringBuilder toOutput = new StringBuilder();
            String[] sentences = str.split("\n");
            for (String sentence : sentences)
            {
                toOutput.append("U: " + sentence + "\n");
            }
            System.out.print(toOutput.toString());
            allSystemReplies.append(toOutput.toString());
        }

        public void newSection(String sectionName)
        {
            String outputStr = "---------------- now in " + sectionName + " --------------------\n";
            System.out.println(outputStr);
            allSystemReplies.append(outputStr + "\n");
        }

        public void systemSays(String str)
        {
            StringBuilder toOutput = new StringBuilder();
            String[] sentences = str.split("\n");
            for (String sentence : sentences)
            {
                toOutput.append("S: " + sentence + "\n");
            }
            toOutput.append("\n");
            System.out.print(toOutput.toString());
            allSystemReplies.append(toOutput.toString());
        }

        public void endTest() throws Exception
        {
            if (testingMode)
            {
                String shouldBe = StringUtils.join(Files.readAllLines(Paths.get(fileName)), "\n");
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
