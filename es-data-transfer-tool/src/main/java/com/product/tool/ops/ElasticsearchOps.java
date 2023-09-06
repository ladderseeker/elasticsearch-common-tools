package com.product.tool.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author XIAO JIN
 * @date 2023/09/05 9:58
 */
@Slf4j
public class ElasticsearchOps {

    public RestHighLevelClient getClient(String username, String password, String endpoint) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return new RestHighLevelClient(
                RestClient.builder(HttpHost.create(endpoint)).setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
        );
    }

    public RestHighLevelClient getClient(String endpoint) {
        return new RestHighLevelClient(RestClient.builder(HttpHost.create(endpoint)));
    }

    public void backupData(RestHighLevelClient client, String indexName, String backupFileName, JsonNode dslQuery) throws IOException {
        log.info("Starting backup...");

        // Prepare Search Request
        final TimeValue scrollTime = TimeValue.timeValueMinutes(5L);
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().size(10000);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (dslQuery != null && !dslQuery.isEmpty()) {
                log.info("Query is {}", dslQuery);
                String querySource = dslQuery.get("query").toString();
                searchSourceBuilder.query(QueryBuilders.wrapperQuery(querySource));
            } else {
                log.info("No specified query, query is matchAll query");
                searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            }
        } catch (Exception e) {
            log.error("Invalid DSL Query.", e);
            System.exit(1);
        }

        searchRequest.source(searchSourceBuilder).scroll(scrollTime);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();


        File file = new File(backupFileName);

        int count = 0;
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            SequenceWriter sequenceWriter = objectMapper.writer().writeValues(fos);
            while (searchHits != null && searchHits.length > 0) {
                for (SearchHit hit : searchHits) {
                    // Serialize to JSON File
                    Map<String, Map<String, String>> indexMetaMap = new HashMap<>();
                    Map<String, String> indexIdMap = new HashMap<String, String>() {{
                        put("_id", hit.getId());
                    }};
                    indexMetaMap.put("index", indexIdMap);
                    sequenceWriter.write(indexMetaMap);
                    fos.write("\n".getBytes());
                    sequenceWriter.write(hit.getSourceAsMap());
                    fos.write("\n".getBytes());
                    count++;
                }

                log.info("Current backup size is {}", count);

                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId).scroll(scrollTime);
                searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);

                scrollId = searchResponse.getScrollId();
                searchHits = searchResponse.getHits().getHits();
            }
        }

        // Clear Scroll context
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);

        log.info("Backup completed, backup doc count is {}", count);
    }

    public void restoreData(RestHighLevelClient client, String newIndexName, String backupFileName) throws IOException {
        log.info("Starting restore...");  // Logging

        // Deserialize Data from JSON File
        int batchSize = 10000;
        int count = 0;
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(backupFileName);
        BulkRequest bulkRequest = new BulkRequest();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String id = null;
            while ((line = br.readLine()) != null) {
                if (count % 2 == 0) {
                    JsonNode data = objectMapper.readTree(line);
                    id = data.get("index").get("_id").asText();
                } else {
                    Map data = objectMapper.readValue(line, Map.class);
                    IndexRequest indexRequest = new IndexRequest(newIndexName);
                    indexRequest.id(id);
                    indexRequest.source(data);
                    bulkRequest.add(indexRequest);
                }
                count++;

                if (count % (batchSize * 2) == 0) {
                    log.info("Bulk index, current indexed data size is {}", count / 2);
                    client.bulk(bulkRequest, RequestOptions.DEFAULT);
                    bulkRequest = new BulkRequest();
                }
            }
            client.bulk(bulkRequest, RequestOptions.DEFAULT);
        }

        log.info("Restore completed, data size is {}", count / 2);
    }
}
