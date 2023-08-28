/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.deployment.model.internal.policy.PolicyTemplateClassLoaderBuilder;

/**
 * Creates instances of {@link PolicyTemplateClassLoaderBuilder}
 */
public interface PolicyTemplateClassLoaderBuilderFactory {

  /**
   * Creates a new builder
   *
   * @return a new builder instance.
   */
  PolicyTemplateClassLoaderBuilder createArtifactClassLoaderBuilder();
}
