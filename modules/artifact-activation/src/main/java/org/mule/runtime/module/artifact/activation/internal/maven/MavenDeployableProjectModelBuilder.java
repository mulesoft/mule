/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static org.mule.runtime.api.deployment.meta.Product.MULE_EE;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
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
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import static org.apache.commons.io.FilenameUtils.getExtension;

import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.pom.parser.api.MavenPomParser;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.activation.api.deployable.MuleProjectStructure;
import org.mule.runtime.module.artifact.activation.api.descriptor.MuleConfigurationsFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Implementation of {@link DeployableProjectModelBuilder} that uses Maven.
 *
 * @since 4.5
 */
public class MavenDeployableProjectModelBuilder extends AbstractMavenDeployableProjectModelBuilder {

  private static final String POM_FILE_PATH = "pom.xml";
  private static final String DEFAULT_PACKAGE_EXPORT = "";
  private static final String JAVA_EXTENSION = "java";
  private static final String PACKAGE_SEPARATOR = ".";
  private static final String CLASS_PATH_SEPARATOR = "/";

  private static final MuleConfigurationsFilter MULE_CONFIGURATIONS_FILTER =
      MuleConfigurationsFilter.defaultMuleConfigurationsFilter();

  private final List<Path> resourcesPath = new ArrayList<>();
  private final boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor;
  private final boolean includeTestDependencies;

  public MavenDeployableProjectModelBuilder(File projectFolder, MavenConfiguration mavenConfiguration,
                                            boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor,
                                            boolean includeTestDependencies) {
    super(mavenConfiguration, projectFolder);
    this.exportAllResourcesAndPackagesIfEmptyLoaderDescriptor = exportAllResourcesAndPackagesIfEmptyLoaderDescriptor;
    this.includeTestDependencies = includeTestDependencies;
  }

  public MavenDeployableProjectModelBuilder(File projectFolder, boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor) {
    this(projectFolder, DEFAULT_MAVEN_CONFIGURATION.get(), exportAllResourcesAndPackagesIfEmptyLoaderDescriptor, false);
  }

  public MavenDeployableProjectModelBuilder(File projectFolder, boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor,
                                            boolean includeTestDependencies) {
    this(projectFolder, DEFAULT_MAVEN_CONFIGURATION.get(), exportAllResourcesAndPackagesIfEmptyLoaderDescriptor,
         includeTestDependencies);
  }

  public MavenDeployableProjectModelBuilder(File projectFolder) {
    this(projectFolder, DEFAULT_MAVEN_CONFIGURATION.get(), false, false);
  }

  @Override
  protected DeployableProjectModel doBuild(MavenPomParser parser, ArtifactCoordinates deployableArtifactCoordinates) {
    final MuleProjectStructure projectStructure =
        new DefaultMuleProjectStructure(projectFolder.toPath(), parser, isIncludeTestDependencies());

    return doBuild(projectStructure, deployableArtifactCoordinates);
  }

  protected DeployableProjectModel doBuild(final MuleProjectStructure projectStructure,
                                           ArtifactCoordinates deployableArtifactCoordinates) {
    // Get exported resources and packages
    try {
      List<String> packages = getAvailablePackages(projectStructure);
      List<String> muleResources = getAvailableMuleResources(projectStructure);
      List<String> nonMuleResources = getAvailableNonMuleResources(projectStructure);
      List<String> allResources = concat(muleResources.stream(), nonMuleResources.stream()).collect(toList());
      Set<String> muleConfigs = getConfigs(projectStructure.getMuleResourcesDirectory(), muleResources);

      return new DeployableProjectModel(packages,
                                        allResources,
                                        resourcesPath,
                                        buildBundleDescriptor(deployableArtifactCoordinates),
                                        getDeployableModelResolver(deployableArtifactCoordinates,
                                                                   allResources,
                                                                   muleConfigs,
                                                                   packages),
                                        of(projectStructure),
                                        projectFolder,
                                        deployableBundleDependencies,
                                        sharedDeployableBundleDescriptors,
                                        additionalPluginDependencies);
    } catch (IOException e) {
      throw new ArtifactActivationException(createStaticMessage("Couldn't search exported packages and resources"), e);
    }
  }

