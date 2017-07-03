/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.api.artifact.ServiceDiscoverer;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.RegistrationException;

import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Default implementation for {@link ServiceDiscoverer}.
 * 
 * @since 4.0
 */
public class DefaultServiceDiscoverer implements ServiceDiscoverer {

  @Inject
  private MuleContext muleContext;

  @Override
  public <T> Optional<T> lookup(Class<T> serviceType) {
    try {
      return of(muleContext.getRegistry().lookupObject(serviceType));
    } catch (RegistrationException e) {
      return empty();
    }
  }

  @Override
  public <T> Collection<T> lookupAll(Class<T> serviceType) {
    return muleContext.getRegistry().lookupObjects(serviceType);
  }
}
