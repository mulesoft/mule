/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.domain;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.application.Application;

/**
 * A domain is a deployable Artifact that contains shared resources for {@link Application}
 * <p/>
 * A domain can just consist of a set of jar libraries to share between the domain applications or it can also contain shared
 * resources such as connectors or other mule components.
 */
@NoImplement
public interface Domain extends DeployableArtifact<org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor> {

  /**
   * @return true if this domain has shared mule components, false if it doesn't
   */
  boolean containsSharedResources();

}
