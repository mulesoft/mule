/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.artifact.descriptor.api;

import org.mule.api.annotation.NoImplement;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Describes a dependency on a bundle.
 *
 * @since 4.9
 */
@NoImplement
public interface BundleDependency {

  BundleScope getScope();

  BundleDescriptor getDescriptor();

  URI getBundleUri();

  List<? extends BundleDependency> getAdditionalDependenciesList();

  List<? extends BundleDependency> getTransitiveDependenciesList();

  Set<String> getPackages();

  Set<String> getResources();

  // /**
  // * Builder for creating a {@link BundleDependency}
  // */
  // interface Builder {
  //
  // /**
  // * This is the descriptor of the bundle.
  // *
  // * @param descriptor the version of the bundle. Cannot be null or empty.
  // * @return the builder
  // */
  // Builder setDescriptor(BundleDescriptor descriptor);
  //
  // /**
  // * Sets the scope of the bundle.
  // *
  // * @param scope scope of the bundle. Non null
  // * @return the builder
  // */
  // Builder setScope(BundleScope scope);
  //
  // Builder setBundleUri(URI bundleUri);
  //
  // Builder setAdditionalDependencies(List<BundleDependency> additionalDependencies);
  //
  // Builder setTransitiveDependencies(List<BundleDependency> transitiveDependencies);
  //
  // Builder setPackages(Set<String> packages);
  //
  // Builder setResources(Set<String> resources);
  //
  // /**
  // * @return a {@code BundleDescriptor} with the previous provided parameters to the builder.
  // */
  // BundleDependency build();
  //
  // }

}
