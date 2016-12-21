/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.io.File;

/**
 * Default implementation of {@link PolicyTemplate}
 */
public class DefaultPolicyTemplate implements PolicyTemplate {

  private final String artifactId;
  private final PolicyTemplateDescriptor descriptor;
  private final ArtifactClassLoader policyClassLoader;

  /**
   * Creates a new policy template artifact
   *
   * @param artifactId artifact unique ID. Non empty.
   * @param descriptor describes the policy to create. Non null.
   * @param policyClassLoader classloader to use on this policy. Non null.
   */
  public DefaultPolicyTemplate(String artifactId, PolicyTemplateDescriptor descriptor, ArtifactClassLoader policyClassLoader) {
    this.artifactId = artifactId;
    this.descriptor = descriptor;
    this.policyClassLoader = policyClassLoader;
  }

  @Override
  public String getArtifactName() {
    return descriptor.getName();
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public PolicyTemplateDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public File[] getResourceFiles() {
    return descriptor.getConfigResourceFiles();
  }

  @Override
  public ArtifactClassLoader getArtifactClassLoader() {
    return policyClassLoader;
  }

  @Override
  public void dispose() {
    policyClassLoader.dispose();
  }

}
