/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.impl.internal.artifact.DeployableArtifactWrapper;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;

import java.io.IOException;

/**
 * Domain wrapper used to notify domain factory that a domain has been disposed or started.
 */
public class DomainWrapper extends DeployableArtifactWrapper<Domain, DomainDescriptor> implements Domain {

  private final DefaultDomainFactory domainFactory;

  protected DomainWrapper(final Domain delegate, final DefaultDomainFactory domainFactory) throws IOException {
    super(delegate);
    this.domainFactory = domainFactory;
  }

  @Override
  public boolean containsSharedResources() {
    return getDelegate().containsSharedResources();
  }

  @Override
  public MuleContext getMuleContext() {
    return getDelegate().getMuleContext();
  }

  @Override
  public void dispose() {
    try {
      getDelegate().dispose();
    } finally {
      domainFactory.dispose(this);
    }
  }
}
