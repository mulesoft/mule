/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.module.artifact.activation.api.deployable.ArtifactModelResolver.applicationModelResolver;
import static org.mule.runtime.module.artifact.activation.api.deployable.ArtifactModelResolver.domainModelResolver;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_APPLICATION_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_DOMAIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.INCLUDE_TEST_DEPENDENCIES;

import static java.lang.String.format;
import static java.nio.file.Files.notExists;
import static java.nio.file.Files.walk;
import static java.nio.file.Paths.get;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import static org.apache.commons.io.FilenameUtils.getExtension;

import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.SettingsSupplierFactory;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.activation.api.descriptor.MuleConfigurationsFilter;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  private static final String DEFAULT_PACKAGE_EXPORT = "";
  private static final String JAVA_EXTENSION = "java";
  private static final String PACKAGE_SEPARATOR = ".";
  private static final String CLASS_PATH_SEPARATOR = "/";
  private static final String DEFAULT_SOURCES_DIRECTORY = "src/main";
  private static final String DEFAULT_SOURCES_JAVA_DIRECTORY = "/java";
  private static final String DEFAULT_RESOURCES_DIRECTORY = "/resources";
  private static final String DEFAULT_MULE_DIRECTORY = "/mule";
  private static final String DEFAULT_TEST_RESOURCES_DIRECTORY = "/test/resources";

  private final List<Path> resourcesPath = new ArrayList<>();
  private final boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor;
  private final boolean includeTestDependencies;
  private final MuleConfigurationsFilter muleConfigurationsFilter = MuleConfigurationsFilter.defaultMuleConfigurationsFilter();

  public MavenDeployableProjectModelBuilder(File projectFolder, MavenConfiguration mavenConfiguration,
                                            boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor,
                                            boolean includeTestDependencies) {
    super(mavenConfiguration, projectFolder);
    this.exportAllResourcesAndPackagesIfEmptyLoaderDescriptor = exportAllResourcesAndPackagesIfEmptyLoaderDescriptor;
    this.includeTestDependencies = includeTestDependencies;
  }

  public MavenDeployableProjectModelBuilder(File projectFolder, boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor) {
    this(projectFolder, getDefaultMavenConfiguration(), exportAllResourcesAndPackagesIfEmptyLoaderDescriptor, false);
  }

  public MavenDeployableProjectModelBuilder(File projectFolder, boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor,
                                            boolean includeTestDependencies) {
    this(projectFolder, getDefaultMavenConfiguration(), exportAllResourcesAndPackagesIfEmptyLoaderDescriptor,
         includeTestDependencies);
  }

  public MavenDeployableProjectModelBuilder(File projectFolder) {
    this(projectFolder, getDefaultMavenConfiguration(), false, false);
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
  protected DeployableProjectModel doBuild(Model pomModel, ArtifactCoordinates deployableArtifactCoordinates) {
    // Get exported resources and packages
    try {
      List<String> packages = getAvailablePackages(pomModel.getBuild());
      List<String> muleResources = getAvailableMuleResources(pomModel.getBuild());
      List<String> nonMuleResources = getAvailableNonMuleResources(pomModel.getBuild());
      List<String> allResources = concat(muleResources.stream(), nonMuleResources.stream()).collect(toList());
      Set<String> muleConfigs = getConfigs(getMuleResourcesDirectory(pomModel.getBuild()), muleResources);

      return new DeployableProjectModel(packages, allResources, resourcesPath,
                                        buildBundleDescriptor(deployableArtifactCoordinates),
                                        getDeployableModelResolver(deployableArtifactCoordinates, allResources, muleConfigs,
                                                                   packages),
                                        projectFolder, deployableBundleDependencies,
                                        sharedDeployableBundleDescriptors, additionalPluginDependencies);
    } catch (IOException e) {
      throw new ArtifactActivationException(createStaticMessage("Couldn't search exported packages and resources"), e);
    }
  }

  private Supplier<MuleDeployableModel> getDeployableModelResolver(ArtifactCoordinates deployableArtifactCoordinates,
                                                                   List<String> allResources, Set<String> muleConfigs,
                                                                   List<String> packages) {
    if (deployableArtifactCoordinates.getClassifier().equals(MULE_APPLICATION_CLASSIFIER)) {
      return () -> {
        MuleApplicationModel applicationModel = applicationModelResolver().resolve(projectFolder);
        if (shouldEditApplicationModel(applicationModel)) {
          applicationModel = buildApplicationModel(applicationModel, allResources, muleConfigs, packages);
        }
        return applicationModel;
      };
    } else if (deployableArtifactCoordinates.getClassifier().equals(MULE_DOMAIN_CLASSIFIER)) {
      // TODO W-12428790 - the domain model creation must take into account the same concerns as the application model does
      return () -> domainModelResolver().resolve(projectFolder);
    } else {
      throw new IllegalStateException("project is not a " + MULE_APPLICATION_CLASSIFIER + " or " + MULE_DOMAIN_CLASSIFIER);
    }
  }

  private boolean shouldEditApplicationModel(MuleApplicationModel applicationModel) {
    return (exportAllResourcesAndPackagesIfEmptyLoaderDescriptor
        && applicationModel.getClassLoaderModelLoaderDescriptor() == null) || includeTestDependencies
        || applicationModel.getConfigs() == null;
  }

  @Override
  protected boolean isIncludeTestDependencies() {
    return includeTestDependencies;
  }

  private MuleApplicationModel buildApplicationModel(MuleApplicationModel applicationModel, List<String> allResources,
                                                     Set<String> muleConfigs, List<String> packages) {
    MuleApplicationModel.MuleApplicationModelBuilder builder = new MuleApplicationModel.MuleApplicationModelBuilder()
        .setName(applicationModel.getName() != null ? applicationModel.getName() : "mule")
        .setMinMuleVersion(applicationModel.getMinMuleVersion())
        .setRequiredProduct(applicationModel.getRequiredProduct())
        .withClassLoaderModelDescriptorLoader(createClassLoaderModelDescriptorLoader(applicationModel
            .getClassLoaderModelLoaderDescriptor(), allResources, packages))
        .withBundleDescriptorLoader(applicationModel.getBundleDescriptorLoader() != null
            ? applicationModel.getBundleDescriptorLoader()
            : new MuleArtifactLoaderDescriptor("mule", emptyMap()))
        .setDomain(applicationModel.getDomain().orElse(null));

    builder.setConfigs(applicationModel.getConfigs() != null ? applicationModel.getConfigs() : muleConfigs);
    builder.setRedeploymentEnabled(applicationModel.isRedeploymentEnabled());
    builder.setSecureProperties(applicationModel.getSecureProperties());
    builder.setLogConfigFile(applicationModel.getLogConfigFile());
    return builder.build();
  }

  private Set<String> getConfigs(String muleResourcesDirectory, List<String> muleResources) {
    return muleResources.stream().filter(muleResource -> muleConfigurationsFilter
        .filter(resolveCandidateConfigsPath(muleResourcesDirectory, muleResource))).collect(toSet());
  }

  private File resolveCandidateConfigsPath(String muleResourcesDirectory, String candidateConfigFileName) {
    return get(projectFolder.getAbsolutePath()).resolve(muleResourcesDirectory).resolve(candidateConfigFileName).toFile();
  }

  private MuleArtifactLoaderDescriptor createClassLoaderModelDescriptorLoader(MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor,
                                                                              List<String> resources, List<String> packages) {
    final String id = classLoaderModelLoaderDescriptor != null ? classLoaderModelLoaderDescriptor.getId() : "mule";
    Map<String, Object> attributes =
        classLoaderModelLoaderDescriptor != null && classLoaderModelLoaderDescriptor.getAttributes() != null
            ? classLoaderModelLoaderDescriptor.getAttributes()
            : new HashMap<>();

    if (exportAllResourcesAndPackagesIfEmptyLoaderDescriptor && classLoaderModelLoaderDescriptor == null) {
      attributes.put(EXPORTED_RESOURCES, resources);
      attributes.put(EXPORTED_PACKAGES, packages);
    }
    if (isIncludeTestDependencies()) {
      attributes.put(INCLUDE_TEST_DEPENDENCIES, "true");
    }

    return new MuleArtifactLoaderDescriptor(id, attributes);
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

  private String getMuleResourcesDirectory(Build build) {
    String sourceDirectory = build.getSourceDirectory() != null ? build.getSourceDirectory() : DEFAULT_SOURCES_DIRECTORY;

    return sourceDirectory.concat(DEFAULT_MULE_DIRECTORY);
  }

  private List<String> getAvailableMuleResources(Build build) throws IOException {
    String muleResourcesDirectory = getMuleResourcesDirectory(build);
    List<String> resources = getResourcesInFolder(muleResourcesDirectory);
    if (resources.isEmpty()) {
      throw new MuleRuntimeException(createStaticMessage(muleResourcesDirectory + " cannot be empty"));
    }

    return resources;
  }

  private List<String> getAvailableNonMuleResources(Build build) throws IOException {
    String sourceDirectory = build.getSourceDirectory() != null ? build.getSourceDirectory() : DEFAULT_SOURCES_DIRECTORY;
    List<String> resources = new ArrayList<>();

    // include test resources if test dependencies have to be considered
    if (isIncludeTestDependencies()) {
      resources.addAll(getResourcesInFolder(sourceDirectory.concat(DEFAULT_TEST_RESOURCES_DIRECTORY)));
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

    return resources;
  }

  private List<String> getResourcesInFolder(String resourcesDirectoryName) throws IOException {
    Path resourcesDirectory = get(projectFolder.getAbsolutePath(), resourcesDirectoryName);

    if (!resourcesDirectory.toFile().exists()) {
      return emptyList();
    }

    resourcesPath.add(resourcesDirectory);

    if (notExists(resourcesDirectory)) {
      return emptyList();
    }

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

  @Override
  protected File getPomFromFolder(File projectFolder) {
    String pomFilePath = "pom.xml";

    File pomFile = new File(projectFolder, pomFilePath);
    checkState(pomFile.exists(),
               format("The pom.xml file for artifact in folder %s could not be found",
                      projectFolder.getAbsolutePath()));

    return pomFile;
  }

}
