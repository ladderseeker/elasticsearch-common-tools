package com.product.tool.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

/**
 * @author XIAO JIN
 * @date 2023/09/05 17:01
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ActionInput {

    private String action;

    private String endpoint;

    private String username;

    private String password;

    private String indexName;

    private String filePath;

    private JsonNode dslQuery;

}
