/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.maven.pom.parser.api.model.BundleScope.SYSTEM;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.globalconfig.api.maven.MavenClientFactory.createMavenClient;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.getDeployableArtifactCoordinates;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.toApplicationModelArtifacts;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.updateArtifactsSharedState;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.updatePackagesResources;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactConstants.getApiClassifiers;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import static com.google.common.collect.Sets.newHashSet;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.maven.client.api.SettingsSupplierFactory;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.pom.parser.api.MavenPomParser;
import org.mule.maven.pom.parser.api.MavenPomParserProvider;
import org.mule.maven.pom.parser.api.model.AdditionalPluginDependencies;
import org.mule.maven.pom.parser.api.model.BundleDependency;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractDeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.activation.internal.deployable.DeployablePluginsDependenciesResolver;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tools.api.classloader.model.ApplicationGAVModel;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractMavenDeployableProjectModelBuilder extends AbstractDeployableProjectModelBuilder {

  protected final MavenConfiguration mavenConfiguration;
  protected final File projectFolder;
  protected List<BundleDependency> deployableMavenBundleDependencies;
  protected List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> deployableBundleDependencies;
  protected Map<ArtifactCoordinates, List<Artifact>> pluginsArtifactDependencies;
  protected Set<BundleDescriptor> sharedDeployableBundleDescriptors;
  protected Map<org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor, List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency>> additionalPluginDependencies;
  protected Map<BundleDescriptor, List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency>> pluginsBundleDependencies;
  protected File deployableArtifactRepositoryFolder;

  protected static MavenConfiguration getDefaultMavenConfiguration() {
    final MavenClientProvider mavenClientProvider =
        discoverProvider(MavenDeployableProjectModelBuilder.class.getClassLoader());

    final Supplier<File> localMavenRepository =
        mavenClientProvider.getLocalRepositorySuppliers().environmentMavenRepositorySupplier();
    final SettingsSupplierFactory settingsSupplierFactory = mavenClientProvider.getSettingsSupplierFactory();
    final Optional<File> globalSettings = settingsSupplierFactory.environmentGlobalSettingsSupplier();
    final Optional<File> userSettings = settingsSupplierFactory.environmentUserSettingsSupplier();
    final Optional<File> settingsSecurity = settingsSupplierFactory.environmentSettingsSecuritySupplier();

    final MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder = newMavenConfigurationBuilder()
        .forcePolicyUpdateNever(true)
        .localMavenRepositoryLocation(localMavenRepository.get());

    globalSettings.ifPresent(mavenConfigurationBuilder::globalSettingsLocation);

    userSettings.ifPresent(mavenConfigurationBuilder::userSettingsLocation);

    settingsSecurity.ifPresent(mavenConfigurationBuilder::settingsSecurityLocation);

    return mavenConfigurationBuilder.build();
  }

  protected AbstractMavenDeployableProjectModelBuilder(MavenConfiguration mavenConfiguration, File projectFolder) {
    this.mavenConfiguration = mavenConfiguration;
    this.projectFolder = projectFolder;
  }

  @Override
  public final DeployableProjectModel build() {
    File pom = getPomFromFolder(projectFolder);

    List<String> activeProfiles = mavenConfiguration.getActiveProfiles().orElse(emptyList());
    MavenPomParser parser = MavenPomParserProvider.discoverProvider().createMavenPomParserClient(pom.toPath(), activeProfiles);

    deployableArtifactRepositoryFolder = this.mavenConfiguration.getLocalMavenRepositoryLocation();

    ArtifactCoordinates deployableArtifactCoordinates = getDeployableProjectArtifactCoordinates(parser);

    MavenClient mavenClient = createMavenClient(mavenConfiguration);

    resolveDeployableDependencies(mavenClient, pom, parser, activeProfiles);

    resolveDeployablePluginsData(deployableMavenBundleDependencies);

    resolveAdditionalPluginDependencies(mavenClient, parser, pluginsArtifactDependencies);

    return doBuild(parser, deployableArtifactCoordinates);
  }

  /**
   * Effectively builds the {@link DeployableProjectModel} with specific behaviour from the implementation.
   *
   * @param deployableArtifactCoordinates artifact coordinates from the deployable.
   * @return the {@link DeployableProjectModel}.
   */
  protected abstract DeployableProjectModel doBuild(MavenPomParser parser, ArtifactCoordinates deployableArtifactCoordinates);

  /**
   * Retrieves the POM file from the deployable project's folder.
   *
   * @param projectFolder the deployable project's folder.
   * @return the deployable project's POM file.
   */
  protected abstract File getPomFromFolder(File projectFolder);

  /**
   * @return whether test dependencies are to be considered for this {@link DeployableProjectModel}.
   */
  protected abstract boolean isIncludeTestDependencies();

  protected final List<String> getAttribute(Map<String, Object> attributes, String attribute) {
    if (attributes == null) {
      return emptyList();
    }

    final Object attributeObject = attributes.getOrDefault(attribute, emptyList());
    checkArgument(attributeObject instanceof List, format("The '%s' attribute must be of '%s', found '%s'", attribute,
                                                          List.class.getName(), attributeObject.getClass().getName()));
    return (List<String>) attributeObject;
  }

  protected final <T> T getSimpleAttribute(Map<String, Object> attributes, String attribute, T defaultValue) {
    return (T) attributes.getOrDefault(attribute, defaultValue);
  }

  protected BundleDescriptor buildBundleDescriptor(ArtifactCoordinates artifactCoordinates) {
    return new BundleDescriptor.Builder()
        .setArtifactId(artifactCoordinates.getArtifactId())
        .setGroupId(artifactCoordinates.getGroupId())
        .setVersion(artifactCoordinates.getVersion())
        .setBaseVersion(artifactCoordinates.getVersion())
        .setType(artifactCoordinates.getType())
        .setClassifier(artifactCoordinates.getClassifier())
        .build();
  }

  private ArtifactCoordinates getDeployableProjectArtifactCoordinates(MavenPomParser parser) {
    ApplicationGAVModel deployableGAVModel =
        new ApplicationGAVModel(parser.getModel().getGroupId(), parser.getModel().getArtifactId(),
                                parser.getModel().getVersion());
    return getDeployableArtifactCoordinates(parser, deployableGAVModel);
  }

  /**
   * Resolves the dependencies of the deployable in the various forms needed to obtain the {@link DeployableProjectModel}.
   *
   * @param mavenClient    the configured {@link MavenClient}.
   * @param pom            POM file.
   * @param activeProfiles active Maven profiles.
   */
  private void resolveDeployableDependencies(MavenClient mavenClient, File pom, MavenPomParser parser,
                                             List<String> activeProfiles) {
    DeployableDependencyResolver deployableDependencyResolver = new DeployableDependencyResolver(mavenClient);

    // Resolve the Maven bundle dependencies
    deployableMavenBundleDependencies =
        deployableDependencyResolver.resolveDeployableDependencies(pom, isIncludeTestDependencies(), getMavenReactorResolver());

    // MTF/MUnit declares the mule-plugin being tested as system scope, therefore its transitive dependencies
    // will not be included in the dependency graph of the deployable artifact and need to be resolved separately
    deployableMavenBundleDependencies = resolveSystemScopeDependencies(mavenClient, deployableMavenBundleDependencies);

    // Get the dependencies as Artifacts, accounting for the shared libraries configuration
    List<Artifact> deployableArtifactDependencies =
        updateArtifactsSharedState(deployableMavenBundleDependencies,
                                   updatePackagesResources(toApplicationModelArtifacts(deployableMavenBundleDependencies)),
                                   parser, activeProfiles);

    // Prepare bundle dependencies as expected by the project model
    deployableBundleDependencies =
        deployableArtifactDependencies.stream()
            .map(artifact -> createBundleDependencyFromPackagerDependency(getDeployableArtifactRepositoryUriResolver())
                .apply(artifact))
            .collect(toList());

    sharedDeployableBundleDescriptors =
        deployableBundleDependencies.stream()
            .filter(bd -> deployableArtifactDependencies.stream()
                .anyMatch(artifact -> artifact.isShared()
                    && bd.getDescriptor().getGroupId().equals(artifact.getArtifactCoordinates().getGroupId())
                    && bd.getDescriptor().getArtifactId().equals(artifact.getArtifactCoordinates().getArtifactId())))
            .map(org.mule.runtime.module.artifact.api.descriptor.BundleDependency::getDescriptor)
            .collect(toSet());
  }

  /**
   * Get the {@link MavenReactorResolver} configured. If it is configured the {@link DeployableDependencyResolver} will look up
   * the dependencies also in this repository. If {@link Optional#empty()} it will look up in the repositories configured in the
   * system.
   *
   * @return an {@link Optional} {@link MavenReactorResolver}.
   */
  protected Optional<MavenReactorResolver> getMavenReactorResolver() {
    return empty();
  }

  private List<BundleDependency> resolveSystemScopeDependencies(MavenClient mavenClient,
                                                                List<BundleDependency> deployableMavenBundleDependencies) {
    List<BundleDependency> systemScopeDependenciesTransitiveDependencies = new ArrayList<>();

    List<BundleDependency> result = deployableMavenBundleDependencies.stream().map(bundleDependency -> {
      if (MULE_PLUGIN_CLASSIFIER.equals(bundleDependency.getDescriptor().getClassifier().orElse(null))
          && SYSTEM.equals(bundleDependency.getScope())) {
        try (MuleSystemPluginMavenReactorResolver reactor =
            new MuleSystemPluginMavenReactorResolver(new File(bundleDependency.getBundleUri()), mavenClient)) {
          BundleDependency systemScopeDependency = mavenClient
              .resolveArtifactDependencies(singletonList(bundleDependency.getDescriptor()),
                                           of(deployableArtifactRepositoryFolder),
                                           of(reactor))
              .get(0);

          systemScopeDependenciesTransitiveDependencies.addAll(collectTransitivePluginDependencies(systemScopeDependency));

          return systemScopeDependency;
        }
      }

      return bundleDependency;
    }).collect(toList());

    result.addAll(systemScopeDependenciesTransitiveDependencies);

    return getUniqueDependencies(result);
  }

  private List<BundleDependency> getUniqueDependencies(List<BundleDependency> dependencies) {
    Set<String> uniqueDependenciesIds = new HashSet<>();

    // Filtering is done this way to preserve the order
    return dependencies.stream().filter(dependency -> {
      org.mule.maven.pom.parser.api.model.BundleDescriptor descriptor = dependency.getDescriptor();
      String pluginKey =
          descriptor.getGroupId() + ":" + descriptor.getArtifactId() + ":" + descriptor.getVersion()
              + descriptor.getClassifier().map(classifier -> ":" + classifier).orElse("");
      boolean isApi = descriptor.getClassifier().map(getApiClassifiers()::contains).orElse(false);
      boolean keep = !uniqueDependenciesIds.contains(pluginKey) || isApi;
      uniqueDependenciesIds.add(pluginKey);
      return keep;
    }).collect(toList());
  }

  private List<BundleDependency> collectTransitivePluginDependencies(BundleDependency rootDependency) {
    List<BundleDependency> allTransitivePluginDependencies = new ArrayList<>();
    for (BundleDependency transitiveDependency : rootDependency.getTransitiveDependencies()) {
      if (transitiveDependency.getDescriptor().getClassifier().map(MULE_PLUGIN_CLASSIFIER::equals).orElse(false)) {
        allTransitivePluginDependencies.add(transitiveDependency);
        allTransitivePluginDependencies.addAll(collectTransitivePluginDependencies(transitiveDependency));
      }
    }

    return allTransitivePluginDependencies;
  }

  private void resolveAdditionalPluginDependencies(MavenClient mavenClient, MavenPomParser parser,
                                                   Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies) {
    // Parse additional plugin dependencies
    Map<org.mule.maven.pom.parser.api.model.ArtifactCoordinates, AdditionalPluginDependencies> initialAdditionalPluginDependencies =
        parser.getPomAdditionalPluginDependenciesForArtifacts();

    AdditionalPluginDependenciesResolver additionalPluginDependenciesResolver =
        getAdditionalPluginDependenciesResolver(mavenClient, new LinkedList<>(initialAdditionalPluginDependencies.values()));

    additionalPluginDependencies = toPluginDependencies(additionalPluginDependenciesResolver
        .resolveDependencies(deployableMavenBundleDependencies, pluginsDependencies));
  }

  protected AdditionalPluginDependenciesResolver getAdditionalPluginDependenciesResolver(MavenClient mavenClient,
                                                                                         List<AdditionalPluginDependencies> initialAdditionalPluginDependencies) {
    return new AdditionalPluginDependenciesResolver(mavenClient,
                                                    initialAdditionalPluginDependencies,
                                                    new File("temp"));
  }

  private void resolveDeployablePluginsData(List<BundleDependency> deployableMavenBundleDependencies) {
    // Resolve the dependencies of each deployable's dependency
    pluginsArtifactDependencies =
        new DeployablePluginsDependenciesResolver().resolve(deployableMavenBundleDependencies);

    Map<ArtifactCoordinates, BundleDescriptor> pluginsBundleDescriptors = new HashMap<>();
    pluginsArtifactDependencies.keySet().forEach(pluginArtifactCoordinates -> pluginsBundleDescriptors
        .put(pluginArtifactCoordinates, buildBundleDescriptor(pluginArtifactCoordinates)));

    pluginsBundleDependencies = new HashMap<>();
    pluginsArtifactDependencies
        .forEach((pluginArtifactCoordinates, pluginDependencies) -> pluginsBundleDependencies.put(pluginsBundleDescriptors
            .get(pluginArtifactCoordinates), pluginDependencies.stream()
                .map(artifact -> createBundleDependencyFromPackagerDependency(getDeployableArtifactRepositoryUriResolver())
                    .apply(artifact))
                .collect(toList())));

    deployableBundleDependencies = deployableBundleDependencies
        .stream()
        .map(dbd -> new org.mule.runtime.module.artifact.api.descriptor.BundleDependency.Builder(dbd)
            .setTransitiveDependencies(pluginsBundleDependencies.get(dbd.getDescriptor()))
            .build())
        .collect(toList());
  }

  private Map<BundleDescriptor, List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency>> toPluginDependencies(Map<BundleDependency, List<BundleDependency>> pluginsAndDependencies) {
    return pluginsAndDependencies.entrySet()
        .stream()
        .collect(toMap(entry -> deployableBundleDependencies.stream()
            .filter(bd -> bd.getDescriptor().getGroupId().equals(entry.getKey().getDescriptor().getGroupId())
                && bd.getDescriptor().getArtifactId().equals(entry.getKey().getDescriptor().getArtifactId()))
            .map(org.mule.runtime.module.artifact.api.descriptor.BundleDependency::getDescriptor)
            .findAny()
            .get(),
                       entry -> {
                         // Get the dependencies as Artifacts, accounting for the shared libraries configuration
                         List<Artifact> deployableArtifactDependencies =
                             updatePackagesResources(toApplicationModelArtifacts(entry.getValue()));

                         // Prepare bundle dependencies as expected by the project model
                         return deployableArtifactDependencies.stream()
                             .map(artifact -> createBundleDependencyFromPackagerDependency(getDeployableArtifactRepositoryUriResolver())
                                 .apply(artifact))
                             .collect(toList());
                       }));
  }

  private Function<Artifact, org.mule.runtime.module.artifact.api.descriptor.BundleDependency> createBundleDependencyFromPackagerDependency(Function<URI, URI> uriResolver) {
    return d -> {
      URI bundle = d.getUri();
      if (!d.getUri().isAbsolute()) {
        bundle = uriResolver.apply(d.getUri());
      }

      return new org.mule.runtime.module.artifact.api.descriptor.BundleDependency.Builder()
          .setDescriptor(
                         new BundleDescriptor.Builder().setArtifactId(d.getArtifactCoordinates().getArtifactId())
                             .setGroupId(d.getArtifactCoordinates().getGroupId())
                             .setClassifier(d.getArtifactCoordinates().getClassifier())
                             .setType(d.getArtifactCoordinates().getType())
                             .setVersion(d.getArtifactCoordinates().getVersion())
                             .setBaseVersion(d.getArtifactCoordinates().getVersion())
                             .build())
          .setBundleUri(bundle)
          .setPackages(d.getPackages() == null ? emptySet() : newHashSet(d.getPackages()))
          .setResources(d.getResources() == null ? emptySet() : newHashSet(d.getResources()))
          .build();
    };
  }

  private Function<URI, URI> getDeployableArtifactRepositoryUriResolver() {
    return uri -> new File(deployableArtifactRepositoryFolder, uri.toString()).toURI();
  }

}
