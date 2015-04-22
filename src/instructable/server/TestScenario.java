package instructable.server;

/**
 * Created by Amos Azaria on 20-Apr-15.
 */
public class TestScenario
{
    public static void main(String[] args)
    {
        IAllUserActions allUserActions = new TopDMAllActions();

        systemSays("Let's start by sending a dummy email to your-self.");
        ActionResponse response;

        response = allUserActions.sendEmail("send an email");
        systemSays(response.sayToUser);

        response = allUserActions.composeEmail("compose an email");
        systemSays(response.sayToUser);

        response = allUserActions.set("set the subject of this email to test", "outgoing email", "subject", "test");
        systemSays(response.sayToUser);

        response = allUserActions.set("set the subject of this email to test", "subject", "test2");
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
