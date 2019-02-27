package instructable.server.ccg;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.*;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.ccg.lambda2.ExpressionSimplifier;
import com.jayantkrish.jklol.ccg.lambda2.StaticAnalysis;
import com.jayantkrish.jklol.ccg.lexicon.SpanFeatureAnnotation;
import com.jayantkrish.jklol.ccg.lexicon.StringContext;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.lisp.LispEval;
import com.jayantkrish.jklol.lisp.SExpression;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.nlpannotation.AnnotatedSentence;
import com.jayantkrish.jklol.preprocessing.DictionaryFeatureVectorGenerator;
import com.jayantkrish.jklol.preprocessing.FeatureGenerator;
import com.jayantkrish.jklol.preprocessing.FeatureVectorGenerator;
import com.jayantkrish.jklol.training.NullLogFunction;
import com.jayantkrish.jklol.util.IndexedList;
import instructable.server.backend.ActionResponse;
import instructable.server.backend.IAllUserActions;
import instructable.server.backend.InfoForCommand;
import instructable.server.dal.InteractionRecording;
import instructable.server.dal.ParserKnowledgeSeeder;
import instructable.server.parser.LispExecutor;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Amos Azaria on 05-May-15.
 */
public class ParserSettings
{
    static final int initialTraining = 10;
    static final int numOfIterationsToRetrain = 3;
    static final boolean treatCorpusAsLearnedExamples = true;//false; //treatCorpusAsLearnedExamples==true should improve performance, but may hide bugs, so should be false during testing.

    private static final Expression2 unknownExpression = ExpressionParser.expression2().parseSingleExpression("(" + IAllUserActions.unknownCommandStr + ")");

    private ParserSettings()
    {

    }

    public ParserSettings(ParserKnowledgeSeeder parserKnowledgeSeeder)
    {
        this(parserKnowledgeSeeder.getInitialLexiconEntries(), parserKnowledgeSeeder.getSynonyms(),
                parserKnowledgeSeeder.getUserDefinedEntries(), parserKnowledgeSeeder.getUnaryRules(),
                new StringFeatureGenerator(), parserKnowledgeSeeder.getAllExamples());
        this.parserKnowledgeSeeder = parserKnowledgeSeeder;
    }

