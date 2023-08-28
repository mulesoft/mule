/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.retry;

import java.util.Optional;

import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

/**
 * Resolves a retry template.
 * 
 * @since 4.4.0, 4.3.1
 */
public interface RetryPolicyTemplateResolver {

  /**
   * Fetch retry policy template.
   * 
   * @param configuration from which the retry policy is fetched and resolved.
   * 
   * @return the resolved retry policy template.
   */
  RetryPolicyTemplate fetchRetryPolicyTemplate(Optional<ConfigurationInstance> configuration);
}
