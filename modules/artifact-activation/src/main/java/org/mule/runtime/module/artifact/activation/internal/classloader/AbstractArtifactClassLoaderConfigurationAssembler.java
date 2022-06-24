/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleHomeFolder;
import static org.mule.runtime.module.artifact.activation.internal.ExecutionContext.isMuleFramework;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import static com.google.common.collect.Sets.newHashSet;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.internal.deployable.DeployableClassLoaderConfigurationBuilder;
import org.mule.runtime.module.artifact.activation.internal.plugin.MuleArtifactPatchingModel;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.ImmutableSet;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.slf4j.Logger;

/**
 * Assembles the class loader configuration for an artifact given all its pieces.
 */
public abstract class AbstractArtifactClassLoaderConfigurationAssembler {

  private static final Logger LOGGER = getLogger(AbstractArtifactClassLoaderConfigurationAssembler.class);

  private static final String MULE_RUNTIME_GROUP_ID = "org.mule.runtime";
  private static final String MULE_RUNTIME_MODULES_GROUP_ID = "com.mulesoft.mule.runtime.modules";

  protected final org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel;

  public AbstractArtifactClassLoaderConfigurationAssembler(org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel) {
    this.packagerClassLoaderModel = packagerClassLoaderModel;
  }

  public ClassLoaderModel createClassLoaderModel() {
    ClassLoaderModelBuilder classLoaderConfigurationBuilder = getClassLoaderConfigurationBuilder();

    classLoaderConfigurationBuilder
        .exportingPackages(newHashSet(packagerClassLoaderModel.getPackages()))
        .exportingResources(newHashSet(packagerClassLoaderModel.getResources()));

    List<BundleDependency> bundleDependencies = getProcessedBundleDependencies();

    // includePatches(classLoaderConfigurationBuilder);

    final List<URL> dependenciesArtifactsUrls =
        loadUrls(getProjectFolder(), bundleDependencies, classLoaderConfigurationBuilder);
    dependenciesArtifactsUrls.forEach(classLoaderConfigurationBuilder::containing);

    if (shouldPopulateLocalPackages()) {
      populateLocalPackages(packagerClassLoaderModel, classLoaderConfigurationBuilder);
    }

    classLoaderConfigurationBuilder.dependingOn(new HashSet<>(bundleDependencies));

    return classLoaderConfigurationBuilder.build();
  }

  // private void includePatches(ClassLoaderModelBuilder classLoaderConfigurationBuilder) {
  // if (!isMuleFramework()) {
  // // TODO W-11202141 - consider artifact patches for the case this is run within a Runtime
  // getArtifactPatches().forEach(classLoaderConfigurationBuilder::containing);
  // }
  // }

  // private List<URL> getArtifactPatches() {
  // List<URL> patches = new ArrayList<>();
  // ArtifactCoordinates currentArtifactCoordinates = packagerClassLoaderModel.getArtifactCoordinates();
  // String artifactId = currentArtifactCoordinates.getGroupId() + ":"
  // + currentArtifactCoordinates.getArtifactId() + ":" + currentArtifactCoordinates.getVersion();
  // try {
  // File muleArtifactPatchesFolder = new File(getMuleHomeFolder(), MULE_ARTIFACT_PATCHES_LOCATION);
  // if (muleArtifactPatchesFolder.exists()) {
  // String[] patchFiles = muleArtifactPatchesFolder.list((dir, name) -> name != null && name.endsWith(PATCH_FILES_EXTENSION));
  // if (patchFiles == null) {
  // LOGGER.warn("No patches available in patches folder for artifact {}",
  // currentArtifactCoordinates.getGroupId() + ":" + currentArtifactCoordinates.getArtifactId());
  // return emptyList();
  // }
  // for (String patchFile : patchFiles) {
  // MuleArtifactPatchingModel muleArtifactPatchingModel = MuleArtifactPatchingModel.loadModel(patchFile);
  // GenericVersionScheme genericVersionScheme = new GenericVersionScheme();
  // Version thisArtifactCoordinatesVersion;
  // try {
  // thisArtifactCoordinatesVersion = genericVersionScheme.parseVersion(currentArtifactCoordinates.getVersion());
  // } catch (Exception e) {
  // LOGGER.warn("Error parsing version {} for artifact {}, patches against this artifact will not be applied",
  // currentArtifactCoordinates.getVersion(),
  // currentArtifactCoordinates.getGroupId() + ":" + currentArtifactCoordinates.getArtifactId());
  // return emptyList();
  // }
  // ArtifactCoordinates patchedArtifactCoordinates = muleArtifactPatchingModel.getArtifactCoordinates();
  // if (patchedArtifactCoordinates.getGroupId().equals(currentArtifactCoordinates.getGroupId()) &&
  // patchedArtifactCoordinates.getArtifactId().equals(currentArtifactCoordinates.getArtifactId()) &&
  // patchedArtifactCoordinates.getClassifier().equals(currentArtifactCoordinates.getClassifier())) {
  // if (muleArtifactPatchingModel.getAffectedVersions()
  // .stream()
  // .anyMatch(affectedVersion -> {
  // try {
  // VersionConstraint versionConstraint = genericVersionScheme.parseVersionConstraint(affectedVersion);
  // return versionConstraint.containsVersion(thisArtifactCoordinatesVersion);
  // } catch (InvalidVersionSpecificationException e) {
  // throw new MuleRuntimeException(createStaticMessage("Could not parse plugin patch affect version: "
  // + affectedVersion), e);
  // }
  // })) {
  // try {
  // patches.add(new File(getMuleHomeFolder(),
  // Paths.get(MULE_ARTIFACT_PATCHES_LOCATION, patchFile).toString())
  // .toURL());
  // LOGGER.info("Patching artifact {} with patch file {}", artifactId, patchFile);
  // } catch (MalformedURLException e) {
  // throw new MuleRuntimeException(e);
  // }
  // }
  // }
  // }
  // }
  // } catch (Exception e) {
  // throw new MuleRuntimeException(createStaticMessage(format("There was an error processing the patches in %s file for artifact
  // %s",
  // MULE_ARTIFACT_PATCHES_LOCATION, artifactId)),
  // e);
  // }
  //
  // return patches;
  // }

