package com.laioffer.job.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laioffer.job.entity.Item;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
// import sun.net.www.http.HttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class GitHubClient {
    // url template
    private static final String URL_TEMPLATE = "https://jobs.github.com/positions.json?description=%s&lat=%s&long=%s";
    private static final String DEFAULT_KEYWORD = "developer";

    public List<Item> search(double lat, double lon, String keyword) {
        if (keyword == null) {
            keyword = DEFAULT_KEYWORD;
        }
        try {
            // tim cook => tim%20cook, tim+cook
            keyword = URLEncoder.encode(keyword, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = String.format(URL_TEMPLATE, keyword, lat, lon);
        CloseableHttpClient httpclient = HttpClients.createDefault();
        // create a custom response handler
        ResponseHandler<List<Item>> responseHandler = response -> {
            // once execute, go to this line
            if (response.getStatusLine().getStatusCode() != 200) {
                return Collections.emptyList();
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return Collections.emptyList();

            }
            ObjectMapper mapper = new ObjectMapper();
            // return Arrays.asList(mapper.readValue(entity.getContent(), Item[].class));
            List<Item> items = Arrays.asList(mapper.readValue(entity.getContent(), Item[].class));
            extractKeyWords(items);
            return items;
        };

        try {
            return httpclient.execute(new HttpGet(url), responseHandler); // call handler
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private void extractKeyWords(List<Item> items) {
        MonkeyLearnClient monkeyLearnClient = new MonkeyLearnClient();
        // ==============================================================================================
        // List<String> descriptions = new ArrayList<>();
        // for (Item item : items) {
        //      description.add(item.getDescription());
        // }

        List<String> descriptions = items.stream().map(Item::getDescription).collect(Collectors.toList());
        // items.stream() ==> provide a for loop structure
        // map the make it a list
        // ==============================================================================================

        List<Set<String>> keywordList = monkeyLearnClient.extract(descriptions);

        for (int i = 0; i < items.size(); i ++) { // loop through items
            if (items.size() != keywordList.size() && i > keywordList.size() - 1) {
                items.get(i).setKeywords(new HashSet<>());
                continue;
            }
            items.get(i).setKeywords(keywordList.get(i));
        }
    }
}
