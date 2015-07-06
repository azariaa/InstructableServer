package instructable.server.ccg;

import com.jayantkrish.jklol.ccg.HeadedSyntacticCategory;
import com.jayantkrish.jklol.ccg.chart.ChartCost;
import com.jayantkrish.jklol.ccg.chart.ChartEntry;
import com.jayantkrish.jklol.models.DiscreteVariable;

/**
 * Created by Amos Azaria on 12-May-15.
 */
public class InstChartCost implements ChartCost
{
    @Override
    public double apply(ChartEntry entry, int spanStart, int spanEnd, int sentenceLength, DiscreteVariable syntaxVarType)
    {
        if (spanStart == 0 && spanEnd == sentenceLength - 1) {
            HeadedSyntacticCategory syntax = (HeadedSyntacticCategory) syntaxVarType.getValue(entry.getHeadedSyntax());

            if (syntax.toString().equals("S{0}") || syntax.toString().equals("UnknownCommand{0}"))// || syntax.toString().equals("FieldVal{0}")) //TODO: remove FieldVal once unary rules changed.
                return 0;
            else
                return Double.NEGATIVE_INFINITY;
        }
        return 0;
    }
}
