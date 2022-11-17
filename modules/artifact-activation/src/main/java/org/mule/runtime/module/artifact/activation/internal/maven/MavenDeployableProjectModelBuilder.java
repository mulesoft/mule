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
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_APPLICATION_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_DOMAIN_CLASSIFIER;

import static java.lang.String.format;
import static java.nio.file.Files.walk;
import static java.nio.file.Paths.get;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.ImmutableMap.of;
import static org.apache.commons.io.FilenameUtils.getExtension;

import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.SettingsSupplierFactory;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.client.internal.AetherMavenClient;
import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;

/**
 * Implementation of {@link DeployableProjectModelBuilder} that uses Maven.
 *
 * @since 4.5
 */
public class MavenDeployableProjectModelBuilder extends AbstractMavenDeployableProjectModelBuilder {

  private final File projectFolder;
  private final List<Path> resourcesPath = new ArrayList<>();
  private boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor = false;

  public MavenDeployableProjectModelBuilder(File projectFolder, MavenConfiguration mavenConfiguration,
                                            boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor) {
    super(mavenConfiguration);
    this.projectFolder = projectFolder;
    this.exportAllResourcesAndPackagesIfEmptyLoaderDescriptor = exportAllResourcesAndPackagesIfEmptyLoaderDescriptor;
  }

  public MavenDeployableProjectModelBuilder(File projectFolder, MavenConfiguration mavenConfiguration) {
    super(mavenConfiguration);
    this.projectFolder = projectFolder;
  }

  public MavenDeployableProjectModelBuilder(File projectFolder, boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor) {
    this(projectFolder, getDefaultMavenConfiguration(), exportAllResourcesAndPackagesIfEmptyLoaderDescriptor);
  }

  public MavenDeployableProjectModelBuilder(File projectFolder) {
    this(projectFolder, getDefaultMavenConfiguration());
  }

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
      List<String> packages = getAvailablePackages(pomModel.getBuild());
      List<String> resources = getAvailableResources(pomModel.getBuild());

      return new DeployableProjectModel(packages, resources, resourcesPath,
                                        buildBundleDescriptor(deployableArtifactCoordinates),
                                        getModelResolver(deployableArtifactCoordinates, resources, packages),
                                        projectFolder, deployableBundleDependencies,
                                        sharedDeployableBundleDescriptors, additionalPluginDependencies);
    } catch (IOException e) {
      throw new ArtifactActivationException(createStaticMessage("Couldn't search exported packages and resources"), e);
    }
  }

  private Supplier<MuleDeployableModel> getModelResolver(ArtifactCoordinates deployableArtifactCoordinates,
                                                         List<String> resources, List<String> packages) {
    if (deployableArtifactCoordinates.getClassifier().equals(MULE_APPLICATION_CLASSIFIER)) {
      return () -> {
        MuleApplicationModel applicationModel = applicationModelResolver().resolve(projectFolder);
        if (exportAllResourcesAndPackagesIfEmptyLoaderDescriptor
            && applicationModel.getClassLoaderModelLoaderDescriptor() == null) {
          applicationModel = buildModelWithResourcesAndClasses(applicationModel, resources, packages);
        }
        return applicationModel;
      };
    } else if (deployableArtifactCoordinates.getClassifier().equals(MULE_DOMAIN_CLASSIFIER)) {
      return () -> domainModelResolver().resolve(projectFolder);
    } else {
      throw new IllegalStateException("project is not a " + MULE_APPLICATION_CLASSIFIER + " or " + MULE_DOMAIN_CLASSIFIER);
    }
  }

  private MuleApplicationModel buildModelWithResourcesAndClasses(MuleApplicationModel applicationModel,
                                                                 List<String> resources, List<String> packages) {
    MuleApplicationModel.MuleApplicationModelBuilder builder = new MuleApplicationModel.MuleApplicationModelBuilder()
        .setName(applicationModel.getName() != null ? applicationModel.getName() : "mule")
        .setMinMuleVersion(applicationModel.getMinMuleVersion())
        .setRequiredProduct(applicationModel.getRequiredProduct())
        .withClassLoaderModelDescriptorLoader(createDescriptorWithResourcesAndClasses(resources, packages))
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

  private MuleArtifactLoaderDescriptor createDescriptorWithResourcesAndClasses(List<String> resources, List<String> packages) {
    Map<String, Object> attributes = of("exportedResources", resources, "exportedPackages", packages);
    return new MuleArtifactLoaderDescriptor("mule", attributes);
  }

  private List<String> getAvailablePackages(Build build) throws IOException {
    String sourceDirectory = build.getSourceDirectory() != null ? build.getSourceDirectory() : DEFAULT_SOURCES_DIRECTORY;
    Path javaDirectory = get(projectFolder.getAbsolutePath(), sourceDirectory.concat(DEFAULT_SOURCES_JAVA_DIRECTORY));
    // look for all the sources under the java directory
    if (!javaDirectory.toFile().exists()) {
      return emptyList();
    }

    List<Path> allJavaFiles = walk(javaDirectory)
        .filter(Files::isRegularFile)
        .collect(toList());
    Predicate<Path> isJavaFile = path -> getExtension(path.toString()).endsWith(JAVA_EXTENSION);

    return allJavaFiles.stream().filter(isJavaFile).map(path -> {
      Path parent = javaDirectory.relativize(path).getParent();
      // if parent is null, it implies "default package" in java, which means we need an empty string for the
      // exportedPackages
      return parent != null ? parent.toString() : DEFAULT_PACKAGE_EXPORT;
    }).map(this::escapeSlashes)
        .map(s -> s.replace(CLASS_PATH_SEPARATOR, PACKAGE_SEPARATOR))
        .distinct()
        .collect(toList());
  }

  private List<String> getAvailableResources(Build build) throws IOException {
    String sourceDirectory = build.getSourceDirectory() != null ? build.getSourceDirectory() : DEFAULT_SOURCES_DIRECTORY;
    List<String> resources = getResourcesInFolder(sourceDirectory.concat(DEFAULT_MULE_DIRECTORY));
    if (resources.isEmpty()) {
      throw new MuleRuntimeException(createStaticMessage(sourceDirectory.concat(DEFAULT_MULE_DIRECTORY) + " cannot be empty"));
    }

    if (build.getResources().isEmpty()) {
      resources.addAll(getResourcesInFolder(sourceDirectory.concat(DEFAULT_RESOURCES_DIRECTORY)));
    } else {
      build.getResources().forEach(r -> {
        try {
          resources.addAll(getResourcesInFolder(r.getDirectory()));
        } catch (IOException e) {
          throw new IllegalStateException("Cannot load files from" + r.getDirectory());
        }
      });
    }
    // TODO W-11203142 - add test resources

    return resources;
  }

  private List<String> getResourcesInFolder(String resourcesDirectoryName) throws IOException {
    Path resourcesDirectory = get(projectFolder.getAbsolutePath(), resourcesDirectoryName);

    if (!resourcesDirectory.toFile().exists()) {
      return emptyList();
    }

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

  private File getPomFromFolder(File projectFolder) {
    String pomFilePath = "pom.xml";

    File pomFile = new File(projectFolder, pomFilePath);
    checkState(pomFile.exists(),
               format("The pom.xml file for artifact in folder %s could not be found",
                      projectFolder.getAbsolutePath()));

    return pomFile;
  }

}
