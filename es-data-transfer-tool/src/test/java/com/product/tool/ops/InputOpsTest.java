package com.product.tool.ops;

import com.product.tool.entity.ActionInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputOpsTest {

    InputOps inputOps = new InputOps();

    @Test
    void estBuildActionInputWithConfig() {
        String[] args = new String[]{"--config", "./doc/transfer-config.json"};
        ActionInput actionInput = inputOps.buildActionInput(args);
        System.out.println(actionInput);
        assertNotNull(actionInput.getDslQuery());
    }

    @Test
    void testBuildActionInputWithArgs() {
        String[] args = new String[]{
                "--action", "backup",
                "-e", " http://localhost:9200",
                "-f", "./doc/my_index-backup.json",
                "-i", "a-tps-alarm-inter-2023.01.11-02.07",
                "-q", "{\"query\":{\"range\":{\"structTime\":{\"gte\":\"2023-01-11T00:10:00.000+08:00\",\"lte\":\"2023-02-07T22:50:00.000+08:00\"}}}}}"
        };
        ActionInput actionInput = inputOps.buildActionInput(args);
        System.out.println(actionInput);
        assertNotNull(actionInput.getDslQuery());
    }

    @Test
    void testBuildActionInputWithArgsNoDsl() {
        String[] args = new String[]{
                "--action", "backup",
                "-e", " http://localhost:9200",
                "-f", "./doc/my_index-backup.json",
                "-i", "a-tps-alarm-inter-2023.01.11-02.07"
        };
        ActionInput actionInput = inputOps.buildActionInput(args);
        System.out.println(actionInput);
        assertNull(actionInput.getDslQuery());
    }
}