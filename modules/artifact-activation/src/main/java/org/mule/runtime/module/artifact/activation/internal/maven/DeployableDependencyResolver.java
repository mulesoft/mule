/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.maven.pom.parser.api.model.BundleDependency;
import org.mule.maven.pom.parser.api.model.BundleScope;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class DeployableDependencyResolver {

  private static final String MULE_DOMAIN_CLASSIFIER = "mule-domain";

  private final MavenClient muleMavenPluginClient;

  public DeployableDependencyResolver(MavenClient muleMavenPluginClient) {
    this.muleMavenPluginClient = muleMavenPluginClient;
  }

  /**
   * Resolve the deployable dependencies, excluding mule domains.
   *
   * @param pomFile                 pom file
   * @param includeTestDependencies true if the test dependencies must be included, false otherwise.
   * @param mavenReactorResolver    {@link MavenReactorResolver}
   */
  public List<BundleDependency> resolveDeployableDependencies(File pomFile, boolean includeTestDependencies,
                                                              Optional<MavenReactorResolver> mavenReactorResolver) {

    return muleMavenPluginClient
        .resolveArtifactDependencies(pomFile, includeTestDependencies, true, empty(), mavenReactorResolver, empty())
        .stream()
        .filter(d -> !(d.getScope() == BundleScope.PROVIDED)
            || d.getDescriptor().getClassifier().map(MULE_DOMAIN_CLASSIFIER::equals).orElse(false))
        .collect(toList());
  }

}
