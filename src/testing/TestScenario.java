package testing;

import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.CcgExample;
import com.jayantkrish.jklol.ccg.CcgParser;
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.ccg.ParametricCcgParser;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import org.apache.commons.lang3.StringUtils;
import instructable.server.*;
import instructable.server.ccg.CcgUtils;
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
            public void addTrainingEg(String originalCommand, List<Expression2> commandsLearnt)
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
        response = allUserActions.createInstance(new InfoForCommand(userSays,null), "contact", "my spouse");
        testHelpers.systemSays(response.sayToUser);

        userSays = "set its email to my.spouse@gmail.com";
        testHelpers.userSays(userSays);
        response = allUserActions.set(new InfoForCommand(userSays,null), "email", "my.spouse@gmail.com");
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
        response = allUserActions.get(new InfoForCommand(userSays,null), "bob", "email");
        testHelpers.systemSays(response.sayToUser);
        //testHelpers.systemSays(response.value.get().toJSONString());

        userSays = "create a contact jane";
        testHelpers.userSays(userSays);
        response = allUserActions.createInstance(new InfoForCommand(userSays,null), "contact", "jane");
        testHelpers.systemSays(response.sayToUser);


        userSays = "take bob's email";
        testHelpers.userSays(userSays);
        response = allUserActions.get(new InfoForCommand(userSays,null), "bob", "email");
        testHelpers.systemSays(response.sayToUser);

        userSays = "and set it as jane's email";
        testHelpers.userSays(userSays);
        response = allUserActions.setFromPreviousGet(new InfoForCommand(userSays,null), "jane", "email");
        testHelpers.systemSays(response.sayToUser);


        userSays = "take bob's email and set it as jane's email";
        testHelpers.userSays(userSays);
        //parser should translate to:
        //(set jane email (get bob email))
        response = allUserActions.get(new InfoForCommand(userSays,null), "bob", "email");
        if (response.value.isPresent())
        {
            response = allUserActions.set(new InfoForCommand(userSays,null), "jane", "email", response.value.get());
        }
        testHelpers.systemSays(response.sayToUser);

        userSays = "set the recipient to be bob's email";
        testHelpers.userSays(userSays);
        //parser should translate to:
        //(set recipient_list (get bob email))
        response = allUserActions.get(new InfoForCommand(userSays,null), "bob", "email");
        if (response.value.isPresent())
        {
            response = allUserActions.set(new InfoForCommand(userSays,null), "recipient list", response.value.get());
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
        response = allUserActions.composeEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "set jane's email to be jane@gmail.com";
        testHelpers.userSays(userSays);
        response = allUserActions.set(new InfoForCommand(userSays,null), "jane", "email", "jane@gmail.com");
        testHelpers.systemSays(response.sayToUser);

        userSays = "make bob the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.set(new InfoForCommand(userSays,null), "recipient list", "bob");
        testHelpers.systemSays(response.sayToUser);

        userSays = "yes";
        testHelpers.userSays(userSays);
        response = allUserActions.yes(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "take bob's email";
        testHelpers.userSays(userSays);
        response = allUserActions.get(new InfoForCommand(userSays,null), "bob", "email");
        testHelpers.systemSays(response.sayToUser);

        userSays = "and set it as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.setFromPreviousGet(new InfoForCommand(userSays,null), "recipient list");
        testHelpers.systemSays(response.sayToUser);

        userSays = "that's it";
        testHelpers.userSays(userSays);
        response = allUserActions.endTeaching(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "make jane the recipient";
        testHelpers.userSays(userSays);
        //should now translate to:
        {
            response = allUserActions.get(new InfoForCommand(userSays,null), "jane", "email");
            if (response.success)
                response = allUserActions.setFromPreviousGet(new InfoForCommand(userSays,null), "recipient list");
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
        response = allUserActions.unknownCommand(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "sure";
        testHelpers.userSays(userSays);
        response = allUserActions.yes(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "first create a new email";
        testHelpers.userSays(userSays);
        response = allUserActions.composeEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "then copy the subject from the incoming email to the outgoing email";
        testHelpers.userSays(userSays);
        response = allUserActions.unknownCommand(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "take the subject from the incoming email";
        testHelpers.userSays(userSays);
        response = allUserActions.get(new InfoForCommand(userSays,null), "inbox", "subject");
        testHelpers.systemSays(response.sayToUser);

        userSays = "and set it as the outgoing email's subject";
        testHelpers.userSays(userSays);
        response = allUserActions.setFromPreviousGet(new InfoForCommand(userSays,null), "outgoing email", "subject");
        testHelpers.systemSays(response.sayToUser);

        userSays = "take the body from the incoming email";
        testHelpers.userSays(userSays);
        response = allUserActions.get(new InfoForCommand(userSays,null), "inbox", "body");
        testHelpers.systemSays(response.sayToUser);

        userSays = "and set it as the body";
        testHelpers.userSays(userSays);
        response = allUserActions.setFromPreviousGet(new InfoForCommand(userSays,null), "outgoing email", "body"); //TODO: should understand since incoming email should not be mutable, or maybe leave for parser
        testHelpers.systemSays(response.sayToUser);

        userSays = "send the email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "oh, yeah, set the recipient as my spouse";
        testHelpers.userSays(userSays);
        // should translate to:
        {
            response = allUserActions.get(new InfoForCommand(userSays,null), "my spouse", "email");
            if (response.success)
                response = allUserActions.setFromPreviousGet(new InfoForCommand(userSays,null), "recipient list");
            testHelpers.systemSays(response.sayToUser);
        }

        userSays = "now send the email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "that's it, you're done!";
        testHelpers.userSays(userSays);
        response = allUserActions.endTeaching(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

    }

    private static void sendingBasicEmail(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.systemSays("Let's start by sending a dummy email to your-self, set the subject to hello and the body to test.");
        ActionResponse response;
        String userSays;

        String[] lexiconEntries = new String[] {"\"send\",\"S{0}\",\"(sendEmail)\",\"0 sendEmail\"", "\"set\",\"(((S{0}/N{3}){0}/N{2}){0}/N{1}){0}\",\"(lambda $3 $2 $1 (set $2 $1 $3))\",\"0 set\",\"set 1 1\",\"set 2 2\",\"set 3 3\""};
        List<LexiconEntry> lexicon = LexiconEntry.parseLexiconEntries(Arrays.asList(lexiconEntries));

        String[][] examples = new String[][] {{"send email", "(sendEmail)"},
                {"set the body of foo to bar", "(set \"foo\" \"body\" \"bar\")"}};

        List<CcgExample> ccgExamples = Lists.newArrayList();
        for (int i = 0; i < examples.length; i++) {
            Expression2 expression = ExpressionParser.expression2().parseSingleExpression(examples[i][1]);
            CcgExample example = CcgUtils.createCcgExample(Arrays.asList(examples[i][0].split(" ")), expression);
            ccgExamples.add(example);
        }

        ParametricCcgParser family = CcgUtils.buildParametricCcgParser(lexicon);
        CcgParser parser = CcgUtils.train(family, ccgExamples);

        userSays = "send an email";
        Expression2 expression = CcgUtils.parse(parser, CcgUtils.tokenize(userSays));
        testHelpers.userSays(userSays);
        response = CcgUtils.evaluate(allUserActions, userSays, expression);
        //response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "yes";
        testHelpers.userSays(userSays);
        response = allUserActions.yes(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "set the subject of the outgoing email to hello";
        testHelpers.userSays(userSays);
        response = allUserActions.set(new InfoForCommand(userSays,null), "outgoing email", "subject", "hello");
        testHelpers.systemSays(response.sayToUser);

        userSays = "put test in body";
        testHelpers.userSays(userSays);
        response = allUserActions.set(new InfoForCommand(userSays,null), "body", "test");
        testHelpers.systemSays(response.sayToUser);

        userSays = "send the email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "set myself as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.set(new InfoForCommand(userSays,null), "recipient list", "myself");
        //how should we know that recipient is recipient list? Leave it for the parser?
        testHelpers.systemSays(response.sayToUser);

        userSays = "set myself@myjob.com as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.set(new InfoForCommand(userSays,null), "recipient list", "myself@myjob.com");
        //should be able to learn something from this!!!
        testHelpers.systemSays(response.sayToUser);

        userSays = "send";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "send";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);
    }


    private static void definingContact(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.newSection("definingContact");

        ActionResponse response;
        String userSays;

        userSays = "compose a new email";
        testHelpers.userSays(userSays);
        response = allUserActions.composeEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.sayToUser);

        userSays = "set my spouse as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.set(new InfoForCommand(userSays,null), "recipient list", "my spouse");
        testHelpers.systemSays(response.sayToUser);

        userSays = "I want to teach you what a contact is";
        testHelpers.userSays(userSays);
        response = allUserActions.defineConcept(new InfoForCommand(userSays,null), "contact");
        testHelpers.systemSays(response.sayToUser);

        userSays = "Define contact!";
        testHelpers.userSays(userSays);
        response = allUserActions.defineConcept(new InfoForCommand(userSays,null), "contact");
        testHelpers.systemSays(response.sayToUser);

        userSays = "add email as a field in contact";
        testHelpers.userSays(userSays);
        response = allUserActions.addFieldToConcept(new InfoForCommand(userSays,null), "contact", "email");
        testHelpers.systemSays(response.sayToUser);

        userSays = "create a contact, call it bob";
        testHelpers.userSays(userSays);
        response = allUserActions.createInstance(new InfoForCommand(userSays,null), "contact", "bob");
        testHelpers.systemSays(response.sayToUser);

        userSays = "set bob's email to baba";
        testHelpers.userSays(userSays);
        response = allUserActions.set(new InfoForCommand(userSays,null), "bob", "email", "baba");
        testHelpers.systemSays(response.sayToUser);

        userSays = "set bob's email to bob@gmail.com";
        testHelpers.userSays(userSays);
        response = allUserActions.set(new InfoForCommand(userSays,null), "bob", "email", "bob@gmail.com");
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
