package instructable.server.dal;

import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by Amos Azaria on 23-Jul-15.
 */
public class FieldChanged implements IFieldChanged
{
    private final String fieldName;
    private final SingleInstance singleInstance;

    FieldChanged(SingleInstance singleInstance, String fieldName)
    {
        this.singleInstance = singleInstance;
        this.fieldName = fieldName;
    }


    /**
     * May be called also if this is the first time the field is assigned a value.
     * Called by FieldHolder itself only
     * @param fieldVal
     */
    @Override
    public void fieldChanged(JSONObject fieldVal)
    {
        //update DB, mayAlreadyExist
        //insert into instanceValTableName () values (userId,conceptName,instanceName,fieldName,fieldVal.toString()) on duplicate key update fieldJSonValColName=fieldVal.toString()
        try (
                Connection connection = InMindDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("insert into "+ DBUtils.instanceValTableName+" (" + DBUtils.userIdColName + "," + DBUtils.conceptColName + "," + DBUtils.instanceColName + "," + DBUtils.fieldColName + "," + DBUtils.fieldJSonValColName + ") values (?,?,?,?,?) on duplicate key update " + DBUtils.fieldJSonValColName + "=?");
        )
        {
            pstmt.setString(1, singleInstance.userId);
            pstmt.setString(2, singleInstance.conceptName);
            pstmt.setString(3, singleInstance.instanceName);
            pstmt.setString(4, fieldName);
            pstmt.setString(5, fieldVal.toString());
            pstmt.setString(6, fieldVal.toString());

            //PreparedStatement pstmt = con.prepareStatement("update "+instanceValTableName+" set "+fieldJSonValColName+" = "+fieldVal.toString()+" where "+userIdColName+"="+userId + " and "+conceptColName+"="+conceptName+" and "+instanceColName+"="+instanceName+" and "+fieldColName+"="+fieldName);

            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}
