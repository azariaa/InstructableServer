package testing;

import instructable.EnvironmentCreatorUtils;
import instructable.server.CommandsToParser;
import instructable.server.IAllUserActions;
import instructable.server.TopDMAllActions;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import org.junit.BeforeClass;
import org.junit.Test;

import testing.TestWithParser.TestHelpers;

public class JunitTest {
  
  private static ParserSettings parserSettings;
  
  private static TestHelpers testHelpers;
  
  private static final boolean testingMode = true;
  private static final String fileName = "June26test.txt";

  @BeforeClass
  public static void classSetUp() {
    parserSettings = EnvironmentCreatorUtils.createParser();
    testHelpers = new TestHelpers(testingMode, fileName);
  }
  
  @Test
  public void testLongSentence() {
    IAllUserActions allUserActions = new TopDMAllActions(new CommandsToParser(parserSettings), (subject, body, copyList, recipientList) -> {});
    testHelpers.systemSays("Let's start by sending a dummy email to your-self, set the subject to hello and the body to test.");
    CcgUtils.SayAndExpression response;
    String userSays;

    userSays = "next email and read email";
    testHelpers.userSays(userSays);
    response = parserSettings.ParseAndEval(allUserActions, userSays);
    //actionResponse = allUserActions.sendEmail(new InfoForCommand(userSays,null));
    testHelpers.systemSays(response.sayToUser);
  }
}