    private ParserSettings(List<String> lexiconEntries, List<String> synonyms, List<String> userDefinedEntries, String[] unaryRules,
                          FeatureGenerator<StringContext, String> featureGenerator, List<String[]> examplesList)
    {
        final String midRowComment = "//";
        final String fullRowComment = "#";
        env = Environment.empty();
        symbolTable = IndexedList.create();
        learnedExamples = new HashMap<>();

        // remove all that appears after a "//" or starts with # (parseLexiconEntries only removes lines that start with "#")
        List<String> lexiconWithoutComments = lexiconEntries.stream()
                .filter(e -> !e.trim().isEmpty() && !e.contains(fullRowComment)) //remove all empty rows and those containing #
                .map(e -> (e.contains(midRowComment) ? e.substring(0, e.indexOf(midRowComment)) : e).trim())
                .filter(e->!e.isEmpty())
                .collect(Collectors.toList());

        // add synonyms. format: newWord, {meaning1,meaning2,...} (i.e, newWord = meaning1 U meaning2)
        List<String> lexEntriesFromSyn = new LinkedList<>();
        for (String synonym : synonyms)
        {
            if (synonym.trim().length() <= 1 || synonym.contains(fullRowComment) || synonym.trim().startsWith(midRowComment)) //comment row
                continue;
            String newWord = synonym.substring(0, synonym.indexOf(","));
            newWord = newWord.replace("^", ",");
            //newWord.replace("\"","");
            String[] meanings = synonym.substring(synonym.indexOf("{") + 1, synonym.indexOf("}")).split(",");
            for (String meaning : meanings)
            {
                String meaningWOQuotes = meaning.trim().replace("\"", "");
                for (String lexiconEntry : lexiconWithoutComments) //could save time if transferred all entries to a map, but this is anyway done only once
                {
                    String entryWord = lexiconEntry.substring(0, lexiconEntry.indexOf(",")).replace("\"", "");
                    if (entryWord.equals(meaningWOQuotes))
                    {
                        String newLexiconEntry = newWord + lexiconEntry.substring(lexiconEntry.indexOf(","));
                        lexEntriesFromSyn.add(newLexiconEntry);
                    }
                }
            }
        }

        List<String> allLexiconEntries = new LinkedList<>(lexiconWithoutComments);
        allLexiconEntries.addAll(lexEntriesFromSyn);
        allLexiconEntries.addAll(userDefinedEntries);

        for (String lexEntry : allLexiconEntries) //binding names. this will not bind names which appear inside logical forms, but it probably good enough.
        {
            String[] tokens = lexEntry.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            //go over all names and bind them
            String name = tokens[2].trim();//get the third item (after two commas, but not in quotes
            if (name.startsWith("\"") && name.endsWith("\""))
                name = name.substring(1,name.length()-1);
            if (!name.startsWith("(") && !name.startsWith("*"))
                env.bindName(name, name.replace("_", " "), symbolTable);
        }

        List<LexiconEntry> lexicon = LexiconEntry.parseLexiconEntries(allLexiconEntries);
        lexicon = CcgUtils.induceLexiconHeadsAndDependencies(lexicon);

        List<CcgUnaryRule> unaryRulesList = Lists.newArrayList();
        for (String unaryRule : unaryRules)
        {
            unaryRulesList.add(CcgUnaryRule.parseFrom(unaryRule));
        }

        posUsed = new HashSet<>();
        posUsed.add(ParametricCcgParser.DEFAULT_POS_TAG);
        posUsed.add(CcgUtils.START_POS_TAG);
        ccgExamples = Lists.newArrayList();
        for (int i = 0; i < examplesList.size(); i++)
        {
            String exSentence = examplesList.get(i)[0];
            String exLogicalForm = examplesList.get(i)[1];
            Expression2 expression = ExpressionParser.expression2().parseSingleExpression(exLogicalForm);
            WeightedCcgExample example = CcgUtils.createCcgExample(exSentence, expression, posUsed, true, null);
            ccgExamples.add(example);
            if (treatCorpusAsLearnedExamples)
            {
                //actually already tokenizes and POS exSentence above and will be doing it again in addToLearnedExamples, but this isn't significant.
                addToLearnedExamples(exSentence, expression, false);
            }
            List<String> allFunctionNames = LispExecutor.allFunctionNames();
            Set<String> freeSet = StaticAnalysis.getFreeVariables(example.getLogicalForm());
            for (String free : freeSet)
            {
                //add all that's not a function name and not a string (starts and ends with ")
                if (!(free.startsWith("\"") && free.endsWith("\"")) && !allFunctionNames.contains(free))
                    env.bindName(free, free.replace("_", " "), symbolTable);
            }
            //StaticAnalysis.inferType() //TODO: Jayant will add this functionality
        }

        List<CcgExample> reformattedExamples = Lists.newArrayList();
        for (WeightedCcgExample w : ccgExamples) {
          reformattedExamples.add(new CcgExample(w.getSentence(), null, null, w.getLogicalForm(), null));
        }
        
        List<StringContext> allContexts = StringContext.getContextsFromExamples(reformattedExamples);
        FeatureVectorGenerator<StringContext> featureVectorGenerator = DictionaryFeatureVectorGenerator
                .createFromData(allContexts, featureGenerator, true);
        this.ccgExamples = CcgUtils.featurizeExamples(ccgExamples, featureVectorGenerator);

        ParametricCcgParser family = CcgUtils.buildParametricCcgParser(lexicon, unaryRulesList,
                posUsed, featureVectorGenerator);
        this.lexicon = Lists.newArrayList(lexicon);
        this.unaryRules = Lists.newArrayList(unaryRulesList);
        this.featureVectorGenerator = featureVectorGenerator;
        this.parserParameters = CcgUtils.train(family, ccgExamples, initialTraining, null);
        this.parser = family.getModelFromParameters(this.parserParameters);
        this.parserFamily = family;
    }

