/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.PRIVILEGED_ARTIFACTS_IDS;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.PRIVILEGED_EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.MULE_DOMAIN_CLASSIFIER;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import static com.google.common.collect.Sets.newHashSet;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.internal.deployable.DeployableClassLoaderConfigurationBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;

/**
 * Assembles the class loader configuration for an artifact given all its pieces.
 */
public abstract class AbstractArtifactClassLoaderConfigurationAssembler {

  private static final Logger LOGGER = getLogger(AbstractArtifactClassLoaderConfigurationAssembler.class);

  private static final String MULE_RUNTIME_GROUP_ID = "org.mule.runtime";
  private static final String MULE_RUNTIME_MODULES_GROUP_ID = "com.mulesoft.mule.runtime.modules";

  private final org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel;
  private final MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor;

  public AbstractArtifactClassLoaderConfigurationAssembler(org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel,
                                                           MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor) {
    this.packagerClassLoaderModel = packagerClassLoaderModel;
    this.muleArtifactLoaderDescriptor = muleArtifactLoaderDescriptor;
  }

  public ClassLoaderModel createClassLoaderModel() {
    ClassLoaderModelBuilder classLoaderConfigurationBuilder = getClassLoaderConfigurationBuilder();

    if (muleArtifactLoaderDescriptor != null) {
      classLoaderConfigurationBuilder
          .exportingPackages(newHashSet(getAttribute(muleArtifactLoaderDescriptor.getAttributes(), EXPORTED_PACKAGES)))
          .exportingResources(newHashSet(getAttribute(muleArtifactLoaderDescriptor.getAttributes(), EXPORTED_RESOURCES)))
          .exportingPrivilegedPackages(new HashSet<>(getAttribute(muleArtifactLoaderDescriptor.getAttributes(),
                                                                  PRIVILEGED_EXPORTED_PACKAGES)),
                                       new HashSet<>(getAttribute(muleArtifactLoaderDescriptor.getAttributes(),
                                                                  PRIVILEGED_ARTIFACTS_IDS)));
    }

    List<BundleDependency> bundleDependencies = getBundleDependencies();

    final List<URL> dependenciesArtifactsUrls =
        loadUrls(getProjectFolder(), bundleDependencies, classLoaderConfigurationBuilder);
    dependenciesArtifactsUrls.forEach(classLoaderConfigurationBuilder::containing);

    if (shouldPopulateLocalPackages()) {
      populateLocalPackages(packagerClassLoaderModel, classLoaderConfigurationBuilder);
    }

    classLoaderConfigurationBuilder.dependingOn(new HashSet<>(bundleDependencies));

    return classLoaderConfigurationBuilder.build();
  }

  private List<String> getAttribute(Map<String, Object> attributes, String attribute) {
    if (attributes == null) {
      return emptyList();
    }

    final Object attributeObject = attributes.getOrDefault(attribute, new ArrayList<String>());
    checkArgument(attributeObject instanceof List, format("The '%s' attribute must be of '%s', found '%s'", attribute,
                                                          List.class.getName(), attributeObject.getClass().getName()));
    return (List<String>) attributeObject;
  }

  protected abstract ClassLoaderModelBuilder getClassLoaderConfigurationBuilder();

  protected boolean shouldPopulateLocalPackages() {
    return true;
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

  protected void populateLocalPackages(org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel,
                                       ClassLoaderModelBuilder classLoaderConfigurationBuilder) {
    ImmutableSet.Builder<String> packagesSetBuilder = ImmutableSet.builder();
    if (packagerClassLoaderModel.getPackages() != null) {
      packagesSetBuilder.add(packagerClassLoaderModel.getPackages());
    }

    ImmutableSet.Builder<String> resourcesSetBuilder = ImmutableSet.builder();
    if (packagerClassLoaderModel.getResources() != null) {
      resourcesSetBuilder.add(packagerClassLoaderModel.getResources());
    }

    packagerClassLoaderModel.getDependencies().forEach(artifact -> {
      if (!MULE_PLUGIN_CLASSIFIER.equals(artifact.getArtifactCoordinates().getClassifier())
          && !MULE_DOMAIN_CLASSIFIER.equals(artifact.getArtifactCoordinates().getClassifier())
          && !validateMuleRuntimeSharedLibrary(artifact.getArtifactCoordinates().getGroupId(),
                                               artifact.getArtifactCoordinates().getArtifactId(),
                                               packagerClassLoaderModel.getArtifactCoordinates()
                                                   .getArtifactId())
          && artifact.getUri() != null) {
        if (artifact.getPackages() != null) {
          packagesSetBuilder.add(artifact.getPackages());
        }
        if (artifact.getResources() != null) {
          resourcesSetBuilder.add(artifact.getResources());
        }
      }
    });

    classLoaderConfigurationBuilder.withLocalPackages(packagesSetBuilder.build());
    classLoaderConfigurationBuilder.withLocalResources(resourcesSetBuilder.build());
  }

  protected org.mule.tools.api.classloader.model.ClassLoaderModel getPackagerClassLoaderModel() {
    return packagerClassLoaderModel;
  }

  protected abstract List<BundleDependency> getBundleDependencies();

  protected abstract File getProjectFolder();
}
