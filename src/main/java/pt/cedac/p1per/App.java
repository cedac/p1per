package pt.cedac.p1per;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pt.cedac.p1per.database.DatabaseManager;
import pt.cedac.p1per.exception.DatabaseConnectionFailedException;
import pt.cedac.p1per.exception.DatabaseSetupFailedException;

import java.io.IOException;

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
            String albumLink = link.select("span.gold > a").first().attr("abs:href");
            print(" * %s", name);
            doc = fetchData(albumLink);
            Elements genre = doc.select("#album-info > section:nth-child(3) > div > ul > li:nth-child(5)");
            print(" --- %s", genre.first().text());

            try {
                DatabaseManager.getInstance().addEntry(name, "placeholder", genre.first().text().split(":")[1]);
            } catch (DatabaseConnectionFailedException e) {
                e.printStackTrace();
            }
        }
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
}
