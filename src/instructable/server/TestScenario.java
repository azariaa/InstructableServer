package instructable.server;

import java.util.List;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class TestScenario
{
    public static void main(String[] args)
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

        sendingBasicEmail(allUserActions);

        definingContact(allUserActions);

    }

    private static void definingContact(IAllUserActions allUserActions)
    {
        ActionResponse response;

        response = allUserActions.composeEmail("compose a new email");
        systemSays(response.sayToUser);

        response = allUserActions.set("set my spouse as the recipient", "recipient list", "my spouse");
        systemSays(response.sayToUser);

        response = allUserActions.defineConcept("I want to teach you what a contact is", "contact");
        systemSays(response.sayToUser);

        response = allUserActions.defineConcept("Define contact!", "contact");
        systemSays(response.sayToUser);

        response = allUserActions.addFieldToConcept("add email as a field in contact", "contact", "email");
        systemSays(response.sayToUser);

        response = allUserActions.createInstance("create a contact, call it bob", "contact", "bob");
        systemSays(response.sayToUser);

        response = allUserActions.set("set bob's email to baba", "bob", "email", "baba");
        systemSays(response.sayToUser);

        response = allUserActions.set("set bob's email to bob@gmail.com", "bob", "email", "bob@gmail.com");
        systemSays(response.sayToUser);
    }

    private static void sendingBasicEmail(IAllUserActions allUserActions)
    {
        systemSays("Let's start by sending a dummy email to your-self, set the subject to hello and the body to test.");
        ActionResponse response;

        response = allUserActions.sendEmail("send an email");
        systemSays(response.sayToUser);

        response = allUserActions.composeEmail("compose an email");
        systemSays(response.sayToUser);

        response = allUserActions.set("set the subject of this email to hello", "outgoing email", "subject", "hello");
        systemSays(response.sayToUser);

        response = allUserActions.set("put test in body", "body", "test");
        systemSays(response.sayToUser);

        response = allUserActions.sendEmail("send the email");
        systemSays(response.sayToUser);

        response = allUserActions.set("set myself as the recipient", "recipient list", "myself");
        //how should we know that recipient is recipient list? Leave it for the parser?
        systemSays(response.sayToUser);

        response = allUserActions.set("set myself@myjob.com as the recipient", "recipient list", "myself@myjob.com");
        //should be able to learn something from this!!!
        systemSays(response.sayToUser);


        response = allUserActions.sendEmail("send");
        systemSays(response.sayToUser);

        response = allUserActions.sendEmail("send");
        systemSays(response.sayToUser);
    }

    public static void systemSays(String str)
    {
        String[] sentences = str.split("\n");
        for (String sentence : sentences)
        {
            System.out.println("S: " + sentence);
        }
        System.out.println();
    }
}
