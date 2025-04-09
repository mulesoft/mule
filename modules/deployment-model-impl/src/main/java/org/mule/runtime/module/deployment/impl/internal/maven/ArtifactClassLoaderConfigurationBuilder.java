/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static org.mule.maven.pom.parser.api.MavenPomParserProvider.discoverProvider;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.getMavenConfig;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.maven.pom.parser.api.MavenPomParser;
import org.mule.maven.pom.parser.api.model.AdditionalPluginDependencies;
import org.mule.maven.pom.parser.api.model.ArtifactCoordinates;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

/**
 * ClassLoaderConfigurationBuilder that adds the concept of Shared Library for the configured dependencies.
 *
 * @since 4.2.0
 */
public abstract class ArtifactClassLoaderConfigurationBuilder extends ClassLoaderConfigurationBuilder {

  private static final Logger LOGGER = getLogger(ArtifactClassLoaderConfigurationBuilder.class);

  private static final String GROUP_ID = "groupId";
  private static final String ARTIFACT_ID = "artifactId";
  private static final String VERSION = "version";
  private static final String CLASSIFIER = "classifier";
  private static final String TYPE = "type";
  protected static final String MULE_PLUGIN = MULE_PLUGIN_CLASSIFIER;

  private boolean processSharedLibraries = false;
  private boolean processAdditionalPluginLibraries = false;
  protected FileJarExplorer fileJarExplorer = new FileJarExplorer();

  protected File artifactFolder;
  protected ArtifactDescriptor deployableArtifactDescriptor;
  protected BundleDescriptor artifactBundleDescriptor;

  public ArtifactClassLoaderConfigurationBuilder(File artifactFolder, BundleDescriptor artifactBundleDescriptor) {
    requireNonNull(artifactFolder, "artifactFolder cannot be null");
    requireNonNull(artifactBundleDescriptor, "artifactBundleDescriptor cannot be null");
    this.artifactBundleDescriptor = artifactBundleDescriptor;
    this.artifactFolder = artifactFolder;
  }

  /**
   * Sets a flag to export the configured shared libraries when building the ClassLoaderConfiguration
   *
   * @return this builder
   * @since 4.2.0
   */
  public ClassLoaderConfigurationBuilder exportingSharedLibraries() {
    this.processSharedLibraries = true;
    return this;
  }

  /**
   * Sets a flag to include additional dependencies for each plugin if the deployable artifact defines them.
   *
   * @since 4.2.0
   * @return
   */
  public ClassLoaderConfigurationBuilder additionalPluginLibraries() {
    this.processAdditionalPluginLibraries = true;
    return this;
  }

  public void setDeployableArtifactDescriptor(ArtifactDescriptor deployableArtifactDescriptor) {
    this.deployableArtifactDescriptor = deployableArtifactDescriptor;
  }

  @Override
  public ClassLoaderConfiguration build() {
    Optional<MavenPomParser> mavenParser = empty();
    if (processSharedLibraries || processAdditionalPluginLibraries) {
      mavenParser = of(discoverProvider().createMavenPomParserClient(artifactFolder.toPath(), getActiveProfiles()));
    }
    if (processSharedLibraries) {
      mavenParser.ifPresent(this::exportSharedLibrariesResourcesAndPackages);
    }
    if (processAdditionalPluginLibraries) {
      mavenParser.ifPresent(this::processAdditionalPluginLibraries);
    }

    return super.build();
  }

  // For testability pourposes
  protected List<String> getActiveProfiles() {
    return getMavenConfig().getActiveProfiles().orElse(emptyList());
  }

  private void exportSharedLibrariesResourcesAndPackages(MavenPomParser parser) {
    doExportSharedLibrariesResourcesAndPackages(parser);
  }

  private void processAdditionalPluginLibraries(MavenPomParser parser) {
    List<AdditionalPluginDependencies> pomAdditionalPluginDependenciesForArtifacts =
        new LinkedList<>(doProcessAdditionalPluginLibraries(parser).values());
    pomAdditionalPluginDependenciesForArtifacts.forEach(additionalDependenciesForArtifact -> {
      List<org.mule.maven.pom.parser.api.model.BundleDescriptor> pluginAdditionalLibraries =
          additionalDependenciesForArtifact.getAdditionalDependencies();
      findBundleDependency(additionalDependenciesForArtifact.getGroupId(), additionalDependenciesForArtifact.getArtifactId(),
                           of(MULE_PLUGIN))
          .ifPresent(pluginArtifactBundleDependency -> {
            replaceBundleDependency(pluginArtifactBundleDependency,
                                    new BundleDependency.Builder(pluginArtifactBundleDependency)
                                        .setAdditionalDependencies(pluginAdditionalLibraries.stream()
                                            .map(additionalDependency -> new BundleDependency.Builder()
                                                .setDescriptor(convertBundleDescriptor(additionalDependency))
                                                .build())
                                            .collect(toList()))
                                        .build());
          });
    });
  }

