/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
   * @param descriptor describes how to build a policy template artifact. Non null
   * @return a {@link PolicyTemplate} artifact from the provided descriptor as a member of the region.
   * @throws PolicyTemplateCreationException when the artifact cannot be created
   */
  PolicyTemplate createArtifact(Application application, PolicyTemplateDescriptor descriptor);
}
