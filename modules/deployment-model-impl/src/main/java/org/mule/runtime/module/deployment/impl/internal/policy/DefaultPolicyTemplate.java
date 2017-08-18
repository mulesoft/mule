/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.io.File;
import java.util.List;

/**
 * Default implementation of {@link PolicyTemplate}
 */
public class DefaultPolicyTemplate implements PolicyTemplate {

  private final String artifactId;
  private final PolicyTemplateDescriptor descriptor;
  private final ArtifactClassLoader policyClassLoader;
  private final List<ArtifactPlugin> artifactPlugins;

  /**
   * Creates a new policy template artifact
   *
   * @param artifactId artifact unique ID. Non empty.
   * @param descriptor describes the policy to create. Non null.
   * @param policyClassLoader classloader to use on this policy. Non null.
   * @param artifactPlugins artifact plugins deployed inside the policy. Non null.
   */
  public DefaultPolicyTemplate(String artifactId, PolicyTemplateDescriptor descriptor, ArtifactClassLoader policyClassLoader,
                               List<ArtifactPlugin> artifactPlugins) {
    checkArgument(!isEmpty(artifactId), "artifactId cannot be empty");
    checkArgument(descriptor != null, "descriptor cannot be null");
    checkArgument(policyClassLoader != null, "policyClassLoader cannot be null");
    checkArgument(artifactPlugins != null, "artifactPlugins cannot be null");

    this.artifactId = artifactId;
    this.descriptor = descriptor;
    this.policyClassLoader = policyClassLoader;
    this.artifactPlugins = artifactPlugins;
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
    return new File[0];
  }

  @Override
  public ArtifactClassLoader getArtifactClassLoader() {
    return policyClassLoader;
  }

  @Override
  public void dispose() {
    policyClassLoader.dispose();
  }

  public List<ArtifactPlugin> getArtifactPlugins() {
    return artifactPlugins;
  }
}
