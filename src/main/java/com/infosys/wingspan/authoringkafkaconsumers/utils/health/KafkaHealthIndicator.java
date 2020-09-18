package com.infosys.wingspan.authoringkafkaconsumers.utils.health;

import org.apache.kafka.clients.admin.*;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Requires bean of class org.apache.kafka.clients.admin.AdminClient
 * /health endpoint must be enabled
 * Can be configured using management.health.custom.kafka.enabled property defaults to true if missing
 */
@Component
@ConditionalOnBean(value = AdminClient.class)
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class)
@ConditionalOnProperty(prefix = "management.health.custom", name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaHealthIndicator implements HealthIndicator {

	final AdminClient kafkaAdminClient;

	public KafkaHealthIndicator(AdminClient kafkaAdminClient) {
		this.kafkaAdminClient = kafkaAdminClient;
	}

	@Override
	public Health health() {
		try {
			final DescribeClusterOptions describeClusterOptions = new DescribeClusterOptions().timeoutMs(1000);
			final DescribeClusterResult describeCluster = kafkaAdminClient.describeCluster(describeClusterOptions);
			// Fetching clusterId from kafka endpoint to verify connection
			describeCluster.clusterId().get();
			return Health.up().build();
		} catch (Exception e) {
			return Health.down()
					.withDetail("error", e.getClass().getName())
					.build();
		}
	}

}
