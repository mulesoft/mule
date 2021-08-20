/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.internal.artifact.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;

/**
 * Factory to create instances of {@link DescriptorLoaderRepository}.
 * 
 * @since 4.5
 */
public class DescriptorLoaderRepositoryFactory {

  private static final SpiServiceRegistry SERVICE_REGISTRY = new SpiServiceRegistry();

  /**
   * @return a newly created {@link DescriptorLoaderRepository} instance.
   */
  public DescriptorLoaderRepository createDescriptorLoaderRepository() {
    return new ServiceRegistryDescriptorLoaderRepository(SERVICE_REGISTRY);
  }
}
