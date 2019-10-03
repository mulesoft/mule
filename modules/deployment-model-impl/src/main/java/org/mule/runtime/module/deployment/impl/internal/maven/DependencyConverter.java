/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.util.stream.Collectors.toList;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;

import java.util.HashMap;
import java.util.Map;

public class DependencyConverter {

  private Map<BundleDescriptor, BundleDependency> cache = new HashMap<>();

  public BundleDependency convert(org.mule.maven.client.api.model.BundleDependency mavenBundleDependency) {
    BundleDependency bundleDependency = cache.get(mavenBundleDependency.getDescriptor());
    if ((bundleDependency != null && bundleDependency.getBundleUri() != null)
        || (bundleDependency != null && mavenBundleDependency.getBundleUri() == null)) {
      return bundleDependency;
    }
    BundleDependency.Builder builder = new BundleDependency.Builder()
        .setScope(BundleScope.valueOf(mavenBundleDependency.getScope().name()))
        .setBundleUri(mavenBundleDependency.getBundleUri())
        .setTransitiveDependencies(mavenBundleDependency.getTransitiveDependencies().stream()
            .filter(transitiveDependency -> !org.mule.maven.client.api.model.BundleScope.PROVIDED
                .equals(transitiveDependency.getScope()))
            .map(this::convert)
            .collect(toList()))
        .setDescriptor(convertBundleDescriptor(mavenBundleDependency.getDescriptor()));
    bundleDependency = builder.build();
    cache.put(mavenBundleDependency.getDescriptor(), bundleDependency);
    return bundleDependency;
  }

  private org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor convertBundleDescriptor(org.mule.maven.client.api.model.BundleDescriptor descriptor) {
    org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.Builder builder =
        new org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.Builder().setGroupId(descriptor.getGroupId())
            .setArtifactId(descriptor.getArtifactId())
            .setVersion(descriptor.getVersion())
            .setBaseVersion(descriptor.getBaseVersion())
            .setType(descriptor.getType());
    descriptor.getClassifier().ifPresent(builder::setClassifier);
    return builder.build();
  }

}
