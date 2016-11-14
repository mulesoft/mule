/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.artifact;

import static java.lang.String.format;
import org.mule.runtime.deployment.model.api.artifact.DependenciesProvider;
import org.mule.runtime.deployment.model.api.artifact.DependencyNotFoundException;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;

import java.io.File;

/**
 * Runtime implementation of {@link DependenciesProvider}, where its default behaviour is to fail every time it's asked
 * to resolve an artifact as every Mule application is self contained.
 *
 * @since 4.0
 */
public class DefaultDependenciesProvider implements DependenciesProvider {

  @Override
  public File resolve(BundleDescriptor bundleDescriptor) {
    throw new DependencyNotFoundException(format("Default implementation of DependenciesProvider cannot resolve dependencies. Dependency that trigger the exception is '%s'",
                                                 bundleDescriptor));
  }

}
