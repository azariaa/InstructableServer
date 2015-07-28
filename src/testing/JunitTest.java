package testing;

import instructable.server.dal.CreateParserFromFiles;
import instructable.server.CommandsToParser;
import instructable.server.IAllUserActions;
import instructable.server.TopDMAllActions;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;

import org.junit.BeforeClass;
import org.junit.Test;

import testing.TestWithParser.TestHelpers;

import java.util.Optional;

public class JunitTest {
  
  private static ParserSettings parserSettings;
  
  private static TestHelpers testHelpers;
  
  private static final boolean testingMode = true;
  private static final String fileName = "June26test.txt";

  @BeforeClass
  public static void classSetUp() {
    parserSettings = CreateParserFromFiles.createParser(Optional.of("tempUser"));
    testHelpers = new TestHelpers(testingMode, fileName);
  }
  
  @Test
  public void testLongSentence1() {
    IAllUserActions allUserActions = new TopDMAllActions(new CommandsToParser(parserSettings), (subject, body, copyList, recipientList) -> {}, false);
    testHelpers.systemSays("Let's start by sending a dummy email to your-self, set the subject to hello and the body to test.");
    CcgUtils.SayAndExpression response;
    String userSays;

    userSays = "next email and read email and send an email to foo@bar.com";
    testHelpers.userSays(userSays);
    response = parserSettings.parseAndEval(allUserActions, userSays);
    //actionResponse = allUserActions.sendEmail(new InfoForCommand(userSays,null));
    testHelpers.systemSays(response.sayToUser);
  }
  
  @Test
  public void testLongSentence2() {
    IAllUserActions allUserActions = new TopDMAllActions(new CommandsToParser(parserSettings), (subject, body, copyList, recipientList) -> {}, false);
    testHelpers.systemSays("Let's start by sending a dummy email to your-self, set the subject to hello, and the body to test.");
    CcgUtils.SayAndExpression response;
    String userSays;

    userSays = "Create new email, set recipient to email's sender and set subject to hi there, send email.";
    testHelpers.userSays(userSays);
    response = parserSettings.parseAndEval(allUserActions, userSays);
    //actionResponse = allUserActions.sendEmail(new InfoForCommand(userSays,null));
    testHelpers.systemSays(response.sayToUser);
  }
}
