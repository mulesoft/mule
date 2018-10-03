/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.util.stream.Collectors.toList;
import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Builder for a {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel} responsible of resolving
 * dependencies when light weight packaging is used for an artifact.
 *
 * @since 4.2.0
 */
public class LightweightClassLoaderModelBuilder extends ArtifactClassLoaderModelBuilder {

  private MavenClient mavenClient;

  public LightweightClassLoaderModelBuilder(File artifactFolder,
                                            MavenClient mavenClient) {
    super(artifactFolder);
    this.mavenClient = mavenClient;
  }

  @Override
  protected List<URI> processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency) {
    return resolveDependencies(bundleDependency.getAdditionalDependencies()).stream()
        .map(org.mule.maven.client.api.model.BundleDependency::getBundleUri).collect(toList());
  }

  //TODO: MULE-15768
  private List<org.mule.maven.client.api.model.BundleDependency> resolveDependencies(Set<BundleDependency> additionalDependencies) {
    return additionalDependencies.stream()
        .map(dependency -> dependency.getDescriptor())
        .map(descriptor -> new org.mule.maven.client.api.model.BundleDescriptor.Builder()
            .setGroupId(descriptor.getGroupId())
            .setArtifactId(descriptor.getArtifactId())
            .setVersion(descriptor.getVersion())
            .setType(descriptor.getType())
            .setClassifier(descriptor.getClassifier().orElse(null)).build())
        .map(bundleDescriptor -> mavenClient.resolveBundleDescriptor(bundleDescriptor))
        .collect(toList());
  }
}
