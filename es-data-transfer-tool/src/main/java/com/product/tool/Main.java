package com.product.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.product.tool.entity.ActionInput;
import com.product.tool.ops.ElasticsearchOps;
import com.product.tool.ops.InputOps;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;

import java.io.*;
import java.util.Objects;

/**
 * @author XIAO JIN
 * @date 2023/08/30 16:53
 */
@Slf4j
public class Main {

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();

        InputOps inputOps = new InputOps();
        ActionInput actionInput = inputOps.buildActionInput(args);

        doAction(actionInput);

        long endTime = System.currentTimeMillis();
        log.info("Elapsed time: " + (endTime - startTime) + " ms");
    }

    private static void doAction(ActionInput actionInput) throws IOException {
        String action = actionInput.getAction();
        String endpoint = actionInput.getEndpoint();
        String username = actionInput.getUsername();
        String password = actionInput.getPassword();
        String indexName = actionInput.getIndexName();
        String filePath = actionInput.getFilePath();
        JsonNode dslQuery = actionInput.getDslQuery();


        ElasticsearchOps esOps = new ElasticsearchOps();
        RestHighLevelClient client = null;
        try {
            if (!Objects.isNull(username) && !Objects.isNull(password) && !username.isEmpty() && !password.isEmpty()) {
                client = esOps.getClient(username, password, endpoint);
            } else {
                client = esOps.getClient(endpoint);
            }

            GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
            boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);

            if ("backup".equalsIgnoreCase(action)) {
                if (!exists) {
                    log.error("Index to backup does not exist.");
                    System.exit(1);
                }

                File file = new File(filePath);
                if (file.exists()) {
                    log.error("The file " + filePath + " already exists. Aborting backup.");
                    System.exit(1);
                }

                esOps.backupData(client, indexName, filePath, dslQuery);
            } else if ("restore".equalsIgnoreCase(action)) {
                if (exists) {
                    log.error("Index to restore already exists. Aborting restore.");
                    System.exit(1);
                }

                File backupFile = new File(filePath);
                if (!backupFile.exists() || !backupFile.canRead()) {
                    log.error("Backup file does not exist or is not readable.");
                    System.exit(1);
                }

                esOps.restoreData(client, indexName, filePath);
            } else {
                log.error("Invalid action. Use 'backup' or 'restore'.");
                System.exit(1);
            }

        } catch (Exception e) {
            log.error("An error occurred: ", e);
            System.exit(1);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
}
