/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
