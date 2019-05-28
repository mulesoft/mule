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
   * Gets a domain compatible with the given bundle descriptor. The version must be compatible
   * (see {@link BundleDescriptorUtils#isCompatibleVersion(String, String)} for more information).
   *
   * @param descriptor Descriptor of the domain to find.
   * @return a {@link Domain} corresponding to the given descriptor or null is no such domain exists.
   */
  Domain getDomain(BundleDescriptor descriptor) throws DomainNotFoundException, IncompatibleDomainVersionException;

  /**
   * Gets a domain which name matches exactly with the given parameter.
   *
   * @param name Name of the domain to find.
   * @return a {@link} corresponding to the given name or null if no such domain exists.
   */
  Domain getDomain(String name) throws DomainNotFoundException;

  /**
   * Checks if exists a domain compatible with the given bundle descriptor. The version must be compatible
   * (see {@link BundleDescriptorUtils#isCompatibleVersion(String, String)} for more information).
   *
   * @param descriptor Descriptor of the domain to find.
   * @return <tt>true</tt> if this repository contains a domain for the specified descriptor, or <tt>false</tt> otherwise.
   */
  boolean contains(BundleDescriptor descriptor);

  /**
   * Checks if exists a domain which name matches exactly with the given parameter.
   *
   * @param name Name of the domain to find.
   * @return <tt>true</tt> if this repository contains a domain with the specified name, or <tt>false</tt> otherwise.
   */
  boolean contains(String name);
}
