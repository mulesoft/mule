/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.maven.pom.parser.api.MavenPomParserProvider.discoverProvider;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.getMavenConfig;
import static org.mule.runtime.module.artifact.activation.api.deployable.ArtifactModelResolver.applicationModelResolver;
import static org.mule.runtime.module.artifact.activation.api.deployable.ArtifactModelResolver.domainModelResolver;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_APPLICATION_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.META_INF;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_FOLDER;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.MULE_DOMAIN_CLASSIFIER;
import static org.mule.tools.api.classloader.AppClassLoaderModelJsonSerializer.deserialize;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import static com.google.common.collect.Sets.newHashSet;
import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;
import org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer;
import org.mule.tools.api.classloader.model.AppClassLoaderModel;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.classloader.model.ClassLoaderModel;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vdurmont.semver4j.Semver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link DeployableProjectModelBuilder} that builds a model based on the files provided within a packaged Mule
 * deployable artifact project.
 *
 * @since 4.5
 */
public class MuleDeployableProjectModelBuilder extends AbstractDeployableProjectModelBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(MuleDeployableProjectModelBuilder.class);

  private static final String GROUP_ID = "groupId";
  private static final String ARTIFACT_ID = "artifactId";

  private final File projectFolder;
  private final Optional<MuleDeployableModel> model;

  public MuleDeployableProjectModelBuilder(File projectFolder) {
    this(projectFolder, empty());
  }

  public MuleDeployableProjectModelBuilder(File projectFolder, Optional<MuleDeployableModel> model) {
    this.projectFolder = projectFolder;
    this.model = model;
  }

  @Override
  public DeployableProjectModel build() {
    AppClassLoaderModel packagerClassLoaderModel = getAppPackagerClassLoaderModel(getClassLoaderModelDescriptor(projectFolder));

    Map<ArtifactCoordinates, BundleDescriptor> bundleDescriptors = packagerClassLoaderModel.getDependencies().stream()
        .collect(toMap(Artifact::getArtifactCoordinates, artifact -> buildBundleDescriptor(artifact.getArtifactCoordinates()),
                       (bundleDescriptor1, bundleDescriptor2) -> bundleDescriptor1));

    Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies =
        getAdditionalPluginDependencies(packagerClassLoaderModel, bundleDescriptors);

    List<BundleDependency> dependencies = getDependencies(packagerClassLoaderModel);

    Set<BundleDescriptor> sharedLibraries = getSharedLibraries(packagerClassLoaderModel, bundleDescriptors);

    List<String> packages =
        packagerClassLoaderModel.getPackages() != null ? asList(packagerClassLoaderModel.getPackages()) : emptyList();
    List<String> resources =
        packagerClassLoaderModel.getResources() != null ? asList(packagerClassLoaderModel.getResources()) : emptyList();

    return new DeployableProjectModel(packages,
                                      resources,
                                      emptyList(),
                                      buildBundleDescriptor(packagerClassLoaderModel.getArtifactCoordinates()),
                                      getModelResolver(packagerClassLoaderModel.getArtifactCoordinates()),
                                      projectFolder, dependencies, sharedLibraries,
                                      additionalPluginDependencies);
  }

  private Map<BundleDescriptor, List<BundleDependency>> getAdditionalPluginDependencies(AppClassLoaderModel packagerClassLoaderModel,
                                                                                        Map<ArtifactCoordinates, BundleDescriptor> bundleDescriptors) {
    return packagerClassLoaderModel
        .getAdditionalPluginDependencies().map(apds -> apds.stream().collect(toMap(plugin -> packagerClassLoaderModel
            .getDependencies().stream()
            .filter(pluginDependency -> StringUtils.equals(pluginDependency.getArtifactCoordinates().getGroupId(),
                                                           plugin.getGroupId())
                && StringUtils.equals(pluginDependency.getArtifactCoordinates().getArtifactId(), plugin.getArtifactId()))
            .map(artifact -> bundleDescriptors.get(artifact.getArtifactCoordinates()))
            .findFirst()
            .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format("Couldn't find plugin '%s' among dependencies",
                                                                                   plugin)))),
                                                                                   plugin -> plugin.getAdditionalDependencies()
                                                                                       .stream()
                                                                                       .map(artifact -> createAdditionalPluginDependencyFromPackagerDependency(artifact,
                                                                                                                                                               packagerClassLoaderModel))
                                                                                       .collect(toList()))))
        .orElse(emptyMap());
  }

  private BundleDependency createAdditionalPluginDependencyFromPackagerDependency(Artifact additionalPluginArtifact,
                                                                                  AppClassLoaderModel packagerClassLoaderModel) {
    BundleDependency additionalPluginDependency =
        createBundleDependencyFromPackagerDependency(getDeployableArtifactRepositoryUriResolver())
            .apply(additionalPluginArtifact);

    if (new Semver(packagerClassLoaderModel.getVersion(), LOOSE).isLowerThan(CLASS_LOADER_MODEL_VERSION_120)) {
      additionalPluginDependency = discoverPackagesAndResources(additionalPluginDependency);
    }

    return additionalPluginDependency;
  }

  private List<BundleDependency> getDependencies(AppClassLoaderModel packagerClassLoaderModel) {
    List<BundleDependency> patchBundleDependencies = getPatchedBundledDependencies(projectFolder);

    List<BundleDependency> dependencies = packagerClassLoaderModel.getDependencies().stream().map(artifact -> {
      Optional<BundleDependency> patchedBundledDependency =
          patchBundleDependencies.stream().filter(bundleDependency -> bundleDependency.getDescriptor().getGroupId()
              .equals(artifact.getArtifactCoordinates().getGroupId()) &&
              bundleDependency.getDescriptor().getArtifactId().equals(artifact.getArtifactCoordinates().getArtifactId()))
              .findAny();
      return patchedBundledDependency
          .orElse(createBundleDependencyFromPackagerDependency(getDeployableArtifactRepositoryUriResolver()).apply(artifact));
    }).collect(toList());

    dependencies = dependencies.stream().map(d -> new BundleDependency.Builder(d)
        .setTransitiveDependencies(getTransitiveDependencies(d))
        .build()).collect(toList());

    if (new Semver(packagerClassLoaderModel.getVersion(), LOOSE).isLowerThan(CLASS_LOADER_MODEL_VERSION_120)) {
      dependencies = discoverPackagesAndResources(dependencies);
    }

    return dependencies;
  }

  private List<BundleDependency> getPatchedBundledDependencies(File artifactFile) {
    List<BundleDependency> patchBundleDependencies = new ArrayList<>();
    File classLoaderModelPatchDescriptor = getClassLoaderModelPatchDescriptor(artifactFile);
    if (classLoaderModelPatchDescriptor.exists()) {
      ClassLoaderModel packagerClassLoaderModelPatch = getPackagerClassLoaderModel(classLoaderModelPatchDescriptor);
      patchBundleDependencies.addAll(packagerClassLoaderModelPatch.getDependencies().stream()
          .map(artifact -> createBundleDependencyFromPackagerDependency(getDeployableArtifactRepositoryUriResolver())
              .apply(artifact))
          .collect(toList()));
    }

    return patchBundleDependencies;
  }

  private List<BundleDependency> getTransitiveDependencies(BundleDependency bundleDependency) {
    if (bundleDependency.getDescriptor().isPlugin() && bundleDependency.getBundleUri() != null) {
      ClassLoaderModel packagerClassLoaderModel =
          getPackagerClassLoaderModel(getPluginClassLoaderModelDescriptor(bundleDependency));

      return packagerClassLoaderModel.getDependencies().stream()
          .map(artifact -> createBundleDependencyFromPackagerDependency(getDeployableArtifactRepositoryUriResolver())
              .apply(artifact))
          .collect(toList());
    }

    return emptyList();
  }

  private BundleDependency discoverPackagesAndResources(BundleDependency dependency) {
    JarExplorer jarExplorer = new FileJarExplorer();

    if (MULE_PLUGIN_CLASSIFIER.equals(dependency.getDescriptor().getClassifier().orElse(null))) {
      return discoverPluginPackagesAndResources(dependency, jarExplorer);
    }

    return addLocalPackagesAndResourcesToDependency(dependency, jarExplorer);
  }

  private List<BundleDependency> discoverPackagesAndResources(List<BundleDependency> dependencies) {
    return dependencies.stream().map(this::discoverPackagesAndResources).collect(toList());
  }

  private BundleDependency discoverPluginPackagesAndResources(BundleDependency dependency, JarExplorer jarExplorer) {
    if (!MULE_PLUGIN_CLASSIFIER.equals(dependency.getDescriptor().getClassifier().orElse(null))) {
      throw new IllegalArgumentException("Expected dependency to be a Mule plugin");
    }

    List<BundleDependency> updatedTransitiveDependenciesList = dependency.getTransitiveDependenciesList()
        .stream().map(transitiveDependency -> addLocalPackagesAndResourcesToDependency(transitiveDependency, jarExplorer))
        .collect(toList());

    JarInfo exploredJar = jarExplorer.explore(dependency.getBundleUri());

    return new BundleDependency.Builder(dependency).setTransitiveDependencies(updatedTransitiveDependenciesList)
        .setPackages(exploredJar.getPackages()).setResources(exploredJar.getResources()).build();
  }

  private BundleDependency addLocalPackagesAndResourcesToDependency(BundleDependency dependency, JarExplorer jarExplorer) {
    if (MULE_PLUGIN_CLASSIFIER.equals(dependency.getDescriptor().getClassifier().orElse(null))
        || MULE_DOMAIN_CLASSIFIER.equals(dependency.getDescriptor().getClassifier().orElse(null))
        || validateMuleRuntimeSharedLibrary(dependency.getDescriptor().getGroupId(),
                                            dependency.getDescriptor().getArtifactId())
        || dependency.getBundleUri() == null) {
      return dependency;
    }

    try {
      final JarInfo exploredJar = jarExplorer.explore(dependency.getBundleUri());
      return new BundleDependency.Builder(dependency).setPackages(exploredJar.getPackages())
          .setResources(exploredJar.getResources()).build();
    } catch (IllegalArgumentException e) {
      // Workaround for MMP-499
      LOGGER.warn("File for dependency artifact not found: '{}'. Skipped localPackages scanning for that artifact.",
                  dependency.getBundleUri());
    }

    return dependency;
  }

  private Set<BundleDescriptor> getSharedLibraries(AppClassLoaderModel packagerClassLoaderModel,
                                                   Map<ArtifactCoordinates, BundleDescriptor> bundleDescriptors) {
    if (new Semver(packagerClassLoaderModel.getVersion(), LOOSE).isLowerThan(CLASS_LOADER_MODEL_VERSION_110)) {
      return discoverProvider().createMavenPomParserClient(projectFolder.toPath(), getActiveProfiles())
          .getSharedLibraries().stream().map(shareLibrary -> {
            if (!validateMuleRuntimeSharedLibrary(shareLibrary.getGroupId(), shareLibrary.getArtifactId())) {
              return bundleDescriptors.values().stream()
                  .filter(bundleDescriptor -> bundleDescriptor.getGroupId().equals(shareLibrary.getGroupId())
                      && bundleDescriptor.getArtifactId().equals(shareLibrary.getArtifactId()))
                  .findAny()
                  .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Shared library '%s:%s' is not among the available dependencies",
                                                                                  shareLibrary.getGroupId(),
                                                                                  shareLibrary.getArtifactId())));
            }
            return null;
          }).filter(Objects::nonNull).collect(toSet());
    } else {
      return packagerClassLoaderModel.getDependencies().stream().filter(Artifact::isShared)
          .map(artifact -> bundleDescriptors.get(artifact.getArtifactCoordinates())).collect(toSet());
    }
  }

  protected final boolean validateMuleRuntimeSharedLibrary(String groupId, String artifactId) {
    if ("org.mule.runtime".equals(groupId)
        || "com.mulesoft.mule.runtime.modules".equals(groupId)) {
      LOGGER
          .warn("Shared library '{}:{}' is a Mule Runtime dependency. It will not be shared by the app in order to avoid classloading issues. Please consider removing it, or at least not putting it as a sharedLibrary.",
                groupId, artifactId);
      return true;
    } else {
      return false;
    }
  }

  protected List<String> getActiveProfiles() {
    return getMavenConfig().getActiveProfiles().orElse(emptyList());
  }

  private File getClassLoaderModelPatchDescriptor(File artifactFile) {
    return new File(artifactFile, CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR_LOCATION);
  }

  private Function<URI, URI> getDeployableArtifactRepositoryUriResolver() {
    return uri -> new File(projectFolder, uri.toString()).toURI();
  }

  private Function<Artifact, BundleDependency> createBundleDependencyFromPackagerDependency(Function<URI, URI> uriResolver) {
    return d -> {
      URI bundle = d.getUri();
      if (!d.getUri().isAbsolute()) {
        bundle = uriResolver.apply(d.getUri());
      }

      return new BundleDependency.Builder()
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

  private Supplier<MuleDeployableModel> getModelResolver(ArtifactCoordinates deployableArtifactCoordinates) {
    return () -> model.orElseGet(() -> {
      final File muleArtifactFolder = new File(projectFolder, MULE_ARTIFACT_FOLDER);
      if (deployableArtifactCoordinates.getClassifier().equals(MULE_APPLICATION_CLASSIFIER)) {
        return applicationModelResolver().resolve(muleArtifactFolder);
      } else if (deployableArtifactCoordinates.getClassifier().equals(MULE_DOMAIN_CLASSIFIER)) {
        return domainModelResolver().resolve(muleArtifactFolder);
      } else {
        throw new IllegalStateException("project is not a " + MULE_APPLICATION_CLASSIFIER + " or " + MULE_DOMAIN_CLASSIFIER);
      }
    });
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

  private File getPluginClassLoaderModelDescriptor(BundleDependency bundleDependency) {
    File artifactFile = new File(bundleDependency.getBundleUri());
    if (isHeavyPackage(artifactFile)) {
      return getClassLoaderModelDescriptor(artifactFile);
    } else {
      char slashChar = '/';
      String path = META_INF + slashChar +
          MULE_ARTIFACT + slashChar +
          bundleDependency.getDescriptor().getGroupId().replace('.', slashChar) + slashChar +
          bundleDependency.getDescriptor().getArtifactId() + slashChar +
          bundleDependency.getDescriptor().getBaseVersion() + slashChar +
          CLASSLOADER_MODEL_JSON_DESCRIPTOR;

      return new File(projectFolder.getAbsolutePath(), path);
    }
  }

  private AppClassLoaderModel getAppPackagerClassLoaderModel(File classLoaderModelDescriptor) {
    AppClassLoaderModel appPackagerClassLoaderModel = deserialize(classLoaderModelDescriptor);

    if (appPackagerClassLoaderModel.getArtifactCoordinates().getClassifier() == null) {
      throw new IllegalArgumentException(format("Artifact coordinates from project's class loader model are missing the 'classifier' attribute. Valid values are '%s' and '%s'.",
                                                MULE_APPLICATION_CLASSIFIER, MULE_DOMAIN_CLASSIFIER));
    }

    return appPackagerClassLoaderModel;
  }

  private ClassLoaderModel getPackagerClassLoaderModel(File classLoaderModelDescriptor) {
    return ClassLoaderModelJsonSerializer.deserialize(classLoaderModelDescriptor);
  }
}
