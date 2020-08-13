/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/*
© 2017 - 2019 Infosys Limited, Bangalore, India. All Rights Reserved. 
Version: 1.10

Except for any free or open source software components embedded in this Infosys proprietary software program (“Program”),
this Program is protected by copyright laws, international treaties and other pending or existing intellectual property rights in India,
the United States and other countries. Except as expressly permitted, any unauthorized reproduction, storage, transmission in any form or
by any means (including without limitation electronic, mechanical, printing, photocopying, recording or otherwise), or any distribution of 
this Program, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible
under the law.

Highly Confidential
 
*/
package com.infosys.wingspan.authoringkafkaconsumers.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchIndexerAsync {

    @Autowired
    private RestHighLevelClient esClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void indexSearchErrors(String message, Exception exception) {

        try {
            List<Map<String, Object>> data = objectMapper.readValue(message, new TypeReference<List<Map<String, Object>>>() {
            });
            for (Map<String, Object> errObj : data) {
                Map<String, Object> indexDoc = new HashMap<>();
                Map<String, Object> dataValue = (Map<String, Object>) errObj.getOrDefault("transactionData", new HashMap<>());
                String neo4jNodeId = errObj.getOrDefault("nodeGraphId", "").toString();
                String lex_id = (String) errObj.getOrDefault("nodeUniqueId", "");
                String opType = (String) errObj.getOrDefault("operationType", "ERROR");
                String rootOrg = (String) errObj.getOrDefault("graphId", "");
                String locale = (String) errObj.getOrDefault("locale", "default_locale");
                String nodeType = (String) errObj.getOrDefault("nodeType", "DEFAULT_VALUE");
                String samzaError = exception.getMessage();
                StackTraceElement[] stackTrace = exception.getStackTrace();

                indexDoc.put("transaction_data", dataValue);
                indexDoc.put("neo4j_node_id", neo4jNodeId);
                indexDoc.put("identifier", lex_id);
                indexDoc.put("operation_type", opType);
                indexDoc.put("neo4j_label", rootOrg);
                indexDoc.put("locale", locale);
                indexDoc.put("node_type", nodeType);
                indexDoc.put("exception_message", samzaError);
                indexDoc.put("exception_stack_trace", stackTrace);

                IndexRequest indexRequest = new IndexRequest();
                indexRequest.index("search_error_index");
                indexRequest.type("searchresources");
                indexRequest.source(indexDoc);
                RestStatus status = esClient.index(indexRequest, RequestOptions.DEFAULT).status();
                int x = status.getStatus();
                if (!(x >= 200 && x < 300)) {
                    throw new Exception(status.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async
    public void indexSearchErrors(String message, List<Map<String, Object>> failures){
        try {
            List<Map<String, Object>> data = objectMapper.readValue(message, new TypeReference<List<Map<String, Object>>>() {
            });
            for (Map<String, Object> errObj : data) {
                Map<String, Object> indexDoc = new HashMap<>();
                Map<String, Object> dataValue = (Map<String, Object>) errObj.getOrDefault("transactionData", new HashMap<>());
                String neo4jNodeId = errObj.getOrDefault("nodeGraphId", "").toString();
                String lex_id = (String) errObj.getOrDefault("nodeUniqueId", "");
                String opType = (String) errObj.getOrDefault("operationType", "ERROR");
                String rootOrg = (String) errObj.getOrDefault("graphId", "");
                String locale = (String) errObj.getOrDefault("locale", "default_locale");
                String nodeType = (String) errObj.getOrDefault("nodeType", "DEFAULT_VALUE");

                indexDoc.put("transaction_data", dataValue);
                indexDoc.put("neo4j_node_id", neo4jNodeId);
                indexDoc.put("identifier", lex_id);
                indexDoc.put("operation_type", opType);
                indexDoc.put("neo4j_label", rootOrg);
                indexDoc.put("locale", locale);
                indexDoc.put("node_type", nodeType);
                indexDoc.put("exception_message", failures);

                IndexRequest indexRequest = new IndexRequest();
                indexRequest.index("search_error_index");
                indexRequest.type("searchresources");
                indexRequest.source(indexDoc);
                RestStatus status = esClient.index(indexRequest, RequestOptions.DEFAULT).status();
                int x = status.getStatus();
                if (!(x >= 200 && x < 300)) {
                    throw new Exception(status.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
