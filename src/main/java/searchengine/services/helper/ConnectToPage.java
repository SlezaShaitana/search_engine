package searchengine.services.helper;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Slf4j
@Component
public class ConnectToPage {
    public Connection connectToPage(String url) throws IOException, InterruptedException {
        Thread.sleep(300);
        Connection connection = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .timeout(10000)
                .ignoreHttpErrors(false)
                .ignoreContentType(true)
                .followRedirects(true)
                .referrer("http://www.google.com");

        return connection;
    }

    public String getTitleFromHtml(String content) {
        Document doc = Jsoup.parse(content);
        return doc.title();
    }

    public Document getDocumentPage(String url) throws IOException, InterruptedException {
        Connection connection = connectToPage(url);
        return connection.get();
    }
}
