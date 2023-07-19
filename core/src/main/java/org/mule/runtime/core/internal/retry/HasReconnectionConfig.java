/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.retry;

import java.util.Optional;

/**
 * Contract interface for a component which may have a {@link ReconnectionConfig}
 *
 * @since 1.0
 */
public interface HasReconnectionConfig {

  Optional<ReconnectionConfig> getReconnectionConfig();
}
