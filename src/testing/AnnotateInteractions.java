package testing;

import instructable.server.backend.IAllUserActions;
import instructable.server.backend.TopDMAllActions;
import instructable.server.ccg.CcgUtils;
import instructable.server.ccg.ParserSettings;
import instructable.server.dal.CreateParserFromFiles;
import instructable.server.dal.DBUtils;
import instructable.server.hirarchy.EmailInfo;
import instructable.server.parser.CommandsToParser;
import instructable.server.senseffect.IIncomingEmailControlling;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by Shashank Srivastava
 *
 * SUMMARY:
 'AnnotateInteractions' can be used to label user utterances (from chat records) with intended system behavior.

 PREREQS:

 1) Put AnnotateInteractions.java in the 'testing' package of 'InstructableServer'

 2) Create a working directory (wd) with two subdirectories:
 (i)  'txt': containing the text files of processed chat records
 (ii) 'annotations' : will contain annotation instances as text files (stored as gameId_uId.txt)

 3) Set line workDir in AnnotateInteractions.java to specify the path to the working directory (wd) in Step 2.

 ANNOTATION:

 The system will brief about available choices at each point.

 After loading the parser, the system asks for a gameId to annotate: specify the valid gameId (number) you want to annotate.

 At each utteranceId (which proceed as odd numbers), the system will show current and old system behavior, and ask for a confirmation.
 a) If everything seems right, you can press 'y' to log this
 b) To ignore this utterance, press 'i' to ignore this (utterance is still logged, but with an 'ignore' stamp)
 c) To manually intervene for some reason (e.g., system behavior isn't correct/a flags is not set right), press 'i' to enter manual model

 MANUAL MODE:
 In manual mode now, you can interact with the system to find the correct annotation for the current utteranceId. Once you enter a command that corresponds to the intended behavior and correct logical notation, you can log this correct response by typing 'enter'.

 In manual mode, you can also change the values of boolean flags currentSystemCorrectlyParses and oldSystemCorrectlyParses to 'true' or 'false' by saying cs:t, cs:f, os:t or os:f . e.g., cs:f would set 'currentSystemCorrectlyParses' to 'false'.

 You can use 'enter' more than once within manual mode. Only the last response will be logged.

 You can jump forward to an utterance x by saying goto x (e.g. goto 133)

 To leave manual mode, say 'exit' or 'quit'. After doing this, the system will take you to the next utteranceId. Before you exit manual mode, be sure to revert the system to the current state.
 */
public class AnnotateInteractions
{
	static Scanner scanIn = new Scanner(System.in);
	static String workDir="C:\\InMind\\TasksLongGoals\\software\\InstructableServer\\data\\anno\\";

	public static void main(String[] args) throws Exception
	{
		runAnnotator();
	}

	private static void runAnnotator() throws Exception
	{

		while (true)
		{
			System.out.println("LOADING MODELS FROM RESET ...");
			String userId = "tempUser";
            DBUtils.clearUserData(userId); //Amos added
			ParserSettings parserSettings = CreateParserFromFiles.createParser(Optional.of(userId));
			IAllUserActions allUserActions = new TopDMAllActions("you@myworkplace.com", userId, new CommandsToParser(parserSettings), (subject, body, copyList, recipientList) -> {
            }, false, Optional.empty(), Optional.empty());
			addContextEmails((TopDMAllActions) allUserActions);
			System.out.println("Ready to annotate!");

			try
			{
				System.out.println("Enter gameId to annotate... [Say exit/quit to leave interface]");	
				String annotatorSays = scanIn.nextLine().toLowerCase();
				if (annotatorSays.equals("exit") || annotatorSays.equals("quit"))
					break;

				String[] toks = annotatorSays.split("\\s+");
				int gameId = Integer.parseInt(toks[0]);
				int uId = (toks.length>1) ? Integer.parseInt(toks[1]): 1;

                List<String> allLines = Files.readAllLines(Paths.get(workDir+"txt/"+gameId+".txt"));


				//Reach line to start annotating
                int lineNo=uId;


                int waitUntilUtterance = 1;
                Boolean didSomethingLastRound = false; //required for undoing
                Boolean didSomethingThisRound = false; //required for undoing
				//begin annotating
				for ( ;lineNo < allLines.size(); lineNo+=2)
                {
                    didSomethingLastRound = didSomethingThisRound;
                    String line = allLines.get(lineNo-1); //list is 0 based

					//Retrieve userUtterance
					System.out.println("\n------------------------------------------------------------------------------------------------------------------\n");
					System.out.println("\nWorking with gameId: "+gameId+"\tuId: "+lineNo);
					String userUtterance = line.split("\t")[3].trim();
					System.out.println("U: " + userUtterance);
					String currentEmail = ((TopDMAllActions) allUserActions).inboxCommandController.getCurrentEmailName();

					//Parse and Execute
					CcgUtils.SayAndExpression response = parserSettings.parseAndEval(allUserActions, userUtterance);
					String parsedLogicalForm = response.lExpression.toString();

					String curResponse = response.sayToUser.trim().replaceAll("\n", "|");
					String oldResponse = allLines.get(lineNo+1-1).split("\t")[3];
					if(oldResponse.startsWith("\"")){
						oldResponse = oldResponse.substring(1, oldResponse.length()-1);
					}
					System.out.println("SysResponse:\t"+curResponse+ "\nOldResponse:\t"+oldResponse); //+"\nPrediction:"+parsedLogicalForm+"\tEnv:"+currentEmail);

					Boolean canParseNow = true;//(!(response.sayToUser.toLowerCase().contains("sorry")));
					Boolean canParseEarlier = //(!oldResponse.toLowerCase().contains("sorry")) &&
                            oldResponse.trim().toLowerCase().equals(response.sayToUser.trim().replaceAll("\n", " ").toLowerCase());//Amos: if they are different, so probably the old one was wrong
                    didSomethingThisRound = response.success;//(!(response.sayToUser.toLowerCase().contains("sorry")));

                    if (waitUntilUtterance > lineNo)
                        continue;

					String cnfString = getConfirmation(gameId, lineNo, parsedLogicalForm, curResponse, canParseNow, canParseEarlier, currentEmail, userUtterance, "Should I log this (y), log as ignored (i) skip (s) back (b) or go to manual mode(m)?").toLowerCase();
					if(cnfString.equals("y")||cnfString.equals("yes")){
						logInstance(gameId, lineNo, parsedLogicalForm, curResponse, canParseNow, canParseEarlier, currentEmail, userUtterance, curResponse, oldResponse, "system");
						System.out.println("Instance logged");
					}else if(cnfString.equals("i")||cnfString.startsWith("ign")){
						//Ignore, do nothing
						logInstance(gameId, lineNo, parsedLogicalForm, curResponse, canParseNow, canParseEarlier, currentEmail, userUtterance, curResponse, oldResponse, "system_IGNORE");
						System.out.println("Instance logged as ignored, moving to next.");
                    }
                    else if(cnfString.equals("s"))
                    {
                        System.out.println("Instance skipped, moving to next.");
                    }
                    else if(cnfString.equals("b"))
                    {
                        System.out.println("moving back");
                        if (didSomethingThisRound)
                            parserSettings.parseAndEval(allUserActions, "undo"); //undo this command
                        if (didSomethingLastRound)
                            parserSettings.parseAndEval(allUserActions, "undo"); //undo previous command, so can redo it
                        lineNo-=4;
                    }
                    else{ //manual mode:

                        canParseNow = false; //Amos: I assume that if we go to manual mode, we didn't get it right.
                        canParseEarlier = false;
						//Manual Annotation and handling
						System.out.println("System is in manual mode now. You can now interact with the system to find the correct annotation for this utterance."
								+ "\n(You can also change the values of boolean flags currentSystem and oldSystem to 'true' or 'false' by saying cs:t, cs:f, os:t or os:f)"
                                + "\nYou can jump forward to an utterance x by saying goto x (e.g. goto 133)"
								+ "\nOnce you enter a command that corresponds to the intended behavior and correct logical notation, you can log this correct response by typing 'enter'."
								+" \nYou can leave this dialogue by saying 'exit'. "
								+ "Before you exit manual mode, be sure to revert the system to the current state.");
						String userSays;
						Boolean logged = false;
						String lf=parsedLogicalForm, sysR=curResponse;
						logInstance(gameId, lineNo, parsedLogicalForm, curResponse, canParseNow, canParseEarlier, currentEmail, userUtterance, curResponse, oldResponse, "system_IGNORE"); //to be overwritten in manual mode
						
						while (true)
						{
							userSays = scanIn.nextLine();
							if (userSays.equals("exit") || userSays.equals("quit")){
								if(!logged){
									System.out.println("Instance ignored, moving to next.");
								}
								break;
							}
							else if( userSays.toLowerCase().equals("enter")){
								String confirm = getConfirmation(gameId, lineNo, lf, sysR, canParseNow, canParseEarlier, currentEmail, userUtterance, "Should I log this(y), or ignore this utterance(i)?").toLowerCase();
								if(confirm.equals("y")||confirm.equals("yes")){
									logInstance(gameId, lineNo, lf, sysR, canParseNow, canParseEarlier, currentEmail, userUtterance, curResponse, oldResponse, "manual");
									logged = true;
									System.out.println("Instance logged");
								}else if(confirm.equals("i")||confirm.startsWith("ign")){
									logInstance(gameId, lineNo, lf, sysR, canParseNow, canParseEarlier, currentEmail, userUtterance, curResponse, oldResponse, "manual_IGNORE");
									System.out.println("Ignored.");
								}
								//break;
							}
							else if( userSays.toLowerCase().startsWith("cs:t")){ canParseNow = true;}
							else if( userSays.toLowerCase().startsWith("cs:f")){ canParseNow = false;}
							else if( userSays.toLowerCase().startsWith("os:t")){ canParseEarlier = true;}
							else if( userSays.toLowerCase().startsWith("os:f")){ canParseEarlier = false;}
                            else if (userSays.toLowerCase().startsWith("goto "))
                            {
                                String[] gotoCommand = userSays.toLowerCase().split(" ");
                                if (gotoCommand.length > 1)
                                {
                                    int skipTo = -1;
                                    try
                                    {
                                        skipTo = Integer.parseInt(gotoCommand[1]);
                                    }
                                    catch (Exception ex)
                                    {
                                        ex.printStackTrace();
                                    }
                                    if (skipTo > 0)
                                    {
                                        waitUntilUtterance = skipTo;
                                        break;
                                    }
                                }
                            }
							else{
                                try
                                {
                                    CcgUtils.SayAndExpression response1 = parserSettings.parseAndEval(allUserActions, userSays);
                                    lf = response1.lExpression.toString();
                                    System.out.println("LOGICAL FORM:" + lf);
                                    sysR = response1.sayToUser.trim().replaceAll("\n", "|");
                                    System.out.println(sysR);
                                }
                                catch (Exception ex)
                                {
                                    ex.printStackTrace();
                                }
							} 
						}		
					} //end Manual annotation
				}
			} 
			catch (Exception e){
				e.printStackTrace();
			}          
		}    
		scanIn.close();	
	}

	private static String getConfirmation(int gameId, int uId, String logicalForm, String sysR, Boolean parseNow, Boolean parseEarlier, String currentEmail, String userUtterance, String question ){
		System.out.println(
				"\nConfirmation" +
						//"GId:"+gameId +
						//"\nuId:"+uId +
						"\n" + userUtterance+
						"\nLF:"+logicalForm +
						"\nsysRes:"+sysR +
						"\nCurSys:"+parseNow +
						"\tOldSys:"+parseEarlier + (parseEarlier ? ("!!!") : "...") +
						"\tTaskId:"+currentEmail
				);
		System.out.print(question+" ");
		String annotatorStr = scanIn.nextLine();
		return annotatorStr;
	}

	private static void logInstance(int gameId, int uId, String logicalForm, String sysR, Boolean parseNow, Boolean parseEarlier, String currentEmail, String userUtterance, String curResponse, String oldResponse, String provenance ){
		String fileName = workDir+"annotations/"+gameId+"_"+uId+".txt";
		try{
			//Over-write instance to a new file in 'annotations' directory
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
			PrintWriter writer = new PrintWriter(new File(fileName));
			writer.println(gameId+"\t"+uId+"\t"+logicalForm+"\t"+sysR+"\t"+parseEarlier+"\t"+parseNow+"\t"+currentEmail
					+"\t"+userUtterance+"\t"+oldResponse+"\t"+curResponse+"\t"+provenance+"\t"+timeStamp);
			writer.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private static void addContextEmails(IIncomingEmailControlling incomingEmailControlling)
	{
		String bossName = "Alex";
		String worker2Name = "Charlie";
		String bossEmail = "alextimetowork@myworkplace.com";
		String momEmail = "momthebest7@bestforyou.com";
		String worker1Email = "caseyousoon8@myworkplace.com";
		String worker2Email = "charlieisasleep4@myworkplace.com";
		String worker3Email = "aaronworkshard3@myworkplace.com";
		String myEmail = "you@myworkplace.com";
		String familyEventDate = "September 28th";

		//no mission
		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker1Email, "Hi there", Arrays.asList(myEmail), new LinkedList<String>(),
				"I'm feeling well today. I hope I will also feel well tomorrow and anytime! Please ignore this email and read the next one."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker1Email, "Another email", Arrays.asList(myEmail), new LinkedList<>(),
				"I felt like sending you another email. I hope that you don't mind. Please ignore this email too and read the next one."));

		//replying
		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(momEmail, "Shirt color", Arrays.asList(myEmail), new LinkedList<>(),
				"I need to know your favorite color for a shirt. Please reply as soon as possible (make sure to use the same subject, as you should always do when replying to emails :)."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(bossEmail, "Task I asked", Arrays.asList(myEmail), new LinkedList<>(),
				"Are you working on the task that I asked you to work on? Please reply immediately ."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker2Email, "Working tomorrow",Arrays.asList(myEmail),new LinkedList<>(),
				"I don't feel like working tomorrow, do I have to? Please reply as soon as possible."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker1Email,"What to do?",Arrays.asList(myEmail),new LinkedList<>(),
				"I'm done with all my tasks, what should I do next? Please reply as soon as possible."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(momEmail,"Are you still at work?",Arrays.asList(myEmail),new LinkedList<>(),
				"I must know if you are still at work. Please reply as soon as possible."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker3Email,"Do you like work?",Arrays.asList(myEmail),new LinkedList<>(),
				"I like my job. Please reply and let me know what you think, as soon as possible."));
		//plain send
		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker2Email,"Tell Alex that I'm on my way",Arrays.asList(myEmail),new LinkedList<>(),
				"Please email Alex saying that I'm on my way. " + worker2Name));
		//forwarding
		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(momEmail,"Family event",Arrays.asList(myEmail),new LinkedList<>(),
				"You must ask your boss to approve your vacation for the family event on "+familyEventDate+". Forward this email to your boss (make sure to use the same subject, and include the whole body, as you should always do when forwarding an email :)."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(bossEmail,"Your vacation",Arrays.asList(myEmail),new LinkedList<>(),
				"Your vacation has been approved. Please forward this email to your mom."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker1Email,worker2Name,Arrays.asList(myEmail),new LinkedList<>(),
				"I asked "+worker2Name+" to do what you said, but I see that it must come from you. Please forward this email to "+worker2Name + "."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(bossEmail,"Party time!",Arrays.asList(myEmail),new LinkedList<>(),
				"We will have a party next Thursday at 4:00pm. Please forward this email to " + worker2Name + "."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(bossEmail,"Work before parting",Arrays.asList(myEmail),new LinkedList<>(),
				"We will all have to work very hard next Monday, Tuesday and Wednesday. Please forward this email to " + worker2Name + "."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(bossEmail,"Rest during the weekend",Arrays.asList(myEmail),new LinkedList<>(),
				"Don't forget to rest well on the weekend so you can work well on Monday, Tuesday and Wednesday. Please forward this email to " + worker2Name + "."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker2Email,
				"Cannot attend the party",Arrays.asList(myEmail),new LinkedList<>(),"I am sorry, but I can't attend next week's party. Please forward this email to " + bossName + "."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker3Email,"Everyone ignores me",Arrays.asList(myEmail),new LinkedList<>(),
				"Thank you so much for not ignoring this email and reading it. Please reply and tell me that you saw it."));

		incomingEmailControlling.addEmailMessageToInbox(new EmailInfo(worker1Email, "I like attention", Arrays.asList(myEmail), new LinkedList<>(),
				"I'm happy that you are reading my email. Please reply and tell me that you saw it."));
	}

}
