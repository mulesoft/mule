/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;

/**
 * Creates {@link PolicyTemplate} instances
 */
public interface PolicyTemplateFactory {

  /**
   * Creates a new policy template artifact
   *
   * @param application class loader where the policy template's class loader will be included. Non null.
   * @param descriptor  describes how to build a policy template artifact. Non null
   * @return a {@link PolicyTemplate} artifact from the provided descriptor as a member of the region.
   * @throws PolicyTemplateCreationException when the artifact cannot be created
   */
  PolicyTemplate createArtifact(Application application, PolicyTemplateDescriptor descriptor);
}
