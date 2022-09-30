/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.maven.client.internal.util.MavenUtils.getPomModelFromFile;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.module.artifact.activation.api.deployable.ArtifactModelResolver.applicationModelResolver;
import static org.mule.runtime.module.artifact.activation.api.deployable.ArtifactModelResolver.domainModelResolver;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.getDeployableArtifactCoordinates;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.toApplicationModelArtifacts;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.updateArtifactsSharedState;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.updatePackagesResources;
import static org.mule.runtime.module.artifact.api.classloader.MuleExtensionsMavenPlugin.MULE_EXTENSIONS_PLUGIN_ARTIFACT_ID;
import static org.mule.runtime.module.artifact.api.classloader.MuleExtensionsMavenPlugin.MULE_EXTENSIONS_PLUGIN_GROUP_ID;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_ARTIFACT_ID;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_GROUP_ID;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_APPLICATION_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_DOMAIN_CLASSIFIER;
import static org.mule.tools.api.classloader.Constants.ADDITIONAL_PLUGIN_DEPENDENCIES_FIELD;
import static org.mule.tools.api.classloader.Constants.PLUGIN_DEPENDENCIES_FIELD;
import static org.mule.tools.api.classloader.Constants.PLUGIN_DEPENDENCY_FIELD;
import static org.mule.tools.api.classloader.Constants.PLUGIN_FIELD;
import static org.mule.tools.api.classloader.model.ArtifactCoordinates.DEFAULT_ARTIFACT_TYPE;

import static java.lang.String.format;
import static java.nio.file.Files.walk;
import static java.nio.file.Paths.get;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.codehaus.plexus.util.xml.Xpp3DomUtils.mergeXpp3Dom;

import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.SettingsSupplierFactory;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.client.internal.AetherMavenClient;
import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.activation.internal.deployable.DeployablePluginsDependenciesResolver;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tools.api.classloader.model.ApplicationGAVModel;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Implementation of {@link DeployableProjectModelBuilder} that uses Maven.
 *
 * @since 4.5
 */
