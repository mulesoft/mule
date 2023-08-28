/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.api.descriptor;

import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

/**
 * Resolves the {@link DomainDescriptor} corresponding to the given name and {@link BundleDescriptor}, wrapping the logic to
 * obtain it.
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
