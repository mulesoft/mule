/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.BundleScope.SYSTEM;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.maven.client.internal.util.MavenUtils.getPomModelFromFile;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.getDeployableArtifactCoordinates;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.toApplicationModelArtifacts;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.updateArtifactsSharedState;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.updatePackagesResources;
import static org.mule.runtime.module.artifact.api.classloader.MuleExtensionsMavenPlugin.MULE_EXTENSIONS_PLUGIN_ARTIFACT_ID;
import static org.mule.runtime.module.artifact.api.classloader.MuleExtensionsMavenPlugin.MULE_EXTENSIONS_PLUGIN_GROUP_ID;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_ARTIFACT_ID;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_GROUP_ID;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.tools.api.classloader.Constants.ADDITIONAL_PLUGIN_DEPENDENCIES_FIELD;
import static org.mule.tools.api.classloader.Constants.PLUGIN_DEPENDENCIES_FIELD;
import static org.mule.tools.api.classloader.Constants.PLUGIN_DEPENDENCY_FIELD;
import static org.mule.tools.api.classloader.Constants.PLUGIN_FIELD;
import static org.mule.tools.api.classloader.model.ArtifactCoordinates.DEFAULT_ARTIFACT_TYPE;

import static java.lang.Math.random;
import static java.lang.String.format;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.codehaus.plexus.util.xml.Xpp3DomUtils.mergeXpp3Dom;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.maven.client.api.SettingsSupplierFactory;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.client.internal.AetherMavenClient;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractDeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.activation.internal.deployable.DeployablePluginsDependenciesResolver;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tools.api.classloader.model.ApplicationGAVModel;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public abstract class AbstractMavenDeployableProjectModelBuilder extends AbstractDeployableProjectModelBuilder {

  private static final String GROUP_ID = "groupId";
  private static final String ARTIFACT_ID = "artifactId";
  private static final String VERSION = "version";

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
    Model pomModel = getPomModelFromFile(pom);

    deployableArtifactRepositoryFolder = this.mavenConfiguration.getLocalMavenRepositoryLocation();

    ArtifactCoordinates deployableArtifactCoordinates = getDeployableProjectArtifactCoordinates(pomModel);

    AetherMavenClient aetherMavenClient = new AetherMavenClient(mavenConfiguration);
    List<String> activeProfiles = mavenConfiguration.getActiveProfiles().orElse(emptyList());

    resolveDeployableDependencies(aetherMavenClient, pom, pomModel, activeProfiles, deployableArtifactCoordinates);

    resolveDeployablePluginsData(deployableMavenBundleDependencies);

    resolveAdditionalPluginDependencies(aetherMavenClient, pomModel, activeProfiles, pluginsArtifactDependencies);

    return doBuild(pomModel, deployableArtifactCoordinates);
  }

  /**
   * Effectively builds the {@link DeployableProjectModel} with specific behaviour from the implementation.
   *
   * @param pomModel                      the POM model.
   * @param deployableArtifactCoordinates artifact coordinates from the deployable.
   * @return the {@link DeployableProjectModel}.
   */
  protected abstract DeployableProjectModel doBuild(Model pomModel, ArtifactCoordinates deployableArtifactCoordinates);

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

  private ArtifactCoordinates getDeployableProjectArtifactCoordinates(Model pomModel) {
    ApplicationGAVModel deployableGAVModel =
        new ApplicationGAVModel(pomModel.getGroupId(), pomModel.getArtifactId(), pomModel.getVersion());
    return getDeployableArtifactCoordinates(pomModel, deployableGAVModel);
  }

  /**
   * Resolves the dependencies of the deployable in the various forms needed to obtain the {@link DeployableProjectModel}.
   *
   * @param aetherMavenClient             the configured {@link AetherMavenClient}.
   * @param pom                           POM file.
   * @param pomModel                      parsed POM model.
   * @param activeProfiles                active Maven profiles.
   * @param deployableArtifactCoordinates artifact coordinates of the deployable.
   */
  private void resolveDeployableDependencies(AetherMavenClient aetherMavenClient, File pom, Model pomModel,
                                             List<String> activeProfiles, ArtifactCoordinates deployableArtifactCoordinates) {
    DeployableDependencyResolver deployableDependencyResolver = new DeployableDependencyResolver(aetherMavenClient);

    // Resolve the Maven bundle dependencies
    deployableMavenBundleDependencies =
        deployableDependencyResolver.resolveDeployableDependencies(pom, isIncludeTestDependencies(), empty());

    // MTF/MUnit declares the mule-plugin being tested as system scope, therefore its transitive dependencies
    // will not be included in the dependency graph of the deployable artifact and need to be resolved separately
    deployableMavenBundleDependencies = resolveSystemScopeDependencies(aetherMavenClient, deployableMavenBundleDependencies);

    // Get the dependencies as Artifacts, accounting for the shared libraries configuration
    List<Artifact> deployableArtifactDependencies =
        updateArtifactsSharedState(deployableMavenBundleDependencies,
                                   updatePackagesResources(toApplicationModelArtifacts(deployableMavenBundleDependencies)),
                                   pomModel, activeProfiles);

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

  private List<BundleDependency> resolveSystemScopeDependencies(AetherMavenClient aetherMavenClient,
                                                                List<BundleDependency> deployableMavenBundleDependencies) {
    return deployableMavenBundleDependencies.stream().map(bundleDependency -> {
      if (MULE_PLUGIN_CLASSIFIER.equals(bundleDependency.getDescriptor().getClassifier().orElse(null))
          && SYSTEM.equals(bundleDependency.getScope())) {
        try (MuleSystemPluginMavenReactorResolver reactor =
            new MuleSystemPluginMavenReactorResolver(new File(bundleDependency.getBundleUri()), aetherMavenClient)) {
          return aetherMavenClient.resolveArtifactDependencies(singletonList(bundleDependency.getDescriptor()),
                                                               of(deployableArtifactRepositoryFolder),
                                                               of(reactor))
              .get(0);
        }
      }

      return bundleDependency;
    }).collect(toList());
  }

  private static class MuleSystemPluginMavenReactorResolver implements MavenReactorResolver, AutoCloseable {

    private static final String POM = "pom";

    private final File temporaryFolder;

    private final Model effectiveModel;

    private final File pomFile;
    private final File artifactFile;

    public MuleSystemPluginMavenReactorResolver(File artifactFile, MavenClient mavenClient) {
      try {
        temporaryFolder = createTempDirectory("tmpDirPrefix" + random()).toFile();
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }

      this.effectiveModel = mavenClient.getEffectiveModel(artifactFile, of(temporaryFolder));

      this.pomFile = effectiveModel.getPomFile();
      this.artifactFile = artifactFile;
    }

    @Override
    public File findArtifact(org.mule.maven.client.api.model.BundleDescriptor bundleDescriptor) {
      if (checkArtifact(bundleDescriptor)) {
        if (bundleDescriptor.getType().equals(POM)) {
          return pomFile;
        } else {
          return artifactFile;
        }
      }
      return null;
    }

    @Override
    public List<String> findVersions(org.mule.maven.client.api.model.BundleDescriptor bundleDescriptor) {
      if (checkArtifact(bundleDescriptor)) {
        return singletonList(this.effectiveModel.getVersion());
      }
      return emptyList();
    }

    private boolean checkArtifact(org.mule.maven.client.api.model.BundleDescriptor bundleDescriptor) {
      return this.effectiveModel.getGroupId().equals(bundleDescriptor.getGroupId())
          && this.effectiveModel.getArtifactId().equals(bundleDescriptor.getArtifactId())
          && this.effectiveModel.getVersion().equals(bundleDescriptor.getVersion());
    }

    @Override
    public void close() {
      deleteQuietly(temporaryFolder);
    }

  }

  private void resolveAdditionalPluginDependencies(AetherMavenClient aetherMavenClient, Model pomModel,
                                                   List<String> activeProfiles,
                                                   Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies) {
    // Parse additional plugin dependencies
    List<org.mule.runtime.module.artifact.activation.internal.plugin.Plugin> initialAdditionalPluginDependencies =
        findArtifactPackagerPlugin(pomModel, activeProfiles)
            .map(this::getAdditionalPluginDependencies).orElse(emptyList());

    AdditionalPluginDependenciesResolver additionalPluginDependenciesResolver =
        new AdditionalPluginDependenciesResolver(aetherMavenClient,
                                                 initialAdditionalPluginDependencies,
                                                 new File("temp"));

    additionalPluginDependencies = toPluginDependencies(additionalPluginDependenciesResolver
        .resolveDependencies(deployableMavenBundleDependencies, pluginsDependencies));
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

  private Optional<org.apache.maven.model.Plugin> findArtifactPackagerPlugin(Model model, List<String> activeProfiles) {
    Stream<org.apache.maven.model.Plugin> basePlugin = Stream.empty();
    Build build = model.getBuild();
    if (build != null) {
      basePlugin = findArtifactPackagerPlugin(build.getPlugins()).map(Stream::of).orElse(Stream.empty());
    }

    // Sort them so the processing is consistent with how Maven calculates the plugin configuration for the effective pom.
    final List<String> sortedActiveProfiles = activeProfiles
        .stream()
        .sorted(String::compareTo)
        .collect(toList());

    final Stream<org.apache.maven.model.Plugin> packagerConfigsForActivePluginsStream = model.getProfiles().stream()
        .filter(profile -> sortedActiveProfiles.contains(profile.getId()))
        .map(profile -> findArtifactPackagerPlugin(profile.getBuild() != null ? profile.getBuild().getPlugins() : null))
        .filter(plugin -> !plugin.equals(empty()))
        .map(Optional::get);

    return concat(basePlugin, packagerConfigsForActivePluginsStream)
        .reduce((p1, p2) -> {
          p1.setConfiguration(mergeXpp3Dom((Xpp3Dom) p2.getConfiguration(), (Xpp3Dom) p1.getConfiguration()));
          p1.getDependencies().addAll(p2.getDependencies());

          return p1;
        });
  }

  private Optional<org.apache.maven.model.Plugin> findArtifactPackagerPlugin(List<org.apache.maven.model.Plugin> plugins) {
    if (plugins != null) {
      return plugins.stream().filter(plugin -> (plugin.getArtifactId().equals(MULE_MAVEN_PLUGIN_ARTIFACT_ID)
          && plugin.getGroupId().equals(MULE_MAVEN_PLUGIN_GROUP_ID)) ||
          (plugin.getArtifactId().equals(MULE_EXTENSIONS_PLUGIN_ARTIFACT_ID) &&
              plugin.getGroupId().equals(MULE_EXTENSIONS_PLUGIN_GROUP_ID)))
          .findFirst();
    } else {
      return empty();
    }
  }

  private List<org.mule.runtime.module.artifact.activation.internal.plugin.Plugin> getAdditionalPluginDependencies(org.apache.maven.model.Plugin packagingPlugin) {
    List<org.mule.runtime.module.artifact.activation.internal.plugin.Plugin> pluginsAdditionalLibraries = new ArrayList<>();
    Object configuration = packagingPlugin.getConfiguration();
    if (configuration != null) {
      Xpp3Dom additionalPluginDependenciesDom = ((Xpp3Dom) configuration).getChild(ADDITIONAL_PLUGIN_DEPENDENCIES_FIELD);
      if (additionalPluginDependenciesDom != null) {
        Xpp3Dom[] pluginsDom = additionalPluginDependenciesDom.getChildren(PLUGIN_FIELD);
        if (pluginsDom != null) {
          for (Xpp3Dom pluginDom : pluginsDom) {
            String pluginGroupId = getChildParameterValue(pluginDom, GROUP_ID, true);
            String pluginArtifactId = getChildParameterValue(pluginDom, ARTIFACT_ID, true);
            List<Dependency> additionalDependencyDependencies = new ArrayList<>();
            Xpp3Dom dependenciesDom = pluginDom.getChild(PLUGIN_DEPENDENCIES_FIELD);
            if (dependenciesDom != null) {
              for (Xpp3Dom dependencyDom : dependenciesDom.getChildren(PLUGIN_DEPENDENCY_FIELD)) {
                Dependency dependency = new Dependency();
                dependency.setGroupId(getChildParameterValue(dependencyDom, GROUP_ID, true));
                dependency
                    .setArtifactId(getChildParameterValue(dependencyDom, ARTIFACT_ID, true));
                dependency.setVersion(getChildParameterValue(dependencyDom, VERSION, true));
                String type = getChildParameterValue(dependencyDom, "type", false);
                dependency.setType(type == null ? DEFAULT_ARTIFACT_TYPE : type);
                dependency.setClassifier(getChildParameterValue(dependencyDom, "classifier", false));
                dependency.setSystemPath(getChildParameterValue(dependencyDom, "systemPath", false));

                additionalDependencyDependencies.add(dependency);
              }
            }
            org.mule.runtime.module.artifact.activation.internal.plugin.Plugin plugin =
                new org.mule.runtime.module.artifact.activation.internal.plugin.Plugin();
            plugin.setGroupId(pluginGroupId);
            plugin.setArtifactId(pluginArtifactId);
            plugin.setAdditionalDependencies(additionalDependencyDependencies);
            pluginsAdditionalLibraries.add(plugin);
          }
        }
      }
    }
    return pluginsAdditionalLibraries;
  }

  private String getChildParameterValue(Xpp3Dom element, String childName, boolean validate) {
    Xpp3Dom child = element.getChild(childName);
    String childValue = child != null ? child.getValue() : null;
    if (StringUtils.isEmpty(childValue) && validate) {
      throw new IllegalArgumentException("Expecting child element with not null value " + childName);
    }
    return childValue;
  }
}
