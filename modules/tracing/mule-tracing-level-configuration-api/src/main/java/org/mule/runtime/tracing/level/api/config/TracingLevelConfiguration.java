package org.mule.runtime.tracing.level.api.config;

/**
 * Allows to configure the desired tracing level
 *
 * @since 4.6.0
 */
public interface TracingLevelConfiguration {

  TracingLevel getTracingLevel();
}
