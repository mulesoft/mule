/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.descriptor;

import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;

import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoader;

import java.util.stream.Stream;

/**
 * Provides utilities for the Runtime to discover {@link BundleDescriptorLoader} and {@link ClassLoaderConfigurationLoader}
 * implementations.
 * 
 * @since 4.5
 */
public final class DescriptorLoaderUtils {

  private DescriptorLoaderUtils() {
    // Nothing to do
  }

  /**
   * Looks up implementations of {@link ComponentBuildingDefinitionProvider} with the provided classloader.
   * 
   * @param classLoader the classlaoder to use for loading the services through SPI.
   * @return the discovered {@link ComponentBuildingDefinitionProvider}.
   */
  public static final <T extends DescriptorLoader> Stream<T> lookupComponentBuildingDefinitionProviders(Class<T> descriptorLoaderClass) {
    return stream(((Iterable<T>) () -> load(descriptorLoaderClass,
                                            DescriptorLoaderUtils.class.getClassLoader())
                                                .iterator())
                                                    .spliterator(),
                  false);
  }

}
