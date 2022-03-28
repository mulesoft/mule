/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.discoverer;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;

/**
 * Immutable implementation of {@link ServiceAssembly}
 *
 * @since 4.2
 */
@Deprecated
public class ImmutableServiceAssembly
    extends org.mule.runtime.module.artifact.activation.api.service.ImmutableServiceAssembly
    implements ServiceAssembly {

  public ImmutableServiceAssembly(String name, ServiceProvider serviceProvider, ClassLoader classLoader,
                                  Class<? extends Service> serviceContract) {
    super(name, serviceProvider, classLoader, serviceContract);
  }
}
