/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import org.mule.runtime.module.artifact.descriptor.DescriptorLoader;

import java.util.Optional;

/**
 * Maintains the registered {@link DescriptorLoader}
 */
public interface DescriptorLoaderRepository {

  /**
   * Gets a descriptor loader from the repository
   *
   * @param id identifies the loader to obtain. Non empty.
   * @param loaderClass class of {@link DescriptorLoader} to search for. No null.
   * @param <T> type of descriptor loader to return
   * @returns an {@link Optional} loader of the given class and ID. It will be empty if there is no loader of that class
   *          registered with that ID.
   */
  <T extends DescriptorLoader> Optional<T> get(String id, Class<T> loaderClass);
}
