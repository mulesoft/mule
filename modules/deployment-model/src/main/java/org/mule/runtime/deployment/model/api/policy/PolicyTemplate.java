/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.policy;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.api.Artifact;

import java.util.List;

/**
 * Represents a policy template artifact.
 *
 * @since 4.0
 */
@NoImplement
public interface PolicyTemplate extends Artifact<PolicyTemplateDescriptor> {

  /**
   * Disposes the artifact releasing any held resources
   */
  void dispose();

  /**
   * @return plugins deployed only inside the policy template
   */
  List<ArtifactPlugin> getArtifactPlugins();

  /**
   * @return plugins the policy depends on
   */
  List<ArtifactPlugin> getOwnArtifactPlugins();
}
