package instructable.server.ccg;

import com.jayantkrish.jklol.ccg.CcgParser;
import com.jayantkrish.jklol.ccg.CcgUnaryRule;
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.util.IndexedList;

import java.util.List;
import java.util.Set;

/**
 * Created by Amos Azaria on 05-May-15.
 */
public class ParserSettings
{
    public List<LexiconEntry> lexicon;
    public List<CcgUnaryRule> unaryRules;
    public CcgParser parser;
    public SufficientStatistics parserParameters;

    public Environment env;
    public IndexedList<String> symbolTable;
    public Set<String> posUsed;
}
