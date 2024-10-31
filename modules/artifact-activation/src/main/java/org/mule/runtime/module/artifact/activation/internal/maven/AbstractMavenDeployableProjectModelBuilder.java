/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.globalconfig.api.maven.MavenClientFactory.createMavenClient;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.findArtifactsSharedDependencies;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.getDeployableArtifactCoordinates;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.toApplicationModelArtifacts;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.updatePackagesResources;
import static org.mule.runtime.module.artifact.activation.internal.maven.MavenUtilsForArtifact.getPomPropertiesFolder;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactConstants.getApiClassifiers;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.SYSTEM;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.compile;
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
import org.mule.maven.pom.parser.api.model.PomParentCoordinates;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractDeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.activation.internal.deployable.DeployablePluginsDependenciesResolver;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tools.api.classloader.model.Artifact;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMavenDeployableProjectModelBuilder extends AbstractDeployableProjectModelBuilder {

  protected static final String PACKAGE_TYPE = "jar";
  private static final MavenPomParserProvider POM_PARSER_PROVIDER = MavenPomParserProvider.discoverProvider();

  protected static final Supplier<MavenConfiguration> DEFAULT_MAVEN_CONFIGURATION =
      new LazyValue<>(AbstractMavenDeployableProjectModelBuilder::getDefaultMavenConfiguration);

  protected final MavenConfiguration mavenConfiguration;
  protected final File projectFolder;
  protected List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> deployableMavenBundleDependencies;
  protected List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> deployableBundleDependencies;
  protected Map<ArtifactCoordinates, List<Artifact>> pluginsArtifactDependencies;
  protected Set<BundleDescriptor> sharedDeployableBundleDescriptors;
  protected Map<BundleDescriptor, List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency>> additionalPluginDependencies;
  protected Map<BundleDescriptor, List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency>> pluginsBundleDependencies;
  protected File deployableArtifactRepositoryFolder;
  private static final Logger logger = LoggerFactory.getLogger(AbstractMavenDeployableProjectModelBuilder.class);
  // This pattern looks for placeholders like ${text}.
  private final static Pattern PLACEHOLDER_PATTERN = compile("\\$\\{\\s*([^}]*)\\s*\\}");


  private static MavenConfiguration getDefaultMavenConfiguration() {
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
  public DeployableProjectModel build() {
    File pom = getPomFromFolder(projectFolder);

    Properties pomProperties;
    Optional<String> version = empty();

    if (projectFolder.isDirectory()) {
      try {
        // 1) look for pom.properties, get the version info from there
        pomProperties = getPomPropertiesFolder(projectFolder);
        version = ofNullable(pomProperties.getProperty("version"));
      } catch (ArtifactDescriptorCreateException e) {
        logger.debug("unable to get version info from pom.properties:" + e.getMessage());
      }
    }

    List<String> activeProfiles = mavenConfiguration.getActiveProfiles().orElse(emptyList());
    MavenPomParser parser = POM_PARSER_PROVIDER.createMavenPomParserClient(pom.toPath(), activeProfiles);

    // 2) if a version is passed using system properties, use the version instead
    String originalPomVersion = getVersion(parser);
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(originalPomVersion);
    if (matcher.find()) {
      String potentialProperty = matcher.group(1);
      if (getProperty(potentialProperty) != null) {
        version = ofNullable(matcher.replaceAll(getProperty(potentialProperty)));
      }
    }
    deployableArtifactRepositoryFolder = this.mavenConfiguration.getLocalMavenRepositoryLocation();

    ArtifactCoordinates deployableArtifactCoordinates = getDeployableProjectArtifactCoordinates(parser, version);

    try (MavenClient mavenClient = createMavenClient(mavenConfiguration)) {
      resolveDeployableDependencies(mavenClient, pom, parser, activeProfiles);

      resolveDeployablePluginsData(deployableMavenBundleDependencies);

      resolveAdditionalPluginDependencies(mavenClient, parser, pluginsArtifactDependencies);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Error while resolving dependencies"), e);
    }

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
        .setType(PACKAGE_TYPE)
        .setClassifier(artifactCoordinates.getClassifier().orElse(null))
        .build();
  }

  /**
   * Retrieves the groupId of the deployable project from the {@link MavenPomParser}.
   *
   * @param parser the {@link MavenPomParser} to retrieve the groupId from.
   *
   * @return the groupId of the deployable project.
   */
  private String getGroupId(MavenPomParser parser) {
    String groupId = parser.getModel().getGroupId();
    if (groupId == null) {
      groupId = parser.getModel().getParent()
          .map(PomParentCoordinates::getGroupId)
          .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Failed to retrieve groupId from the artifact, trying to retrieve from parent POM but parent POM is not present")));
      if (groupId == null) {
        throw new MuleRuntimeException(createStaticMessage("GroupId is null in both current and parent POM"));
      }
    }
    return groupId;
  }

  /**
   * Retrieves the artifactId of the deployable project from the {@link MavenPomParser}.
   *
   * @param parser the {@link MavenPomParser} to retrieve the artifactId from.
   *
   * @return the artifactId of the deployable project.
   */
  private String getArtifactId(MavenPomParser parser) {
    String artifactId = parser.getModel().getArtifactId();
    if (artifactId == null) {
      throw new MuleRuntimeException(createStaticMessage("ArtifactId is null in the POM"));
    }
    return artifactId;
  }

  /**
   * Retrieves the version of the deployable project from the {@link MavenPomParser}.
   *
   * @param parser the {@link MavenPomParser} to retrieve the version from.
   *
   * @return the version of the deployable project.
   */
  private String getVersion(MavenPomParser parser) {
    String version = parser.getModel().getVersion();
    if (version == null) {
      version = parser.getModel().getParent()
          .map(PomParentCoordinates::getVersion)
          .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Failed to retrieve version from the artifact, trying to retrieve from parent POM but parent POM is not present")));
      if (version == null) {
        throw new MuleRuntimeException(createStaticMessage("Version is null in both current and parent POM"));
      }
    }
    return version;
  }

  /**
   * Retrieves the {@link ArtifactCoordinates} of the deployable project from the {@link MavenPomParser}.
   *
   * @param parser the {@link MavenPomParser} to retrieve the {@link ArtifactCoordinates} from.
   *
   * @return the {@link ArtifactCoordinates} of the deployable project.
   */
  private ArtifactCoordinates getDeployableProjectArtifactCoordinates(MavenPomParser parser, Optional<String> version) {
    return getDeployableArtifactCoordinates(getGroupId(parser),
                                            getArtifactId(parser),
                                            version.orElse(getVersion(parser)),
                                            parser.getModel().getPackaging());
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
        updatePackagesResources(toApplicationModelArtifacts(deployableMavenBundleDependencies));
    List<Artifact> deployableArtifactSharedDependencies =
        findArtifactsSharedDependencies(deployableMavenBundleDependencies,
                                        deployableArtifactDependencies,
                                        parser.getSharedLibraries()
                                            .stream()
                                            .map(sharedLibrary -> sharedLibrary.getGroupId() + ":"
                                                + sharedLibrary.getArtifactId())
                                            .collect(toList()),
                                        activeProfiles);

    // Prepare bundle dependencies as expected by the project model
    deployableBundleDependencies =
        deployableArtifactDependencies.stream()
            .map(artifact -> createBundleDependencyFromPackagerDependency(getDeployableArtifactRepositoryUriResolver())
                .apply(artifact))
            .collect(toList());

    sharedDeployableBundleDescriptors =
        deployableBundleDependencies.stream()
            .filter(bd -> deployableArtifactSharedDependencies.stream()
                .anyMatch(artifact -> bd.getDescriptor().getGroupId().equals(artifact.getArtifactCoordinates().getGroupId())
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

  private List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> resolveSystemScopeDependencies(MavenClient mavenClient,
                                                                                                                List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> deployableMavenBundleDependencies) {
    List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> systemScopeDependenciesTransitiveDependencies =
        new ArrayList<>();

    List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> result = deployableMavenBundleDependencies
        .stream()
        .map(bundleDependency -> {
          if (MULE_PLUGIN_CLASSIFIER.equals(bundleDependency.getDescriptor().getClassifier().orElse(null))
              && SYSTEM.equals(bundleDependency.getScope())) {
            try (MuleSystemPluginMavenReactorResolver reactor =
                new MuleSystemPluginMavenReactorResolver(new File(bundleDependency.getBundleUri()), mavenClient)) {
              final BundleDependency mvnBundleDependency = mavenClient
                  .resolveArtifactDependencies(singletonList(new org.mule.maven.pom.parser.api.model.BundleDescriptor.Builder()
                      .setGroupId(bundleDependency.getDescriptor().getGroupId())
                      .setArtifactId(bundleDependency.getDescriptor().getArtifactId())
                      .setClassifier(bundleDependency.getDescriptor().getClassifier().orElse(null))
                      .setType(bundleDependency.getDescriptor().getType())
                      .setVersion(bundleDependency.getDescriptor().getVersion())
                      .setBaseVersion(bundleDependency.getDescriptor().getVersion())
                      .build()),
                                               of(deployableArtifactRepositoryFolder),
                                               of(reactor))
                  .get(0);

              org.mule.runtime.module.artifact.api.descriptor.BundleDependency systemScopeDependency =
                  from(mvnBundleDependency);

              systemScopeDependenciesTransitiveDependencies.addAll(collectTransitivePluginDependencies(systemScopeDependency));

              return systemScopeDependency;
            }
          }

          return bundleDependency;
        }).collect(toList());

    result.addAll(systemScopeDependenciesTransitiveDependencies);

    return getUniqueDependencies(result);
  }

  private org.mule.runtime.module.artifact.api.descriptor.BundleDependency from(org.mule.maven.pom.parser.api.model.BundleDependency d) {
    return org.mule.runtime.module.artifact.api.descriptor.BundleDependency.builder()
        .setDescriptor(BundleDescriptor.builder()
            .setArtifactId(d.getDescriptor().getArtifactId())
            .setGroupId(d.getDescriptor().getGroupId())
            .setClassifier(d.getDescriptor().getClassifier().orElse(null))
            .setType(d.getDescriptor().getType())
            .setVersion(d.getDescriptor().getVersion())
            .setBaseVersion(d.getDescriptor().getVersion())
            .build())
        .setBundleUri(d.getBundleUri())
        .setTransitiveDependencies(d.getTransitiveDependencies()
            .stream()
            .map(this::from)
            .collect(toList()))
        .setScope(org.mule.runtime.module.artifact.api.descriptor.BundleScope.valueOf(d.getScope().name()))
        .build();
  }

  private List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> getUniqueDependencies(List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> dependencies) {
    Set<String> uniqueDependenciesIds = new HashSet<>();

    // Filtering is done this way to preserve the order
    return dependencies.stream().filter(dependency -> {
      BundleDescriptor descriptor = dependency.getDescriptor();
      String pluginKey =
          descriptor.getGroupId() + ":" + descriptor.getArtifactId() + ":" + descriptor.getVersion()
              + descriptor.getClassifier().map(classifier -> ":" + classifier).orElse("");
      boolean isApi = descriptor.getClassifier().map(getApiClassifiers()::contains).orElse(false);
      boolean keep = !uniqueDependenciesIds.contains(pluginKey) || isApi;
      uniqueDependenciesIds.add(pluginKey);
      return keep;
    }).collect(toList());
  }

  private List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> collectTransitivePluginDependencies(org.mule.runtime.module.artifact.api.descriptor.BundleDependency rootDependency) {
    List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> allTransitivePluginDependencies = new ArrayList<>();
    for (org.mule.runtime.module.artifact.api.descriptor.BundleDependency transitiveDependency : rootDependency
        .getTransitiveDependenciesList()) {
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

  private void resolveDeployablePluginsData(List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> deployableMavenBundleDependencies) {
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

  private Map<BundleDescriptor, List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency>> toPluginDependencies(Map<org.mule.runtime.module.artifact.api.descriptor.BundleDependency, List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency>> pluginsAndDependencies) {
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
                         new BundleDescriptor.Builder()
                             .setGroupId(d.getArtifactCoordinates().getGroupId())
                             .setArtifactId(d.getArtifactCoordinates().getArtifactId())
                             .setClassifier(d.getArtifactCoordinates().getClassifier())
                             .setType(d.getArtifactCoordinates().getType())
                             .setVersion(d.getArtifactCoordinates().getVersion())
                             .setBaseVersion(d.getArtifactCoordinates().getVersion())
                             .build())
          .setBundleUri(bundle)
          .setPackages(d.getPackages() == null ? emptySet() : newHashSet(d.getPackages()))
          .setResources(d.getResources() == null ? emptySet() : newHashSet(d.getResources()))
          .setTransitiveDependencies(deployableBundleDependencies)
          .build();
    };
  }

  private Function<URI, URI> getDeployableArtifactRepositoryUriResolver() {
    return uri -> new File(deployableArtifactRepositoryFolder, uri.toString()).toURI();
  }

}
