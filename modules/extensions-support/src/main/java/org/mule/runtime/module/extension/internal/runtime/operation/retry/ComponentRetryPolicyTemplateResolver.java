/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.retry;

import java.util.Optional;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

/**
 * Resolves a retry policy template for a component processor. If the component processor has a retry policy template (for example
 * if it is overridden in the operation), it returns it. Otherwise it retrieves the policy template from the operation.
 *
 * @since 4.4.0, 4.3.1
 */
public class ComponentRetryPolicyTemplateResolver implements RetryPolicyTemplateResolver {

  private final RetryPolicyTemplate componentRetryPolicyTemplate;
  private final ConnectionManagerAdapter connectionManager;
  private final RetryPolicyTemplate fallbackRetryPolicyTemplate = new NoRetryPolicyTemplate();

  public ComponentRetryPolicyTemplateResolver(RetryPolicyTemplate retryPolicyTemplate,
                                              ConnectionManagerAdapter connectionManager) {
    this.componentRetryPolicyTemplate = retryPolicyTemplate;
    this.connectionManager = connectionManager;
  }

  public RetryPolicyTemplate fetchRetryPolicyTemplate(Optional<ConfigurationInstance> configuration) {
    // No need to take into account the deployment model for retries in the a processor. That only applies
    // to reconnection attempts during deployment. For processors it is only needed to use
    // the retry policy defined in the processor or the one defined in the reconnection config
    // without further ado. Fails on deployment property shouldn't be taken into account here.
    // The previous behavior was removed (MULE-19342).
    if (componentRetryPolicyTemplate == null) {
      return resolveRetryTemplateFromConnectionConfig(configuration);
    }

    return componentRetryPolicyTemplate;
  }

  private RetryPolicyTemplate resolveRetryTemplateFromConnectionConfig(Optional<ConfigurationInstance> configuration) {
    return configuration
        .map(config -> config.getConnectionProvider().orElse(null))
        .map(provider -> connectionManager.getRetryTemplateFor((ConnectionProvider<? extends Object>) provider))
        .orElse(fallbackRetryPolicyTemplate);
  }

}
