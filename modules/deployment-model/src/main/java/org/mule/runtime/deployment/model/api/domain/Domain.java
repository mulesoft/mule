/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.domain;

import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.application.Application;

/**
 * A domain is a deployable Artifact that contains shared resources for {@link Application}
 * <p/>
 * A domain can just consist of a set of jar libraries to share between the domain applications or it can also contain shared
 * resources such as connectors or other mule components.
 */
public interface Domain extends DeployableArtifact<DomainDescriptor> {

  /**
   * @return true if this domain has shared mule components, false if it doesn't
   */
  boolean containsSharedResources();

}
