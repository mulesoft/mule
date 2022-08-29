/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.descriptor;

import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

/**
 * Resolves the {@link DomainDescriptor} described by its name and the {@link BundleDescriptor}, wrapping the logic to obtain it.
 *
 * @since 4.5
 */
public interface DomainDescriptorResolver {

  /**
   * @return the default implementation of a {@link DomainDescriptorResolver}.
   */
  static DomainDescriptorResolver noDomainDescriptorResolver() {
    return (domainName, bundleDescriptor) -> null;
  }

  /**
   * Holds the logic to obtain a {@link DomainDescriptor} based on the given domain name and {@link BundleDescriptor}.
   *
   *
   * @param name             domain name.
   * @param bundleDescriptor the bundle descriptor of the domain to get the artifact descriptor for.
   * @return returns a {@link DomainDescriptor} corresponding to the given {@link BundleDescriptor}.
   */
  DomainDescriptor resolve(String name, BundleDescriptor bundleDescriptor) throws DomainDescriptorResolutionException;

}
