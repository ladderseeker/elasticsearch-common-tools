package com.product.tool.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.product.tool.entity.ActionInput;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;

/**
 * @author XIAO JIN
 * @date 2023/09/05 17:16
 */
@Slf4j
public class InputOps {

    public ActionInput buildActionInput(String[] args) {
        Options options = buildOptions();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("config")) {
                log.info("Parse from config file {}", cmd.getOptionValue("config"));

                String configFilePath = cmd.getOptionValue("config");
                File configFile = new File(configFilePath);

                if (!configFile.exists()) {
                    log.error("The config file " + configFilePath + " not exists, please check the config path.");
                    System.exit(1);
                }
                return objectMapper.readValue(configFile, ActionInput.class);
            } else {
                if (!cmd.hasOption("a") ||
                        !cmd.hasOption("e") ||
                        !cmd.hasOption("f") ||
                        !cmd.hasOption("i")) {
                    log.error("Missing required arguments. Please add all the required options");
                    printUsage(options);
                    System.exit(1);
                }

                // Required
                String action = cmd.getOptionValue("a");
                String endpoint = cmd.getOptionValue("e");
                String filePath = cmd.getOptionValue("f");
                String indexName = cmd.getOptionValue("i");

                // Optional, may be null
                String username = cmd.getOptionValue("u");
                String password = cmd.getOptionValue("p");

                String dslQueryStr = cmd.getOptionValue("q");

                JsonNode dslQuery = dslQueryStr == null ? null : objectMapper.readTree(dslQueryStr);

                return new ActionInput(action, endpoint, username, password, indexName, filePath, dslQuery);
            }
        } catch (ParseException parseException) {
            log.error("Parse arg exception, please check the input arg format", parseException);
            printUsage(options);
            System.exit(1);
        } catch (IOException e) {
            log.error("parse json config file fail, please check the config json file.", e);
            printUsage(options);
            System.exit(1);
        }
        return null;
    }

    private Options buildOptions() {
        Options options = new Options();

        Option configOption = new Option("c", "config", true, "Config file path, use config file to determine action. (Other option would not work)");
        configOption.setRequired(false);
        options.addOption(configOption);

        Option actionOption = new Option("a", "action", true, "Action to perform (backup or restore)");
        actionOption.setRequired(false);
        options.addOption(actionOption);

        Option endpointOption = new Option("e", "endpoint", true, "Elasticsearch endpoint (e.g., <http://localhost:9200>)");
        endpointOption.setRequired(false);
        options.addOption(endpointOption);

        Option filePathOption = new Option("f", "filePath", true, "File path for backup or restore");
        filePathOption.setRequired(false);
        options.addOption(filePathOption);

        Option indexOption = new Option("i", "indexName", true, "Index name");
        indexOption.setRequired(false);
        options.addOption(indexOption);

        Option dslQueryOption = new Option("q", "dslQuery", true, "DSL Query for backup");
        dslQueryOption.setRequired(false);
        options.addOption(dslQueryOption);

        Option usernameOption = new Option("u", "username", true, "Elasticsearch username for authentication");
        usernameOption.setRequired(false);
        options.addOption(usernameOption);

        Option passwordOption = new Option("p", "password", true, "Elasticsearch password for authentication");
        passwordOption.setRequired(false);
        options.addOption(passwordOption);

        return options;
    }

    private void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Elasticsearch Backup and Restore Utility", options);
    }
}