  private Supplier<MuleDeployableModel> getDeployableModelResolver(ArtifactCoordinates deployableArtifactCoordinates,
                                                                   List<String> allResources, Set<String> muleConfigs,
                                                                   List<String> packages) {
    if (deployableArtifactCoordinates.getClassifier().map(MULE_APPLICATION_CLASSIFIER::equals).orElse(false)) {
      return () -> buildApplicationModel(applicationModelResolver().resolve(projectFolder),
                                         allResources, muleConfigs, packages);
    } else if (deployableArtifactCoordinates.getClassifier().map(MULE_DOMAIN_CLASSIFIER::equals).orElse(false)) {
      return () -> buildDomainModel(domainModelResolver().resolve(projectFolder),
                                    allResources, muleConfigs, packages);
    } else {
      throw new IllegalStateException("project is not a " + MULE_APPLICATION_CLASSIFIER + " or " + MULE_DOMAIN_CLASSIFIER);
    }
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
        .setRequiredProduct(applicationModel.getRequiredProduct() != null ? applicationModel.getRequiredProduct() : MULE_EE)
        .withClassLoaderModelDescriptorLoader(createClassLoaderModelDescriptorLoader(applicationModel
            .getClassLoaderModelLoaderDescriptor(), allResources, packages))
        .withBundleDescriptorLoader(applicationModel.getBundleDescriptorLoader() != null
            ? applicationModel.getBundleDescriptorLoader()
            : new MuleArtifactLoaderDescriptor("mule", emptyMap()))
        .setDomain(applicationModel.getDomain().orElse(null));

    builder.setConfigs(applicationModel.getConfigs() != null ? applicationModel.getConfigs() : muleConfigs);
    builder.setRedeploymentEnabled(applicationModel.isRedeploymentEnabled());
    builder.setSecureProperties(applicationModel.getSecureProperties());
    builder.setSupportedJavaVersions(applicationModel.getSupportedJavaVersions());
    builder.setLogConfigFile(applicationModel.getLogConfigFile());
    return builder.build();
  }

  private MuleDomainModel buildDomainModel(MuleDomainModel domainModel, List<String> allResources,
                                           Set<String> muleConfigs, List<String> packages) {
    MuleDomainModel.MuleDomainModelBuilder builder = new MuleDomainModel.MuleDomainModelBuilder()
        .setName(domainModel.getName() != null ? domainModel.getName() : "mule")
        .setMinMuleVersion(domainModel.getMinMuleVersion())
        .setRequiredProduct(domainModel.getRequiredProduct() != null ? domainModel.getRequiredProduct() : MULE_EE)
        .withClassLoaderModelDescriptorLoader(createClassLoaderModelDescriptorLoader(domainModel
            .getClassLoaderModelLoaderDescriptor(), allResources, packages))
        .withBundleDescriptorLoader(domainModel.getBundleDescriptorLoader() != null
            ? domainModel.getBundleDescriptorLoader()
            : new MuleArtifactLoaderDescriptor("mule", emptyMap()));

    builder.setConfigs(domainModel.getConfigs() != null ? domainModel.getConfigs() : muleConfigs);
    builder.setRedeploymentEnabled(domainModel.isRedeploymentEnabled());
    builder.setSecureProperties(domainModel.getSecureProperties());
    builder.setSupportedJavaVersions(domainModel.getSupportedJavaVersions());
    builder.setLogConfigFile(domainModel.getLogConfigFile());
    return builder.build();
  }

  private Set<String> getConfigs(Path muleResourcesDirectory, List<String> muleResources) {
    return muleResources.stream().filter(muleResource -> MULE_CONFIGURATIONS_FILTER
        .filter(resolveCandidateConfigsPath(muleResourcesDirectory, muleResource))).collect(toSet());
  }

  private File resolveCandidateConfigsPath(Path muleResourcesDirectory, String candidateConfigFileName) {
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

  private List<String> getAvailablePackages(MuleProjectStructure projectStructure) throws IOException {
    Path javaDirectory = projectStructure.getJavaDirectory();

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

  private List<String> getAvailableMuleResources(MuleProjectStructure projectStructure) throws IOException {
    List<String> resources = getResourcesInFolder(projectStructure.getMuleResourcesDirectory());
    if (resources.isEmpty()) {
      throw new IOException(projectStructure.getMuleResourcesDirectory().toAbsolutePath().toString()
          + " cannot be empty");
    }

    return resources;
  }

  private List<String> getAvailableNonMuleResources(MuleProjectStructure projectStructure) throws IOException {
    List<String> resources = new ArrayList<>();
    final Collection<Path> resourcesDirectories = projectStructure.getResourcesDirectories();
    for (Path r : resourcesDirectories) {
      try {
        resources.addAll(getResourcesInFolder(r));
      } catch (IOException e) {
        throw new IOException("Cannot load files from" + r + ": " + e.getMessage());
      }
    }

    return resources;
  }

  private List<String> getResourcesInFolder(Path resourcesDirectory) throws IOException {
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
    File pomFile = new File(projectFolder, POM_FILE_PATH);

    if (!pomFile.exists()) {
      throw new IllegalStateException(format("The pom.xml file for artifact in folder %s could not be found",
                                             projectFolder.getAbsolutePath()));
    }

    return pomFile;
  }

}
