/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static java.util.Optional.empty;

import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleScope;
import org.mule.maven.client.internal.AetherMavenClient;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DeployableDependencyResolver {

  protected static final String MULE_DOMAIN_CLASSIFIER = "mule-domain";

  private final AetherMavenClient muleMavenPluginClient;

  public DeployableDependencyResolver(AetherMavenClient muleMavenPluginClient) {
    this.muleMavenPluginClient = muleMavenPluginClient;
  }

  /**
   * Resolve the application dependencies, excluding mule domains.
   *
   * @param pomFile                 pom file
   * @param includeTestDependencies true if the test dependencies must be included, false otherwise.
   * @param mavenReactorResolver    {@link MavenReactorResolver}
   */
  public List<BundleDependency> resolveApplicationDependencies(File pomFile, boolean includeTestDependencies,
                                                               Optional<MavenReactorResolver> mavenReactorResolver) {

    return muleMavenPluginClient
        .resolveArtifactDependencies(pomFile, includeTestDependencies, true, empty(), mavenReactorResolver, empty())
        .stream()
        .filter(d -> !(d.getScope() == BundleScope.PROVIDED) || (d.getDescriptor().getClassifier().isPresent()
            && d.getDescriptor().getClassifier().get().equals(MULE_DOMAIN_CLASSIFIER)))
        .collect(Collectors.toList());
  }

}
