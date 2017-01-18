/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.api.policy;

import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Describes how to create a {@link PolicyTemplate} artifact
 *
 * @since 4.0
 */
public class PolicyTemplateDescriptor extends ArtifactDescriptor {

  public static final String DEFAULT_POLICY_CONFIGURATION_RESOURCE = "policy.xml";
  public static final String META_INF = "META-INF";
  public static final String MULE_POLICY_JSON = "mule-policy.json";

  private File[] configResourceFiles;
  private Set<ArtifactPluginDescriptor> plugins = new HashSet<>(0);

  /**
   * Creates a new descriptor for a named artifact
   *
   * @param name artifact name. Non empty.
   */
  public PolicyTemplateDescriptor(String name) {
    super(name);
  }

  public File[] getConfigResourceFiles() {
    return configResourceFiles;
  }

  public void setConfigResourceFiles(File[] configResourceFiles) {
    this.configResourceFiles = configResourceFiles;
  }

  public Set<ArtifactPluginDescriptor> getPlugins() {
    return plugins;
  }

  public void setPlugins(Set<ArtifactPluginDescriptor> plugins) {
    this.plugins = plugins;
  }
}
