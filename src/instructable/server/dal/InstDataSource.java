package instructable.server.dal;

//import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by Amos Azaria on 23-Jul-15.
 * A static class holding the DataSource.
 * (dataSource is immutable, so this is thread safe. However all connections must be created when needed and closed of course).
 * Should really use resource file...
 */
public class InstDataSource
{
    private static final String userName = "root";
    private static final String password = "InMind7";
    private static final int portNum = 3306;
    private static final String dbName = "instructable_kb";
    private static final String serverName = "localhost";


    static private MysqlDataSource dataSource = null;

    static private void initialize()
    {
        dataSource = new MysqlDataSource();
        dataSource.setDatabaseName(dbName);
        dataSource.setUser(userName);
        dataSource.setPassword(password);
        dataSource.setPort(portNum);
        dataSource.setServerName(serverName);

        try
        {
            dataSource.setUseSSL(false);
            dataSource.setServerTimezone("UTC");
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    static public DataSource getDataSource()
    {
        if (dataSource == null)
            initialize();
        return dataSource;
    }

    private InstDataSource()
    {
    }
}
