package instructable.server.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by Amos Azaria on 13-Aug-15.
 *
 * static class meant to record what user said, what the system replied and the logical form
 */
public class InteractionRecording
{
    private static final String interactionTable = "interaction";
    private static final String userIdCol = "user_id";
    private static final String fullAltCol = "full_alt";
    private static final String selSentenceCol = "sentence";
    private static final String logicalFormCol = "logical_form";
    private static final String systemReplyCol = "reply";
    private static final String isSuccessCol = "success";

    private InteractionRecording()
    {
        //static class
    }

    static public void addUserUtterance(String userId, List<String> fullUserAlternatives, String selectedSentence, String logicalForm, String systemReply, boolean isSuccess)
    {
        try (
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("insert into "+ interactionTable + " (" + userIdCol + "," + fullAltCol + "," + selSentenceCol + "," + logicalFormCol + "," + systemReplyCol + "," + isSuccessCol + ") values (?,?,?,?,?,?)");
        )
        {
            pstmt.setString(1, userId);
            pstmt.setString(2, String.join("^", fullUserAlternatives));
            pstmt.setString(3, selectedSentence);
            pstmt.setString(4, logicalForm);
            pstmt.setString(5, systemReply);
            pstmt.setBoolean(6, isSuccess);

            //PreparedStatement pstmt = con.prepareStatement("update "+instanceValTableName+" set "+fieldJSonValColName+" = "+fieldVal.toString()+" where "+userIdColName+"="+userId + " and "+conceptColName+"="+conceptName+" and "+instanceColName+"="+instanceName+" and "+fieldColName+"="+fieldName);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}
