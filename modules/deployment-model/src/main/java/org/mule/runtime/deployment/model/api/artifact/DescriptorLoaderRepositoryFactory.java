/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
