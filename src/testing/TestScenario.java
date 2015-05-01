package testing;

import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.CcgExample;
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.ccg.ParametricCcgParser;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import instructable.server.*;
import instructable.server.ccg.CcgUtils;
import instructable.server.hirarchy.EmailMessage;
import org.apache.commons.lang3.StringUtils;

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
    static String fileName = "May1test.txt";

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


    private static void sendingBasicEmail(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.systemSays("Let's start by sending a dummy email to your-self, set the subject to hello and the body to test.");
        ActionResponse response;
        String userSays;

        String[] lexiconEntries = new String[] {"\"send\",\"S{0}\",\"(sendEmail)\",\"0 sendEmail\"",
            "\"set\",\"(((S{0}/String{3}){0}/N{2}){0}/N{1}){0}\",\"(lambda $1 $2 $3 (set $2 $1 $3))\",\"0 set\",\"set 1 1\",\"set 2 2\",\"set 3 3\"",

            "body,N{0},body,0 body",
                "email,N{0},outgoing_email,0 outgoing_email",
                "recipient,N{0},recipientList,0 recipientList",
                "subject,N{0},subject,0 subject",
                "bob,N{0},bob,0 bob"};
        List<LexiconEntry> lexicon = LexiconEntry.parseLexiconEntries(Arrays.asList(lexiconEntries));

        //String[][] examples = new String[][] {{"send email", "(sendEmail)"},
                //{"set the body of bob to bar", "(set bob body \"bar\")"},
                //{"set the recipient list of the email to myself@myjob.com", "(set outgoing_email recipient_list \"myself@myjob.com\")"}};//,
                //{"set the subject of this email to hello", "(set outgoing_email subject \"hello\")"}};

        //what will I do with
        //set foo's body to bar (this changes the order of the variables).
        //put bar in foo's body

        List<String[]> examples = CcgUtils.loadExamples(Paths.get("data/examples.csv"));
        List<CcgExample> ccgExamples = Lists.newArrayList();
        for (int i = 0; i < examples.size(); i++) {
            Expression2 expression = ExpressionParser.expression2().parseSingleExpression(examples.get(i)[1]);
            CcgExample example = CcgUtils.createCcgExample(Arrays.asList(examples.get(i)[0].split(" ")), expression);
            ccgExamples.add(example);
        }

        ParametricCcgParser family = CcgUtils.buildParametricCcgParser(lexicon);
        //CcgParser parser = CcgUtils.train(family, ccgExamples);

        userSays = "send an email";
        testHelpers.userSays(userSays);
        //Expression2 expression = CcgUtils.parse(parser, CcgUtils.tokenize(userSays));
        //response = CcgUtils.evaluate(allUserActions, userSays, expression);
        response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "yes";
        testHelpers.userSays(userSays);
        response = allUserActions.yes(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set the subject of this email to hello";
        // (set (get this_email subject) "hello)
        testHelpers.userSays(userSays);
        //expression = CcgUtils.parse(parser, CcgUtils.tokenize(userSays));
        //response = CcgUtils.evaluate(allUserActions, userSays, null);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "outgoing email", "subject");
        if (response.isSuccess())
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "hello");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "put test in body";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays, null), "body");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "test");
        }
        testHelpers.systemSays(response.getSayToUser());

        userSays = "send the email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set myself as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "myself");
        }
        //how should we know that recipient is recipient list? Leave it for the parser?
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set myself@myjob.com as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "myself@myjob.com");
        }
        //could learn something from this?!
        testHelpers.systemSays(response.getSayToUser());

        userSays = "send";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "send";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.getSayToUser());
    }


    private static void definingContact(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.newSection("definingContact");

        ActionResponse response;
        String userSays;

        userSays = "compose a new email";
        testHelpers.userSays(userSays);
        response = allUserActions.composeEmail(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set my spouse as the recipient";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (response.isSuccess())
        {
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "my spouse");
        }
        testHelpers.systemSays(response.getSayToUser());

        userSays = "I want to teach you what a contact is";
        testHelpers.userSays(userSays);
        response = allUserActions.defineConcept(new InfoForCommand(userSays,null), "contact");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "Define contact!";
        testHelpers.userSays(userSays);
        response = allUserActions.defineConcept(new InfoForCommand(userSays,null), "contact");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "add email as a field in contact";
        testHelpers.userSays(userSays);
        response = allUserActions.addFieldToConcept(new InfoForCommand(userSays,null), "contact", "email");
        testHelpers.systemSays(response.getSayToUser());

        userSays = "create a contact, call it bob";
        testHelpers.userSays(userSays);
        response = allUserActions.createInstance(new InfoForCommand(userSays,null), "contact", "bob");
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


    private static void setFromGet(IAllUserActions allUserActions, TestHelpers testHelpers)
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
        response = allUserActions.createInstance(new InfoForCommand(userSays,null), "contact", "jane");
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
            response = allUserActions.setFieldFromPreviousEval(new InfoForCommand(userSays, null), response.getField());
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
                ActionResponse bobEmailFieldVal = allUserActions.evalField(new InfoForCommand(userSays, null) , bobEmailField.getField());

                if (bobEmailFieldVal.isSuccess())
                {
                    response = allUserActions.setFieldFromFieldVal(new InfoForCommand(userSays, null), janeEmailField.getField(), bobEmailFieldVal.getValue());
                }
            }
        }
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set the recipient to be bob's email";
        testHelpers.userSays(userSays);
        //parser should translate to:
        //(set (get recipient_list) (eval (get bob email)))
        ActionResponse recipientField = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (recipientField.isSuccess())
        {
            ActionResponse bobEmailField = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "bob", "email");

            if (bobEmailField.isSuccess())
            {
                ActionResponse bobEmailFieldVal = allUserActions.evalField(new InfoForCommand(userSays, null) , bobEmailField.getField());

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
        recipientField = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (recipientField.isSuccess())
        {
            response = allUserActions.addToFieldFromString(new InfoForCommand(userSays, null), recipientField.getField(), "nana@gmail.com");
        }

        testHelpers.systemSays(response.getSayToUser());
    }


    private static void teachingToSetRecipientAsContact(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.newSection("teachingToSetRecipientAsContact");

        ActionResponse response;
        String userSays;

        userSays = "compose an email";
        testHelpers.userSays(userSays);
        response = allUserActions.composeEmail(new InfoForCommand(userSays,null));
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
        response = allUserActions.yes(new InfoForCommand(userSays,null));
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
        response = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
        if (response.isSuccess())
        {
            //but then it will fail here...
            response = allUserActions.setFieldFromPreviousEval(new InfoForCommand(userSays, null), response.getField());
        }
        testHelpers.systemSays(response.getSayToUser());

        userSays = "that's it";
        testHelpers.userSays(userSays);
        response = allUserActions.endLearning(new InfoForCommand(userSays, null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "make jane the recipient";
        testHelpers.userSays(userSays);
        //should now translate to:
        {
            response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "jane", "email");
            if (response.isSuccess())
            {
                //maybe this should be done automatically every time.
                response = allUserActions.evalField(new InfoForCommand(userSays, null), response.getField());

                if (response.isSuccess())
                {
                    response = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
                    if (response.isSuccess())
                    {
                        //but then it will fail here...
                        response = allUserActions.setFieldFromPreviousEval(new InfoForCommand(userSays, null), response.getField());
                    }
                }
            }
        }
        testHelpers.systemSays(response.getSayToUser());

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
        testHelpers.systemSays(response.getSayToUser());

        userSays = "set its email to my.spouse@gmail.com";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays,null), "email");
        if (response.isSuccess())
            response = allUserActions.setFieldFromString(new InfoForCommand(userSays, null), response.getField(), "my.spouse@gmail.com");
        testHelpers.systemSays(response.getSayToUser());

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


    private static void learningToForwardAnEmail(IAllUserActions allUserActions, TestHelpers testHelpers)
    {
        testHelpers.newSection("learningToForwardAnEmail");

        ActionResponse response;
        String userSays;

        userSays = "forward this email to my spouse";
        testHelpers.userSays(userSays);
        response = allUserActions.unknownCommand(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "sure";
        testHelpers.userSays(userSays);
        response = allUserActions.yes(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "first create a new email";
        testHelpers.userSays(userSays);
        response = allUserActions.composeEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "then copy the subject from the incoming email to the outgoing email";
        testHelpers.userSays(userSays);
        response = allUserActions.unknownCommand(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "take the subject from the incoming email";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "inbox", "subject");
        if (response.isSuccess())
            response = allUserActions.evalField(new InfoForCommand(userSays,null), response.getField());
        testHelpers.systemSays(response.getSayToUser());

        userSays = "and set it as the outgoing email's subject";
        testHelpers.userSays(userSays);
        response = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "outgoing email", "subject");
        if (response.isSuccess())
            response = allUserActions.setFieldFromPreviousEval(new InfoForCommand(userSays, null), response.getField());
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
            response = allUserActions.setFieldFromPreviousEval(new InfoForCommand(userSays, null), response.getField());
        testHelpers.systemSays(response.getSayToUser());

        userSays = "send the email";
        testHelpers.userSays(userSays);
        response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "oh, yeah, set the recipient as my spouse";
        testHelpers.userSays(userSays);
        // should translate to: (set (get recipient_list) (eval (get my_spouse email)))
        {
            ActionResponse recipientField = allUserActions.getProbFieldByFieldName(new InfoForCommand(userSays, null), "recipient list");
            if (recipientField.isSuccess())
            {
                ActionResponse spouseEmailField = allUserActions.getProbFieldByInstanceNameAndFieldName(new InfoForCommand(userSays, null), "my spouse", "email");

                if (spouseEmailField.isSuccess())
                {
                    ActionResponse spouseEmailFieldVal = allUserActions.evalField(new InfoForCommand(userSays, null) , spouseEmailField.getField());

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
        response = allUserActions.sendEmail(new InfoForCommand(userSays,null));
        testHelpers.systemSays(response.getSayToUser());

        userSays = "that's it, you're done!";
        testHelpers.userSays(userSays);
        response = allUserActions.endLearning(new InfoForCommand(userSays, null));
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
