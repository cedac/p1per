package pt.cedac.p1per.database;

import pt.cedac.p1per.exception.DatabaseConnectionFailedException;
import pt.cedac.p1per.exception.DatabaseSetupFailedException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by cedac on 29/09/16.
 */
public class DatabaseManager {
    private static DatabaseManager instance = null;
    public static final String DB_NAME = "data.db";
    private Connection conn = null;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }

        return instance;
    }

    public Connection getConnection() throws DatabaseConnectionFailedException {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            return conn;
        } catch ( Exception e ) {
            throw new DatabaseConnectionFailedException();
        }
    }

    public void prepareDatabase() throws DatabaseSetupFailedException {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);

            stmt = c.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS INFO " +
                    "(ID INTEGER PRIMARY KEY     AUTOINCREMENT," +
                    " ARTIST           TEXT    NOT NULL, " +
                    " ALBUM            TEXT     NOT NULL, " +
                    " GENRES           TEXT, " +
                    " DATE             TEXT)";
            stmt.executeUpdate(sql);
            stmt.close();

            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);

            stmt = c.createStatement();
            sql = "CREATE TABLE IF NOT EXISTS UPDATES " +
                    "(ID INTEGER PRIMARY KEY     AUTOINCREMENT," +
                    " DATE           TEXT    NOT NULL) ";
            stmt.executeUpdate(sql);
            stmt.close();

            stmt = c.createStatement();
            sql = "CREATE TABLE IF NOT EXISTS LASTREPORT " +
                    "(LAST) ";
            stmt.executeUpdate(sql);
            stmt.close();

            c.close();
        } catch ( Exception e ) {
            throw new DatabaseSetupFailedException();
        }
    }

    public void addEntry(String artist, String album, String genres) throws DatabaseConnectionFailedException {
        if (conn == null) {
            try {
                this.getConnection();
            } catch (DatabaseConnectionFailedException e) {
                throw new DatabaseConnectionFailedException();
            }
        }


        Statement stmt = null;
        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String sql = "INSERT INTO INFO (ARTIST,ALBUM,GENRES) " +
                "VALUES (\'" + artist + "\', " + "'placeholder'" +", \'" + genres + "\');";
        try {
            stmt.executeUpdate(sql);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
