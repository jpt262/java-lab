package com.weworklab.searcher2;

import io.reactivex.Flowable;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.reactivex.schedulers.Schedulers;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by jtres on 2/17/18.
 */
@Component
public class SourceUrls {

    final static Logger logger = Logger.getLogger(SourceUrls.class);

    @Value("${input.search}")
    private String searchInput;

    private Integer batchIndex = 19;
    private Integer requestsCompleted = 0;
    List<String> targetUrls = new ArrayList<>();
    Map<Integer, Map<Integer, String>> output = new HashMap();

    PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();

    RequestConfig config = RequestConfig
            .custom()
            .setConnectTimeout(5 * 1000)
            .setCookieSpec(CookieSpecs.STANDARD)
            .build();

    CloseableHttpClient client
            = HttpClients.custom().setConnectionManager(poolingConnManager)
            .setDefaultRequestConfig(config)
            .setRedirectStrategy(new LaxRedirectStrategy())
            .build();

    public SourceUrls() throws IOException {
    }

    public void createRow(String targetUrl, String value, Integer index) {
        HashMap items = new HashMap();
        items.put(0, targetUrl);
        items.put(1, value);
        output.put(index, items);
    }

    public void fetchContent(String targetUrl, Integer index) {
        String searchTerm = " ".concat(searchInput).concat(" ");
        String report = "";
        String error = "";

        try {
            HttpGet getMethod = new HttpGet(targetUrl);

            int hardTimeout = 15; // seconds
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (getMethod != null) {
                        getMethod.abort();
                    }
                }
            };
            new Timer(true).schedule(task, hardTimeout * 1000);

            CloseableHttpResponse response = client.execute(getMethod);

            report = EntityUtils
                    .toString(response.getEntity())
                    .replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ")
                    .toLowerCase();
            report = Boolean.toString(report.indexOf(searchTerm.toLowerCase()) > -1);

            response.close();
        } catch (Exception e) {
            error = e.toString();

            logger.error(error);
        } finally {
            if (error.length() == 0) {
                createRow(targetUrl, report, index);
                return;
            }
            createRow(targetUrl, error, index);
        }
    }

    public void init() throws IOException {
        int originalBatchIndex = batchIndex;
        String sourceUrls = "https://s3.amazonaws.com/fieldlens-public/urls.txt";
        HttpGet getMethod = new HttpGet(sourceUrls);
        CloseableHttpResponse response = client.execute(getMethod);
        String[] content = EntityUtils
                .toString(response.getEntity())
                .split("\n");

        poolingConnManager.setMaxTotal(20);
        poolingConnManager.setDefaultMaxPerRoute(4);

        Arrays.stream(content).skip(1).forEach((d) -> {
            String row = d;
            String[] cells = row.split(",");
            Site site = new Site(Integer.parseInt(cells[0]),
                    cells[1].replace("\"", ""),
                    Integer.parseInt(cells[2]),
                    Integer.parseInt(cells[3]),
                    Float.parseFloat(cells[4]),
                    Float.parseFloat(cells[5]));

            String url = site.getUrl();
            String targetUrl = "http://".concat(url).replace("\"", "");
            targetUrls.add(targetUrl);

            if(site.getIndex() <= originalBatchIndex) {
                HttpHost host = new HttpHost(targetUrl, 80);
                poolingConnManager.setMaxPerRoute(new HttpRoute(host), 1);
                runFlowable(targetUrl, site.getIndex());
            }
            return;
        });

        logger.info(targetUrls.size());
        logger.info("URLs parser");
        logger.info("Fetching content");
    }

    private void completedFlowable() throws IOException {
        requestsCompleted++;
        batchIndex++;
        logger.info("Requests completed: ".concat(Integer.toString(requestsCompleted)));
        if(batchIndex < targetUrls.size()) {
            logger.info("Fetching: ".concat(targetUrls.get(batchIndex)));
            logger.info("Fetching: ".concat(Integer.toString(batchIndex)));
            HttpHost host = new HttpHost(targetUrls.get(batchIndex), 80);
            poolingConnManager.setMaxPerRoute(new HttpRoute(host), 1);
            runFlowable(targetUrls.get(batchIndex), batchIndex);
        }
        if(requestsCompleted == targetUrls.size()) {
            writeCsv();
        }
    }

    private FileWriter writeRow(FileWriter writer, String col1, String col2) throws IOException {
        writer.append(col1);
        writer.append(',');
        writer.append(col2);
        writer.append('\n');
        return writer;
    }

    private void writeCsv() throws IOException {
        FileWriter writer = new FileWriter("results.txt");

        writeRow(writer, "URL", "Result");

        for(int i = 0; i < output.size(); i++) {
            try {
                writeRow(writer, output.get(i).get(0), output.get(i).get(1));
            } catch (Exception e) {
                logger.error(e);
            }
        }

        writer.flush();
        writer.close();

        logger.info("File written");
    }

    private void runFlowable(String targetUrl, Integer index) {
        Flowable.fromCallable(() -> {
            fetchContent(targetUrl, index);
            return targetUrl;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe((e) -> {}, (e) -> {}, () -> {
                    this.completedFlowable();
                    logger.info(index);
                    logger.info(targetUrl);
                });
    }
}
