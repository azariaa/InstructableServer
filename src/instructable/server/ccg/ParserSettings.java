package instructable.server.ccg;

import com.jayantkrish.jklol.ccg.CcgParser;
import com.jayantkrish.jklol.lisp.Environment;
import com.jayantkrish.jklol.util.IndexedList;

import java.util.Set;

/**
 * Created by Amos Azaria on 05-May-15.
 */
public class ParserSettings
{
    public CcgParser parser;
    public Environment env;
    public IndexedList<String> symbolTable;
    public Set<String> posUsed;
}
