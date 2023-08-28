/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
