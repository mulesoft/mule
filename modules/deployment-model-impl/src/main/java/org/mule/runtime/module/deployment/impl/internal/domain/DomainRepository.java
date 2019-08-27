/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.domain;

import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorUtils;

/**
 * Provides access to {@link Domain} available on the container
 */
public interface DomainRepository {

  /**
   * Gets the domain matching given name.
   *
   * @param name domain name to find. Non empty.
   * @return a {@link Domain} corresponding to the given name or null is no such domain exists.
   *
   * @throws DomainNotFoundException if didn't find any compatible domain.
   */
  Domain getDomain(String name) throws DomainNotFoundException;

  boolean contains(String name);


  // TODO: Add exceptions:
  //  - Ambiguous
  //  - Incompatible?
  /**
   * Gets a domain compatible with the given bundle descriptor. The version must be compatible
   * (see {@link BundleDescriptorUtils#isCompatibleVersion(String, String)} for more information).
   *
   * @param descriptor Descriptor of the domain to find.
   * @return a {@link Domain} corresponding to the given descriptor or null is no such domain exists.
   * 
   * @throws DomainNotFoundException if didn't find any compatible domain.
   */
  Domain getCompatibleDomain(BundleDescriptor descriptor) throws DomainNotFoundException;

  /**
   * Checks if exists a domain compatible with the given bundle descriptor. The version must be compatible
   * (see {@link BundleDescriptorUtils#isCompatibleVersion(String, String)} for more information).
   *
   * @param descriptor Descriptor of the domain to find.
   * @return <tt>true</tt> if this repository contains a domain for the specified descriptor, or <tt>false</tt> otherwise.
   */
  boolean containsCompatible(BundleDescriptor descriptor);
}
