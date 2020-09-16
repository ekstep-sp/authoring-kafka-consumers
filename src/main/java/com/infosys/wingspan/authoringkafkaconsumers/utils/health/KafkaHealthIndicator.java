package com.infosys.wingspan.authoringkafkaconsumers.utils.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

	//if you have multiple instances, inject as Map<String, KafkaStreams>
	//Spring will map KafkaStreams instances by bean names present in context
	//so you can provide status details for each stream by name
	@Autowired
	KafkaAdmin kafkaAdmin;

	@Override
	public Health health() {
		try {
			Logger.getAnonymousLogger().info("Entering Health Check");
			AdminClient kafkaAdminClient = KafkaAdminClient.create(kafkaAdmin.getConfig());
			final DescribeClusterOptions describeClusterOptions = new DescribeClusterOptions().timeoutMs(1000);
			final DescribeClusterResult describeCluster = kafkaAdminClient.describeCluster(describeClusterOptions);
			final String clusterId = describeCluster.clusterId().get();
			final int nodeCount = describeCluster.nodes().get().size();
			return Health.up()
					.withDetail("clusterId", clusterId)
					.withDetail("nodeCount", nodeCount)
					.build();
		} catch (Exception e) {
			return Health.down()
					.withException(e)
					.build();
		}
	}

}