  protected Map<ArtifactCoordinates, AdditionalPluginDependencies> doProcessAdditionalPluginLibraries(MavenPomParser parser) {
    return parser.getPomAdditionalPluginDependenciesForArtifacts();
  }

  protected void replaceBundleDependency(BundleDependency original, BundleDependency modified) {
    this.dependencies.remove(original);
    this.dependencies.add(modified);
  }

  /**
   * Template method for exporting shared libraries and packages. By default, the pom needs to be parsed again to find which
   * dependencies need to be shared.
   */
  protected void doExportSharedLibrariesResourcesAndPackages(MavenPomParser parser) {
    parser.getSharedLibraries().stream().forEach(shareLibrary -> {
      if (!validateMuleRuntimeSharedLibrary(shareLibrary.getGroupId(), shareLibrary.getArtifactId())) {
        findAndExportSharedLibrary(shareLibrary.getGroupId(), shareLibrary.getArtifactId());
      }
    });
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

  protected void findAndExportSharedLibrary(String groupId, String artifactId) {
    Optional<BundleDependency> bundleDependencyOptional = findBundleDependency(groupId, artifactId, empty());
    BundleDependency bundleDependency =
        bundleDependencyOptional.orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format(
                                                                                                       "Dependency %s:%s could not be found within the artifact %s. It must be declared within the maven dependencies of the artifact.",
                                                                                                       groupId,
                                                                                                       artifactId, artifactFolder
                                                                                                           .getName()))));
    JarInfo jarInfo = fileJarExplorer.explore(bundleDependency.getBundleUri());
    this.exportingPackages(jarInfo.getPackages());
    this.exportingResources(jarInfo.getResources());
  }

  protected Optional<BundleDependency> findBundleDependency(String groupId, String artifactId,
                                                            Optional<String> classifierOptional) {
    return dependencies.stream()
        .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals(artifactId)
            && bundleDependency.getDescriptor().getGroupId().equals(groupId)
            && classifierOptional
                .map(classifier -> classifier.equals(bundleDependency.getDescriptor().getClassifier().orElse(null))).orElse(true))
        .findFirst();
  }

  public List<URL> includeAdditionalPluginDependencies() {
    final List<URL> dependenciesArtifactsUrls = new ArrayList<>();

    if (deployableArtifactDescriptor != null) {
      deployableArtifactDescriptor.getClassLoaderConfiguration().getDependencies().stream()
          .filter(bundleDescriptor -> bundleDescriptor.getDescriptor().isPlugin())
          .filter(bundleDescriptor -> bundleDescriptor.getDescriptor().getGroupId()
              .equals(this.artifactBundleDescriptor.getGroupId())
              && bundleDescriptor.getDescriptor().getArtifactId().equals(this.artifactBundleDescriptor.getArtifactId()))
          .filter(bundleDependency -> bundleDependency.getAdditionalDependenciesList() != null
              && !bundleDependency.getAdditionalDependenciesList().isEmpty())
          .forEach(bundleDependency -> processPluginAdditionalDependenciesURIs(bundleDependency)
              .forEach(uri -> {
                final URL dependencyArtifactUrl;
                try {
                  dependencyArtifactUrl = uri.toURL();
                } catch (MalformedURLException e) {
                  throw new ArtifactDescriptorCreateException(format("There was an exception obtaining the URL for the artifact [%s], file [%s]",
                                                                     artifactFolder.getAbsolutePath(),
                                                                     uri),
                                                              e);
                }
                containing(dependencyArtifactUrl);
                dependenciesArtifactsUrls.add(dependencyArtifactUrl);
              }));
    }

    return dependenciesArtifactsUrls;
  }

  protected abstract List<URI> processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency);

  private org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor convertBundleDescriptor(org.mule.maven.pom.parser.api.model.BundleDescriptor descriptor) {
    org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.Builder builder =
        new org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.Builder().setGroupId(descriptor.getGroupId())
            .setArtifactId(descriptor.getArtifactId())
            .setVersion(descriptor.getVersion());

    if (descriptor.getBaseVersion() != null) {
      builder.setBaseVersion(descriptor.getBaseVersion());
    }
    if (descriptor.getType() != null) {
      builder.setType(descriptor.getType());
    }
    descriptor.getClassifier().ifPresent(builder::setClassifier);
    return builder.build();
  }

}
