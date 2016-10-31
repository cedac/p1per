package pt.cedac.p1per.database;

import pt.cedac.p1per.exception.DatabaseConnectionFailedException;
import pt.cedac.p1per.exception.DatabaseSetupFailedException;

import java.sql.*;

/**
 * Created by cedac on 29/09/16.
 */
public class DatabaseManager {
    private static DatabaseManager instance = null;
    public static final String DB_NAME = "data.db";
    public static final String ARTIST = "ARTIST";
    public static final String ALBUM = "ALBUM";
    public static final String GENRES = "GENRES";
    public static final String DATE = "DATE";

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
                     ARTIST + "          TEXT    NOT NULL, " +
                     ALBUM + "           TEXT     NOT NULL, " +
                     GENRES + "          TEXT, " +
                     DATE + "            TEXT)";
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

            stmt = c.createStatement();
            sql = "INSERT INTO  LASTREPORT VALUES(0)";
            stmt.executeUpdate(sql);

            stmt.close();

            c.close();
        } catch ( Exception e ) {
            throw new DatabaseSetupFailedException();
        }
    }

    public void addEntry(String artist, String album, String genres) throws DatabaseConnectionFailedException {

        if (checkIfExists(artist, album)) return;

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

        String get = "INSERT INTO INFO (ARTIST,ALBUM,GENRES) " +
                "VALUES (\'" + artist + "', '" + album +"', '" + genres + "');";
        try {
            stmt.executeUpdate(get);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private boolean checkIfExists(String artistToCheck, String albumToCheck) throws DatabaseConnectionFailedException {
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

        String checkIfExists = "SELECT * FROM INFO" +
                " WHERE " + ARTIST + " = '" + artistToCheck +"'" +
                " AND " + ALBUM + " = '" + albumToCheck + "' LIMIT 1;";

        System.out.print("Executing : " + checkIfExists + "\n Returning: ");

        ResultSet res;

        try {
            res = stmt.executeQuery(checkIfExists);

            boolean exists = res.next();
            System.out.println(exists);

            stmt.close();
            return exists;
        } catch (SQLException e) {
            throw new DatabaseConnectionFailedException();
        }

    }

    public ResultSet getAllEntrysSince(int lastEntry) throws DatabaseConnectionFailedException {
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

        lastEntry = lastEntry == 0 ? -1 : lastEntry; //hack to treat zero

        String sql = "SELECT * FROM INFO " +
                        "WHERE 'ID' > " + lastEntry + ";";

        try {
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new DatabaseConnectionFailedException();
        }
    }

    public int getLastUpdate() throws DatabaseConnectionFailedException {
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


        String sql = "SELECT * FROM LASTREPORT";
        ResultSet res;
        try {
            res = stmt.executeQuery(sql);
            res.next();
            return res.getInt("LAST");
        } catch (SQLException e) {
            throw  new DatabaseConnectionFailedException();
        }
    }
}
