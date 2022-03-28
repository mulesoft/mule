/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.api.discoverer;


import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.artifact.activation.api.service.ServiceAssembly;
import org.mule.runtime.module.artifact.activation.api.service.ServiceResolutionError;

import java.util.List;

/**
 * Discovers the {@link ServiceProvider} available in the container.
 */
@NoImplement
@Deprecated
public interface ServiceProviderDiscoverer
    extends org.mule.runtime.module.artifact.activation.api.service.ServiceProviderDiscoverer {

  @Override
  List<ServiceAssembly> discover() throws ServiceResolutionError, org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
}
