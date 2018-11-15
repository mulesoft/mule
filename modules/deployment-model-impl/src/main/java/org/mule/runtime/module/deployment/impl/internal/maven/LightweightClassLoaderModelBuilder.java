/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Builder for a {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel} responsible of resolving dependencies
 * when light weight packaging is used for an artifact.
 *
 * @since 4.1.5
 */
public class LightweightClassLoaderModelBuilder extends ArtifactClassLoaderModelBuilder {

  private MavenClient mavenClient;
  private File temporaryFolder;

  public LightweightClassLoaderModelBuilder(File artifactFolder,
                                            MavenClient mavenClient,
                                            File temporaryFolder) {
    super(artifactFolder);
    this.mavenClient = mavenClient;
  }

  @Override
  protected List<URI> processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency) {
    return resolveDependencies(bundleDependency.getAdditionalDependencies()).stream()
        .map(org.mule.maven.client.api.model.BundleDependency::getBundleUri).collect(toList());
  }

  // TODO: MULE-15768
  //TODO: MULE-16026 all the following logic is duplicated from the mule-packager.
  private List<org.mule.maven.client.api.model.BundleDependency> resolveDependencies(Set<BundleDependency> additionalDependencies) {
    List<org.mule.maven.client.api.model.BundleDependency> resolvedAdditionalDependencies = new ArrayList<>();
    additionalDependencies.stream()
        .map(BundleDependency::getDescriptor)
        .map(descriptor -> new org.mule.maven.client.api.model.BundleDescriptor.Builder()
            .setGroupId(descriptor.getGroupId())
            .setArtifactId(descriptor.getArtifactId())
            .setVersion(descriptor.getVersion())
            .setType(descriptor.getType())
            .setClassifier(descriptor.getClassifier().orElse(null)).build())
        .forEach(bundleDescriptor -> {
          resolveDependency(bundleDescriptor)
              .forEach(additionalDep -> updateAdditionalDependencyOrFail(resolvedAdditionalDependencies, additionalDep));
        });
    return resolvedAdditionalDependencies;
  }

  private List<org.mule.maven.client.api.model.BundleDependency> resolveDependency(BundleDescriptor dependencyBundleDescriptor) {
    List<org.mule.maven.client.api.model.BundleDependency> resolvedDependencies = new ArrayList<>();
    resolvedDependencies.add(mavenClient.resolveBundleDescriptor(dependencyBundleDescriptor));
    resolvedDependencies.addAll(mavenClient.resolveBundleDescriptorDependencies(false, false, dependencyBundleDescriptor));
    return resolvedDependencies;
  }

  private void updateAdditionalDependencyOrFail(List<org.mule.maven.client.api.model.BundleDependency> additionalDependencies,
                                                org.mule.maven.client.api.model.BundleDependency bundleDependency) {
    Reference<org.mule.maven.client.api.model.BundleDependency> replace = new Reference<>();
    additionalDependencies.stream()
        .filter(additionalBundleDependency -> StringUtils.equals(additionalBundleDependency.getDescriptor().getGroupId(),
                                                                 bundleDependency.getDescriptor().getGroupId())
            &&
            StringUtils.equals(additionalBundleDependency.getDescriptor().getArtifactId(),
                               bundleDependency.getDescriptor().getArtifactId()))
        .findFirst()
        .map(additionalBundleDependency -> {
          String additionalBundleDependencyVersion = additionalBundleDependency.getDescriptor().getVersion();
          String bundleDependencyVersion = bundleDependency.getDescriptor().getVersion();
          if (areSameMajor(bundleDependencyVersion, additionalBundleDependencyVersion)) {
            if (isNewerVersion(bundleDependencyVersion, additionalBundleDependencyVersion)) {
              replace.set(additionalBundleDependency);
            }
          } else {
            throw new MuleRuntimeException(createStaticMessage("Attempting to add different major versions of the same dependency as additional plugin dependency. If this is not explicitly defined, check transitive dependencies."
                +
                lineSeparator() +
                "These are: " +
                lineSeparator() +
                additionalBundleDependency.toString() +
                lineSeparator() +
                bundleDependency.toString()));
          }
          return true;
        }).orElseGet(
                     () -> additionalDependencies.add(bundleDependency));
    if (replace.get() != null) {
      additionalDependencies.remove(replace.get());
      additionalDependencies.add(bundleDependency);
    }
  }

  private boolean isNewerVersion(String dependencyA, String dependencyB) {
    try {
      return new MuleVersion(dependencyA).newerThan(dependencyB);
    } catch (IllegalArgumentException e) {
      // If not using semver lets just compare the strings.
      return dependencyA.compareTo(dependencyB) > 0;
    }
  }

  private boolean areSameMajor(String dependencyA, String dependencyB) {
    try {
      return new MuleVersion(dependencyA).getMajor() == new MuleVersion(dependencyB).getMajor();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

}
