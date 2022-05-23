/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.validateMuleRuntimeSharedLibrary;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;

import static java.lang.String.format;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Assembles the class loader configuration for an artifact given all its pieces.
 */
public abstract class AbstractArtifactClassLoaderConfigurationAssembler {

  private final org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel;

  public AbstractArtifactClassLoaderConfigurationAssembler(org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel) {
    this.packagerClassLoaderModel = packagerClassLoaderModel;
  }

  public ClassLoaderModel createClassLoaderModel() {
    ArtifactClassLoaderConfigurationBuilder artifactClassLoaderConfigurationBuilder =
        new ArtifactClassLoaderConfigurationBuilder(packagerClassLoaderModel, getProjectFolder());

    artifactClassLoaderConfigurationBuilder
        .exportingPackages(getExportedPackages())
        .exportingResources(getExportedResources());
    // TODO: add privileged packages and resources

    List<BundleDependency> bundleDependencies = getProcessedBundleDependencies();

    final List<URL> dependenciesArtifactsUrls =
        loadUrls(getProjectFolder(), bundleDependencies, artifactClassLoaderConfigurationBuilder);
    dependenciesArtifactsUrls.forEach(artifactClassLoaderConfigurationBuilder::containing);

    if (shouldPopulateLocalPackages()) {
      populateLocalPackages(packagerClassLoaderModel, artifactClassLoaderConfigurationBuilder);
    }

    artifactClassLoaderConfigurationBuilder.dependingOn(new HashSet<>(bundleDependencies));

    return artifactClassLoaderConfigurationBuilder.build();
  }

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
   * {@link #addArtifactSpecificClassLoaderConfiguration(ArtifactClassLoaderConfigurationBuilder)}
   *
   * @param artifactFile                            the artifact file for which the {@link ClassLoaderModel class loader
   *                                                configuration} is being generated.
   * @param dependencies                            the dependencies resolved for this artifact.
   * @param artifactClassLoaderConfigurationBuilder the builder of the {@link ClassLoaderModel class loader configuration}
   */
  private List<URL> loadUrls(File artifactFile,
                             List<BundleDependency> dependencies,
                             ArtifactClassLoaderConfigurationBuilder artifactClassLoaderConfigurationBuilder) {
    final List<URL> dependenciesArtifactsUrls = new ArrayList<>();

    // TODO: consider artifact patches for the case this is run within a Runtime

    final URL artifactFileUrl = getUrl(artifactFile, artifactFile);
    dependenciesArtifactsUrls.add(artifactFileUrl);

    dependenciesArtifactsUrls.addAll(addArtifactSpecificClassLoaderConfiguration(artifactClassLoaderConfigurationBuilder));
    dependenciesArtifactsUrls.addAll(addDependenciesToClasspathUrls(artifactFile, dependencies));

    return dependenciesArtifactsUrls;
  }

  /**
   * Template method to add artifact specific configuration to the {@link ArtifactClassLoaderConfigurationBuilder}.
   *
   * @param artifactClassLoaderConfigurationBuilder the builder used to generate {@link ClassLoaderModel class loader
   *                                                configuration} of the artifact.
   */
  protected Collection<URL> addArtifactSpecificClassLoaderConfiguration(ArtifactClassLoaderConfigurationBuilder artifactClassLoaderConfigurationBuilder) {
    return emptyList();
  }

  private static List<URL> addDependenciesToClasspathUrls(File artifactFile,
                                                          List<BundleDependency> dependencies) {
    final List<URL> dependenciesArtifactsUrls = new ArrayList<>();

    dependencies.stream()
        .filter(dependency -> !MULE_PLUGIN_CLASSIFIER.equals(dependency.getDescriptor().getClassifier().orElse(null)))
        .filter(dependency -> dependency.getBundleUri() != null)
        .filter(dependency -> !validateMuleRuntimeSharedLibrary(dependency.getDescriptor().getGroupId(),
                                                                dependency.getDescriptor().getArtifactId(),
                                                                artifactFile.getName()))
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

  private URL getUrl(File artifactFile, File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new ArtifactActivationException(createStaticMessage(format("There was an exception obtaining the URL for the artifact [%s], file [%s]",
                                                                       artifactFile.getAbsolutePath(), file.getAbsolutePath())),
                                            e);
    }
  }

  private void populateLocalPackages(org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel,
                                     ArtifactClassLoaderConfigurationBuilder artifactClassLoaderConfigurationBuilder) {
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

    artifactClassLoaderConfigurationBuilder.withLocalPackages(packagesSetBuilder.build());
    artifactClassLoaderConfigurationBuilder.withLocalResources(resourcesSetBuilder.build());
  }

  protected abstract List<BundleDependency> getBundleDependencies();

  protected abstract Set<String> getExportedPackages();

  protected abstract Set<String> getExportedResources();

  protected abstract File getProjectFolder();
}
