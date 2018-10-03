/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenBundleDescriptorLoader.getBundleDescriptor;
import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.net.MalformedURLException;
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
  private DeployableArtifactDescriptor deployableArtifactDescriptor;
  private BundleDescriptor artifactBundleDescriptor;

  public LightweightClassLoaderModelBuilder(File artifactFolder,
                                            MavenClient mavenClient) {
    super(artifactFolder);
    this.artifactBundleDescriptor = getBundleDescriptor(artifactFolder);
    this.mavenClient = mavenClient;
  }

  @Override
  public void includeAdditionalPluginDependencies() {
    if (deployableArtifactDescriptor != null) {
      deployableArtifactDescriptor.getClassLoaderModel().getDependencies().stream()
          .filter(bundleDescriptor -> bundleDescriptor.getDescriptor().isPlugin())
          .filter(bundleDescriptor -> bundleDescriptor.getDescriptor().getGroupId()
              .equals(this.artifactBundleDescriptor.getGroupId())
              && bundleDescriptor.getDescriptor().getArtifactId().equals(this.artifactBundleDescriptor.getArtifactId()))
          .filter(bundleDependency -> bundleDependency.getAdditionalDependencies() != null
              && !bundleDependency.getAdditionalDependencies().isEmpty())
          .forEach(bundleDependency -> resolveDependencies(bundleDependency.getAdditionalDependencies()).stream()
              .forEach(additionalPluginDependency -> {
                try {
                  containing(additionalPluginDependency.getBundleUri().toURL());
                } catch (MalformedURLException e) {
                  throw new ArtifactDescriptorCreateException(
                                                              format("There was an exception obtaining the URL for the artifact [%s], file [%s]",
                                                                     artifactFolder.getAbsolutePath(),
                                                                     additionalPluginDependency.getBundleUri()),
                                                              e);
                }
              }));
    }
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

  public void setDeployableArtifactDescriptor(DeployableArtifactDescriptor deployableArtifactDescriptor) {
    this.deployableArtifactDescriptor = deployableArtifactDescriptor;
  }

}