    public ActionResponse evaluate(IAllUserActions allUserActions, String userSays, Expression2 expression, Optional<Date> userTime)
    {

        LispExecutor lispExecutor = new LispExecutor(allUserActions, new InfoForCommand(userSays, expression, userTime));

        //env.bindName("sendEmail", lispExecutor.getFunction("sendEmail"), symbolTable);
        //env.bindName("setFieldFromString", lispExecutor.getFunction("setFieldFromString"), symbolTable);
        //env.bindName("getProbFieldByInstanceNameAndFieldName", lispExecutor.getFunction("getProbFieldByInstanceNameAndFieldName"), symbolTable);

        List<LispExecutor.FunctionToExecute> functionToExecutes = lispExecutor.getAllFunctions();
        //this is ok, since we use the same function name it should override any old definition
        functionToExecutes.forEach(function -> env.bindName(function.getFunctionName(), function, symbolTable));

        //change in evaluation that every unknown name will change to it as a string (changing "_" with " ")
        //env.bindName("outgoing_email", "outgoing email", symbolTable);

        LispEval lispEval = new LispEval(symbolTable);
        SExpression sExpression = ExpressionParser.sExpression(symbolTable).parseSingleExpression(expression.toString());

        ActionResponse response;

        LispEval.EvalResult result = lispEval.eval(sExpression, env);
        response = (ActionResponse) result.getValue();

        return response;
    }

    public CcgUtils.SayAndExpression parseAndEval(IAllUserActions allUserActions, String userSays, Optional<Date> userTime)
    {
        return parseAndEval(Optional.empty(), allUserActions, new LinkedList<>(Collections.singleton(userSays)), userTime);
    }

    /**
     * Will parse the sentences in the list by order and will execute the first sentence which doesn't parse to unknown.
     * If all parse to unknown, will use the first sentence.
     * @param userId olny required if would like to store interaction in DB.
     * @param allUserActions
     * @param userSays
     * @return
     */
    public CcgUtils.SayAndExpression parseAndEval(Optional<String> userId, IAllUserActions allUserActions, List<String> userSays, Optional<Date> userTime)
    {
        Preconditions.checkArgument(!userSays.isEmpty());
        Optional<CcgUtils.SayAndExpression> specialCase = execIfSpecialCase(userId, allUserActions, userSays);
        if (specialCase.isPresent())
            return specialCase.get();
        for (String sentence : userSays) //trying all alternatives
        {
            Expression2 expression = parse(sentence);
            if (!expression.equals(unknownExpression) || failNextCommand)
            {
                failNextCommand = false;
                return executeLogicalForm(userId, allUserActions, userSays, sentence, expression, userTime);
            }
        }
        //all alternatives failed
        failNextCommand = false;
        return executeLogicalForm(userId, allUserActions, userSays, userSays.get(0), unknownExpression, userTime);
    }

