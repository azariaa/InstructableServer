package instructable.server.ccg;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.jayantkrish.jklol.ccg.CcgParser;
import com.jayantkrish.jklol.ccg.CcgUnaryRule;
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.ccg.ParametricCcgParser;
import com.jayantkrish.jklol.ccg.lexicon.FeaturizedLexiconScorer.StringContext;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.preprocessing.FeatureVectorGenerator;
import com.jayantkrish.jklol.util.IndexedList;

/**
 * Created by Amos Azaria on 05-May-15.
 */
public class ParserSettings implements Cloneable
{

    @Override
    public ParserSettings clone()
    {
        ParserSettings parserSettings = new ParserSettings();
        parserSettings.lexicon = new LinkedList<>(lexicon); //(LinkedList<LexiconEntry>)lexicon.clone();
        parserSettings.unaryRules = new LinkedList<>(unaryRules);
        parserSettings.featureVectorGenerator = featureVectorGenerator;
        parserSettings.parser = parser;
        parserSettings.parserFamily = parserFamily;
        parserSettings.parserParameters = parserParameters.duplicate();
        parserSettings.env = env;
        parserSettings.symbolTable = new IndexedList<String>(symbolTable);
        parserSettings.posUsed = posUsed;
        return parserSettings;
    }

    public List<LexiconEntry> lexicon;
    public List<CcgUnaryRule> unaryRules;
    public FeatureVectorGenerator<StringContext> featureVectorGenerator;
    public CcgParser parser;
    public ParametricCcgParser parserFamily;
    public SufficientStatistics parserParameters;

    public Environment env;
    public IndexedList<String> symbolTable;
    public Set<String> posUsed;
}
