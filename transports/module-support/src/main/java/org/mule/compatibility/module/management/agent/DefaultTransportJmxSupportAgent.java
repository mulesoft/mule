/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.management.agent;

import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.DOMAIN;

import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.module.management.agent.AbstractJmxAgent;
import org.mule.runtime.module.management.agent.AbstractJmxAgentExtension;
import org.mule.runtime.module.management.agent.DefaultJmxSupportAgent;

public class DefaultTransportJmxSupportAgent extends DefaultJmxSupportAgent {

  @Override
  protected AbstractJmxAgent lookupJmxAgent(MuleRegistry registry) throws RegistrationException {
    // there must be only one jmx agent, so lookup by type instead
    ArtifactType artifactType = muleContext.getArtifactType();
    if (APP.equals(artifactType)) {
      return registry.lookupObject(TransportsJmxApplicationAgent.class);
    } else if (DOMAIN.equals(artifactType)) {
      return registry.lookupObject(TransportsJmxDomainAgent.class);
    } else {
      AbstractJmxAgent lookupObject = (AbstractJmxAgent) registry.lookupObject(AbstractJmxAgentExtension.class);
      if (lookupObject == null) {
        lookupObject = registry.lookupObject(AbstractTransportsJmxAgent.class);
      }
      return lookupObject;
    }
  }
}
