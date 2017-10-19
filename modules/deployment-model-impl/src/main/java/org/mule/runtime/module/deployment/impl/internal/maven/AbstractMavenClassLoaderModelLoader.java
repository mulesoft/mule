/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static com.google.common.io.Files.createTempDir;
import static java.lang.Boolean.getBoolean;
import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.mule.maven.client.api.model.BundleScope.PROVIDED;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleHomeFolder;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.REPOSITORY_FOLDER;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.INCLUDE_TEST_DEPENDENCIES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.PRIVILEGED_ARTIFACTS_IDS;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.PRIVILEGED_EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils.isStandalone;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.deserialize;
import org.mule.maven.client.api.LocalRepositorySupplierFactory;
import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.tools.api.classloader.model.Artifact;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of {@link ClassLoaderModelLoader} that resolves the dependencies for all the mule artifacts and create
 * the {@link ClassLoaderModel}. It lets the implementations of this class to add artifact's specific class loader URLs
 * 
 * @since 4.0
 */
public abstract class AbstractMavenClassLoaderModelLoader implements ClassLoaderModelLoader {

  public static final String CLASSLOADER_MODEL_JSON_DESCRIPTOR = "classloader-model.json";
  public static final String CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR = "classloader-model-patch.json";
  public static final String CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION =
      Paths.get("META-INF", "mule-artifact", CLASSLOADER_MODEL_JSON_DESCRIPTOR).toString();
  public static final String CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR_LOCATION =
      Paths.get("META-INF", "mule-artifact", CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR).toString();

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final LocalRepositorySupplierFactory localRepositorySupplierFactory;
  private MavenClient mavenClient;

  public AbstractMavenClassLoaderModelLoader(MavenClient mavenClient,
                                             LocalRepositorySupplierFactory localRepositorySupplierFactory) {
    this.mavenClient = mavenClient;
    this.localRepositorySupplierFactory = localRepositorySupplierFactory;
  }

  @Override
  public String getId() {
    return MULE_LOADER_ID;
  }

  /**
   * Given an artifact location, it will resolve its dependencies on a Maven based mechanism. It will assume there's a repository
   * folder to look for the artifacts in it (which includes both JAR files as well as POM ones).
   * <p/>
   * It takes care of the transitive compile and runtime dependencies, from which will take the URLs to add them to the resulting
   * {@link ClassLoaderModel}, and it will also consume all Mule plugin dependencies so that further validations can check whether
   * or not all plugins are loaded in memory before running an application.
   * <p/>
   * Finally, it will also tell the resulting {@link ClassLoaderModel} which packages and/or resources has to export, consuming
   * the attributes from the {@link MuleArtifactLoaderDescriptor#getAttributes()} map.
   *
   * @param artifactFile {@link File} where the current plugin to work with.
   * @param attributes a set of attributes to work with, where the current implementation of this class will look for
   *        {@link ArtifactDescriptorConstants#EXPORTED_PACKAGES} and {@link ArtifactDescriptorConstants#EXPORTED_RESOURCES}
   * @return a {@link ClassLoaderModel} loaded with all its dependencies and URLs.
   */
  @Override
  public final ClassLoaderModel load(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    return createClassLoaderModel(artifactFile, attributes, artifactType);
  }

  private ClassLoaderModel createClassLoaderModel(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    if (isHeavyPackage(artifactFile)) {
      return createHeavyPackageClassLoaderModel(artifactFile, attributes);
    } else {
      return createLightPackageClassLoaderModel(artifactFile, attributes, artifactType);
    }
  }

  private ClassLoaderModel createHeavyPackageClassLoaderModel(File artifactFile, Map<String, Object> attributes) {
    File classLoaderModelDescriptor = getClassLoaderModelDescriptor(artifactFile);

    org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel =
        deserialize(classLoaderModelDescriptor);
    File deployableArtifactRepositoryFolder = getDeployableArtifactRepositoryFolder(artifactFile);

    final ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModel.ClassLoaderModelBuilder();
    classLoaderModelBuilder
        .exportingPackages(new HashSet<>(getAttribute(attributes, EXPORTED_PACKAGES)))
        .exportingPrivilegedPackages(new HashSet<>(getAttribute(attributes, PRIVILEGED_EXPORTED_PACKAGES)),
                                     new HashSet<>(getAttribute(attributes, PRIVILEGED_ARTIFACTS_IDS)))
        .exportingResources(new HashSet<>(getAttribute(attributes, EXPORTED_RESOURCES)));

    Set<BundleDependency> patchBundleDependencies =
        getPatchedBundledDependencies(artifactFile, deployableArtifactRepositoryFolder);

    Set<BundleDependency> bundleDependencies =
        packagerClassLoaderModel.getDependencies().stream().map(artifact -> {
          Optional<BundleDependency> patchedBundledDependency =
              patchBundleDependencies.stream().filter(bundleDependency -> bundleDependency.getDescriptor().getGroupId()
                  .equals(artifact.getArtifactCoordinates().getGroupId()) &&
                  bundleDependency.getDescriptor().getArtifactId().equals(artifact.getArtifactCoordinates().getArtifactId()))
                  .findAny();
          return patchedBundledDependency
              .orElse(createBundleDependencyFromPackagerDependency(deployableArtifactRepositoryFolder).apply(artifact));
        }).collect(toSet());

    loadUrls(artifactFile, classLoaderModelBuilder, bundleDependencies);
    classLoaderModelBuilder.dependingOn(bundleDependencies);

    return classLoaderModelBuilder.build();
  }

