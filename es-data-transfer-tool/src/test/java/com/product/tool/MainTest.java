package com.product.tool;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class MainTest {

    @Test
    void testConfigFile() throws IOException {
        String[] args = new String[]{"--config", "./doc/transfer-config.json"};
        Main.main(args);
    }

    @Test
    void testArgsBackup() throws IOException {
        String[] args = new String[]{
                "--action", "backup",
                "-e", "http://localhost:9200",
                "-f", "./doc/my_index-backup.json",
                "-i", "a-tps-alarm-inter-2023.01.11-02.07",
                "-q", "{\"query\":{\"range\":{\"structTime\":{\"gte\":\"2023-01-11T00:00:00.000+08:00\",\"lte\":\"2023-01-21T00:00:00.000+08:00\"}}}}}"
        };
        Main.main(args);
    }
}