/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.artifact.internal.util.ServiceRegistryDescriptorLoaderRepository;

/**
 * Factory to create instances of {@link DescriptorLoaderRepository}.
 *
 * @since 4.5
 */
public class DescriptorLoaderRepositoryFactory {

  /**
   * @return a newly created {@link DescriptorLoaderRepository} instance.
   */
  public DescriptorLoaderRepository createDescriptorLoaderRepository() {
    return new ServiceRegistryDescriptorLoaderRepository();
  }
}