  private Set<BundleDependency> getPatchedBundledDependencies(File artifactFile, File deployableArtifactRepositoryFolder) {
    Set<BundleDependency> patchBundleDependencies = new HashSet<>();
    File classLoaderModelPatchDescriptor = getClassLoaderModelPatchDescriptor(artifactFile);
    if (classLoaderModelPatchDescriptor.exists()) {
      org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModelPatch =
          deserialize(classLoaderModelPatchDescriptor);
      patchBundleDependencies.addAll(packagerClassLoaderModelPatch.getDependencies().stream()
          .map(artifact -> createBundleDependencyFromPackagerDependency(deployableArtifactRepositoryFolder).apply(artifact))
          .collect(toSet()));
    }
    return patchBundleDependencies;
  }

  private File getDeployableArtifactRepositoryFolder(File artifactFile) {
    if (artifactFile.isDirectory()) {
      return artifactFile;
    }

    return findRepositoryFolder(artifactFile).getParentFile();
  }

  private File findRepositoryFolder(File artifactFile) {
    while (!getMuleHomeFolder().equals(artifactFile) && !REPOSITORY_FOLDER.equals(artifactFile.getName())) {
      artifactFile = artifactFile.getParentFile();
    }

    if (!REPOSITORY_FOLDER.equals(artifactFile.getName()) || !artifactFile.isDirectory()) {
      throw new IllegalStateException("Unable to find repository folder for artifact " + artifactFile.getAbsolutePath());
    }

    return artifactFile;
  }

  private Function<Artifact, BundleDependency> createBundleDependencyFromPackagerDependency(File artifactFile) {
    return d -> {
      File bundle = new File(artifactFile, d.getUri().toString());

      return new BundleDependency.Builder()
          .setDescriptor(new BundleDescriptor.Builder().setArtifactId(d.getArtifactCoordinates().getArtifactId())
              .setGroupId(d.getArtifactCoordinates().getGroupId())
              .setClassifier(d.getArtifactCoordinates().getClassifier())
              .setType(d.getArtifactCoordinates().getType()).setVersion(d.getArtifactCoordinates().getVersion())
              .build())
          .setBundleUri(bundle.toURI())
          .build();
    };
  }

  private boolean isHeavyPackage(File artifactFile) {
    return getClassLoaderModelDescriptor(artifactFile).exists();
  }

  protected File getClassLoaderModelDescriptor(File artifactFile) {
    return new File(artifactFile, CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION);
  }

  protected File getClassLoaderModelPatchDescriptor(File artifactFile) {
    return new File(artifactFile, CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR_LOCATION);
  }

  private ClassLoaderModel createLightPackageClassLoaderModel(File artifactFile, Map<String, Object> attributes,
                                                              ArtifactType artifactType) {
    File containerRepository;
    if (isStandalone() && !getBoolean("mule.mode.embedded")) {
      containerRepository = new File(getMuleHomeFolder(), "repository");
      if (!containerRepository.exists()) {
        if (!containerRepository.mkdirs()) {
          // check again since it may have been created already.
          if (!containerRepository.exists()) {
            throw new MuleRuntimeException(I18nMessageFactory
                .createStaticMessage("Failure creating repository folder in MULE_HOME folder "
                    + containerRepository.getAbsolutePath()));
          }
        }
      }
    }

    File localMavenRepositoryLocation = mavenClient.getMavenConfiguration().getLocalMavenRepositoryLocation();

    Supplier<File> compositeRepoLocationSupplier =
        localRepositorySupplierFactory
            .composeSuppliers(localRepositorySupplierFactory.artifactFolderRepositorySupplier(artifactFile,
                                                                                              localMavenRepositoryLocation),
                              localRepositorySupplierFactory.fixedFolderSupplier(localMavenRepositoryLocation));
    File mavenRepository = compositeRepoLocationSupplier.get();
    File temporaryDirectory = createTempDir();
    try {
      List<org.mule.maven.client.api.model.BundleDependency> dependencies =
          mavenClient.resolveArtifactDependencies(artifactFile, includeTestDependencies(attributes),
                                                  includeProvidedDependencies(artifactType), of(mavenRepository),
                                                  of(temporaryDirectory));
      final ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModel.ClassLoaderModelBuilder();
      classLoaderModelBuilder
          .exportingPackages(new HashSet<>(getAttribute(attributes, EXPORTED_PACKAGES)))
          .exportingPrivilegedPackages(new HashSet<>(getAttribute(attributes, PRIVILEGED_EXPORTED_PACKAGES)),
                                       new HashSet<>(getAttribute(attributes, PRIVILEGED_ARTIFACTS_IDS)))
          .exportingResources(new HashSet<>(getAttribute(attributes, EXPORTED_RESOURCES)))
          .includeTestDependencies(valueOf(getSimpleAttribute(attributes, INCLUDE_TEST_DEPENDENCIES, "false")));
      Set<BundleDependency> bundleDependencies =
          dependencies.stream().filter(mavenClientDependency -> !mavenClientDependency.getScope().equals(PROVIDED))
              .map(mavenClientDependency -> convertBundleDependency(mavenClientDependency)).collect(toSet());
      loadUrls(artifactFile, classLoaderModelBuilder, bundleDependencies);
      Set<BundleDependency> allBundleDependencies =
          dependencies.stream().map(mavenClientDependency -> convertBundleDependency(mavenClientDependency)).collect(toSet());
      classLoaderModelBuilder.dependingOn(allBundleDependencies);
      return classLoaderModelBuilder.build();
    } finally {
      deleteQuietly(temporaryDirectory);
    }
  }

