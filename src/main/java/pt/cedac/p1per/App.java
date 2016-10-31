package pt.cedac.p1per;

import com.google.api.services.gmail.Gmail;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pt.cedac.p1per.database.DatabaseManager;
import pt.cedac.p1per.exception.DatabaseConnectionFailedException;
import pt.cedac.p1per.exception.DatabaseSetupFailedException;
import pt.cedac.p1per.mailing.GoogleMail;
import pt.cedac.p1per.mailing.GoogleUtil;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by cedac on 16/09/16.
 */
public class App {
    public static void main(String[] args) {
        Validate.isTrue(args.length == 1, "usage: supply url to fetch");
        String url = args[0];
        print("Fetching %s...", url);

        Document doc = null;
        doc = fetchData(url);

        Elements links = doc.select("#featured-right > div:nth-child(2) > div > ul > li:nth-child(1) > ul li");

        java.sql.Connection conn = bootDatabase();

        print("\nLinks: (%d)", links.size());
        for (Element link : links) {
            String name = link.text().split(" : ")[0];
            String album = link.text().split(" : ")[1];
            String albumLink = link.select("span.gold > a").first().attr("abs:href");
            print(" * %s", name);
            doc = fetchData(albumLink);
            Elements genre = doc.select("#album-info > section:nth-child(3) > div > ul > li:nth-child(5)");
            print(" --- %s", genre.first().text());

            try {
                DatabaseManager.getInstance().addEntry(name, album, genre.first().text().split(":")[1]);
            } catch (DatabaseConnectionFailedException e) {
                //should try again FIXME!!
            }
        }

        ArrayList<String> report = makeReport();
        printReport(report);
        mailReport(report);

    }

    private static java.sql.Connection bootDatabase() {
        java.sql.Connection conn = null;

        try {
            DatabaseManager.getInstance().prepareDatabase();
        } catch (DatabaseSetupFailedException e) {
            e.printStackTrace();
        }

        try {
            conn = DatabaseManager.getInstance().getConnection();
        } catch (DatabaseConnectionFailedException e) {
            e.printStackTrace();
        }

        return conn;
    }

    private static Document fetchFromURL(String url) throws IOException {
        Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36").get();
        return doc;
    }

    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }

    private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width-1) + ".";
        else
            return s;
    }

    private static Document fetchData (String url) {
        int numberOfTries = 0;
        int maxTries = 5;

        while (true) {
            try {
                return fetchFromURL(url);
            } catch (IOException e) {
                numberOfTries++;
                if (numberOfTries >= maxTries) {
                    System.out.println("[PANIC] FAILED TO CONNECT " + numberOfTries + "TIMES");
                    e.printStackTrace();
                } else {
                    System.out.println("Try " + numberOfTries + "failed. TRYING AGAIN!");
                }
            }
        }
    }

    private static ArrayList<String> makeReport() {
        try {
            int lastUpdate = DatabaseManager.getInstance().getLastUpdate();
            ResultSet valuesToReport = DatabaseManager.getInstance().getAllEntrysSince(lastUpdate);
            ArrayList<String> lines = new ArrayList<String>();
            String artist;
            String album;
            String genres;
            String youtubeLink;
            String googleLink;

            while (valuesToReport.next()) {
                artist = valuesToReport.getString(DatabaseManager.ARTIST);
                album = valuesToReport.getString(DatabaseManager.ALBUM);
                genres = valuesToReport.getString(DatabaseManager.GENRES);
                youtubeLink = "https://www.youtube.com/results?search_query=" + artist.replace(" ", "+");
                googleLink = "https://www.google.pt/search?q=" + artist.replace(" ", "+");
                lines.add("Artist: " + artist + "\n" +
                            "Album: " + album + "\n" +
                            "Genres: " + genres + "\n" +
                            "Youtube: " + youtubeLink + "\n" +
                            "General Info: " + googleLink + "\n" +
                            "\n");
            }

            return lines;

        } catch (DatabaseConnectionFailedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void printReport(ArrayList<String> lines) {
        for (String line : lines) {
            System.out.print(line);
        }
    }

    private static void mailReport(ArrayList<String> lines) {
        String messageString = "Hello, \n here's your P1per Report :) \n\n";

        for (String line : lines) {
            messageString += line;
        }

        messageString += "\n\nHave a great day! :D,\nYours truly, \nP1per";

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        Date date = new Date();
        String dateString = dateFormat.format(date);

        try {
            MimeMessage mimeMessage = GoogleMail.createEmail("carlosalvc@gmail.com",
                    "p1per.report@gmail.com", "P1per report " + dateString,  messageString);
            GoogleMail.sendMessage(GoogleUtil.getGmailService(), "me", mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
