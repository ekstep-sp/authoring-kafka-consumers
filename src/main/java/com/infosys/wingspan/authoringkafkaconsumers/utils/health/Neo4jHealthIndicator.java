package com.infosys.wingspan.authoringkafkaconsumers.utils.health;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.exceptions.SessionExpiredException;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Requires bean of class org.neo4j.driver.v1.Driver
 * /health endpoint must be enabled
 * Can be configured using management.health.custom.neo4j.enabled property defaults to true if missing
 */
@Component
@ConditionalOnBean(value = Driver.class)
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class)
@ConditionalOnProperty(prefix = "management.health.custom", name = "neo4j.enabled", havingValue = "true", matchIfMissing = true)
public class Neo4jHealthIndicator implements HealthIndicator {
	private static final Log logger = LogFactory.getLog(Neo4jHealthIndicator.class);
	static final String CYPHER = "RETURN 1 AS result";
	static final String MESSAGE_HEALTH_CHECK_FAILED = "Neo4j health check failed";
	static final String MESSAGE_SESSION_EXPIRED = "Neo4j session has expired, retrying one single time to retrieve server health.";

	private final Driver driver;

	public Neo4jHealthIndicator(Driver driver) {
		this.driver = driver;
	}

	@Override
	public Health health() {
		try {
			try {
				this.runHealthCheckQuery();
			} catch (SessionExpiredException var4) {
				// Retry configured for 1 Attempt in case of SessionExpiredException
				logger.warn(MESSAGE_SESSION_EXPIRED);
				this.runHealthCheckQuery();
			}
			return Health.up().build();
		} catch (Exception var5) {
			logger.error(MESSAGE_HEALTH_CHECK_FAILED);
			return Health.down().withDetail("error", var5.getClass().getName()).build();
		}
	}

	void runHealthCheckQuery() {
		Session session = this.driver.session(AccessMode.WRITE);
		Throwable var2 = null;
		try {
			session.run(CYPHER).consume();
		} catch (Throwable var13) {
			var2 = var13;
			throw var13;
		} finally {
			if (session != null) {
				if (var2 != null) {
					try {
						session.close();
					} catch (Throwable var12) {
						var2.addSuppressed(var12);
					}
				} else {
					session.close();
				}
			}
		}
	}
}