  protected abstract ClassLoaderModelBuilder getClassLoaderConfigurationBuilder();

  protected boolean shouldPopulateLocalPackages() {
    return true;
  }

  protected List<BundleDependency> getProcessedBundleDependencies() {
    return getBundleDependencies();
  }

  /**
   * Loads the URLs of the class loader for this artifact.
   * <p>
   * It lets implementations add artifact specific URLs by letting them override
   * {@link #addArtifactSpecificClassLoaderConfiguration(ClassLoaderModelBuilder)}
   *
   * @param artifactFile                    the artifact file for which the {@link ClassLoaderModel class loader configuration} is
   *                                        being generated.
   * @param dependencies                    the dependencies resolved for this artifact.
   * @param classLoaderConfigurationBuilder the builder of the {@link ClassLoaderModel class loader configuration}
   */
  private List<URL> loadUrls(File artifactFile,
                             List<BundleDependency> dependencies,
                             ClassLoaderModelBuilder classLoaderConfigurationBuilder) {
    final List<URL> dependenciesArtifactsUrls = new ArrayList<>();

    dependenciesArtifactsUrls.add(getUrl(artifactFile));
    dependenciesArtifactsUrls.addAll(addArtifactSpecificClassLoaderConfiguration(classLoaderConfigurationBuilder));
    dependenciesArtifactsUrls.addAll(addDependenciesToClasspathUrls(dependencies));

    return dependenciesArtifactsUrls;
  }

  /**
   * Template method to add artifact specific configuration to the {@link DeployableClassLoaderConfigurationBuilder}.
   *
   * @param classLoaderConfigurationBuilder the builder used to generate {@link ClassLoaderModel class loader configuration} of
   *                                        the artifact.
   */
  protected Collection<URL> addArtifactSpecificClassLoaderConfiguration(ClassLoaderModelBuilder classLoaderConfigurationBuilder) {
    return emptyList();
  }

  private List<URL> addDependenciesToClasspathUrls(List<BundleDependency> dependencies) {
    final List<URL> dependenciesArtifactsUrls = new ArrayList<>();

    dependencies.stream()
        .filter(dependency -> !MULE_PLUGIN_CLASSIFIER.equals(dependency.getDescriptor().getClassifier().orElse(null)))
        .filter(dependency -> dependency.getBundleUri() != null)
        .filter(dependency -> !validateMuleRuntimeSharedLibrary(dependency.getDescriptor().getGroupId(),
                                                                dependency.getDescriptor().getArtifactId(),
                                                                packagerClassLoaderModel.getArtifactCoordinates()
                                                                    .getArtifactId()))
        .forEach(dependency -> {
          final URL dependencyArtifactUrl;
          try {
            dependencyArtifactUrl = dependency.getBundleUri().toURL();
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }
          dependenciesArtifactsUrls.add(dependencyArtifactUrl);
        });

    return dependenciesArtifactsUrls;
  }

  private URL getUrl(File artifactFile) {
    try {
      return artifactFile.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new ArtifactActivationException(createStaticMessage(format("There was an exception obtaining the URL for the artifact [%s]",
                                                                       artifactFile.getAbsolutePath())),
                                            e);
    }
  }

  private boolean validateMuleRuntimeSharedLibrary(String groupId, String artifactId, String artifactFileName) {
    if (MULE_RUNTIME_GROUP_ID.equals(groupId)
        || MULE_RUNTIME_MODULES_GROUP_ID.equals(groupId)) {
      LOGGER.warn("Shared library '{}:{}' is a Mule Runtime dependency."
          + " It will not be used by '{}' in order to avoid classloading issues."
          + " Please consider removing it, or at least not putting it as a sharedLibrary.",
                  groupId, artifactId, artifactFileName);
      return true;
    } else {
      return false;
    }
  }

  private void populateLocalPackages(org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel,
                                     ClassLoaderModelBuilder classLoaderConfigurationBuilder) {
    ImmutableSet.Builder<String> packagesSetBuilder = ImmutableSet.builder();
    if (packagerClassLoaderModel.getPackages() != null) {
      packagesSetBuilder.add(packagerClassLoaderModel.getPackages());
    }

    ImmutableSet.Builder<String> resourcesSetBuilder = ImmutableSet.builder();
    if (packagerClassLoaderModel.getResources() != null) {
      resourcesSetBuilder.add(packagerClassLoaderModel.getResources());
    }

    packagerClassLoaderModel.getDependencies().stream().forEach(artifact -> {
      if (artifact.getPackages() != null) {
        packagesSetBuilder.add(artifact.getPackages());
      }
      if (artifact.getResources() != null) {
        resourcesSetBuilder.add(artifact.getResources());
      }
    });

    classLoaderConfigurationBuilder.withLocalPackages(packagesSetBuilder.build());
    classLoaderConfigurationBuilder.withLocalResources(resourcesSetBuilder.build());
  }

  protected abstract List<BundleDependency> getBundleDependencies();

  protected abstract File getProjectFolder();
}