  protected abstract boolean includeProvidedDependencies(ArtifactType artifactType);

  protected BundleDependency convertBundleDependency(org.mule.maven.client.api.model.BundleDependency mavenClientDependency) {
    BundleDependency.Builder builder = new BundleDependency.Builder()
        .setScope(BundleScope.valueOf(mavenClientDependency.getScope().name()))
        .setBundleUri(mavenClientDependency.getBundleUri())
        .setDescriptor(convertBundleDescriptor(mavenClientDependency.getDescriptor()));
    return builder.build();
  }

  private BundleDescriptor convertBundleDescriptor(org.mule.maven.client.api.model.BundleDescriptor descriptor) {
    BundleDescriptor.Builder builder = new BundleDescriptor.Builder().setGroupId(descriptor.getGroupId())
        .setArtifactId(descriptor.getArtifactId())
        // Use baseVersion as it will refer to the unresolved meta version (case of SNAPSHOTS instead of timestamp versions)
        .setVersion(descriptor.getBaseVersion())
        .setType(descriptor.getType());
    descriptor.getClassifier().ifPresent(builder::setClassifier);
    return builder.build();
  }

  /**
   * Template method to get the unresolved pom model from the artifact file
   *
   * @param artifactFile the artifact file
   * @return the pom model
   */
  protected Model loadPomModel(File artifactFile) {
    return mavenClient.getRawPomModel(artifactFile);
  }

  /**
   * Loads the URLs of the class loader for this artifact.
   * <p>
   * It let's implementations to add artifact specific URLs by letting them override
   * {@link #addArtifactSpecificClassloaderConfiguration(File, ClassLoaderModel.ClassLoaderModelBuilder, Set)}
   * 
   * @param artifactFile the artifact file for which the {@link ClassLoaderModel} is being generated.
   * @param classLoaderModelBuilder the builder of the {@link ClassLoaderModel}
   * @param dependencies the dependencies resolved for this artifact.
   */
  protected void loadUrls(File artifactFile, ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder,
                          Set<BundleDependency> dependencies) {
    addArtifactSpecificClassloaderConfiguration(artifactFile, classLoaderModelBuilder, dependencies);
    addDependenciesToClasspathUrls(classLoaderModelBuilder, dependencies);
  }

  private void addDependenciesToClasspathUrls(ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder,
                                              Set<BundleDependency> dependencies) {
    dependencies.stream()
        .filter(dependency -> !MULE_PLUGIN_CLASSIFIER.equals(dependency.getDescriptor().getClassifier().orElse(null)))
        .filter(dependency -> dependency.getBundleUri() != null)
        .forEach(dependency -> {
          try {
            classLoaderModelBuilder.containing(dependency.getBundleUri().toURL());
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }
        });
  }

  private List<String> getAttribute(Map<String, Object> attributes, String attribute) {
    final Object attributeObject = attributes.getOrDefault(attribute, new ArrayList<String>());
    checkArgument(attributeObject instanceof List, format("The '%s' attribute must be of '%s', found '%s'", attribute,
                                                          List.class.getName(), attributeObject.getClass().getName()));
    return (List<String>) attributeObject;
  }

  private <T> T getSimpleAttribute(Map<String, Object> attributes, String attribute, T defaultValue) {
    return (T) attributes.getOrDefault(attribute, defaultValue);
  }

  /**
   * Template method to enable/disable test dependencies as part of the artifact classpath.
   *
   * @return true if test dependencies must be part of the artifact classpath, false otherwise.
   */
  protected boolean includeTestDependencies(Map<String, Object> attributes) {
    return false;
  }

  /**
   * Template method to add artifact specific configuration to the {@link ClassLoaderModel.ClassLoaderModelBuilder}
   *
   * @param artifactFile the artifact file from which the classloader model is generated.
   * @param classLoaderModelBuilder the builder used to generate {@link ClassLoaderModel} of the artifact.
   * @param dependencies the set of dependencies of the artifact.
   */
  protected void addArtifactSpecificClassloaderConfiguration(File artifactFile,
                                                             ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder,
                                                             Set<BundleDependency> dependencies) {

  }

}