public class MavenDeployableProjectModelBuilder
    implements DeployableProjectModelBuilder {

  private static final String DEFAULT_PACKAGE_EXPORT = "";
  private static final String JAVA_EXTENSION = "java";
  private static final String PACKAGE_SEPARATOR = ".";

  private static final String GROUP_ID = "groupId";
  private static final String ARTIFACT_ID = "artifactId";
  private static final String VERSION = "version";

  private static final String CLASS_PATH_SEPARATOR = "/";

  private static final String DEFAULT_SOURCES_DIRECTORY = "src/main";
  private static final String DEFAULT_SOURCES_JAVA_DIRECTORY = "/java";
  private static final String DEFAULT_RESOURCES_DIRECTORY = "/resources";
  private static final String DEFAULT_MULE_DIRECTORY = "/mule";

  private final File projectFolder;
  private final MavenConfiguration mavenConfiguration;
  private List<String> packages = emptyList();
  private List<String> resources = emptyList();

  private final List<Path> resourcesPath = new ArrayList<>();
  private List<BundleDependency> deployableMavenBundleDependencies;
  private Map<ArtifactCoordinates, List<Artifact>> pluginsArtifactDependencies;
  private List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency> deployableBundleDependencies;
  private Set<org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor> sharedDeployableBundleDescriptors;
  private Map<org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor, List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency>> additionalPluginDependencies;
  private Map<BundleDescriptor, List<org.mule.runtime.module.artifact.api.descriptor.BundleDependency>> pluginsBundleDependencies;
  private File deployableArtifactRepositoryFolder;
  private boolean exportResourcesAndPackagedIfEmptyLoaderDescriptor = false;

  public MavenDeployableProjectModelBuilder(File projectFolder, MavenConfiguration mavenConfiguration,
                                            boolean exportResourcesAndPackagedIfEmptyLoaderDescriptor) {
    this.projectFolder = projectFolder;
    this.mavenConfiguration = mavenConfiguration;
    this.exportResourcesAndPackagedIfEmptyLoaderDescriptor = exportResourcesAndPackagedIfEmptyLoaderDescriptor;
  }

  public MavenDeployableProjectModelBuilder(File projectFolder, MavenConfiguration mavenConfiguration) {
    this.projectFolder = projectFolder;
    this.mavenConfiguration = mavenConfiguration;
  }

  public MavenDeployableProjectModelBuilder(File projectFolder, boolean exportResourcesAndPackagedIfEmptyLoaderDescriptor) {
    this(projectFolder, getDefaultMavenConfiguration(), exportResourcesAndPackagedIfEmptyLoaderDescriptor);
  }

  public MavenDeployableProjectModelBuilder(File projectFolder) {
    this(projectFolder, getDefaultMavenConfiguration());
  }



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

  @Override
  public DeployableProjectModel build() {
    File pom = getPomFromFolder(projectFolder);
    Model pomModel = getPomModelFromFile(pom);

    deployableArtifactRepositoryFolder = this.mavenConfiguration.getLocalMavenRepositoryLocation();

    ArtifactCoordinates deployableArtifactCoordinates = getDeployableProjectArtifactCoordinates(pomModel);

    AetherMavenClient aetherMavenClient = new AetherMavenClient(mavenConfiguration);
    List<String> activeProfiles = mavenConfiguration.getActiveProfiles().orElse(emptyList());

    resolveDeployableDependencies(aetherMavenClient, pom, pomModel, activeProfiles);

    resolveDeployablePluginsData(deployableMavenBundleDependencies);

    resolveAdditionalPluginDependencies(aetherMavenClient, pomModel, activeProfiles, pluginsArtifactDependencies);

    // Get exported resources and packages
    try {
      getAvailablePackagesAndResources(pomModel.getBuild());
    } catch (IOException e) {
      throw new ArtifactActivationException(createStaticMessage("Couldn't search exported packages and resources"), e);
    }

    return new DeployableProjectModel(packages, resources, resourcesPath,
                                      buildBundleDescriptor(deployableArtifactCoordinates),
                                      getModelResolver(deployableArtifactCoordinates),
                                      projectFolder, deployableBundleDependencies,
                                      sharedDeployableBundleDescriptors, additionalPluginDependencies);
  }

  private Supplier<MuleDeployableModel> getModelResolver(ArtifactCoordinates deployableArtifactCoordinates) {
    if (deployableArtifactCoordinates.getClassifier().equals(MULE_APPLICATION_CLASSIFIER)) {
      return () -> {
        MuleApplicationModel applicationModel = applicationModelResolver().resolve(projectFolder);
        if (exportResourcesAndPackagedIfEmptyLoaderDescriptor && applicationModel.getClassLoaderModelLoaderDescriptor() == null) {
          applicationModel = buildModelWithResourcesAndClasses(applicationModel);
        }
        return applicationModel;
      };
    } else if (deployableArtifactCoordinates.getClassifier().equals(MULE_DOMAIN_CLASSIFIER)) {
      return () -> domainModelResolver().resolve(projectFolder);
    } else {
      throw new IllegalStateException("project is not a " + MULE_APPLICATION_CLASSIFIER + " or " + MULE_DOMAIN_CLASSIFIER);
    }
  }

  private MuleApplicationModel buildModelWithResourcesAndClasses(MuleApplicationModel applicationModel) {
    MuleApplicationModel.MuleApplicationModelBuilder builder = new MuleApplicationModel.MuleApplicationModelBuilder()
        .setName(applicationModel.getName() != null ? applicationModel.getName() : "mule")
        .setMinMuleVersion(applicationModel.getMinMuleVersion())
        .setRequiredProduct(applicationModel.getRequiredProduct())
        .withClassLoaderModelDescriptorLoader(createDescriptorWithResourcesAndClasses())
        .withBundleDescriptorLoader(applicationModel.getBundleDescriptorLoader() != null
            ? applicationModel.getBundleDescriptorLoader()
            : new MuleArtifactLoaderDescriptor("mule", emptyMap()))
        .setDomain(applicationModel.getDomain().orElse(null));
    builder.setConfigs(applicationModel.getConfigs());
    builder.setRedeploymentEnabled(applicationModel.isRedeploymentEnabled());
    builder.setSecureProperties(applicationModel.getSecureProperties());
    builder.setLogConfigFile(applicationModel.getLogConfigFile());
    return builder.build();
  }

  private MuleArtifactLoaderDescriptor createDescriptorWithResourcesAndClasses() {
    Map<String, Object> attributes = of("exportedResources", resources, "exportedPackages", packages);
    return new MuleArtifactLoaderDescriptor("mule", attributes);
  }

  private ArtifactCoordinates getDeployableProjectArtifactCoordinates(Model pomModel) {
    ApplicationGAVModel deployableGAVModel =
        new ApplicationGAVModel(pomModel.getGroupId(), pomModel.getArtifactId(), pomModel.getVersion());
    return getDeployableArtifactCoordinates(pomModel, deployableGAVModel);
  }

  /**
   * Resolves the dependencies of the deployable in the various forms needed to obtain the {@link DeployableProjectModel}.
   *
   * @param aetherMavenClient the configured {@link AetherMavenClient}.
   * @param pom               POM file.
   * @param pomModel          parsed POM model.
   * @param activeProfiles    active Maven profiles.
   */
  private void resolveDeployableDependencies(AetherMavenClient aetherMavenClient, File pom, Model pomModel,
                                             List<String> activeProfiles) {
    DeployableDependencyResolver deployableDependencyResolver = new DeployableDependencyResolver(aetherMavenClient);

    // Resolve the Maven bundle dependencies
    // TODO W-11203142 - determine the inclusion of test dependencies
    deployableMavenBundleDependencies = deployableDependencyResolver.resolveDeployableDependencies(pom, false, empty());

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

  private BundleDescriptor buildBundleDescriptor(ArtifactCoordinates artifactCoordinates) {
    return new BundleDescriptor.Builder()
        .setArtifactId(artifactCoordinates.getArtifactId())
        .setGroupId(artifactCoordinates.getGroupId())
        .setVersion(artifactCoordinates.getVersion())
        .setBaseVersion(artifactCoordinates.getVersion())
        .setType(artifactCoordinates.getType())
        .setClassifier(artifactCoordinates.getClassifier())
        .build();
  }

  private void getAvailablePackagesAndResources(Build build) throws IOException {
    String sourceDirectory = build.getSourceDirectory() != null ? build.getSourceDirectory() : DEFAULT_SOURCES_DIRECTORY;
    Path javaDirectory = get(projectFolder.getAbsolutePath(), sourceDirectory.concat(DEFAULT_SOURCES_JAVA_DIRECTORY));
    // look for all the sources under the java directory
    List<Path> allJavaFiles = walk(javaDirectory)
        .filter(Files::isRegularFile)
        .collect(toList());
    Predicate<Path> isJavaFile = path -> getExtension(path.toString()).endsWith(JAVA_EXTENSION);
    packages = allJavaFiles.stream()
        .filter(isJavaFile)
        .map(path -> {
          Path parent = javaDirectory.relativize(path).getParent();
          // if parent is null, it implies "default package" in java, which means we need an empty string for the
          // exportedPackages
          return parent != null ? parent.toString() : DEFAULT_PACKAGE_EXPORT;
        })
        .map(this::escapeSlashes)
        .map(s -> s.replace(CLASS_PATH_SEPARATOR, PACKAGE_SEPARATOR))
        .distinct()
        .collect(toList());

    if (build.getResources().isEmpty()) {
      resources = getResourcesInFolder(sourceDirectory.concat(DEFAULT_RESOURCES_DIRECTORY));
    } else {
      resources = build.getResources().stream()
          .flatMap(r -> {
            try {
              return getResourcesInFolder(r.getDirectory()).stream();
            } catch (IOException e) {
              throw new IllegalStateException("Cannot load files from" + r.getDirectory());
            }
          }).collect(toList());
    }
    resources.addAll(getResourcesInFolder(sourceDirectory.concat(DEFAULT_MULE_DIRECTORY)));

    // TODO W-11203142 - add test resources
  }

  private List<String> getResourcesInFolder(String resourcesDirectoryName) throws IOException {
    Path resourcesDirectory = get(projectFolder.getAbsolutePath(), resourcesDirectoryName);
    resourcesPath.add(resourcesDirectory);
    // look for all the sources under the resources directory
    List<Path> allResourcesFiles = walk(resourcesDirectory)
        .filter(Files::isRegularFile)
        .collect(toList());

    return allResourcesFiles.stream()
        .map(resourcesDirectory::relativize)
        .map(Path::toString)
        .map(this::escapeSlashes)
        .collect(toList());
  }

  private String escapeSlashes(String p) {
    return p.replace("\\", CLASS_PATH_SEPARATOR);
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

  private File getPomFromFolder(File projectFolder) {
    String pomFilePath = "pom.xml";

    File pomFile = new File(projectFolder, pomFilePath);
    checkState(pomFile.exists(),
               format("The pom.xml file for artifact in folder %s could not be found",
                      projectFolder.getAbsolutePath()));

    return pomFile;
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
