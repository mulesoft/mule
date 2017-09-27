/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.api.policy;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;

/**
 * Describes how to create a {@link PolicyTemplate} artifact
 *
 * @since 4.0
 */
public class PolicyTemplateDescriptor extends ArtifactDescriptor {

  public static final String DEFAULT_POLICY_CONFIGURATION_RESOURCE = "policy.xml";
  public static final String META_INF = "META-INF";
  public static final String MULE_ARTIFACT = "mule-artifact";
  protected static final String POLICY_EXPORTED_PACKAGES_ERROR = "A policy template artifact cannot export packages";
  protected static final String POLICY_EXPORTED_RESOURCE_ERROR = "A policy template artifact cannot export resources";

  private Set<ArtifactPluginDescriptor> plugins = new HashSet<>(0);

  /**
   * Creates a new descriptor for a named artifact
   *
   * @param name artifact name. Non empty.
   */
  public PolicyTemplateDescriptor(String name) {
    super(name);
  }

  public PolicyTemplateDescriptor(String name, Optional<Properties> deploymentProperties) {
    super(name, deploymentProperties);
  }

  public Set<ArtifactPluginDescriptor> getPlugins() {
    return plugins;
  }

  public void setPlugins(Set<ArtifactPluginDescriptor> plugins) {
    this.plugins = plugins;
  }

  @Override
  public void setClassLoaderModel(ClassLoaderModel classLoaderModel) {
    checkArgument(classLoaderModel.getExportedPackages().isEmpty(), POLICY_EXPORTED_PACKAGES_ERROR);
    checkArgument(classLoaderModel.getExportedResources().isEmpty(), POLICY_EXPORTED_RESOURCE_ERROR);

    super.setClassLoaderModel(classLoaderModel);
  }
}
