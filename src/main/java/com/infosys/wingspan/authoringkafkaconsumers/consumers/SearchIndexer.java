/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/*
  © 2017 - 2019 Infosys Limited, Bangalore, India. All Rights Reserved.
  Version: 1.10
  <p>
  Except for any free or open source software components embedded in this Infosys proprietary software program (“Program”),
  this Program is protected by copyright laws, international treaties and other pending or existing intellectual property rights in India,
  the United States and other countries. Except as expressly permitted, any unauthorized reproduction, storage, transmission in any form or
  by any means (including without limitation electronic, mechanical, printing, photocopying, recording or otherwise), or any distribution of
  this Program, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible
  under the law.
  <p>
  Highly Confidential
 */
package com.infosys.wingspan.authoringkafkaconsumers.consumers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.wingspan.authoringkafkaconsumers.consumers.impl.SearchIndexerImpl;
import com.infosys.wingspan.authoringkafkaconsumers.utils.ElasticsearchIndexerAsync;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SearchIndexer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ElasticsearchIndexerAsync elasticsearchIndexerAsync;

    @Value("${infosys.spring.kafka.search.indexer.error.output.topic}")
    String searchIndexerErrorTopic;

    @Autowired
    private SearchIndexerImpl searchIndexerImpl;

    private ObjectMapper objectMapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(SearchIndexer.class);

    private String searchIndexerErrorTopicKey = "search-indexer-error";

    @SuppressWarnings("unchecked")
    @KafkaListener(clientIdPrefix = "searcher-indexer", groupId = "new-es-replication-group", topics = "learning-graph-events")
    public void listenSearch(ConsumerRecord<?, ?> consumerRecord, Acknowledgment acknowledgment) {
        UUID uuid = UUID.randomUUID();

        String message = String.valueOf(consumerRecord.value());

        String logMsg = "" + consumerRecord.partition() + "-" + consumerRecord.offset();
        logger.info(uuid + "->" + "SEARCH INDEXER START - " + logMsg);

        List<Map<String, Object>> failures;
        try {
            List<Map<String, Object>> data = objectMapper.readValue(message, new TypeReference<List<Map<String, Object>>>() {});

            failures = searchIndexerImpl.processMessageEnvelope(data, uuid);

            if (!failures.isEmpty()) {
                kafkaTemplate.send(searchIndexerErrorTopic, searchIndexerErrorTopicKey, objectMapper.writeValueAsString(failures));
                logger.error(uuid + "->" + "SEARCH INDEXER FAILURE - " + logMsg + "-" + failures.size());
                elasticsearchIndexerAsync.indexSearchErrors(message, failures);
            } else
                logger.info(uuid + "->" + "SEARCH INDEXER SUCCESS - " + logMsg);
        } catch (Exception e) {
            e.printStackTrace();
            kafkaTemplate.send(searchIndexerErrorTopic, searchIndexerErrorTopicKey, message + "--->" + e.getMessage());
            logger.error(uuid + "->" + "SEARCH INDEXER FATAL - " + logMsg);
            elasticsearchIndexerAsync.indexSearchErrors(message, e);
        }

        acknowledgment.acknowledge();
    }
}