    private Optional<CcgUtils.SayAndExpression> execIfSpecialCase(Optional<String> userId, IAllUserActions allUserActions, List<String> userSays)
    {
        String commandType = "FINISHED_RECORDING" + ":";
        if (userSays.size() == 1 && userSays.get(0).startsWith(commandType))
        {
            String sentence = userSays.get(0);
            String jsonPart = sentence.substring(commandType.length());
            try
            {
                JSONObject asJson = new JSONObject(jsonPart);
                ActionResponse response = allUserActions.userHasDemonstrated(new InfoForCommand(sentence, unknownExpression, Optional.empty()), asJson); //unknownExpression is used instead of null
                String sayToUserOrExec = response.getSayToUserOrExec();
                if (userId.isPresent())
                    InteractionRecording.addUserUtterance(userId.get(), userSays, sentence, "", sayToUserOrExec, response.isSuccess());
                return Optional.of(new CcgUtils.SayAndExpression(sayToUserOrExec, commandType, response.isSuccess()));
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
            return Optional.of(new CcgUtils.SayAndExpression("There is some error with your demonstration, please try again.", commandType, false));
        }
        return Optional.empty();
    }

    private CcgUtils.SayAndExpression executeLogicalForm(Optional<String> userId,
                                                         IAllUserActions allUserActions,
                                                         List<String> userSays,
                                                         String sentence,
                                                         Expression2 expression,
                                                         Optional<Date> userTime)
    {
        System.out.println("debug:" + expression.toString());
        ActionResponse response = this.evaluate(allUserActions, sentence, expression, userTime);
        String sayToUserOrExec = response.getSayToUserOrExec();
        if (userId.isPresent())
            InteractionRecording.addUserUtterance(userId.get(), userSays, sentence, expression.toString(), sayToUserOrExec, response.isSuccess());
        return new CcgUtils.SayAndExpression(sayToUserOrExec, expression.toString(), response.isSuccess());
    }

    /**
     * Adds new lexicon entries and unary rules to the grammar of the
     * CCG parser in {@code settings}.
     * Not using unaryRules. If required, need to add customizable unaryRules to parserKnowledgeSeeder
     */
    public void updateParserGrammar(List<LexiconEntry> lexiconEntries, boolean willTrainLater)//, List<CcgUnaryRule> unaryRules)
    {
        //filter out entries which are already present in the lexicon
        List<LexiconEntry> lexEntriesToAdd = new LinkedList<>();//tried doing this with lambda, but ended-up writing myself...
        for (LexiconEntry candidate : lexiconEntries)
        {
            boolean isNewCandidate = true;
            for (LexiconEntry current : lexicon)
            {
                if (current.toCsvString().equals(candidate.toCsvString()))
                {
                    isNewCandidate = false;
                    break;
                }
            }
            if (isNewCandidate)
                lexEntriesToAdd.add(candidate);
        }
        if (lexEntriesToAdd.size() > 0)
        {
            parserKnowledgeSeeder.addNewUserLexicons(lexEntriesToAdd.stream().map(LexiconEntry::toCsvString).collect(Collectors.toList())); //updating the DB
            lexicon.addAll(lexEntriesToAdd);
            this.unaryRules.addAll(unaryRules);
            if (!willTrainLater)
                updateGrammarFromExisting();
        }
    }

    private void updateGrammarFromExisting()
    {
        ParametricCcgParser newFamily = CcgUtils.buildParametricCcgParser(lexicon, this.unaryRules,
                posUsed, featureVectorGenerator);
        SufficientStatistics newParameters = newFamily.getNewSufficientStatistics();
        newParameters.transferParameters(parserParameters);
        parserParameters = newParameters;
        parserFamily = newFamily;
        parser = newFamily.getModelFromParameters(parserParameters);
    }

    public void updateParserGrammar(String newLexicon)
    {
        List<String> lexiconAsList = new LinkedList<>();
        lexiconAsList.add(newLexicon);
        List<LexiconEntry> lexiconEntries = LexiconEntry.parseLexiconEntries(lexiconAsList);
        this.updateParserGrammar(lexiconEntries, false);//, new LinkedList<>());
    }

    public void removeFromParserGrammar(String lexiconToRemove)
    {
        if (parserKnowledgeSeeder.hasUserDefinedLex(lexiconToRemove))
        {
            parserKnowledgeSeeder.removeUserDefinedLex(lexiconToRemove); //updating the DB

            List<String> lexiconAsList = new LinkedList<>();
            lexiconAsList.add(lexiconToRemove);
            List<LexiconEntry> lexiconEntries = LexiconEntry.parseLexiconEntries(lexiconAsList);
            //lexicon.removeAll(lexiconEntries);
            LexiconEntry lexiconEntryToRemove = lexiconEntries.get(0);
            lexicon = lexicon.stream().filter(lex -> !lex.toCsvString().startsWith(lexiconEntryToRemove.toCsvString())).collect(Collectors.toList()); //the lexicon may have additional information (all the number thingies)
            updateGrammarFromExisting();
        }
    }


    /**
     * Parses a sentence text using {@code parser} to produce a
     * logical form. The text is represented by a list of tokens
     * (e.g., obtained by splitting the input on spaces). The returned
     * logical form is given in a LISP S-Expression format.
     *
     * @param sentence
     * @return
     */
    private Expression2 parse(String sentence)
    {
        CcgInference inferenceAlg = new CcgExactInference(null, -1L, Integer.MAX_VALUE, 1);
        ExpressionSimplifier simplifier = CcgUtils.getExpressionSimplifier();

        //List<String> pos = Collections.nCopies(tokens.size(), ParametricCcgParser.DEFAULT_POS_TAG);
        List<String> tokens = new LinkedList<>();
        List<String> poss = new LinkedList<>();
        tokens.add(CcgUtils.startSymbol);
        poss.add(CcgUtils.START_POS_TAG);
        CcgUtils.tokenizeAndPOS(sentence, tokens, poss, false, posUsed);

        //if we know what to return for this exact example, simply return it:
        String jointTokenizedSentence = String.join(" ", tokens);
        if (learnedExamples.containsKey(jointTokenizedSentence))
            return learnedExamples.get(jointTokenizedSentence);

        AnnotatedSentence supertaggedSentence = new AnnotatedSentence(tokens, poss);
        SpanFeatureAnnotation annotation = SpanFeatureAnnotation.annotate(supertaggedSentence, featureVectorGenerator);
        supertaggedSentence = supertaggedSentence.addAnnotation(CcgUtils.STRING_FEATURE_ANNOTATION_NAME, annotation);

        //      //if we want to return only sentences and fieldVal in upper level:         //DiscreteVariable dv = parser.getSyntaxVarType();

        CcgParse parse = inferenceAlg.getBestParse(parser, supertaggedSentence, new InstChartCost(), new NullLogFunction());
        //if parse fails , or if we want to fail on this command we parse to unknownCommand
        Expression2 expression;
        if (parse == null ||
                (failNextCommand && !parse.getLogicalForm().hasSubexpression(ExpressionParser.expression2().parseSingleExpression("(" + IAllUserActions.cancelStr + ")")))) //fail all but a "cancel" command
        {
            expression = unknownExpression;
        }
        else
        {
            expression = parse.getLogicalForm();
        }

        return simplifier.apply(expression);
    }

    /**
     *
     * @param originalCommand
     * @param commandsLearnt
     * @return if learned any generalization (i.e. if any of the induced lexicon entries receive an argument)
     */
    public boolean addTrainingEg(String originalCommand, List<Expression2> commandsLearnt)
    {
        Expression2 expressionLearnt = CcgUtils.combineCommands(commandsLearnt);
        //we first tokenize the sentence (after adding the start symbol) then we join back the tokens, to make sure that it matches future sentences with identical tokens.
        addToLearnedExamples(originalCommand, expressionLearnt, true);

        WeightedCcgExample example = CcgUtils.createCcgExample(originalCommand, expressionLearnt, posUsed, false, featureVectorGenerator);

        CcgUtils.LexiconInductionRet lexiconInductionRet = CcgUtils.induceLexiconEntriesHeuristic(example, parser);
        System.out.println(lexiconInductionRet.lexiconEntries);

        updateParserGrammar(lexiconInductionRet.lexiconEntries, true);//, Lists.newArrayList());

        if (!lexiconInductionRet.newExamplesAdded.isEmpty())
            ccgExamples.addAll(lexiconInductionRet.newExamplesAdded);
        else //if we don't have any alternative examples, we use the original one
            ccgExamples.add(example);

//        ParametricCcgParser family = CcgUtils.buildParametricCcgParser(lexicon, unaryRules,
//                posUsed, featureVectorGenerator);
//        this.parserParameters = CcgUtils.train(family, ccgExamples, initialTraining, null);
//        this.parser = family.getModelFromParameters(this.parserParameters);
//        this.parserFamily = family;

        return lexiconInductionRet.foundAnyGeneralization;
    }

    private void addToLearnedExamples(String originalCommand, Expression2 expressionLearnt, boolean updateDB)
    {
        LinkedList<String> tokens = new LinkedList<>();
        List<String> dummy = new LinkedList<>(); //don't need POS
        CcgUtils.tokenizeAndPOS(originalCommand, tokens, dummy, false, posUsed);
        String jointTokenizedSentence = String.join(" ", tokens);
        //we update db before we add the startSymbol
        if (updateDB)
        {
            parserKnowledgeSeeder.addNewUserExample(new String[] {jointTokenizedSentence, expressionLearnt.toString()});
        }
        tokens.addFirst(CcgUtils.startSymbol);
        jointTokenizedSentence = String.join(" ", tokens);
        learnedExamples.put(jointTokenizedSentence, expressionLearnt);
    }

    public void retrain()
    {
        retrain(numOfIterationsToRetrain);
    }

    public void retrain(int iterations)
    {
        updateGrammarFromExisting();
        SufficientStatistics newParameters = CcgUtils.train(parserFamily,
                ccgExamples, iterations, parserParameters);
        
        parser = parserFamily.getModelFromParameters(newParameters);
        this.parserParameters = newParameters;
    }


    /**
     * Important: This function can only be called if the current ParserSettings is general (not for a specific user)!
     * @param userId
     * set alwaysCreateNew to true when running experiments and when not accessing the db.
     * @return
     */
    public ParserSettings createPSFromGeneralForUser(String userId, boolean alwaysCreateNew)
    {
        Preconditions.checkState(parserKnowledgeSeeder.isGeneralUser());
        if (alwaysCreateNew || !ParserKnowledgeSeeder.userExists(userId))
        {
            return createPSFromGeneralForNewUser(userId);
        }
        else
        {
            return new ParserSettings(new ParserKnowledgeSeeder(parserKnowledgeSeeder, userId));
        }
    }

    /**
     * Important: This function can only be called if the current ParserSettings is general (not for a specific user), and the user is new in the system!
     * @param newUserId
     * @return
     */
    public ParserSettings createPSFromGeneralForNewUser(String newUserId)
    {
        //should also check that newUserId is actually new.
        ParserSettings parserSettings = new ParserSettings();
        parserSettings.ccgExamples = new LinkedList<>(ccgExamples);
        parserSettings.lexicon = new LinkedList<>(lexicon); //(LinkedList<LexiconEntry>)lexicon.clone();
        parserSettings.unaryRules = new LinkedList<>(unaryRules);
        parserSettings.featureVectorGenerator = featureVectorGenerator;
        parserSettings.parserFamily = parserFamily;
        parserSettings.parser = parser; //parserFamily.getModelFromParameters(parserParameters);
        parserSettings.parserParameters = parserParameters.duplicate();
        parserSettings.env = new Environment(env);
        parserSettings.symbolTable = new IndexedList<String>(symbolTable);
        parserSettings.posUsed = posUsed;
        parserSettings.learnedExamples = new HashMap<>(learnedExamples);
        parserSettings.parserKnowledgeSeeder = new ParserKnowledgeSeeder(parserKnowledgeSeeder, newUserId);
        return parserSettings;
    }

    ParserKnowledgeSeeder parserKnowledgeSeeder;

    public List<WeightedCcgExample> ccgExamples;
    //learnedExamples examples are matched BEFORE parsing //the String key in these examples are the tokens joined back using " " (this is done to improve performance using hash-map)
    // if the (treatCorpusAsLearnedExamples==true) so copies all corpus in ccgExamples to learnedExamples
    public Map<String, Expression2> learnedExamples;
    public List<LexiconEntry> lexicon;
    public List<CcgUnaryRule> unaryRules;
    public FeatureVectorGenerator<StringContext> featureVectorGenerator;
    public CcgParser parser;
    public ParametricCcgParser parserFamily;
    public SufficientStatistics parserParameters;

    public Environment env;
    public IndexedList<String> symbolTable;
    public Set<String> posUsed;


    private boolean failNextCommand = false;

    public void failNextCommand()
    {
        this.failNextCommand = true;
    }
}
